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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class HealthMetricRepository {
    
    private static final String TAG = "HealthMetricRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_METRICS = "healthMetrics";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final HealthMetricDao dao;
    private final ExecutorService executorService;
    private final Context context;
    
    public HealthMetricRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dao = AppDatabase.getInstance(context).healthMetricDao();
        this.executorService = Executors.newCachedThreadPool();
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
        
        // Set metadata
        metric.setUserId(userId);
        metric.setNeedsSync(true);
        metric.setLastSyncedAt(new Date());
        
        // Generate ID if not exists
        if (metric.getId() == null || metric.getId().isEmpty()) {
            metric.setId(db.collection("temp").document().getId());
        }
        
        // Step 1: Save to Room (instant, works offline)
        executorService.execute(() -> {
            try {
                dao.insertMetric(metric);
                Log.d(TAG, "✅ Saved to local database");
                
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
        
        // Step 1: Return data from Room (instant, works offline)
        LiveData<List<HealthMetric>> roomData = dao.getMetricsForUser(userId);
        
        // Step 2: Sync from Firestore in background
        syncFromFirestore(userId);
        
        // Step 3: Log (no conversion needed anymore!)
        return Transformations.map(roomData, metrics -> {
            Log.d(TAG, "📊 Loaded " + metrics.size() + " metrics from local cache");
            return metrics;
        });
    }
    
    /**
     * Sync metrics from Firestore to Room
     */
    private void syncFromFirestore(String userId) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .orderBy("measuredAt", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "⚠️ Firestore sync error (using cached data)", error);
                    return;
                }
                
                if (snapshots != null && !snapshots.isEmpty()) {
                    executorService.execute(() -> {
                        List<HealthMetric> metrics = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            HealthMetric metric = parseFirestoreDocument(doc, userId);
                            if (metric != null) {
                                metrics.add(metric);
                            }
                        }
                        
                        // Update Room database
                        dao.insertMetrics(metrics);
                        Log.d(TAG, "🔄 Synced " + metrics.size() + " metrics from Firestore");
                    });
                }
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
        
        // Return from Room, Firestore sync already handled by loadHealthMetrics()
        return dao.getMetricsByType(userId, type);
    }
    
    /**
     * Delete a health metric (OFFLINE-FIRST)
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
        
        // Delete from Room first
        executorService.execute(() -> {
            dao.deleteMetric(metricId);
            Log.d(TAG, "✅ Deleted from local database");
            
            // Then delete from Firestore
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_METRICS)
                .document(metricId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Deleted from Firestore");
                    future.complete(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Firestore delete failed", e);
                    future.complete(true); // Still success because deleted locally
                });
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
