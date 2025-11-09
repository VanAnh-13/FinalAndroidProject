package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.util.Log;
import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.model.HealthMetric;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * RxJava-based Repository for Health Metrics
 * 
 * Benefits of RxJava:
 * - Powerful operators (map, flatMap, filter, debounce, etc.)
 * - Easy thread management (subscribeOn, observeOn)
 * - Backpressure handling (Flowable)
 * - Error handling (onErrorReturn, retry, etc.)
 * - Composable async operations
 * 
 * Use Cases:
 * - Complex data transformations
 * - Chaining multiple async operations
 * - Debouncing user input
 * - Combining multiple data sources
 */
public class RxHealthMetricRepository {
    
    private static final String TAG = "RxHealthMetricRepo";
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_METRICS = "healthMetrics";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final HealthMetricDao dao;
    
    public RxHealthMetricRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dao = AppDatabase.getInstance(context).healthMetricDao();
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    // ==================== SAVE OPERATIONS ====================
    
    /**
     * Save health metric (RxJava - Offline-First)
     * Returns Completable that completes when saved to Room
     * Firestore sync happens in background
     */
    public Completable saveHealthMetric(HealthMetric metric) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Completable.error(new IllegalStateException("User not logged in"));
        }
        
        // Set metadata
        metric.setUserId(userId);
        metric.setNeedsSync(true);
        if (metric.getId() == null || metric.getId().isEmpty()) {
            metric.setId(db.collection("temp").document().getId());
        }
        
        return dao.insertMetricRx(metric)
            .subscribeOn(Schedulers.io())
            .doOnComplete(() -> {
                Log.d(TAG, "✅ Saved to local database (RxJava)");
                // Sync to Firestore in background (fire and forget)
                syncToFirestore(metric)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        () -> Log.d(TAG, "✅ Synced to Firestore"),
                        error -> Log.w(TAG, "⚠️ Firestore sync failed", error)
                    );
            });
    }
    
    /**
     * Save multiple metrics at once
     */
    public Completable saveHealthMetrics(List<HealthMetric> metrics) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Completable.error(new IllegalStateException("User not logged in"));
        }
        
        for (HealthMetric metric : metrics) {
            metric.setUserId(userId);
            metric.setNeedsSync(true);
            if (metric.getId() == null || metric.getId().isEmpty()) {
                metric.setId(db.collection("temp").document().getId());
            }
        }
        
        return dao.insertMetricsRx(metrics)
            .subscribeOn(Schedulers.io())
            .doOnComplete(() -> Log.d(TAG, "✅ Saved " + metrics.size() + " metrics"));
    }
    
    /**
     * Sync metric to Firestore (internal method)
     */
    private Completable syncToFirestore(HealthMetric metric) {
        return Completable.create(emitter -> {
            Map<String, Object> metricData = new HashMap<>();
            metricData.put("type", metric.getType());
            metricData.put("unit", getUnitForType(metric.getType()));
            metricData.put("measuredAt", new Timestamp(metric.getMeasuredAt()));
            metricData.put("note", metric.getNotes() != null ? metric.getNotes() : "");
            metricData.put("createdAt", Timestamp.now());
            
            if (metric.getType().equals("blood_pressure")) {
                Map<String, Object> value = new HashMap<>();
                value.put("systolic", (int) metric.getSystolic());
                value.put("diastolic", (int) metric.getDiastolic());
                metricData.put("value", value);
            } else {
                metricData.put("value", metric.getValue());
            }
            
            db.collection(COLLECTION_USERS)
                .document(metric.getUserId())
                .collection(SUBCOLLECTION_METRICS)
                .document(metric.getId())
                .set(metricData)
                .addOnSuccessListener(aVoid -> {
                    // Mark as synced
                    dao.markAsSynced(metric.getId(), System.currentTimeMillis());
                    emitter.onComplete();
                })
                .addOnFailureListener(emitter::onError);
        });
    }
    
    // ==================== LOAD OPERATIONS ====================
    
    /**
     * Load all metrics (Flowable - reactive stream with backpressure)
     * Automatically updates when Room data changes
     */
    public Flowable<List<HealthMetric>> loadHealthMetrics() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Flowable.just(new ArrayList<>());
        }
        
        // Start background sync
        startBackgroundSync(userId);
        
        return dao.getMetricsForUserRx(userId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(metrics -> Log.d(TAG, "📊 Loaded " + metrics.size() + " metrics (RxJava)"))
            .doOnError(error -> Log.e(TAG, "Error loading metrics", error));
    }
    
    /**
     * Load metrics by type
     */
    public Flowable<List<HealthMetric>> loadMetricsByType(String type) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Flowable.just(new ArrayList<>());
        }
        
        return dao.getMetricsByTypeRx(userId, type)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Get single metric by ID
     */
    public Maybe<HealthMetric> getMetricById(String metricId) {
        return dao.getMetricByIdRx(metricId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Get latest metric by type
     */
    public Maybe<HealthMetric> getLatestMetricByType(String type) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Maybe.empty();
        }
        
        return dao.getLatestMetricByTypeRx(userId, type)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Get metrics count
     */
    public Single<Integer> getMetricsCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Single.just(0);
        }
        
        return dao.getMetricsCountRx(userId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Delete metric
     */
    public Completable deleteHealthMetric(String metricId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Completable.error(new IllegalStateException("User not logged in"));
        }
        
        return dao.deleteMetricRx(metricId)
            .subscribeOn(Schedulers.io())
            .doOnComplete(() -> {
                Log.d(TAG, "✅ Deleted from local database");
                // Delete from Firestore in background
                db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(SUBCOLLECTION_METRICS)
                    .document(metricId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Deleted from Firestore"))
                    .addOnFailureListener(e -> Log.w(TAG, "⚠️ Firestore delete failed", e));
            });
    }
    
    // ==================== ADVANCED RxJava OPERATIONS ====================
    
    /**
     * Get average value for a metric type (example of RxJava power)
     */
    public Single<Double> getAverageValue(String type) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Single.just(0.0);
        }
        
        return dao.getMetricsByTypeRx(userId, type)
            .firstOrError()
            .subscribeOn(Schedulers.computation())
            .map(metrics -> {
                if (metrics.isEmpty()) return 0.0;
                
                double sum = 0;
                int count = 0;
                
                for (HealthMetric metric : metrics) {
                    if (metric.getValue() != 0.0) {
                        sum += metric.getValue();
                        count++;
                    }
                }
                
                return count > 0 ? sum / count : 0.0;
            })
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Get metrics within date range
     */
    public Single<List<HealthMetric>> getMetricsInRange(Date startDate, Date endDate) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Single.just(new ArrayList<>());
        }
        
        return dao.getMetricsForUserRx(userId)
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .map(metrics -> {
                List<HealthMetric> filtered = new ArrayList<>();
                for (HealthMetric metric : metrics) {
                    Date measuredAt = metric.getMeasuredAt();
                    if (measuredAt != null && 
                        !measuredAt.before(startDate) && 
                        !measuredAt.after(endDate)) {
                        filtered.add(metric);
                    }
                }
                return filtered;
            })
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Sync all pending changes to Firestore
     */
    public Completable syncPendingChanges() {
        return dao.getMetricsNeedingSyncRx()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(metrics -> {
                List<Completable> syncTasks = new ArrayList<>();
                for (HealthMetric metric : metrics) {
                    syncTasks.add(syncToFirestore(metric));
                }
                return Completable.merge(syncTasks);
            })
            .doOnComplete(() -> Log.d(TAG, "✅ All pending changes synced"));
    }
    
    // ==================== BACKGROUND SYNC ====================
    
    /**
     * Start background sync from Firestore
     */
    private void startBackgroundSync(String userId) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_METRICS)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "⚠️ Firestore sync error", error);
                    return;
                }
                
                if (snapshots != null && !snapshots.isEmpty()) {
                    List<HealthMetric> metrics = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        HealthMetric metric = parseFirestoreDocument(doc, userId);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    }
                    
                    // Update Room in background
                    dao.insertMetricsRx(metrics)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            () -> Log.d(TAG, "🔄 Synced " + metrics.size() + " metrics from Firestore"),
                            err -> Log.e(TAG, "Error syncing", err)
                        );
                }
            });
    }
    
    // ==================== PARSER METHOD ====================
    
    private HealthMetric parseFirestoreDocument(QueryDocumentSnapshot doc, String userId) {
        try {
            HealthMetric metric = new HealthMetric();
            metric.setId(doc.getId());
            metric.setUserId(userId);
            metric.setType(doc.getString("type"));
            metric.setNotes(doc.getString("note"));
            metric.setNeedsSync(false);
            metric.setSynced(true);
            metric.setLastSyncedAt(new Date());
            
            Timestamp measuredAt = doc.getTimestamp("measuredAt");
            if (measuredAt != null) {
                metric.setMeasuredAt(measuredAt.toDate());
            }
            
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
    
    private String getUnitForType(String type) {
        switch (type) {
            case "blood_pressure": return "mmHg";
            case "blood_sugar": return "mg/dL";
            case "weight": return "kg";
            case "heart_rate": return "bpm";
            case "temperature": return "°C";
            default: return "";
        }
    }
}
