package com.example.healthylifehub.workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.utils.notification.NotificationScheduler;
import com.example.healthylifehub.utils.app.ProgressCalculator;
import com.example.healthylifehub.utils.reminder.DeadlineWarningNotificationManager;

/**
 * Background worker for extending reminder deadlines
 * Requirements: 2.4 (deadline extension functionality)
 */
public class DeadlineExtensionWorker extends Worker {
    
    private static final String TAG = "DeadlineExtensionWorker";
    
    // Broadcast action for UI updates
    public static final String ACTION_DEADLINE_EXTENDED = "com.example.healthylifehub.DEADLINE_EXTENDED";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_NEW_DEADLINE = "new_deadline";
    public static final String EXTRA_EXTENSION_DAYS = "extension_days";
    
    public DeadlineExtensionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "⏰ DeadlineExtensionWorker started (Attempt: " + (getRunAttemptCount() + 1) + ")");
        
        // Get input data
        String reminderId = getInputData().getString("reminder_id");
        long newDeadline = getInputData().getLong("new_deadline", 0);
        long extensionDays = getInputData().getLong("extension_days", 7);
        
        // Validate input data
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.e(TAG, "❌ Invalid reminder ID - cannot proceed");
            return Result.failure();
        }
        
        if (newDeadline <= System.currentTimeMillis()) {
            Log.e(TAG, "❌ Invalid new deadline - must be in the future");
            return Result.failure();
        }
        
        Log.d(TAG, "📝 Processing deadline extension for reminder: " + reminderId);
        Log.d(TAG, "⏰ New deadline: " + new java.util.Date(newDeadline) + 
                  " (Extension: " + extensionDays + " days)");
        
        try {
            // Get database instance
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            if (database == null) {
                Log.e(TAG, "❌ Failed to get database instance");
                return shouldRetry() ? Result.retry() : Result.failure();
            }
            
            ReminderDao reminderDao = database.reminderDao();
            
            // Get the reminder
            Reminder reminder = reminderDao.getReminderById(reminderId);
            if (reminder == null) {
                Log.e(TAG, "❌ Reminder not found: " + reminderId);
                return Result.failure();
            }
            
            // Store old deadline for logging
            Long oldDeadline = reminder.getDeadline();
            
            // Update reminder with new deadline
            reminder.setDeadline(newDeadline);
            reminder.setUpdatedAt(System.currentTimeMillis());
            
            // Recalculate total expected based on new deadline
            ProgressCalculator progressCalculator = new ProgressCalculator();
            int newTotalExpected = progressCalculator.calculateTotalExpected(reminder);
            reminder.setTotalExpected(newTotalExpected);
            
            // Update reminder in database
            reminderDao.update(reminder);
            
            Log.d(TAG, "✅ Updated reminder deadline: " + reminder.getTitle());
            Log.d(TAG, "  - Old deadline: " + (oldDeadline != null ? new java.util.Date(oldDeadline) : "None"));
            Log.d(TAG, "  - New deadline: " + new java.util.Date(newDeadline));
            Log.d(TAG, "  - New total expected: " + newTotalExpected);
            
            // Reschedule notifications with new deadline
            NotificationScheduler notificationScheduler = new NotificationScheduler(getApplicationContext());
            int scheduledCount = notificationScheduler.rescheduleNotifications(reminder);
            
            Log.d(TAG, "✅ Rescheduled " + scheduledCount + " notifications for extended deadline");
            
            // Cancel any existing deadline warning notification
            DeadlineWarningNotificationManager warningManager = 
                new DeadlineWarningNotificationManager(getApplicationContext());
            warningManager.cancelDeadlineWarning(reminderId);
            
            // Send success notification to user
            sendExtensionSuccessNotification(reminder, extensionDays);
            
            // Send local broadcast to update UI
            sendUpdateBroadcast(reminderId, newDeadline, extensionDays);
            
            long executionTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "✅ DeadlineExtensionWorker completed successfully in " + executionTime + "ms");
            
            return Result.success();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "❌ DeadlineExtensionWorker failed after " + executionTime + "ms", e);
            
            return shouldRetry() ? Result.retry() : Result.failure();
        }
    }
    
    /**
     * Send success notification to user about deadline extension
     * 
     * @param reminder The reminder with extended deadline
     * @param extensionDays Number of days extended
     */
    private void sendExtensionSuccessNotification(Reminder reminder, long extensionDays) {
        try {
            // Use the existing notification system to show success
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                androidx.core.app.NotificationCompat.Builder builder = 
                    new androidx.core.app.NotificationCompat.Builder(getApplicationContext(), "smart_reminders")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("✅ Đã gia hạn thành công")
                        .setContentText(reminder.getTitle() + " đã được gia hạn " + extensionDays + " ngày")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);
                
                // Generate unique notification ID
                int notificationId = 3000 + reminder.getReminderId().hashCode();
                notificationManager.notify(notificationId, builder.build());
                
                Log.d(TAG, "✅ Sent extension success notification");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send extension success notification", e);
            // Don't fail the work for this
        }
    }
    
    /**
     * Send local broadcast for UI updates
     * 
     * @param reminderId The reminder ID
     * @param newDeadline The new deadline
     * @param extensionDays Number of days extended
     */
    private void sendUpdateBroadcast(String reminderId, long newDeadline, long extensionDays) {
        try {
            Intent updateIntent = new Intent(ACTION_DEADLINE_EXTENDED);
            updateIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
            updateIntent.putExtra(EXTRA_NEW_DEADLINE, newDeadline);
            updateIntent.putExtra(EXTRA_EXTENSION_DAYS, extensionDays);
            
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(updateIntent);
            Log.d(TAG, "✅ Sent deadline extension broadcast for reminder: " + reminderId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send deadline extension broadcast", e);
            // Don't throw - this is not critical for the main operation
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