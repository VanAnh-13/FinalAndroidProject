package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
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
 * Repository for managing health metrics with FIRESTORE OFFLINE-FIRST architecture
 * 
 * Tận dụng tối đa Firestore offline capabilities:
 * 1. Firestore tự động cache dữ liệu locally
 * 2. Firestore tự động sync khi có mạng
 * 3. Không cần Room database phức tạp
 * 4. Sử dụng Firestore listeners cho real-time updates
 * 
 * Follows the structure defined in Project_Summary.md
 */
public class HealthMetricRepository {
    
    private static final String TAG = "HealthMetricRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_METRICS = "healthMetrics";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final ExecutorService executorService;
    private final Context context;
    
    public HealthMetricRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executorService = Executors.newCachedThreadPool();
        
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
     * Save health metric (FIRESTORE OFFLINE-FIRST)
     * Tận dụng Firestore offline caching - không cần Room
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
        
        // Save directly to Firestore - Firestore handles offline caching
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .document(metric.getId())
            .set(metricData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Saved to Firestore (cached offline if no network)");
                future.complete(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error saving to Firestore", e);
                future.complete(false);
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
     * Load all health metrics for current user (FIRESTORE OFFLINE-FIRST)
     * Tận dụng Firestore offline cache - không cần Room
     * 
     * @return LiveData<List<HealthMetric>>
     */
    public LiveData<List<HealthMetric>> loadHealthMetrics() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            return new MutableLiveData<>(new ArrayList<>());
        }
        
        MutableLiveData<List<HealthMetric>> metricsLiveData = new MutableLiveData<>();
        
        // Sử dụng Firestore listener với offline support
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .orderBy("measuredAt", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "⚠️ Firestore listener error", error);
                    metricsLiveData.postValue(new ArrayList<>());
                    return;
                }
                
                List<HealthMetric> metrics = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        HealthMetric metric = parseFirestoreDocument(doc, userId);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    }
                }
                
                Log.d(TAG, "📊 Loaded " + metrics.size() + " metrics from Firestore (offline cache if no network)");
                metricsLiveData.postValue(metrics);
            });
        
        return metricsLiveData;
    }
    
    /**
     * Load metrics by type (FIRESTORE OFFLINE-FIRST)
     * @param type Metric type (blood_pressure, blood_sugar, etc.)
     * @return LiveData<List<HealthMetric>>
     */
    public LiveData<List<HealthMetric>> loadMetricsByType(String type) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        
        MutableLiveData<List<HealthMetric>> metricsLiveData = new MutableLiveData<>();
        
        // Query Firestore directly với filter theo type
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .whereEqualTo("type", type)
            .orderBy("measuredAt", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "⚠️ Firestore listener error for type: " + type, error);
                    metricsLiveData.postValue(new ArrayList<>());
                    return;
                }
                
                List<HealthMetric> metrics = new ArrayList<>();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        HealthMetric metric = parseFirestoreDocument(doc, userId);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    }
                }
                
                Log.d(TAG, "📊 Loaded " + metrics.size() + " metrics of type " + type + " from Firestore");
                metricsLiveData.postValue(metrics);
            });
        
        return metricsLiveData;
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
