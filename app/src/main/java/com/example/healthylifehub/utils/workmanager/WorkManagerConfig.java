package com.example.healthylifehub.utils.workmanager;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.healthylifehub.workers.PromotionalNotificationWorker;
import com.example.healthylifehub.workers.ReminderUpdateWorker;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager configuration utility for smart reminder system
 */
public class WorkManagerConfig {
    
    private static final String TAG = "WorkManagerConfig";
    
    public static final String TAG_REMINDER_UPDATE = "reminder_update";
    public static final String TAG_REMINDER_ACTION = "reminder_action";
    public static final String TAG_PROMOTIONAL = "promotional_notification";
    public static final String TAG_CRITICAL = "critical";
    
    private static final long INITIAL_BACKOFF_DELAY_MILLIS = 30000;
    
    public static String enqueueReminderUpdate(Context context, String reminderId, 
                                             String actionType, long timestamp, long scheduledTime) {
        
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.e(TAG, "❌ Cannot enqueue update - invalid reminder ID");
            return null;
        }
        
        if (actionType == null || (!actionType.equals("completed") && !actionType.equals("skipped"))) {
            Log.e(TAG, "❌ Cannot enqueue update - invalid action type: " + actionType);
            return null;
        }
        
        try {
            Data inputData = new Data.Builder()
                .putString("reminder_id", reminderId)
                .putString("action_type", actionType)
                .putLong("timestamp", timestamp)
                .putLong("scheduled_time", scheduledTime)
                .build();
            
            Constraints constraints = createReminderUpdateConstraints();
            
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
    
    private static Constraints createReminderUpdateConstraints() {
        return new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(false)
            .build();
    }
    
    public static Constraints createNonCriticalConstraints() {
        return new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(true)
            .build();
    }
    
    public static void cancelReminderWork(Context context, String reminderId) {
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("reminder_" + reminderId);
            Log.d(TAG, "✅ Cancelled all work for reminder: " + reminderId);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel work for reminder: " + reminderId, e);
        }
    }
    
    public static void cancelAllReminderWork(Context context) {
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER_UPDATE);
            Log.d(TAG, "✅ Cancelled all reminder update work");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel all reminder work", e);
        }
    }
    
    public static void logWorkInfo(Context context, String workId) {
        try {
            WorkManager.getInstance(context)
                .getWorkInfoById(java.util.UUID.fromString(workId))
                .addListener(() -> {
                    Log.d(TAG, "Work info requested for ID: " + workId);
                }, context.getMainExecutor());
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to get work info for ID: " + workId, e);
        }
    }
    
    public static void setupPromotionalNotifications(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();
            
            androidx.work.PeriodicWorkRequest workRequest = 
                new androidx.work.PeriodicWorkRequest.Builder(
                    PromotionalNotificationWorker.class,
                    3, TimeUnit.DAYS,
                    1, TimeUnit.HOURS
                )
                .setConstraints(constraints)
                .addTag(TAG_PROMOTIONAL)
                .build();
            
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
    
    public static void cancelPromotionalNotifications(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("promotional_notifications");
            Log.d(TAG, "✅ Cancelled promotional notifications");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel promotional notifications", e);
        }
    }
    
    public static void pruneWork(Context context) {
        try {
            WorkManager.getInstance(context).pruneWork();
            Log.d(TAG, "✅ Pruned completed work entries");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to prune work entries", e);
        }
    }
}
