package com.example.healthylifehub.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.utils.reminder.DeadlineWarningNotificationManager;

import java.util.List;

/**
 * Background worker for sending deadline warning notifications
 * Sends notifications 3 days before reminder deadlines
 * Requirements: 2.5
 */
public class DeadlineWarningWorker extends Worker {
    
    private static final String TAG = "DeadlineWarningWorker";
    
    public DeadlineWarningWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "⚠️ DeadlineWarningWorker started (Attempt: " + (getRunAttemptCount() + 1) + ")");
        
        try {
            // Get database instance
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            if (database == null) {
                Log.e(TAG, "❌ Failed to get database instance");
                return shouldRetry() ? Result.retry() : Result.failure();
            }
            
            ReminderDao reminderDao = database.reminderDao();
            DeadlineWarningNotificationManager warningManager = 
                new DeadlineWarningNotificationManager(getApplicationContext());
            
            // Get all active reminders to check for approaching deadlines
            List<Reminder> allActiveReminders = reminderDao.getAllActiveReminders();
            
            if (allActiveReminders.isEmpty()) {
                Log.d(TAG, "📭 No active reminders found for warning check");
                return Result.success();
            }
            
            long currentTime = System.currentTimeMillis();
            long threeDaysFromNow = currentTime + (3 * 24 * 60 * 60 * 1000L);
            
            int totalProcessed = 0;
            int warningsSent = 0;
            
            // Process each active reminder
            for (Reminder reminder : allActiveReminders) {
                totalProcessed++;
                
                try {
                    // Check if reminder needs deadline warning
                    if (needsDeadlineWarning(reminder, currentTime, threeDaysFromNow)) {
                        
                        // Send warning notification
                        boolean sent = warningManager.sendDeadlineWarning(reminder);
                        
                        if (sent) {
                            warningsSent++;
                            Log.d(TAG, "⚠️ Sent deadline warning for: " + reminder.getTitle() + 
                                      " (Deadline: " + new java.util.Date(reminder.getDeadline()) + ")");
                        } else {
                            Log.w(TAG, "⚠️ Failed to send deadline warning for: " + reminder.getTitle());
                        }
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to process reminder for warning: " + reminder.getReminderId(), e);
                    // Continue processing other reminders
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Log.d(TAG, "✅ DeadlineWarningWorker completed successfully in " + executionTime + "ms");
            Log.d(TAG, "📊 Warning Summary:");
            Log.d(TAG, "  - Total reminders processed: " + totalProcessed);
            Log.d(TAG, "  - Warning notifications sent: " + warningsSent);
            
            return Result.success();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "❌ DeadlineWarningWorker failed after " + executionTime + "ms", e);
            
            return shouldRetry() ? Result.retry() : Result.failure();
        }
    }
    
    /**
     * Check if a reminder needs deadline warning
     * Requirements: 2.5
     * 
     * @param reminder The reminder to check
     * @param currentTime Current timestamp
     * @param threeDaysFromNow Timestamp 3 days from now
     * @return true if warning should be sent
     */
    private boolean needsDeadlineWarning(Reminder reminder, long currentTime, long threeDaysFromNow) {
        // Must be active and have a deadline
        if (!reminder.isActive() || !reminder.hasDeadline()) {
            return false;
        }
        
        // Must not be expired or completed
        if (reminder.isExpired() || reminder.isCompleted()) {
            return false;
        }
        
        long deadline = reminder.getDeadline();
        
        // Deadline must be within the next 3 days
        if (deadline > threeDaysFromNow) {
            return false;
        }
        
        // Deadline must not have passed
        if (deadline <= currentTime) {
            return false;
        }
        
        // Check if we've already sent a warning recently (within last 12 hours)
        // This prevents spam notifications
        return !hasRecentWarning(reminder.getReminderId());
    }
    
    /**
     * Check if we've sent a warning for this reminder recently
     * This prevents spam notifications
     * 
     * @param reminderId The reminder ID
     * @return true if warning was sent recently
     */
    private boolean hasRecentWarning(String reminderId) {
        // For now, we'll implement a simple check
        // In a full implementation, you might store warning timestamps in SharedPreferences
        // or a separate database table
        
        // TODO: Implement proper warning tracking to prevent spam
        // For now, assume no recent warnings to ensure warnings are sent
        return false;
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