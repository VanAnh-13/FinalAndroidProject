package com.example.healthylifehub.sync;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.healthylifehub.workers.DataSyncWorker;

import java.util.concurrent.TimeUnit;

/**
 * Sync Manager
 * Manages data synchronization scheduling and execution
 */
public class SyncManager {
    
    private static final String TAG = "SyncManager";
    private static final String PERIODIC_SYNC_WORK = "periodic_data_sync";
    private static final String IMMEDIATE_SYNC_WORK = "immediate_data_sync";
    
    // Sync intervals
    private static final long SYNC_INTERVAL_MINUTES = 15;
    private static final long SYNC_FLEX_MINUTES = 5;
    
    private final Context context;
    private final WorkManager workManager;
    
    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.workManager = WorkManager.getInstance(this.context);
    }
    
    /**
     * Schedule periodic background sync
     * Runs every 15 minutes when network is available
     */
    public void schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic sync");
        
        // Constraints: require network connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)  // Don't sync when battery is low
                .build();
        
        // Create periodic work request
        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                DataSyncWorker.class,
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
                SYNC_FLEX_MINUTES,
                TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .addTag("data_sync")
        .build();
        
        // Enqueue with KEEP policy (don't replace if already scheduled)
        workManager.enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
        );
        
        Log.d(TAG, "Periodic sync scheduled successfully");
    }
    
    /**
     * Trigger immediate sync
     * Useful when user makes changes or pulls to refresh
     */
    public void triggerImmediateSync() {
        Log.d(TAG, "Triggering immediate sync");
        
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(DataSyncWorker.class)
                .setConstraints(constraints)
                .addTag("immediate_sync")
                .build();
        
        workManager.enqueue(syncWorkRequest);
        
        Log.d(TAG, "Immediate sync triggered");
    }
    
    /**
     * Trigger sync on WiFi only (for large data)
     */
    public void triggerWiFiSync() {
        Log.d(TAG, "Triggering WiFi-only sync");
        
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)  // WiFi only
                .build();
        
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(DataSyncWorker.class)
                .setConstraints(constraints)
                .addTag("wifi_sync")
                .build();
        
        workManager.enqueue(syncWorkRequest);
    }
    
    /**
     * Cancel all sync work
     */
    public void cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work");
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK);
        workManager.cancelAllWorkByTag("data_sync");
        workManager.cancelAllWorkByTag("immediate_sync");
    }
    
    /**
     * Cancel periodic sync only
     */
    public void cancelPeriodicSync() {
        Log.d(TAG, "Cancelling periodic sync");
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK);
    }
}
