package com.example.healthylifehub.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.utils.NotificationScheduler;

import java.util.List;

/**
 * Background worker for cleaning up expired reminders
 * Automatically deactivates reminders that have passed their deadline
 * Requirements: 2.4, 7.6
 */
public class DeadlineCleanupWorker extends Worker {
    
    private static final String TAG = "DeadlineCleanupWorker";
    
    public DeadlineCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "🧹 DeadlineCleanupWorker started (Attempt: " + (getRunAttemptCount() + 1) + ")");
        
        try {
            // Get database instance
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            if (database == null) {
                Log.e(TAG, "❌ Failed to get database instance");
                return shouldRetry() ? Result.retry() : Result.failure();
            }
            
            ReminderDao reminderDao = database.reminderDao();
            NotificationScheduler notificationScheduler = new NotificationScheduler(getApplicationContext());
            
            // Get all active reminders to check for expiration
            List<Reminder> allActiveReminders = reminderDao.getAllActiveReminders();
            
            if (allActiveReminders.isEmpty()) {
                Log.d(TAG, "📭 No active reminders found for cleanup");
                return Result.success();
            }
            
            long currentTime = System.currentTimeMillis();
            int totalProcessed = 0;
            int deactivatedCount = 0;
            int notificationsCancelled = 0;
            
            // Process each active reminder
            for (Reminder reminder : allActiveReminders) {
                totalProcessed++;
                
                try {
                    // Check if reminder is expired
                    if (reminder.hasDeadline() && reminder.isExpired()) {
                        
                        // Calculate final progress before deactivation
                        float finalProgress = reminder.getProgressPercentage();
                        
                        Log.d(TAG, "⏰ Processing expired reminder: " + reminder.getTitle() + 
                                  " (Final progress: " + String.format("%.1f", finalProgress) + "%)");
                        
                        // Deactivate the reminder
                        reminder.setActive(false);
                        reminder.setUpdatedAt(currentTime);
                        
                        reminderDao.update(reminder);
                        deactivatedCount++;
                        
                        // Cancel any pending notifications for this reminder
                        notificationScheduler.cancelNotifications(reminder.getReminderId());
                        notificationsCancelled++;
                        
                        Log.d(TAG, "🔒 Deactivated expired reminder: " + reminder.getTitle() + 
                                  " (ID: " + reminder.getReminderId() + ")");
                        
                    } else if (reminder.hasDeadline() && reminder.isCompleted()) {
                        
                        // Cancel notifications for completed reminders
                        notificationScheduler.cancelNotifications(reminder.getReminderId());
                        notificationsCancelled++;
                        
                        Log.d(TAG, "✅ Cancelled notifications for completed reminder: " + reminder.getTitle());
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to process reminder: " + reminder.getReminderId(), e);
                    // Continue processing other reminders
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Log.d(TAG, "✅ DeadlineCleanupWorker completed successfully in " + executionTime + "ms");
            Log.d(TAG, "📊 Cleanup Summary:");
            Log.d(TAG, "  - Total reminders processed: " + totalProcessed);
            Log.d(TAG, "  - Reminders deactivated: " + deactivatedCount);
            Log.d(TAG, "  - Notifications cancelled: " + notificationsCancelled);
            
            return Result.success();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "❌ DeadlineCleanupWorker failed after " + executionTime + "ms", e);
            
            return shouldRetry() ? Result.retry() : Result.failure();
        }
    }
    
    /**
     * Determine if work should be retried based on attempt count
     * 
     * @return true if work should be retried
     */
    private boolean shouldRetry() {
        int maxRetries = 3;
        int currentAttempt = getRunAttemptCount();
        
        if (currentAttempt < maxRetries) {
            Log.d(TAG, "🔄 Scheduling retry... Attempt: " + (currentAttempt + 1) + "/" + maxRetries);
            return true;
        } else {
            Log.e(TAG, "🛑 Max retry attempts (" + maxRetries + ") reached, marking as failure");
            return false;
        }
    }
}