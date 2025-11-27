package com.example.healthylifehub.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.utils.notification.NotificationScheduler;
import com.example.healthylifehub.utils.notification.SmartNotificationManager;

/**
 * BroadcastReceiver for handling scheduled reminder notifications
 * Receives alarms from NotificationScheduler and displays interactive notifications
 * Requirements: 6.1, 6.4
 */
public class NotificationReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NotificationReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "⚠️ Received null intent or action");
            return;
        }
        
        String action = intent.getAction();
        Log.d(TAG, "📨 Received broadcast with action: " + action);
        
        if (NotificationScheduler.ACTION_REMINDER_NOTIFICATION.equals(action)) {
            handleReminderNotification(context, intent);
        } else {
            Log.w(TAG, "⚠️ Unknown action received: " + action);
        }
    }
    
    /**
     * Handle scheduled reminder notification
     * Requirements: 6.1, 6.4
     * 
     * @param context Application context
     * @param intent Intent containing reminder data
     */
    private void handleReminderNotification(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminder_id");
        String reminderTitle = intent.getStringExtra("reminder_title");
        String reminderDescription = intent.getStringExtra("reminder_description");
        int sequenceNumber = intent.getIntExtra("sequence_number", 0);
        long scheduledTime = intent.getLongExtra("scheduled_time", System.currentTimeMillis());
        
        Log.d(TAG, "🔔 Processing notification for reminder: " + reminderId + " (sequence: " + sequenceNumber + ")");
        
        // Validate required data
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.e(TAG, "❌ Invalid reminder ID in notification intent");
            return;
        }
        
        try {
            // Verify reminder is still valid for notification
            if (!isReminderValidForNotification(context, reminderId)) {
                Log.d(TAG, "📴 Reminder no longer valid for notification: " + reminderId);
                return;
            }
            
            // Get fresh reminder data from database
            Reminder reminder = getReminderFromDatabase(context, reminderId);
            if (reminder == null) {
                Log.w(TAG, "⚠️ Reminder not found in database: " + reminderId);
                return;
            }
            
            // Create notification ID based on reminder and sequence
            int notificationId = createNotificationId(reminderId, sequenceNumber);
            
            // Send interactive notification
            SmartNotificationManager notificationManager = new SmartNotificationManager(context);
            notificationManager.sendInteractiveNotification(reminder, notificationId);
            
            Log.d(TAG, "✅ Sent interactive notification for reminder: " + reminder.getTitle() + 
                      " (ID: " + notificationId + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to handle reminder notification for: " + reminderId, e);
        }
    }
    
    /**
     * Verify reminder is still valid for showing notifications
     * Requirements: 6.4
     * 
     * @param context Application context
     * @param reminderId The reminder ID to check
     * @return true if reminder should show notification
     */
    private boolean isReminderValidForNotification(Context context, String reminderId) {
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            if (database == null) {
                Log.e(TAG, "❌ Cannot access database to validate reminder");
                return false;
            }
            
            Reminder reminder = database.reminderDao().getReminderById(reminderId);
            if (reminder == null) {
                Log.d(TAG, "📭 Reminder not found: " + reminderId);
                return false;
            }
            
            // Check if reminder is still active
            if (!reminder.isActive()) {
                Log.d(TAG, "📴 Reminder is inactive: " + reminderId);
                return false;
            }
            
            // Check if reminder is expired
            if (reminder.isExpired()) {
                Log.d(TAG, "⏰ Reminder is expired: " + reminderId);
                return false;
            }
            
            // Check if reminder is completed
            if (reminder.isCompleted()) {
                Log.d(TAG, "✅ Reminder is completed: " + reminderId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error validating reminder for notification: " + reminderId, e);
            return false;
        }
    }
    
    /**
     * Get fresh reminder data from database
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     * @return Reminder object or null if not found
     */
    private Reminder getReminderFromDatabase(Context context, String reminderId) {
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            if (database == null) {
                Log.e(TAG, "❌ Cannot access database");
                return null;
            }
            
            return database.reminderDao().getReminderById(reminderId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting reminder from database: " + reminderId, e);
            return null;
        }
    }
    
    /**
     * Create unique notification ID based on reminder ID and sequence number
     * 
     * @param reminderId The reminder ID
     * @param sequenceNumber The sequence number
     * @return Unique notification ID
     */
    private int createNotificationId(String reminderId, int sequenceNumber) {
        // Create a unique but deterministic ID
        String combined = reminderId + "_" + sequenceNumber;
        return Math.abs(combined.hashCode());
    }
}