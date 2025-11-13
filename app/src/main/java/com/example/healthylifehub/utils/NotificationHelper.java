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
import com.example.healthylifehub.ui.reminders.detail.ReminderDetailActivity;
import com.example.healthylifehub.MainActivity;
import com.example.healthylifehub.receivers.ReminderReceiver;

/**
 * Enhanced NotificationHelper with multiple channels and permission checking
 * Follows project pattern: Centralized notification management
 */
public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    
    // Notification Channels
    private static final String CHANNEL_REMINDERS = "reminder_channel";
    private static final String CHANNEL_HEALTH_ALERTS = "health_alerts_channel"; 
    private static final String CHANNEL_SUGGESTIONS = "suggestions_channel";
    private static final String CHANNEL_GENERAL = "general_channel";
    
    // Channel Names
    private static final String CHANNEL_REMINDERS_NAME = "Nhắc nhở sức khỏe";
    private static final String CHANNEL_HEALTH_ALERTS_NAME = "Cảnh báo sức khỏe";
    private static final String CHANNEL_SUGGESTIONS_NAME = "Gợi ý thông minh";
    private static final String CHANNEL_GENERAL_NAME = "Thông báo chung";
    
    // Channel Descriptions
    private static final String CHANNEL_REMINDERS_DESC = "Thông báo nhắc nhở uống thuốc và kiểm tra sức khỏe";
    private static final String CHANNEL_HEALTH_ALERTS_DESC = "Cảnh báo bất thường về chỉ số sức khỏe";
    private static final String CHANNEL_SUGGESTIONS_DESC = "Gợi ý cải thiện thói quen sức khỏe";
    private static final String CHANNEL_GENERAL_DESC = "Thông báo chung của ứng dụng";
    
    /**
     * Create all notification channels
     * @param context Application context
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                createReminderChannel(manager);
                createHealthAlertsChannel(manager);
                createSuggestionsChannel(manager);
                createGeneralChannel(manager);
                Log.d(TAG, "✅ Created all notification channels");
            }
        }
    }
    
    private static void createReminderChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_REMINDERS,
                CHANNEL_REMINDERS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_REMINDERS_DESC);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            manager.createNotificationChannel(channel);
        }
    }
    
    private static void createHealthAlertsChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_HEALTH_ALERTS,
                CHANNEL_HEALTH_ALERTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_HEALTH_ALERTS_DESC);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000}); // More urgent pattern
            manager.createNotificationChannel(channel);
        }
    }
    
    private static void createSuggestionsChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_SUGGESTIONS,
                CHANNEL_SUGGESTIONS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_SUGGESTIONS_DESC);
            channel.enableVibration(false); // Less intrusive
            channel.enableLights(true);
            manager.createNotificationChannel(channel);
        }
    }
    
    private static void createGeneralChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_GENERAL,
                CHANNEL_GENERAL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_GENERAL_DESC);
            channel.enableVibration(true);
            channel.enableLights(true);
            manager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Show reminder notification with enhanced features
     * @param context Application context
     * @param reminderId Reminder ID
     * @param title Notification title
     * @param description Notification description
     * @param notificationId Unique notification ID
     */
    public static void showReminderNotification(
            Context context,
            String reminderId,
            String title,
            String description,
            int notificationId
    ) {
        // Check permission first
        if (!PermissionManager.isNotificationPermissionGranted(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted, cannot show notification");
            return;
        }
        
        createNotificationChannel(context);
        
        // Main intent - open ReminderDetailActivity
        Intent intent = new Intent(context, ReminderDetailActivity.class);
        intent.putExtra("reminderId", reminderId);
        intent.putExtra("reminderTitle", title);
        intent.putExtra("reminderDescription", description);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Complete action intent
        Intent completeIntent = new Intent(context, ReminderReceiver.class);
        completeIntent.setAction("COMPLETE_REMINDER");
        completeIntent.putExtra("reminderId", reminderId);
        completeIntent.putExtra("notificationId", notificationId);
        
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000, // Unique request code
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Snooze action intent  
        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.setAction("SNOOZE_REMINDER");
        snoozeIntent.putExtra("reminderId", reminderId);
        snoozeIntent.putExtra("notificationId", notificationId);
        
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2000, // Unique request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle(title)
            .setContentText(description != null && !description.isEmpty() ? description : "Đã đến giờ nhắc nhở!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Add action buttons
            .addAction(R.drawable.ic_check, "Hoàn thành", completePendingIntent)
            .addAction(R.drawable.ic_time, "Báo lại (10p)", snoozePendingIntent);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "✅ Showed reminder notification: " + title);
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Failed to show notification - permission denied", e);
        }
    }
    
    /**
     * Show health alert notification (for anomalies, critical values)
     * @param context Application context
     * @param title Alert title
     * @param message Alert message
     * @param notificationId Unique notification ID
     */
    public static void showHealthAlertNotification(Context context, String title, String message, int notificationId) {
        if (!PermissionManager.isNotificationPermissionGranted(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted, cannot show health alert");
            return;
        }
        
        createNotificationChannel(context);
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_HEALTH_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.d(TAG, "✅ Showed health alert: " + title);
    }
    
    /**
     * Show suggestion notification (for AI recommendations)
     * @param context Application context
     * @param title Suggestion title
     * @param message Suggestion message
     * @param notificationId Unique notification ID
     */
    public static void showSuggestionNotification(Context context, String title, String message, int notificationId) {
        if (!PermissionManager.isNotificationPermissionGranted(context)) {
            return;
        }
        
        createNotificationChannel(context);
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SUGGESTIONS)
            .setSmallIcon(R.drawable.ic_lightbulb)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.d(TAG, "✅ Showed suggestion: " + title);
    }
    
    /**
     * Show general notification
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     * @param notificationId Unique notification ID
     */
    public static void showGeneralNotification(Context context, String title, String message, int notificationId) {
        if (!PermissionManager.isNotificationPermissionGranted(context)) {
            return;
        }
        
        createNotificationChannel(context);
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.d(TAG, "✅ Showed general notification: " + title);
    }
    
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);
        Log.d(TAG, "✅ Cancelled notification: " + notificationId);
    }
    
    /**
     * Cancel all notifications
     * @param context Application context
     */
    public static void cancelAllNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
        Log.d(TAG, "✅ Cancelled all notifications");
    }
    
    /**
     * Check if notifications are enabled for the app
     * @param context Application context
     * @return true if enabled
     */
    public static boolean areNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
}
