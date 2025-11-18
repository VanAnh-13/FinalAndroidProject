package com.example.healthylifehub.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.MetricHistory;
import com.example.healthylifehub.data.model.MetricItem;
import com.example.healthylifehub.data.cache.CacheManager;
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
 */
public class MetricsRepository extends FirebaseRepository {
    
    private static final String TAG = "MetricsRepository";
    private static final String COLLECTION_HEALTH_METRICS = "healthMetrics";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault());
    private final android.content.Context context;
    
    public MetricsRepository(android.content.Context context) {
        this.context = context;
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
                            Object valueObj = doc.get("value");
                            
                            if (type != null && valueObj != null && measuredAt != null) {
                                String displayValue = formatMetricValue(type, valueObj, unit);
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
     * Load metric history for a specific metric type
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
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .whereEqualTo("type", metricType)
            .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    historyLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<MetricHistory> history = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            String unit = doc.getString("unit");
                            com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                            Object valueObj = doc.get("value");
                            
                            if (valueObj != null && measuredAt != null) {
                                // Extract only the numeric value (without unit)
                                String numericValue = extractNumericValue(metricType, valueObj);
                                String displayTime = formatHistoryTime(measuredAt.toDate());
                                
                                // Store numeric value + unit separately for calculations
                                history.add(new MetricHistory(numericValue, displayTime, unit));
                            }
                        } catch (Exception e) {
                            // Skip invalid records
                        }
                    }
                    historyLiveData.setValue(history);
                }
            });
        
        return historyLiveData;
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
                            return systolic + "/" + diastolic + " " + (unit != null ? unit : "mmHg");
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
                            return celsius + " " + (unit != null ? unit : "°C");
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            // Return N/A if parsing fails
        }
        return "N/A";
    }
    
    /**
     * Get default unit for metric type
     */
    private String getDefaultUnit(String type) {
        switch (type) {
            case "heart_rate": return "bpm";
            case "blood_sugar": return "mg/dL";
            case "weight": return "kg";
            case "temperature": return "°C";
            default: return "";
        }
    }
    
    /**
     * Get display title for metric type
     */
    private String getMetricTitle(String type) {
        switch (type) {
            case "blood_pressure": return "Huyết áp";
            case "heart_rate": return "Nhịp tim";
            case "blood_sugar": return "Đường huyết";
            case "weight": return "Cân nặng";
            case "temperature": return "Nhiệt độ";
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
        return "0";
    }
    
    /**
     * Extract numeric part from string (removes unit)
     * Examples: "90 bpm" → "90", "120/80 mmHg" → "120/80", "37.5°C" → "37.5"
     */
    private String extractNumericFromString(String str) {
        if (str == null || str.isEmpty()) {
            return "0";
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
        return result.isEmpty() ? "0" : result;
    }
    
    /**
     * Format history time from Date
     * ✅ FIX: Use standard format matching ChartDataProcessor to avoid parse errors
     */
    private String formatHistoryTime(Date date) {
        // Use format compatible with ChartDataProcessor.INPUT_DATE_FORMAT
        SimpleDateFormat historyFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
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

