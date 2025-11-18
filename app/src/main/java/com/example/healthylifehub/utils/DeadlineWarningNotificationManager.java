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
import com.example.healthylifehub.ui.reminders.detail.ReminderDetailActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages deadline warning notifications
 * Sends notifications when reminders are approaching their deadline
 * Requirements: 2.5
 */
public class DeadlineWarningNotificationManager {
    
    private static final String TAG = "DeadlineWarningNotificationManager";
    
    // Notification channel for deadline warnings
    private static final String CHANNEL_ID = "deadline_warnings";
    private static final String CHANNEL_NAME = "Deadline Warnings";
    private static final String CHANNEL_DESCRIPTION = "Notifications for reminders approaching their deadline";
    
    // Notification ID base for deadline warnings
    private static final int NOTIFICATION_ID_BASE = 2000;
    
    private final Context context;
    private final NotificationManagerCompat notificationManager;
    
    public DeadlineWarningNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(context);
        
        createNotificationChannel();
    }
    
    /**
     * Create notification channel for deadline warnings (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.YELLOW);
            
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Created deadline warning notification channel");
            }
        }
    }
    
    /**
     * Send deadline warning notification for a reminder
     * Requirements: 2.5
     * 
     * @param reminder The reminder approaching deadline
     * @return true if notification was sent successfully
     */
    public boolean sendDeadlineWarning(Reminder reminder) {
        if (reminder == null || !reminder.hasDeadline()) {
            Log.w(TAG, "⚠️ Cannot send deadline warning - invalid reminder");
            return false;
        }
        
        try {
            // Calculate time remaining
            long currentTime = System.currentTimeMillis();
            long deadline = reminder.getDeadline();
            long timeRemaining = deadline - currentTime;
            
            if (timeRemaining <= 0) {
                Log.w(TAG, "⚠️ Cannot send deadline warning - reminder already expired");
                return false;
            }
            
            // Format deadline warning message
            String timeRemainingText = formatTimeRemaining(timeRemaining);
            String deadlineText = formatDeadline(deadline);
            
            // Calculate current progress
            float progress = reminder.getProgressPercentage();
            String progressText = String.format("%.1f%% hoàn thành", progress);
            
            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("⚠️ Nhắc nhở sắp hết hạn")
                    .setContentText(reminder.getTitle() + " - " + timeRemainingText)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(reminder.getTitle() + "\n" +
                                   "Hết hạn: " + deadlineText + "\n" +
                                   "Còn lại: " + timeRemainingText + "\n" +
                                   "Tiến độ: " + progressText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 250, 250, 250})
                    .setLights(android.graphics.Color.YELLOW, 1000, 1000);
            
            // Add action to open reminder detail
            Intent detailIntent = new Intent(context, ReminderDetailActivity.class);
            detailIntent.putExtra("reminder_id", reminder.getReminderId());
            detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent detailPendingIntent = PendingIntent.getActivity(
                    context,
                    reminder.getReminderId().hashCode(),
                    detailIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            builder.setContentIntent(detailPendingIntent);
            
            // Add action button to extend deadline
            Intent extendIntent = new Intent(context, com.example.healthylifehub.receivers.DeadlineExtensionReceiver.class);
            extendIntent.setAction("EXTEND_DEADLINE");
            extendIntent.putExtra("reminder_id", reminder.getReminderId());
            
            PendingIntent extendPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (reminder.getReminderId() + "_extend").hashCode(),
                    extendIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            builder.addAction(android.R.drawable.ic_menu_recent_history, "Gia hạn", extendPendingIntent);
            
            // Generate unique notification ID
            int notificationId = NOTIFICATION_ID_BASE + reminder.getReminderId().hashCode();
            
            // Send notification with permission check
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                } else {
                    Log.w(TAG, "⚠️ Notification permission not granted");
                    return false;
                }
            } else {
                notificationManager.notify(notificationId, builder.build());
            }
            
            Log.d(TAG, "✅ Sent deadline warning notification for: " + reminder.getTitle() + 
                      " (Time remaining: " + timeRemainingText + ")");
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send deadline warning notification", e);
            return false;
        }
    }
    
    /**
     * Format time remaining in a human-readable way
     * 
     * @param timeRemaining Time remaining in milliseconds
     * @return Formatted time string
     */
    private String formatTimeRemaining(long timeRemaining) {
        long days = timeRemaining / (24 * 60 * 60 * 1000L);
        long hours = (timeRemaining % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);
        
        if (days > 0) {
            if (hours > 0) {
                return String.format("%d ngày %d giờ", days, hours);
            } else {
                return String.format("%d ngày", days);
            }
        } else if (hours > 0) {
            long minutes = (timeRemaining % (60 * 60 * 1000L)) / (60 * 1000L);
            if (minutes > 0) {
                return String.format("%d giờ %d phút", hours, minutes);
            } else {
                return String.format("%d giờ", hours);
            }
        } else {
            long minutes = timeRemaining / (60 * 1000L);
            return String.format("%d phút", Math.max(1, minutes));
        }
    }
    
    /**
     * Format deadline date and time
     * 
     * @param deadline Deadline timestamp
     * @return Formatted deadline string
     */
    private String formatDeadline(long deadline) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(deadline));
    }
    
    /**
     * Cancel deadline warning notification for a reminder
     * 
     * @param reminderId The reminder ID
     */
    public void cancelDeadlineWarning(String reminderId) {
        if (reminderId == null || reminderId.trim().isEmpty()) {
            return;
        }
        
        try {
            int notificationId = NOTIFICATION_ID_BASE + reminderId.hashCode();
            notificationManager.cancel(notificationId);
            
            Log.d(TAG, "🚫 Cancelled deadline warning notification for: " + reminderId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel deadline warning notification", e);
        }
    }
    
    /**
     * Check if notifications are enabled for this app
     * 
     * @return true if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        return notificationManager.areNotificationsEnabled();
    }
    
    /**
     * Get the importance level of the deadline warning channel
     * 
     * @return Importance level (Android 8.0+)
     */
    public int getChannelImportance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
                if (channel != null) {
                    return channel.getImportance();
                }
            }
        }
        return NotificationManager.IMPORTANCE_DEFAULT;
    }
}