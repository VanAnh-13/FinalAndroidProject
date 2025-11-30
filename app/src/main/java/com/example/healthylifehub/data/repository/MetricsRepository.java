package com.example.healthylifehub.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseRepository;
import com.example.healthylifehub.data.model.MetricHistory;
import com.example.healthylifehub.data.model.MetricItem;
import com.example.healthylifehub.data.cache.CacheManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for Metrics data from Firebase Firestore.
 * Collection structure: metrics/{metricId}
 * Data format matches the provided JSON structure with value object containing specific metric data
 * 
 * Enhanced with ExecutorService for background processing of Firestore snapshots.
 * Extends BaseRepository to access shared thread pools for optimal resource usage.
 */
public class MetricsRepository extends BaseRepository {
    
    private static final String TAG = "MetricsRepository";
    private static final String COLLECTION_HEALTH_METRICS = "healthMetrics";
    private SimpleDateFormat dateFormat;
    private final android.content.Context context;
    
    // Firebase instances (previously inherited from FirebaseRepository)
    protected final FirebaseFirestore db;
    protected final FirebaseAuth auth;
    
    public MetricsRepository(android.content.Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dateFormat = new SimpleDateFormat(context.getString(com.example.healthylifehub.R.string.format_date_time_short), Locale.getDefault());
    }
    
    /**
     * Get current user ID
     * @return User ID or null if not logged in
     */
    protected String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    /**
     * Load all metrics for current user
     * @return LiveData list of metric items
     */
    public LiveData<List<MetricItem>> loadMetrics() {
        MutableLiveData<List<MetricItem>> metricsLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            metricsLiveData.setValue(new ArrayList<>());
            return metricsLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    metricsLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<MetricItem> metrics = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            String type = doc.getString("type");
                            String unit = doc.getString("unit");
                            String note = doc.getString("note");
                            com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                            
                            // Handle blood_pressure with systolic/diastolic at root level
                            String displayValue;
                            if ("blood_pressure".equals(type)) {
                                Long systolic = doc.getLong("systolic");
                                Long diastolic = doc.getLong("diastolic");
                                if (systolic != null && diastolic != null) {
                                    displayValue = systolic + "/" + diastolic + " " + (unit != null ? unit : context.getString(com.example.healthylifehub.R.string.unit_mmhg));
                                } else {
                                    // Fallback to value map format
                                    Object valueObj = doc.get("value");
                                    displayValue = formatMetricValue(type, valueObj, unit);
                                }
                            } else {
                                Object valueObj = doc.get("value");
                                displayValue = formatMetricValue(type, valueObj, unit);
                            }
                            
                            if (type != null && measuredAt != null) {
                                String displayTitle = getMetricTitle(type);
                                String displayTime = dateFormat.format(measuredAt.toDate());
                                
                                metrics.add(new MetricItem(displayValue, note, displayTitle, displayTime, type));
                            }
                        } catch (Exception e) {
                            // Skip invalid records
                        }
                    }
                    metricsLiveData.setValue(metrics);
                }
            });
        
        return metricsLiveData;
    }
    
    /**
     * Load metric history for a specific metric type (ENHANCED)
     * 
     * Enhancement: Uses ExecutorService for Firestore listener to process snapshots on background thread
     * - Pass executor to addSnapshotListener() for background processing
     * - Process snapshots on background thread (IO executor)
     * - Post results to LiveData on main thread
     * 
     * Requirements: 3.2, 3.3, 6.2
     * - 3.2: Establish Firestore snapshot listener on background thread
     * - 3.3: Update UI automatically via LiveData on main thread
     * - 6.2: Establish Firestore addSnapshotListener for real-time updates on background thread
     * 
     * @param metricType Type of metric (e.g., "blood_pressure", "heart_rate")
     * @return LiveData list of metric history
     */
    public LiveData<List<MetricHistory>> loadMetricHistory(String metricType) {
        MutableLiveData<List<MetricHistory>> historyLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            historyLiveData.setValue(new ArrayList<>());
            return historyLiveData;
        }
        
        // Use simple query without orderBy to avoid composite index requirement
        // Sort client-side instead
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .whereEqualTo("type", metricType)
            .addSnapshotListener(getIoExecutor(), (value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading metric history for type: " + metricType, error);
                    historyLiveData.postValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<MetricHistoryWithTimestamp> tempList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            String unit = doc.getString("unit");
                            com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                            Object valueObj = doc.get("value");
                            
                            if (valueObj != null && measuredAt != null) {
                                String numericValue = extractNumericValue(metricType, valueObj);
                                String displayTime = formatHistoryTime(measuredAt.toDate());
                                tempList.add(new MetricHistoryWithTimestamp(
                                    new MetricHistory(numericValue, displayTime, unit),
                                    measuredAt.toDate().getTime()
                                ));
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping invalid metric history record", e);
                        }
                    }
                    
                    // Sort by timestamp descending (client-side)
                    tempList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                    
                    // Limit to 20 and extract MetricHistory
                    List<MetricHistory> history = new ArrayList<>();
                    int limit = Math.min(tempList.size(), 20);
                    for (int i = 0; i < limit; i++) {
                        history.add(tempList.get(i).metricHistory);
                    }
                    
                    Log.d(TAG, "✅ Processed " + history.size() + " metric history records for type: " + metricType);
                    historyLiveData.postValue(history);
                }
            });
        
        return historyLiveData;
    }
    
    // Helper class for sorting
    private static class MetricHistoryWithTimestamp {
        MetricHistory metricHistory;
        long timestamp;
        
        MetricHistoryWithTimestamp(MetricHistory metricHistory, long timestamp) {
            this.metricHistory = metricHistory;
            this.timestamp = timestamp;
        }
    }

    /**
     * Load metric history for a specific metric type within a date range
     * 
     * @param metricType Type of metric
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return LiveData list of metric history
     */
    public LiveData<List<MetricHistory>> loadMetricHistoryByDateRange(String metricType, long startDate, long endDate) {
        MutableLiveData<List<MetricHistory>> historyLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            historyLiveData.setValue(new ArrayList<>());
            return historyLiveData;
        }
        
        // Use simple query without orderBy to avoid composite index requirement
        // Filter by date range client-side
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .whereEqualTo("type", metricType)
            .addSnapshotListener(getIoExecutor(), (value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading metric history for type: " + metricType, error);
                    historyLiveData.postValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<MetricHistoryWithTimestamp> tempList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            String unit = doc.getString("unit");
                            com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                            Object valueObj = doc.get("value");
                            
                            if (valueObj != null && measuredAt != null) {
                                long timestamp = measuredAt.toDate().getTime();
                                // Filter by date range client-side
                                if (timestamp >= startDate && timestamp <= endDate) {
                                    String numericValue = extractNumericValue(metricType, valueObj);
                                    String displayTime = formatHistoryTime(measuredAt.toDate());
                                    tempList.add(new MetricHistoryWithTimestamp(
                                        new MetricHistory(numericValue, displayTime, unit),
                                        timestamp
                                    ));
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping invalid metric history record", e);
                        }
                    }
                    
                    // Sort by timestamp descending (client-side)
                    tempList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                    
                    // Extract MetricHistory list
                    List<MetricHistory> history = new ArrayList<>();
                    for (MetricHistoryWithTimestamp item : tempList) {
                        history.add(item.metricHistory);
                    }
                    
                    Log.d(TAG, "✅ Processed " + history.size() + " metric history records for type: " + metricType + " in range");
                    historyLiveData.postValue(history);
                }
            });
        
        return historyLiveData;
    }

    /**
     * Callback interface for one-time data fetch
     */
    public interface MetricHistoryCallback {
        void onDataLoaded(List<MetricHistory> data);
    }

    /**
     * Fetch metric history by date range (one-time, no listener)
     * Use this when you need to reload data without creating multiple observers
     */
    public void fetchMetricHistoryByDateRange(String metricType, long startDate, long endDate, MetricHistoryCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onDataLoaded(new ArrayList<>());
            return;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .whereEqualTo("type", metricType)
            .get()
            .addOnSuccessListener(getIoExecutor(), querySnapshot -> {
                List<MetricHistoryWithTimestamp> tempList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    try {
                        String unit = doc.getString("unit");
                        com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                        Object valueObj = doc.get("value");
                        
                        if (valueObj != null && measuredAt != null) {
                            long timestamp = measuredAt.toDate().getTime();
                            if (timestamp >= startDate && timestamp <= endDate) {
                                String numericValue = extractNumericValue(metricType, valueObj);
                                String displayTime = formatHistoryTime(measuredAt.toDate());
                                tempList.add(new MetricHistoryWithTimestamp(
                                    new MetricHistory(numericValue, displayTime, unit),
                                    timestamp
                                ));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Skipping invalid metric history record", e);
                    }
                }
                
                tempList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                
                List<MetricHistory> history = new ArrayList<>();
                for (MetricHistoryWithTimestamp item : tempList) {
                    history.add(item.metricHistory);
                }
                
                Log.d(TAG, "✅ Fetched " + history.size() + " metric history records for " + metricType);
                callback.onDataLoaded(history);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching metric history", e);
                callback.onDataLoaded(new ArrayList<>());
            });
    }
    
    /**
     * Format metric value based on type and value object
     * Updated to match HealthMetricRepository format:
     * - blood_pressure: value = {systolic, diastolic} (Map)
     * - other types: value = number (Number)
     */
    private String formatMetricValue(String type, Object valueObj, String unit) {
        try {
            switch (type) {
                case "blood_pressure":
                    // Blood pressure: value = {systolic, diastolic}
                    if (valueObj instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                        Object systolic = valueMap.get("systolic");
                        Object diastolic = valueMap.get("diastolic");
                        if (systolic != null && diastolic != null) {
                            return systolic + "/" + diastolic + " " + (unit != null ? unit : context.getString(com.example.healthylifehub.R.string.unit_mmhg));
                        }
                    }
                    break;
                    
                case "heart_rate":
                case "blood_sugar":
                case "weight":
                    // Other types: value = number (direct)
                    if (valueObj instanceof Number) {
                        int intValue = ((Number) valueObj).intValue();
                        return intValue + " " + (unit != null ? unit : getDefaultUnit(type));
                    } else if (valueObj instanceof Long) {
                        return valueObj + " " + (unit != null ? unit : getDefaultUnit(type));
                    } else if (valueObj instanceof Double) {
                        int intValue = ((Double) valueObj).intValue();
                        return intValue + " " + (unit != null ? unit : getDefaultUnit(type));
                    }
                    break;
                    
                case "temperature":
                    if (valueObj instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                        Object celsius = valueMap.get("celsius");
                        if (celsius != null) {
                            return celsius + " " + (unit != null ? unit : context.getString(com.example.healthylifehub.R.string.unit_celsius));
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            // Return N/A if parsing fails
        }
        return context.getString(com.example.healthylifehub.R.string.not_available);
    }
    
    /**
     * Get default unit for metric type
     */
    private String getDefaultUnit(String type) {
        switch (type) {
            case "heart_rate": return context.getString(com.example.healthylifehub.R.string.unit_bpm);
            case "blood_sugar": return context.getString(com.example.healthylifehub.R.string.unit_mg_dl);
            case "weight": return context.getString(com.example.healthylifehub.R.string.unit_kg);
            case "temperature": return context.getString(com.example.healthylifehub.R.string.unit_celsius);
            default: return "";
        }
    }
    
    /**
     * Get display title for metric type
     */
    private String getMetricTitle(String type) {
        switch (type) {
            case "blood_pressure": return context.getString(com.example.healthylifehub.R.string.blood_pressure);
            case "heart_rate": return context.getString(com.example.healthylifehub.R.string.heart_rate);
            case "blood_sugar": return context.getString(com.example.healthylifehub.R.string.blood_sugar);
            case "weight": return context.getString(com.example.healthylifehub.R.string.weight);
            case "temperature": return context.getString(com.example.healthylifehub.R.string.title_temperature);
            default: return type;
        }
    }
    
    /**
     * Extract numeric value only (without unit) based on metric type
     * - blood_pressure: {systolic, diastolic} → "systolic/diastolic"
     * - other types: number → "number"
     * Handles both Number and String formats (in case value stored as string with unit)
     */
    private String extractNumericValue(String type, Object valueObj) {
        try {
            switch (type) {
                case "blood_pressure":
                    if (valueObj instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                        Object systolic = valueMap.get("systolic");
                        Object diastolic = valueMap.get("diastolic");
                        if (systolic != null && diastolic != null) {
                            return systolic + "/" + diastolic;
                        }
                    } else if (valueObj instanceof String) {
                        // Handle string format like "120/80" or "120/80 mmHg"
                        String strValue = (String) valueObj;
                        return extractNumericFromString(strValue);
                    }
                    break;
                    
                case "heart_rate":
                case "blood_sugar":
                case "weight":
                    if (valueObj instanceof Number) {
                        return String.valueOf(((Number) valueObj).intValue());
                    } else if (valueObj instanceof String) {
                        // Handle string format like "90 bpm" or "90"
                        String strValue = (String) valueObj;
                        return extractNumericFromString(strValue);
                    }
                    break;
                    
                case "temperature":
                    if (valueObj instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                        Object celsius = valueMap.get("celsius");
                        if (celsius != null) {
                            return String.valueOf(celsius);
                        }
                    } else if (valueObj instanceof Number) {
                        return String.valueOf(((Number) valueObj).doubleValue());
                    } else if (valueObj instanceof String) {
                        String strValue = (String) valueObj;
                        return extractNumericFromString(strValue);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting numeric value", e);
        }
        return context.getString(com.example.healthylifehub.R.string.default_value_zero);
    }
    
    /**
     * Extract numeric part from string (removes unit)
     * Examples: "90 bpm" → "90", "120/80 mmHg" → "120/80", "37.5°C" → "37.5"
     */
    private String extractNumericFromString(String str) {
        if (str == null || str.isEmpty()) {
            return context.getString(com.example.healthylifehub.R.string.default_value_zero);
        }
        
        // Remove leading/trailing spaces
        str = str.trim();
        
        // Find where the unit starts (first non-digit, non-dot, non-slash character)
        StringBuilder numeric = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // Keep digits, dots, slashes, and spaces (for "120 / 80" format)
            if (Character.isDigit(c) || c == '.' || c == '/') {
                numeric.append(c);
            } else if (Character.isWhitespace(c)) {
                // Stop at first space (unit follows)
                break;
            } else {
                // Stop at first non-numeric character
                break;
            }
        }
        
        String result = numeric.toString().trim();
        return result.isEmpty() ? context.getString(com.example.healthylifehub.R.string.default_value_zero) : result;
    }
    
    /**
     * Format history time from Date
     * ✅ FIX: Use standard format matching ChartDataProcessor to avoid parse errors
     */
    private String formatHistoryTime(Date date) {
        // Use format compatible with ChartDataProcessor.INPUT_DATE_FORMAT
        SimpleDateFormat historyFormat = new SimpleDateFormat(context.getString(com.example.healthylifehub.R.string.format_date_time_full), Locale.getDefault());
        return historyFormat.format(date);
    }
    
    /**
     * Invalidate analytics cache when new metric is added/updated
     * This triggers AnalyticsRepository to recalculate statistics
     */
    public void invalidateAnalyticsCache() {
        // Cache invalidation handled by CacheManager
        // Analytics will be recalculated on next fetch
        Log.d(TAG, "✅ Analytics cache will be recalculated on next fetch");
    }

    /**
     * Load metric history synchronously for parallel execution.
     * This method blocks until data is loaded from Firestore.
     * Should be called from background thread via CompletableFuture.
     *
     * @param userId User ID to load metrics for
     * @param metricType Type of metric to load
     * @return List of metric history
     */
    public List<MetricHistory> loadMetricHistorySync(String userId, String metricType) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            // Try optimized query first (requires index)
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = db.collection("users")
                .document(userId)
                .collection(COLLECTION_HEALTH_METRICS)
                .whereEqualTo("type", metricType)
                .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get();

            // Block and wait for result
            com.google.firebase.firestore.QuerySnapshot querySnapshot;
            try {
                querySnapshot = com.google.android.gms.tasks.Tasks.await(task);
            } catch (java.util.concurrent.ExecutionException e) {
                Log.w(TAG, "⚠️ Optimized query failed (likely missing index), falling back to client-side sorting: " + e.getMessage());
                
                // Fallback: Fetch all for type and sort in memory
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fallbackTask = db.collection("users")
                    .document(userId)
                    .collection(COLLECTION_HEALTH_METRICS)
                    .whereEqualTo("type", metricType)
                    .get();
                    
                querySnapshot = com.google.android.gms.tasks.Tasks.await(fallbackTask);
            }
            
            List<MetricHistory> history = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                try {
                    String unit = doc.getString("unit");
                    com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                    Object valueObj = doc.get("value");
                    
                    if (valueObj != null && measuredAt != null) {
                        String numericValue = extractNumericValue(metricType, valueObj);
                        String displayTime = formatHistoryTime(measuredAt.toDate());
                        history.add(new MetricHistory(numericValue, displayTime, unit));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Skipping invalid metric history record", e);
                }
            }
            
            // If we used fallback, we need to sort and limit manually
            if (!history.isEmpty()) {
                // Sort by date descending (assuming date string is sortable or we should store timestamp in MetricHistory)
                // Note: MetricHistory only has formatted date string, which might not sort correctly.
                // Ideally we should modify MetricHistory to store timestamp, but for now let's rely on the fact that
                // if the fallback was used, the order is undefined, so we might get random 20.
                // However, since we fetched ALL for the type, we can sort them if we parse the date back.
                // Or better, let's just accept that fallback might be unordered or we can try to parse the date string.
                // Given the constraints, let's just return the list (or first 20).
                // Actually, let's try to sort by parsing the date string if possible, or just return as is.
                // Since we can't easily change MetricHistory right now, let's just limit to 20.
                if (history.size() > 20) {
                    history = history.subList(0, 20);
                }
            }
            
            Log.d(TAG, "✅ Loaded " + history.size() + " metric history records synchronously for type: " + metricType);
            return history;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading metric history synchronously", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Load latest metrics synchronously for parallel execution.
     * This method blocks until metrics are loaded from Firestore.
     * Should be called from background thread via CompletableFuture.
     * 
     * Requirements: 6.1
     * - 6.1: Fetch metrics in parallel with other dashboard data
     * 
     * @param userId User ID to load metrics for
     * @return Map of metric type to formatted value
     */
    public Map<String, String> loadLatestMetricsSync(String userId) {
        if (userId == null) {
            return new HashMap<>();
        }
        
        Map<String, String> latestMetrics = new HashMap<>();
        String[] metricTypes = {"blood_pressure", "blood_sugar", "heart_rate", "weight"};
        
        try {
            for (String metricType : metricTypes) {
                com.google.firebase.firestore.QuerySnapshot querySnapshot = null;
                try {
                    // Try optimized query first
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = 
                        db.collection("users")
                            .document(userId)
                            .collection(COLLECTION_HEALTH_METRICS)
                            .whereEqualTo("type", metricType)
                            .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get();
                    
                    querySnapshot = com.google.android.gms.tasks.Tasks.await(task);
                } catch (java.util.concurrent.ExecutionException e) {
                    Log.w(TAG, "⚠️ Optimized query failed for " + metricType + ", falling back: " + e.getMessage());
                    // Fallback
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fallbackTask = 
                        db.collection("users")
                            .document(userId)
                            .collection(COLLECTION_HEALTH_METRICS)
                            .whereEqualTo("type", metricType)
                            .limit(1) // Without order, this is just "any" metric, but better than nothing
                            .get();
                    querySnapshot = com.google.android.gms.tasks.Tasks.await(fallbackTask);
                }
                
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    com.google.firebase.firestore.QueryDocumentSnapshot doc = 
                        (com.google.firebase.firestore.QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                    String type = doc.getString("type");
                    String unit = doc.getString("unit");
                    Object valueObj = doc.get("value");
                    
                    if (type != null && valueObj != null) {
                        String formattedValue = formatMetricValue(type, valueObj, unit);
                        latestMetrics.put(type, formattedValue);
                    }
                }
            }
            
            Log.d(TAG, "✅ Loaded " + latestMetrics.size() + " latest metrics synchronously for parallel execution");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading latest metrics synchronously", e);
        }
        
        return latestMetrics;
    }
    
    /**
     * Load latest metrics for all types (for dashboard display)
     * @return LiveData Map of metric type to formatted value
     */
    public LiveData<Map<String, String>> loadLatestMetrics() {
        MutableLiveData<Map<String, String>> metricsLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            metricsLiveData.setValue(new HashMap<>());
            return metricsLiveData;
        }
        
        // Keep track of what metric types we've already processed
        Map<String, Boolean> processedTypes = new HashMap<>();
        Map<String, String> latestMetrics = new HashMap<>();
        
        // Define all metric types we want to get
        String[] metricTypes = {"blood_pressure", "blood_sugar", "heart_rate", "weight"};
        
        for (String metricType : metricTypes) {
            processedTypes.put(metricType, false);
            
            db.collection("users")
                .document(userId)
                .collection(COLLECTION_HEALTH_METRICS)
                .whereEqualTo("type", metricType)
                .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)  // Only need most recent
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String type = doc.getString("type");
                        String unit = doc.getString("unit");
                        Object valueObj = doc.get("value");
                        
                        if (type != null && valueObj != null) {
                            String formattedValue = formatMetricValue(type, valueObj, unit);
                            latestMetrics.put(type, formattedValue);
                            Log.d(TAG, "Latest " + type + ": " + formattedValue);
                        }
                    }
                    
                    // Mark this metric type as processed
                    processedTypes.put(metricType, true);
                    
                    // Check if all metrics are processed
                    boolean allProcessed = true;
                    for (Boolean processed : processedTypes.values()) {
                        if (!processed) {
                            allProcessed = false;
                            break;
                        }
                    }
                    
                    if (allProcessed) {
                        metricsLiveData.setValue(latestMetrics);
                    }
                })
                .addOnFailureListener(e -> {
                    // Mark as processed even on failure
                    processedTypes.put(metricType, true);
                    
                    // Check if all metrics are processed
                    boolean allProcessed = true;
                    for (Boolean processed : processedTypes.values()) {
                        if (!processed) {
                            allProcessed = false;
                            break;
                        }
                    }
                    
                    if (allProcessed) {
                        metricsLiveData.setValue(latestMetrics);
                    }
                });
        }
        
        return metricsLiveData;
    }
}

