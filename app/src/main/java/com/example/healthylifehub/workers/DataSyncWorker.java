package com.example.healthylifehub.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.local.dao.SyncStatusDao;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.SyncStatus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Background Worker for Data Synchronization
 * Implements UC-HLH-12: Offline-First Data Sync
 * 
 * Syncs data between Room (local) and Firestore (cloud)
 * - Upload local changes to Firestore
 * - Download remote changes to Room
 * - Handle conflicts with configurable strategies
 */
public class DataSyncWorker extends Worker {
    
    private static final String TAG = "DataSyncWorker";
    private static final int THREAD_POOL_SIZE = 4;
    private static final int SYNC_TIMEOUT_MINUTES = 5;
    
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final ExecutorService executor;
    
    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.db = AppDatabase.getInstance(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DataSyncWorker started");
        
        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, skipping sync");
            return Result.success();
        }
        
        String userId = currentUser.getUid();
        
        try {
            // Parallel sync of multiple collections
            CompletableFuture<Void> syncMetrics = CompletableFuture.runAsync(
                () -> syncHealthMetrics(userId), 
                executor
            );
            
            CompletableFuture<Void> syncReminders = CompletableFuture.runAsync(
                () -> syncReminders(userId), 
                executor
            );
            
            // Wait for all syncs to complete (with timeout)
            CompletableFuture.allOf(syncMetrics, syncReminders)
                .get(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            Log.d(TAG, "DataSyncWorker completed successfully");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "DataSyncWorker failed", e);
            return Result.retry();
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Sync Health Metrics
     * 1. Upload local changes to Firestore
     * 2. Download remote changes to Room
     * 3. Handle conflicts
     */
    private void syncHealthMetrics(String userId) {
        Log.d(TAG, "Syncing health metrics for user: " + userId);
        
        HealthMetricDao metricDao = db.healthMetricDao();
        SyncStatusDao syncDao = db.syncStatusDao();
        
        try {
            // Step 1: Get entities that need sync
            List<SyncStatus> needsSync = syncDao.getEntitiesNeedingSyncByType(userId, "health_metric");
            Log.d(TAG, "Found " + needsSync.size() + " health metrics needing sync");
            
            // Step 2: Upload each to Firestore
            for (SyncStatus status : needsSync) {
                try {
                    syncDao.markSyncing(status.getEntityId());
                    
                    HealthMetric metric = metricDao.getMetricById(status.getEntityId());
                    if (metric != null) {
                        uploadHealthMetric(userId, metric);
                        syncDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
                        Log.d(TAG, "Uploaded health metric: " + status.getEntityId());
                    }
                } catch (Exception e) {
                    syncDao.markSyncFailed(status.getEntityId(), e.getMessage());
                    Log.e(TAG, "Failed to upload health metric: " + status.getEntityId(), e);
                }
            }
            
            // Step 3: Download recent changes from Firestore
            downloadHealthMetrics(userId, metricDao);
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing health metrics", e);
        }
    }
    
    /**
     * Sync Reminders
     */
    private void syncReminders(String userId) {
        Log.d(TAG, "Syncing reminders for user: " + userId);
        
        ReminderDao reminderDao = db.reminderDao();
        SyncStatusDao syncDao = db.syncStatusDao();
        
        try {
            // Step 1: Get entities that need sync
            List<SyncStatus> needsSync = syncDao.getEntitiesNeedingSyncByType(userId, "reminder");
            Log.d(TAG, "Found " + needsSync.size() + " reminders needing sync");
            
            // Step 2: Upload each to Firestore
            for (SyncStatus status : needsSync) {
                try {
                    syncDao.markSyncing(status.getEntityId());
                    
                    Reminder reminder = reminderDao.getReminderById(status.getEntityId());
                    if (reminder != null) {
                        uploadReminder(userId, reminder);
                        syncDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
                        Log.d(TAG, "Uploaded reminder: " + status.getEntityId());
                    }
                } catch (Exception e) {
                    syncDao.markSyncFailed(status.getEntityId(), e.getMessage());
                    Log.e(TAG, "Failed to upload reminder: " + status.getEntityId(), e);
                }
            }
            
            // Step 3: Download recent changes from Firestore
            downloadReminders(userId, reminderDao);
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing reminders", e);
        }
    }
    
    // ==================== UPLOAD METHODS ====================
    
    private void uploadHealthMetric(String userId, HealthMetric metric) throws Exception {
        // TODO: Implement Firestore upload
        // This should use HealthMetricRepository.saveMetric()
        Log.d(TAG, "Uploading health metric to Firestore: " + metric.getId());
    }
    
    private void uploadReminder(String userId, Reminder reminder) throws Exception {
        // TODO: Implement Firestore upload
        // This should use RemindersRepository.createReminder() or updateReminder()
        Log.d(TAG, "Uploading reminder to Firestore: " + reminder.getReminderId());
    }
    
    // ==================== DOWNLOAD METHODS ====================
    
    private void downloadHealthMetrics(String userId, HealthMetricDao dao) {
        // TODO: Implement Firestore download
        // Query Firestore for metrics updated after last sync
        // Insert/update in Room
        Log.d(TAG, "Downloading health metrics from Firestore");
    }
    
    private void downloadReminders(String userId, ReminderDao dao) {
        // TODO: Implement Firestore download
        // Query Firestore for reminders updated after last sync
        // Insert/update in Room
        Log.d(TAG, "Downloading reminders from Firestore");
    }
    
    // ==================== CONFLICT RESOLUTION ====================
    
    /**
     * Resolve conflict between local and remote data
     * Default strategy: Last Write Wins
     */
    private <T> T resolveConflict(T localData, T remoteData, String strategy) {
        switch (strategy) {
            case "last_write_wins":
                // Compare timestamps and keep newer one
                // For now, just return remote (Firestore is source of truth)
                return remoteData;
                
            case "local_wins":
                return localData;
                
            case "remote_wins":
                return remoteData;
                
            case "manual":
                // Mark for manual resolution
                // Store both versions for user to choose
                return null;
                
            default:
                return remoteData;
        }
    }
    
    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "DataSyncWorker stopped");
        executor.shutdownNow();
    }
}
