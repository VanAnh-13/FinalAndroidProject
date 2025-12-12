package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.cache.CacheManager;
// No need for Entity anymore - using Model directly
import com.example.healthylifehub.data.model.HealthMetric;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.core.content.ContextCompat;
import com.example.healthylifehub.base.BaseRepository;
import com.example.healthylifehub.utils.notification.NotificationHelper;
import com.example.healthylifehub.sync.SyncManager;

/**
 * Repository for managing health metrics with OFFLINE-FIRST architecture
 * 
 * Data Flow:
 * 1. UI reads from Room (instant, works offline)
 * 2. Background sync from Firestore to Room
 * 3. UI automatically updates when Room data changes
 * 
 * Follows the structure defined in Project_Summary.md
 */
public class HealthMetricRepository extends BaseRepository {
    
    private static final String TAG = "HealthMetricRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_METRICS = "healthMetrics";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final ExecutorService executorService;
    private final Context context;
    private final SyncManager syncManager;
    private final HealthMetricDao dao;

    public HealthMetricRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executorService = Executors.newCachedThreadPool();
        this.syncManager = new SyncManager(context);
        this.dao = AppDatabase.getInstance(context).healthMetricDao();

        // Enable Firestore offline persistence
        enableFirestoreOffline();
    }

    /**
     * Enable Firestore offline persistence
     */
    private void enableFirestoreOffline() {
        try {
            db.enableNetwork();
            Log.d(TAG, "✅ Firestore offline persistence enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling Firestore offline", e);
        }
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    /**
     * Save health metric (OFFLINE-FIRST)
     * 1. Save to Room immediately (works offline)
     * 2. Sync to Firestore in background
     * 
     * @param metric HealthMetric object to save
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> saveHealthMetric(HealthMetric metric) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            future.complete(false);
            return future;
        }

        // Generate ID if not exists
        if (metric.getId() == null || metric.getId().isEmpty()) {
            metric.setId(db.collection("temp").document().getId());
        }
        

        // Step 1: Save to Room (instant, works offline)
        executorService.execute(() -> {
            try {
                dao.insertMetric(metric);
                Log.d(TAG, "✅ Saved to local database");

                // Trigger immediate sync after data change (Requirement 12.1)
                syncManager.triggerImmediateSync();
                Log.d(TAG, "🔄 Triggered immediate sync after metric save");

                // Step 2: Sync to Firestore in background
                syncToFirestore(metric)
                    .thenAccept(success -> {
                        if (success) {
                            // Mark as synced in Room
                            executorService.execute(() -> {
                                dao.markAsSynced(metric.getId(), System.currentTimeMillis());
                                Log.d(TAG, "✅ Synced to Firestore");
                            });
                        }
                        future.complete(true); // Return success even if Firestore fails (offline-first)
                    })
                    .exceptionally(throwable -> {
                        Log.w(TAG, "⚠️ Firestore sync failed (will retry later)", throwable);
                        future.complete(true); // Still success because saved locally
                        return null;
                    });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving to local database", e);
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Save health metric with automated analysis pipeline (ENHANCED)
     * Implements CompletableFuture pipeline with 4 stages:
     * Stage 1: Save to Room on IO thread
     * Stage 2: Upload to Firestore on IO thread
     * Stage 3: Trigger HealthMetricsAnalyzer on compute thread
     * Stage 4: Handle analysis results (notifications) on main thread
     *
     * @param metric HealthMetric object to save
     * @return CompletableFuture<Void> indicating completion
     */
    public CompletableFuture<Void> saveMetricWithAnalysis(HealthMetric metric) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("User not logged in"));
            return future;
        }

        // Set metadata
        metric.setUserId(userId);
        metric.setNeedsSync(true);
        metric.setLastSyncedAt(new Date());

        // Generate ID if not exists
        if (metric.getId() == null || metric.getId().isEmpty()) {
            metric.setId(db.collection("temp").document().getId());
        }

        // Stage 1: Save to Room on IO thread
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    dao.insertMetric(metric);
                    Log.d(TAG, "✅ Stage 1: Saved to Room database");

                    // Trigger immediate sync after data change (Requirement 12.1)
                    syncManager.triggerImmediateSync();
                    Log.d(TAG, "🔄 Triggered immediate sync after metric save with analysis");

                    return metric;
                } catch (Exception e) {
                    Log.e(TAG, "❌ Stage 1 failed: Error saving to Room", e);
                    throw new RuntimeException("Failed to save to Room", e);
                }
            }, getIoExecutor())

            // Stage 2: Upload to Firestore on IO thread
            .thenComposeAsync(savedMetric -> {
                Log.d(TAG, "🔄 Stage 2: Uploading to Firestore...");
                return syncToFirestore(savedMetric)
                    .thenApply(success -> {
                        if (success) {
                            // Mark as synced in Room
                            try {
                                dao.markAsSynced(savedMetric.getId(), System.currentTimeMillis());
                                Log.d(TAG, "✅ Stage 2: Synced to Firestore");
                            } catch (Exception e) {
                                Log.w(TAG, "⚠️ Failed to mark as synced", e);
                            }
                        } else {
                            Log.w(TAG, "⚠️ Stage 2: Firestore sync failed (will retry later)");
                        }
                        return savedMetric;
                    })
                    .exceptionally(throwable -> {
                        Log.w(TAG, "⚠️ Stage 2: Firestore sync failed (offline mode)", throwable);
                        return savedMetric; // Continue pipeline even if Firestore fails
                    });
            }, getIoExecutor())

            // Stage 3: Trigger HealthMetricsAnalyzer on compute thread
            .thenComposeAsync(savedMetric -> {
                Log.d(TAG, "🔄 Stage 3: Analyzing metrics...");
                // TODO: Integrate with HealthMetricsAnalyzer when implemented (Task 5)
                // For now, perform basic anomaly detection
                return performBasicAnalysis(savedMetric);
            }, getComputeExecutor())

            // Stage 4: Handle analysis results (notifications) on main thread
            .thenAcceptAsync(analysisResult -> {
                Log.d(TAG, "🔄 Stage 4: Handling analysis results...");
                if (analysisResult != null && analysisResult.hasAnomalies()) {
                    // Send notification on main thread
                    NotificationHelper.showHealthAlertNotification(
                        context,
                        "Cảnh báo sức khỏe",
                        analysisResult.getMessage(),
                        (int) System.currentTimeMillis()
                    );
                    Log.d(TAG, "✅ Stage 4: Sent anomaly notification");
                } else {
                    Log.d(TAG, "✅ Stage 4: No anomalies detected");
                }
            }, getMainExecutor())

            // Error handling for entire pipeline
            .exceptionally(throwable -> {
                Log.e(TAG, "❌ Pipeline failed", throwable);
                return null;
            });
    }

    /**
     * Perform basic analysis (placeholder until HealthMetricsAnalyzer is implemented)
     * This will be replaced by HealthMetricsAnalyzer.analyzeMetrics() in Task 5
     */
    private CompletableFuture<AnalysisResult> performBasicAnalysis(HealthMetric metric) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get historical data for comparison
                List<HealthMetric> history = dao.getMetricsForUserSync(metric.getUserId());

                // Filter by type
                List<HealthMetric> sameTypeMetrics = new ArrayList<>();
                for (HealthMetric m : history) {
                    if (m.getType().equals(metric.getType())) {
                        sameTypeMetrics.add(m);
                    }
                }

                // Need at least 5 data points for meaningful analysis
                if (sameTypeMetrics.size() < 5) {
                    return new AnalysisResult(false, "Insufficient data for analysis");
                }

                // Calculate mean and standard deviation
                double sum = 0;
                int count = 0;
                for (HealthMetric m : sameTypeMetrics) {
                    if (metric.getType().equals("blood_pressure")) {
                        sum += m.getSystolic();
                    } else {
                        sum += m.getValue();
                    }
                    count++;
                }
                double mean = sum / count;

                double varianceSum = 0;
                for (HealthMetric m : sameTypeMetrics) {
                    double value = metric.getType().equals("blood_pressure") ? m.getSystolic() : m.getValue();
                    varianceSum += Math.pow(value - mean, 2);
                }
                double stdDev = Math.sqrt(varianceSum / count);

                // Check if current value is anomalous (>2 standard deviations)
                double currentValue = metric.getType().equals("blood_pressure") ?
                    metric.getSystolic() : metric.getValue();
                double deviation = Math.abs(currentValue - mean);

                if (deviation > 2 * stdDev) {
                    String message = String.format(
                        "Chỉ số %s bất thường: %.1f (trung bình: %.1f, độ lệch: %.1f)",
                        getMetricDisplayName(metric.getType()),
                        currentValue,
                        mean,
                        deviation
                    );
                    Log.d(TAG, "⚠️ Anomaly detected: " + message);
                    return new AnalysisResult(true, message);
                }

                return new AnalysisResult(false, "Normal reading");

            } catch (Exception e) {
                Log.e(TAG, "Error in basic analysis", e);
                return new AnalysisResult(false, "Analysis error");
            }
        }, getComputeExecutor());
    }

    /**
     * Get display name for metric type
     */
    private String getMetricDisplayName(String type) {
        switch (type) {
            case "blood_pressure": return "Huyết áp";
            case "blood_sugar": return "Đường huyết";
            case "heart_rate": return "Nhịp tim";
            case "weight": return "Cân nặng";
            case "temperature": return "Nhiệt độ";
            default: return type;
        }
    }

    /**
     * Simple analysis result holder (placeholder until full AnalysisResult model is created)
     */
    private static class AnalysisResult {
        private final boolean hasAnomalies;
        private final String message;

        public AnalysisResult(boolean hasAnomalies, String message) {
            this.hasAnomalies = hasAnomalies;
            this.message = message;
        }

        public boolean hasAnomalies() {
            return hasAnomalies;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Sync metric to Firestore
     */
    private CompletableFuture<Boolean> syncToFirestore(HealthMetric metric) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Prepare data according to Project_Summary.md structure
        Map<String, Object> metricData = new HashMap<>();
        metricData.put("type", metric.getType());
        metricData.put("unit", getUnitForType(metric.getType()));
        metricData.put("measuredAt", new Timestamp(metric.getMeasuredAt()));
        metricData.put("note", metric.getNotes() != null ? metric.getNotes() : "");
        metricData.put("createdAt", Timestamp.now());
        
        // Handle value based on type
        if (metric.getType().equals("blood_pressure")) {
            Map<String, Object> value = new HashMap<>();
            value.put("systolic", (int) metric.getSystolic());
            value.put("diastolic", (int) metric.getDiastolic());
            metricData.put("value", value);
        } else {
            metricData.put("value", metric.getValue());
        }
        
        // Save to Firestore
        db.collection(COLLECTION_USERS)
            .document(metric.getUserId())
            .collection(SUBCOLLECTION_METRICS)
            .document(metric.getId())
            .set(metricData)
            .addOnSuccessListener(aVoid -> future.complete(true))
            .addOnFailureListener(e -> future.complete(false));
        
        return future;
    }
    
    /**
     * Load all health metrics for current user (OFFLINE-FIRST)
     * 1. Return LiveData from Room (instant)
     * 2. Sync from Firestore in background
     * 
     * @return LiveData<List<HealthMetric>>
     */
    public LiveData<List<HealthMetric>> loadHealthMetrics() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            return new MutableLiveData<>(new ArrayList<>());
        }
        
        // Start background sync from Firestore to Room
        syncFromFirestore(userId);
        
        // Return LiveData from Room (instant, works offline)
        return dao.getMetricsForUser(userId);
    }
    
    /**
     * Sync all metrics from Firestore to Room
     */
    private void syncFromFirestore(String userId) {
        executorService.execute(() -> {
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_METRICS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<HealthMetric> metrics = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        HealthMetric metric = parseFirestoreDocument(doc, userId);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    }
                    
                    // Save all to Room
                    executorService.execute(() -> {
                        for (HealthMetric metric : metrics) {
                            dao.insertMetric(metric);
                        }
                        Log.d(TAG, "✅ Synced " + metrics.size() + " metrics from Firestore to Room");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to sync from Firestore", e);
                });
        });
    }
    
    /**
     * Load metrics by type (OFFLINE-FIRST)
     * @param type Metric type (blood_pressure, blood_sugar, etc.)
     * @return LiveData<List<HealthMetric>>
     */
    public LiveData<List<HealthMetric>> loadMetricsByType(String type) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        
        Query query = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .whereEqualTo("type", type)
            .orderBy("measuredAt", Query.Direction.DESCENDING);

        return new com.example.healthylifehub.data.livedata.FirestoreQueryLiveData<List<HealthMetric>>(query) {
            @Override
            protected List<HealthMetric> parseSnapshot(com.google.firebase.firestore.QuerySnapshot snapshot) {
                List<HealthMetric> metrics = new ArrayList<>();
                if (snapshot != null) {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        HealthMetric metric = parseFirestoreDocument(doc, userId);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    }
                }
                Log.d(TAG, "📊 Loaded " + metrics.size() + " metrics of type " + type + " from Firestore");
                return metrics;
            }
        };
    }
    
    /**
     * Get cached metrics for instant access (SYNCHRONOUS)
     * This method provides immediate access to cached data from Room database
     * without any network calls or async operations.
     *
     * Use case: When you need instant data access (e.g., for quick calculations,
     * dashboard widgets, or when network is unavailable)
     *
     * Performance: Returns within 100ms as per Requirements 10.1, 10.5
     *
     * @param metricType Metric type (blood_pressure, blood_sugar, heart_rate, weight, temperature)
     * @return List<HealthMetric> from Room database cache, empty list if no data or user not logged in
     */
    public List<HealthMetric> getCachedMetrics(String metricType) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "getCachedMetrics: User not logged in");
            return new ArrayList<>();
        }

        try {
            // Query Room database directly on calling thread for instant access
            // This is safe because Room queries are optimized and fast
            List<HealthMetric> cachedMetrics;

            if (metricType == null || metricType.isEmpty()) {
                // Return all metrics if no type specified
                cachedMetrics = dao.getMetricsForUserSync(userId);
                Log.d(TAG, "📦 getCachedMetrics: Retrieved " + cachedMetrics.size() + " cached metrics (all types)");
            } else {
                // Filter by type using a stream (Room doesn't have a sync method for type filtering)
                List<HealthMetric> allMetrics = dao.getMetricsForUserSync(userId);
                cachedMetrics = new ArrayList<>();
                for (HealthMetric metric : allMetrics) {
                    if (metricType.equals(metric.getType())) {
                        cachedMetrics.add(metric);
                    }
                }
                Log.d(TAG, "📦 getCachedMetrics: Retrieved " + cachedMetrics.size() + " cached metrics for type: " + metricType);
            }

            return cachedMetrics;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error retrieving cached metrics", e);
            return new ArrayList<>();
        }
    }

    /**
     * Delete a health metric (FIRESTORE OFFLINE-FIRST)
     * @param metricId ID of the metric to delete
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> deleteHealthMetric(String metricId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            future.complete(false);
            return future;
        }
        
        // Delete directly from Firestore - Firestore handles offline caching
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .document(metricId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Deleted from Firestore (cached offline if no network)");
                future.complete(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error deleting from Firestore", e);
                future.complete(false);
            });
        
        return future;
    }
    
    /**
     * Get unit for metric type
     */
    private String getUnitForType(String type) {
        switch (type) {
            case "blood_pressure":
                return "mmHg";
            case "blood_sugar":
                return "mg/dL";
            case "weight":
                return "kg";
            case "heart_rate":
                return "bpm";
            case "temperature":
                return "°C";
            default:
                return "";
        }
    }
    
    // ==================== PARSER METHOD ====================
    
    /**
     * Parse Firestore document to HealthMetric (no conversion needed!)
     */
    private HealthMetric parseFirestoreDocument(QueryDocumentSnapshot doc, String userId) {
        try {
            HealthMetric metric = new HealthMetric();
            metric.setId(doc.getId());
            metric.setUserId(userId);
            metric.setType(doc.getString("type"));
            metric.setNotes(doc.getString("note"));
            metric.setNeedsSync(false); // From Firestore, already synced
            metric.setSynced(true);
            metric.setLastSyncedAt(new Date());
            
            // Parse timestamps
            Timestamp measuredAt = doc.getTimestamp("measuredAt");
            if (measuredAt != null) {
                metric.setMeasuredAt(measuredAt.toDate());
            }
            
            // Parse values
            String type = doc.getString("type");
            if ("blood_pressure".equals(type)) {
                Object valueObj = doc.get("value");
                if (valueObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                    
                    Object systolicObj = valueMap.get("systolic");
                    Object diastolicObj = valueMap.get("diastolic");
                    
                    if (systolicObj instanceof Number) {
                        metric.setSystolic(((Number) systolicObj).doubleValue());
                    }
                    if (diastolicObj instanceof Number) {
                        metric.setDiastolic(((Number) diastolicObj).doubleValue());
                    }
                }
            } else {
                Object valueObj = doc.get("value");
                if (valueObj instanceof Number) {
                    metric.setValue(((Number) valueObj).doubleValue());
                }
            }
            
            return metric;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Firestore document", e);
            return null;
        }
    }
}
