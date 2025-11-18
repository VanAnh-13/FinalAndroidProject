package com.example.healthylifehub.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.receivers.ReminderActionReceiver;

/**
 * SmartNotificationManager for interactive reminder notifications
 * Supports action buttons for completing and skipping reminders
 * Requirements: 1.1, 1.4, 8.1
 */
public class SmartNotificationManager {
    
    private static final String TAG = "SmartNotificationManager";
    
    // Smart Reminder Channel
    private static final String CHANNEL_SMART_REMINDERS = "smart_reminder_channel";
    private static final String CHANNEL_SMART_REMINDERS_NAME = "Nhắc nhở thông minh";
    private static final String CHANNEL_SMART_REMINDERS_DESC = "Thông báo nhắc nhở với khả năng tương tác trực tiếp";
    
    // Action constants
    public static final String ACTION_COMPLETE = "ACTION_COMPLETE_REMINDER";
    public static final String ACTION_SKIP = "ACTION_SKIP_REMINDER";
    
    private final Context context;
    private final NotificationManagerCompat notificationManager;
    
    public SmartNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(this.context);
        
        // Initialize logger
        ReminderLogger.initialize(this.context);
        
        createNotificationChannel();
    }
    
    /**
     * Create notification channel for smart reminders with proper importance level
     * Requirements: 1.1, 8.1
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_SMART_REMINDERS,
                    CHANNEL_SMART_REMINDERS_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                );
                
                channel.setDescription(CHANNEL_SMART_REMINDERS_DESC);
                channel.enableVibration(true);
                channel.enableLights(true);
                channel.setVibrationPattern(new long[]{0, 500, 200, 500});
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                
                manager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Created smart reminder notification channel");
            }
        }
    }
    
    /**
     * Send interactive notification with action buttons and comprehensive error handling
     * Requirements: 1.1, 1.4, 8.1, 8.5
     * 
     * @param reminder The reminder object
     * @param notificationId Unique notification ID
     */
    public void sendInteractiveNotification(Reminder reminder, int notificationId) {
        // Validate input parameters
        if (reminder == null) {
            ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                "Null reminder passed to sendInteractiveNotification", 
                "NotificationId: " + notificationId, null);
            
            ErrorHandler.handleValidationError(context, 
                ErrorHandler.ERROR_REMINDER_NOT_FOUND, "reminder");
            return;
        }
        
        if (reminder.getReminderId() == null || reminder.getReminderId().trim().isEmpty()) {
            ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                "Invalid reminder ID in notification", 
                "Title: " + reminder.getTitle() + ", NotificationId: " + notificationId, null);
            
            ErrorHandler.handleValidationError(context, 
                ErrorHandler.ERROR_REMINDER_NOT_FOUND, "reminder_id");
            return;
        }
        
        // Check notification permission with comprehensive error handling
        if (!PermissionManager.isNotificationPermissionGranted(context)) {
            ReminderLogger.logWarning(ReminderLogger.LogCategory.NOTIFICATION, 
                "Notification permission not granted", 
                "ReminderId: " + reminder.getReminderId());
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_PERMISSION_DENIED, null, 
                reminder.getReminderId(), false);
            return;
        }
        
        // Log notification attempt
        ReminderLogger.logNotificationEvent("sending", reminder.getReminderId(), 
            "Title: " + reminder.getTitle() + ", NotificationId: " + notificationId);
        
        try {
            // Validate notification manager
            if (!areNotificationsEnabled()) {
                ReminderLogger.logWarning(ReminderLogger.LogCategory.NOTIFICATION, 
                    "Notifications are disabled by user", 
                    "ReminderId: " + reminder.getReminderId());
                
                ErrorHandler.handleNotificationError(context, 
                    ErrorHandler.ERROR_NOTIFICATION_PERMISSION_DENIED, null, 
                    reminder.getReminderId(), false);
                return;
            }
            
            // Build notification with error handling for each component
            NotificationCompat.Builder builder = createNotificationBuilder(reminder, notificationId);
            if (builder == null) {
                return; // Error already logged in createNotificationBuilder
            }
            
            // Send notification with retry capability
            sendNotificationWithRetry(builder, notificationId, reminder.getReminderId(), 0);
            
        } catch (SecurityException e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Security exception sending notification", 
                "ReminderId: " + reminder.getReminderId() + ", NotificationId: " + notificationId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_PERMISSION_DENIED, e, 
                reminder.getReminderId(), false);
                
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Unexpected error sending notification", 
                "ReminderId: " + reminder.getReminderId() + ", NotificationId: " + notificationId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_SEND_FAILED, e, 
                reminder.getReminderId(), true);
        }
    }
    
    /**
     * Create complete action with PendingIntent
     * Requirements: 1.2, 8.1
     * 
     * @param reminderId The reminder ID
     * @param notificationId The notification ID
     * @return NotificationCompat.Action for completing reminder
     */
    private NotificationCompat.Action createCompleteAction(String reminderId, int notificationId) {
        Intent intent = new Intent(context, ReminderActionReceiver.class);
        intent.setAction(ACTION_COMPLETE);
        intent.putExtra("reminder_id", reminderId);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("timestamp", System.currentTimeMillis());
        
        // Use unique request code to avoid PendingIntent conflicts
        int requestCode = (reminderId + "_complete").hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Action.Builder(
            R.drawable.ic_check_circle,
            "Hoàn thành",
            pendingIntent
        ).build();
    }
    
    /**
     * Create skip action with PendingIntent
     * Requirements: 1.3, 8.1
     * 
     * @param reminderId The reminder ID
     * @param notificationId The notification ID
     * @return NotificationCompat.Action for skipping reminder
     */
    private NotificationCompat.Action createSkipAction(String reminderId, int notificationId) {
        Intent intent = new Intent(context, ReminderActionReceiver.class);
        intent.setAction(ACTION_SKIP);
        intent.putExtra("reminder_id", reminderId);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("timestamp", System.currentTimeMillis());
        
        // Use unique request code to avoid PendingIntent conflicts
        int requestCode = (reminderId + "_skip").hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Action.Builder(
            R.drawable.ic_close,
            "Bỏ qua",
            pendingIntent
        ).build();
    }
    
    /**
     * Get notification text based on reminder content
     * 
     * @param reminder The reminder object
     * @return Formatted notification text
     */
    private String getNotificationText(Reminder reminder) {
        if (reminder.getDescription() != null && !reminder.getDescription().trim().isEmpty()) {
            return reminder.getDescription();
        }
        
        // Default text based on reminder type
        if (reminder.getMedicineId() != null) {
            return "Đã đến giờ uống thuốc!";
        } else {
            return "Đã đến giờ nhắc nhở sức khỏe!";
        }
    }
    
    /**
     * Create notification builder with comprehensive error handling
     * Requirements: 1.1, 8.5
     * 
     * @param reminder The reminder object
     * @param notificationId The notification ID
     * @return NotificationCompat.Builder or null if error occurred
     */
    private NotificationCompat.Builder createNotificationBuilder(Reminder reminder, int notificationId) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SMART_REMINDERS)
                .setSmallIcon(R.drawable.ic_notifications_active)
                .setContentTitle(reminder.getTitle())
                .setContentText(getNotificationText(reminder))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false) // Don't auto-dismiss, let actions handle it
                .setOngoing(false)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getNotificationText(reminder)));
            
            // Add action buttons with error handling
            try {
                NotificationCompat.Action completeAction = createCompleteAction(reminder.getReminderId(), notificationId);
                NotificationCompat.Action skipAction = createSkipAction(reminder.getReminderId(), notificationId);
                
                if (completeAction != null && skipAction != null) {
                    builder.addAction(completeAction);
                    builder.addAction(skipAction);
                } else {
                    ReminderLogger.logWarning(ReminderLogger.LogCategory.NOTIFICATION, 
                        "Failed to create action buttons", 
                        "ReminderId: " + reminder.getReminderId());
                }
            } catch (Exception e) {
                ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                    "Error creating action buttons", 
                    "ReminderId: " + reminder.getReminderId(), e);
                // Continue without action buttons rather than failing completely
            }
            
            // Add progress info if reminder has deadline
            try {
                if (reminder.hasDeadline()) {
                    String progressText = String.format("Tiến độ: %d/%d (%.0f%%)", 
                        reminder.getCompletedCount(), 
                        reminder.getTotalExpected(), 
                        reminder.getProgressPercentage());
                    builder.setSubText(progressText);
                }
            } catch (Exception e) {
                ReminderLogger.logWarning(ReminderLogger.LogCategory.NOTIFICATION, 
                    "Error adding progress info", 
                    "ReminderId: " + reminder.getReminderId() + ", Error: " + e.getMessage());
                // Continue without progress info
            }
            
            return builder;
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Failed to create notification builder", 
                "ReminderId: " + reminder.getReminderId(), e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_SEND_FAILED, e, 
                reminder.getReminderId(), true);
            
            return null;
        }
    }
    
    /**
     * Send notification with retry capability
     * Requirements: 8.5
     * 
     * @param builder The notification builder
     * @param notificationId The notification ID
     * @param reminderId The reminder ID
     * @param retryCount Current retry count
     */
    private void sendNotificationWithRetry(NotificationCompat.Builder builder, int notificationId, 
                                         String reminderId, int retryCount) {
        final int MAX_RETRIES = 2;
        
        try {
            notificationManager.notify(notificationId, builder.build());
            
            // Log successful notification
            ReminderLogger.logNotificationEvent("sent", reminderId, 
                "NotificationId: " + notificationId + ", Attempt: " + (retryCount + 1));
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Failed to send notification", 
                String.format("ReminderId: %s, NotificationId: %d, Attempt: %d", 
                    reminderId, notificationId, retryCount + 1), e);
            
            if (retryCount < MAX_RETRIES && RetryManager.isRetryableException(e)) {
                // Schedule retry with delay
                ReminderLogger.logRetryAttempt(
                    "notification_" + reminderId, retryCount + 1, MAX_RETRIES + 1, false, 
                    "Scheduling notification retry");
                
                // Simple retry mechanism - in production, you might use a more sophisticated approach
                try {
                    Thread.sleep(1000 * (retryCount + 1)); // Exponential backoff
                    sendNotificationWithRetry(builder, notificationId, reminderId, retryCount + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                        "Notification retry interrupted", "ReminderId: " + reminderId, ie);
                }
            } else {
                // Max retries exceeded or non-retryable error
                ErrorHandler.handleNotificationError(context, 
                    ErrorHandler.ERROR_NOTIFICATION_SEND_FAILED, e, reminderId, false);
            }
        }
    }
    
    /**
     * Cancel notification by ID with comprehensive error handling
     * Requirements: 1.4, 8.5
     * 
     * @param notificationId The notification ID to cancel
     */
    public void cancelNotification(int notificationId) {
        try {
            notificationManager.cancel(notificationId);
            
            ReminderLogger.logNotificationEvent("cancelled", String.valueOf(notificationId), 
                "NotificationId: " + notificationId);
            
        } catch (SecurityException e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Security exception cancelling notification", 
                "NotificationId: " + notificationId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_PERMISSION_DENIED, e, 
                String.valueOf(notificationId), false);
                
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Unexpected error cancelling notification", 
                "NotificationId: " + notificationId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, e, 
                String.valueOf(notificationId), false);
        }
    }
    
    /**
     * Cancel all smart reminder notifications
     */
    public void cancelAllNotifications() {
        try {
            notificationManager.cancelAll();
            Log.d(TAG, "✅ Cancelled all smart reminder notifications");
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Failed to cancel all notifications - permission denied", e);
        }
    }
    
    /**
     * Check if notifications are enabled
     * 
     * @return true if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        return notificationManager.areNotificationsEnabled();
    }
    
    /**
     * Get notification channel ID for smart reminders
     * 
     * @return Channel ID string
     */
    public static String getChannelId() {
        return CHANNEL_SMART_REMINDERS;
    }
}