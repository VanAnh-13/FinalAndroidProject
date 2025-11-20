package com.example.healthylifehub.utils;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.healthylifehub.workers.PromotionalNotificationWorker;
import com.example.healthylifehub.workers.ReminderUpdateWorker;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager configuration utility for smart reminder system
 * Provides standardized work request creation with proper constraints and policies
 * Requirements: 1.5, 8.5
 */
public class WorkManagerConfig {
    
    private static final String TAG = "WorkManagerConfig";
    
    // Work tags for identification and management
    public static final String TAG_REMINDER_UPDATE = "reminder_update";
    public static final String TAG_REMINDER_ACTION = "reminder_action";
    public static final String TAG_PROMOTIONAL = "promotional_notification";
    public static final String TAG_CRITICAL = "critical";
    
    // Retry policies
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_DELAY_MILLIS = 30000; // 30 seconds
    
    /**
     * Create and enqueue a reminder update work request with optimal constraints
     * Requirements: 1.5, 8.5
     * 
     * @param context Application context
     * @param reminderId The reminder ID to update
     * @param actionType "completed" or "skipped"
     * @param timestamp Action timestamp
     * @param scheduledTime Originally scheduled time
     * @return Work request ID for tracking
     */
    public static String enqueueReminderUpdate(Context context, String reminderId, 
                                             String actionType, long timestamp, long scheduledTime) {
        
        // Validate input parameters
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.e(TAG, "❌ Cannot enqueue update - invalid reminder ID");
            return null;
        }
        
        if (actionType == null || (!actionType.equals("completed") && !actionType.equals("skipped"))) {
            Log.e(TAG, "❌ Cannot enqueue update - invalid action type: " + actionType);
            return null;
        }
        
        try {
            // Create input data
            Data inputData = new Data.Builder()
                .putString("reminder_id", reminderId)
                .putString("action_type", actionType)
                .putLong("timestamp", timestamp)
                .putLong("scheduled_time", scheduledTime)
                .build();
            
            // Create constraints for optimal execution
            Constraints constraints = createReminderUpdateConstraints();
            
            // Create work request with retry policy
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ReminderUpdateWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG_REMINDER_UPDATE)
                .addTag(TAG_REMINDER_ACTION)
                .addTag(TAG_CRITICAL)
                .addTag("reminder_" + reminderId)
                .build();
            
            // Enqueue the work
            WorkManager.getInstance(context).enqueue(workRequest);
            
            String workId = workRequest.getId().toString();
            Log.d(TAG, "✅ Enqueued ReminderUpdateWorker with ID: " + workId + 
                      " for " + actionType + " action on reminder: " + reminderId);
            
            return workId;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to enqueue reminder update work", e);
            return null;
        }
    }
    
    /**
     * Create optimized constraints for reminder update operations
     * Requirements: 8.5
     * 
     * @return Constraints for reminder update work
     */
    private static Constraints createReminderUpdateConstraints() {
        return new Constraints.Builder()
            // Don't require network - database operations are local
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            // Don't require charging - quick database operations
            .setRequiresCharging(false)
            // Allow on low battery - critical user actions should be processed
            .setRequiresBatteryNotLow(false)
            // Don't require device idle - user expects immediate response
            .setRequiresDeviceIdle(false)
            // Don't require storage not low - minimal storage impact
            .setRequiresStorageNotLow(false)
            .build();
    }
    
    /**
     * Create constraints for non-critical background operations
     * 
     * @return Constraints for non-critical work
     */
    public static Constraints createNonCriticalConstraints() {
        return new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(true)
            .build();
    }
    
    /**
     * Cancel all pending reminder update work for a specific reminder
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     */
    public static void cancelReminderWork(Context context, String reminderId) {
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("reminder_" + reminderId);
            Log.d(TAG, "✅ Cancelled all work for reminder: " + reminderId);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel work for reminder: " + reminderId, e);
        }
    }
    
    /**
     * Cancel all reminder update work
     * 
     * @param context Application context
     */
    public static void cancelAllReminderWork(Context context) {
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER_UPDATE);
            Log.d(TAG, "✅ Cancelled all reminder update work");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel all reminder work", e);
        }
    }
    
    /**
     * Get work info for debugging and monitoring
     * 
     * @param context Application context
     * @param workId Work request ID
     */
    public static void logWorkInfo(Context context, String workId) {
        try {
            WorkManager.getInstance(context)
                .getWorkInfoById(java.util.UUID.fromString(workId))
                .addListener(() -> {
                    // Work info logging handled by WorkManager internally
                    Log.d(TAG, "Work info requested for ID: " + workId);
                }, context.getMainExecutor());
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to get work info for ID: " + workId, e);
        }
    }
    
    /**
     * Setup periodic promotional notifications (every 3 days)
     * 
     * @param context Application context
     */
    public static void setupPromotionalNotifications(Context context) {
        try {
            // Create constraints - only when device is idle and battery not low
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();
            
            // Create periodic work request (every 3 days)
            androidx.work.PeriodicWorkRequest workRequest = 
                new androidx.work.PeriodicWorkRequest.Builder(
                    PromotionalNotificationWorker.class,
                    3, TimeUnit.DAYS,
                    1, TimeUnit.HOURS // Flex interval
                )
                .setConstraints(constraints)
                .addTag(TAG_PROMOTIONAL)
                .build();
            
            // Enqueue with replace policy to avoid duplicates
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "promotional_notifications",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            );
            
            Log.d(TAG, "✅ Setup promotional notifications (every 3 days)");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to setup promotional notifications", e);
        }
    }
    
    /**
     * Cancel promotional notifications
     * 
     * @param context Application context
     */
    public static void cancelPromotionalNotifications(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("promotional_notifications");
            Log.d(TAG, "✅ Cancelled promotional notifications");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel promotional notifications", e);
        }
    }
    
    /**
     * Prune completed work to prevent database bloat
     * Should be called periodically (e.g., on app startup)
     * 
     * @param context Application context
     */
    public static void pruneWork(Context context) {
        try {
            WorkManager.getInstance(context).pruneWork();
            Log.d(TAG, "✅ Pruned completed work entries");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to prune work entries", e);
        }
    }
}