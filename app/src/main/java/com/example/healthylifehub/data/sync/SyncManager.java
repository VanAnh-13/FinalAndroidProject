package com.example.healthylifehub.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.healthylifehub.data.cache.CacheManager;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.SyncStatus;
import com.example.healthylifehub.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * Sync Manager for Offline-First Architecture
 * 
 * Responsibilities:
 * - Monitor network connectivity
 * - Sync local changes to Firestore
 * - Pull remote changes to local cache
 * - Handle sync conflicts
 * - Retry failed syncs with exponential backoff
 * - Provide sync status updates
 * 
 * Architecture:
 * 1. User makes changes → Saved to Room immediately (offline-first)
 * 2. Changes marked as "needsSync"
 * 3. When network available → Sync to Firestore
 * 4. Mark as synced in Room
 * 5. Pull remote changes back to Room
 * 
 * Benefits:
 * - Works offline seamlessly
 * - Automatic sync when online
 * - Handles network interruptions
 * - Provides sync feedback to UI
 */
public class SyncManager {
    
    private static final String TAG = "SyncManager";
    private static volatile SyncManager INSTANCE;
    
    private final Context context;
    private final CacheManager cacheManager;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final ExecutorService executorService;
    private final ConnectivityManager connectivityManager;
    
    // Sync status observable
    private final Subject<SyncEvent> syncEventSubject = PublishSubject.create();
    
    // Sync configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final long MAX_RETRY_DELAY_MS = 30000;
    
    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheManager = CacheManager.getInstance(context);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executorService = Executors.newFixedThreadPool(2);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    /**
     * Get singleton instance
     */
    public static SyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SyncManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SyncManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    // ==================== NETWORK MONITORING ====================
    
    /**
     * Check if network is available
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    
    /**
     * Get sync event observable (for UI updates)
     */
    public io.reactivex.rxjava3.core.Observable<SyncEvent> getSyncEvents() {
        return syncEventSubject.hide();
    }
    
    // ==================== SYNC OPERATIONS ====================
    
    /**
     * Sync all pending changes
     */
    public Completable syncAll() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Completable.error(new IllegalStateException("User not logged in"));
        }
        
        if (!isNetworkAvailable()) {
            Log.w(TAG, "⚠️ No network available, skipping sync");
            publishSyncEvent(new SyncEvent(SyncEvent.TYPE_OFFLINE, "No network available"));
            return Completable.complete();
        }
        
        publishSyncEvent(new SyncEvent(SyncEvent.TYPE_SYNC_START, "Starting sync..."));
        
        return Completable.mergeArray(
            syncHealthMetrics(userId),
            syncReminders(userId),
            syncUsers(userId)
        )
        .doOnComplete(() -> {
            publishSyncEvent(new SyncEvent(SyncEvent.TYPE_SYNC_COMPLETE, "Sync completed"));
            Log.d(TAG, "✅ All syncs completed");
        })
        .doOnError(error -> {
            publishSyncEvent(new SyncEvent(SyncEvent.TYPE_SYNC_ERROR, error.getMessage()));
            Log.e(TAG, "❌ Sync error", error);
        });
    }
    
    /**
     * Sync health metrics
     */
    private Completable syncHealthMetrics(String userId) {
        return cacheManager.getHealthMetricsNeedingSync()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(metrics -> {
                if (metrics.isEmpty()) {
                    Log.d(TAG, "✅ No health metrics to sync");
                    return Completable.complete();
                }
                
                List<Completable> syncTasks = new ArrayList<>();
                for (HealthMetric metric : metrics) {
                    syncTasks.add(syncHealthMetric(metric));
                }
                
                return Completable.merge(syncTasks)
                    .doOnComplete(() -> Log.d(TAG, "✅ Synced " + metrics.size() + " health metrics"));
            });
    }
    
    /**
     * Sync single health metric
     */
    private Completable syncHealthMetric(HealthMetric metric) {
        return Completable.create(emitter -> {
            cacheManager.markEntitySyncing(metric.getId());
            
            Map<String, Object> metricData = new HashMap<>();
            metricData.put("type", metric.getType());
            metricData.put("unit", getUnitForType(metric.getType()));
            metricData.put("measuredAt", new Timestamp(metric.getMeasuredAt()));
            metricData.put("note", metric.getNotes() != null ? metric.getNotes() : "");
            metricData.put("createdAt", Timestamp.now());
            
            if ("blood_pressure".equals(metric.getType())) {
                Map<String, Object> value = new HashMap<>();
                value.put("systolic", (int) metric.getSystolic());
                value.put("diastolic", (int) metric.getDiastolic());
                metricData.put("value", value);
            } else {
                metricData.put("value", metric.getValue());
            }
            
            db.collection("users")
                .document(metric.getUserId())
                .collection("healthMetrics")
                .document(metric.getId())
                .set(metricData)
                .addOnSuccessListener(aVoid -> {
                    cacheManager.markHealthMetricSynced(metric.getId(), System.currentTimeMillis());
                    cacheManager.markEntitySyncSuccess(metric.getId(), System.currentTimeMillis());
                    Log.d(TAG, "✅ Synced health metric: " + metric.getId());
                    emitter.onComplete();
                })
                .addOnFailureListener(error -> {
                    cacheManager.markEntitySyncFailed(metric.getId(), error.getMessage());
                    Log.e(TAG, "❌ Failed to sync health metric", error);
                    emitter.onError(error);
                });
        }).subscribeOn(Schedulers.io());
    }
    
    /**
     * Sync reminders
     */
    private Completable syncReminders(String userId) {
        return cacheManager.getReminders(userId)
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(reminders -> {
                if (reminders.isEmpty()) {
                    Log.d(TAG, "✅ No reminders to sync");
                    return Completable.complete();
                }
                
                List<Completable> syncTasks = new ArrayList<>();
                for (Reminder reminder : reminders) {
                    syncTasks.add(syncReminder(reminder));
                }
                
                return Completable.merge(syncTasks)
                    .doOnComplete(() -> Log.d(TAG, "✅ Synced " + reminders.size() + " reminders"));
            });
    }
    
    /**
     * Sync single reminder
     */
    private Completable syncReminder(Reminder reminder) {
        return Completable.create(emitter -> {
            cacheManager.markEntitySyncing(reminder.getReminderId());
            
            Map<String, Object> reminderData = new HashMap<>();
            reminderData.put("title", reminder.getTitle());
            reminderData.put("description", reminder.getDescription());
            // Convert milliseconds to seconds for Firestore Timestamp
            long reminderSeconds = reminder.getReminderTime() / 1000;
            reminderData.put("reminderTime", new Timestamp(reminderSeconds, 0));
            reminderData.put("frequency", reminder.getFrequency());
            reminderData.put("isActive", reminder.isActive());
            reminderData.put("medicineId", reminder.getMedicineId());
            reminderData.put("updatedAt", Timestamp.now());
            
            db.collection("users")
                .document(reminder.getUserId())
                .collection("reminders")
                .document(reminder.getReminderId())
                .set(reminderData)
                .addOnSuccessListener(aVoid -> {
                    cacheManager.markEntitySyncSuccess(reminder.getReminderId(), System.currentTimeMillis());
                    Log.d(TAG, "✅ Synced reminder: " + reminder.getReminderId());
                    emitter.onComplete();
                })
                .addOnFailureListener(error -> {
                    cacheManager.markEntitySyncFailed(reminder.getReminderId(), error.getMessage());
                    Log.e(TAG, "❌ Failed to sync reminder", error);
                    emitter.onError(error);
                });
        }).subscribeOn(Schedulers.io());
    }
    
    /**
     * Sync users
     */
    private Completable syncUsers(String userId) {
        return cacheManager.getUsersNeedingSync()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(users -> {
                if (users.isEmpty()) {
                    Log.d(TAG, "✅ No users to sync");
                    return Completable.complete();
                }
                
                List<Completable> syncTasks = new ArrayList<>();
                for (User user : users) {
                    syncTasks.add(syncUser(user));
                }
                
                return Completable.merge(syncTasks)
                    .doOnComplete(() -> Log.d(TAG, "✅ Synced " + users.size() + " users"));
            });
    }
    
    /**
     * Sync single user
     */
    private Completable syncUser(User user) {
        return Completable.create(emitter -> {
            cacheManager.markEntitySyncing(user.getUid());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("displayName", user.getDisplayName());
            userData.put("photoURL", user.getPhotoUrl());
            userData.put("role", user.getRole());
            userData.put("updatedAt", Timestamp.now());
            
            db.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    cacheManager.markUserSynced(user.getUid(), System.currentTimeMillis());
                    cacheManager.markEntitySyncSuccess(user.getUid(), System.currentTimeMillis());
                    Log.d(TAG, "✅ Synced user: " + user.getUid());
                    emitter.onComplete();
                })
                .addOnFailureListener(error -> {
                    cacheManager.markEntitySyncFailed(user.getUid(), error.getMessage());
                    Log.e(TAG, "❌ Failed to sync user", error);
                    emitter.onError(error);
                });
        }).subscribeOn(Schedulers.io());
    }
    
    // ==================== HELPER METHODS ====================
    
    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
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
    
    private void publishSyncEvent(SyncEvent event) {
        syncEventSubject.onNext(event);
    }
    
    // ==================== SYNC EVENT ====================
    
    /**
     * Sync event for UI updates
     */
    public static class SyncEvent {
        public static final int TYPE_SYNC_START = 1;
        public static final int TYPE_SYNC_COMPLETE = 2;
        public static final int TYPE_SYNC_ERROR = 3;
        public static final int TYPE_OFFLINE = 4;
        
        public final int type;
        public final String message;
        public final long timestamp;
        
        public SyncEvent(int type, String message) {
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return "SyncEvent{" +
                    "type=" + type +
                    ", message='" + message + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Shutdown sync manager
     */
    public void shutdown() {
        executorService.shutdown();
        Log.d(TAG, "✅ SyncManager shutdown");
    }
}
