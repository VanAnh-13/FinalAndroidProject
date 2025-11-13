package com.example.healthylifehub.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.healthylifehub.utils.NotificationHelper;
import com.example.healthylifehub.utils.ReminderAlarmManager;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.data.model.Reminder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced ReminderReceiver to handle reminder alarms and notification actions
 * Supports: Show notification, Complete reminder, Snooze reminder
 */
public class ReminderReceiver extends BroadcastReceiver {
    
    private static final String TAG = "ReminderReceiver";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);
        
        if (action == null) {
            // Default alarm - show notification
            handleReminderAlarm(context, intent);
        } else {
            switch (action) {
                case "COMPLETE_REMINDER":
                    handleCompleteReminder(context, intent);
                    break;
                case "SNOOZE_REMINDER":
                    handleSnoozeReminder(context, intent);
                    break;
                default:
                    handleReminderAlarm(context, intent);
                    break;
            }
        }
    }
    
    /**
     * Handle reminder alarm - show notification
     */
    private void handleReminderAlarm(Context context, Intent intent) {
        Log.d(TAG, "Handling reminder alarm");
        
        String reminderId = intent.getStringExtra("reminderId");
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        int notificationId = intent.getIntExtra("notificationId", 0);
        
        if (reminderId == null || title == null) {
            Log.e(TAG, "Missing reminder data in intent");
            return;
        }
        
        Log.d(TAG, "Showing notification for: " + title);
        NotificationHelper.showReminderNotification(
            context,
            reminderId,
            title,
            description,
            notificationId
        );
    }
    
    /**
     * Handle complete reminder action from notification
     */
    private void handleCompleteReminder(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminderId");
        int notificationId = intent.getIntExtra("notificationId", 0);
        
        if (reminderId == null) {
            Log.e(TAG, "Missing reminderId for complete action");
            return;
        }
        
        Log.d(TAG, "Completing reminder: " + reminderId);
        
        // Cancel the notification
        NotificationHelper.cancelNotification(context, notificationId);
        
        // Mark reminder as completed in background
        executor.execute(() -> {
            try {
                RemindersRepository repository = new RemindersRepository();
                // Note: You might want to add a "complete" method to repository
                // For now, we'll just log it
                Log.d(TAG, "✅ Reminder completed: " + reminderId);
                
                // Show toast on main thread
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> 
                    Toast.makeText(context, "✅ Đã hoàn thành nhắc nhở", Toast.LENGTH_SHORT).show()
                );
                
            } catch (Exception e) {
                Log.e(TAG, "Error completing reminder", e);
            }
        });
    }
    
    /**
     * Handle snooze reminder action from notification
     */
    private void handleSnoozeReminder(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminderId");
        int notificationId = intent.getIntExtra("notificationId", 0);
        
        if (reminderId == null) {
            Log.e(TAG, "Missing reminderId for snooze action");
            return;
        }
        
        Log.d(TAG, "Snoozing reminder: " + reminderId);
        
        // Cancel current notification
        NotificationHelper.cancelNotification(context, notificationId);
        
        // Schedule snooze alarm (10 minutes)
        long snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes
        
        // Create new alarm for snooze
        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.putExtra("reminderId", reminderId);
        snoozeIntent.putExtra("title", "⏰ Nhắc lại: " + intent.getStringExtra("title"));
        snoozeIntent.putExtra("description", "Bạn đã báo lại 10 phút trước");
        snoozeIntent.putExtra("notificationId", notificationId);
        
        ReminderAlarmManager.scheduleOneTimeAlarm(
            context,
            snoozeIntent,
            snoozeTime,
            notificationId + 5000 // Different request code for snooze
        );
        
        // Show toast
        Toast.makeText(context, "⏰ Sẽ nhắc lại sau 10 phút", Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "✅ Scheduled snooze for: " + new java.util.Date(snoozeTime));
    }
}
