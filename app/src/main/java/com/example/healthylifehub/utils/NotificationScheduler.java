package com.example.healthylifehub.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.receivers.NotificationReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Smart notification scheduling system for reminders with deadlines
 * Handles scheduling, cancellation, and rescheduling of reminder notifications
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
public class NotificationScheduler {
    
    private static final String TAG = "NotificationScheduler";
    
    private final Context context;
    private final AlarmManager alarmManager;
    
    // Action for scheduled notifications
    public static final String ACTION_REMINDER_NOTIFICATION = "com.example.healthylifehub.REMINDER_NOTIFICATION";
    
    public NotificationScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Schedule all notifications for a reminder from current time to deadline
     * Requirements: 6.1
     * 
     * @param reminder The reminder to schedule notifications for
     * @return Number of notifications scheduled
     */
    public int scheduleNotifications(Reminder reminder) {
        if (reminder == null) {
            Log.w(TAG, "⚠️ Cannot schedule notifications - reminder is null");
            return 0;
        }
        
        // Validate reminder is active and not expired
        if (!isValidForScheduling(reminder)) {
            Log.w(TAG, "⚠️ Reminder not valid for scheduling: " + reminder.getReminderId());
            return 0;
        }
        
        Log.d(TAG, "📅 Scheduling notifications for reminder: " + reminder.getTitle());
        
        try {
            // Calculate notification times
            List<Long> notificationTimes = calculateNotificationTimes(reminder);
            
            if (notificationTimes.isEmpty()) {
                Log.d(TAG, "📭 No notifications to schedule for reminder: " + reminder.getReminderId());
                return 0;
            }
            
            int scheduledCount = 0;
            
            // Schedule each notification
            for (int i = 0; i < notificationTimes.size(); i++) {
                long notificationTime = notificationTimes.get(i);
                
                if (scheduleNotification(reminder, notificationTime, i)) {
                    scheduledCount++;
                }
            }
            
            Log.d(TAG, "✅ Scheduled " + scheduledCount + " notifications for reminder: " + reminder.getTitle());
            return scheduledCount;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to schedule notifications for reminder: " + reminder.getReminderId(), e);
            return 0;
        }
    }
    
    /**
     * Cancel all notifications for a specific reminder
     * Requirements: 6.2, 6.3
     * 
     * @param reminderId The reminder ID to cancel notifications for
     */
    public void cancelNotifications(String reminderId) {
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Cannot cancel notifications - invalid reminder ID");
            return;
        }
        
        Log.d(TAG, "🚫 Cancelling notifications for reminder: " + reminderId);
        
        try {
            // Cancel up to 1000 potential notifications (should be more than enough)
            int cancelledCount = 0;
            
            for (int i = 0; i < 1000; i++) {
                PendingIntent pendingIntent = createNotificationPendingIntent(reminderId, i);
                
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                    cancelledCount++;
                } else {
                    // If we can't create the PendingIntent, likely no more to cancel
                    break;
                }
            }
            
            Log.d(TAG, "✅ Cancelled " + cancelledCount + " notifications for reminder: " + reminderId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel notifications for reminder: " + reminderId, e);
        }
    }
    
    /**
     * Reschedule notifications when reminder is updated
     * Requirements: 6.2, 6.3
     * 
     * @param reminder The updated reminder
     * @return Number of notifications rescheduled
     */
    public int rescheduleNotifications(Reminder reminder) {
        if (reminder == null) {
            Log.w(TAG, "⚠️ Cannot reschedule notifications - reminder is null");
            return 0;
        }
        
        Log.d(TAG, "🔄 Rescheduling notifications for reminder: " + reminder.getTitle());
        
        // First cancel existing notifications
        cancelNotifications(reminder.getReminderId());
        
        // Then schedule new notifications if reminder is still valid
        if (isValidForScheduling(reminder)) {
            return scheduleNotifications(reminder);
        } else {
            Log.d(TAG, "📭 Reminder no longer valid for scheduling: " + reminder.getReminderId());
            return 0;
        }
    }
    
    /**
     * Reschedule notifications for all active reminders (e.g., on app restart)
     * Requirements: 6.5
     * 
     * @param activeReminders List of active reminders
     * @return Total number of notifications scheduled
     */
    public int rescheduleAllNotifications(List<Reminder> activeReminders) {
        if (activeReminders == null || activeReminders.isEmpty()) {
            Log.d(TAG, "📭 No active reminders to reschedule");
            return 0;
        }
        
        Log.d(TAG, "🔄 Rescheduling notifications for " + activeReminders.size() + " active reminders");
        
        int totalScheduled = 0;
        
        for (Reminder reminder : activeReminders) {
            try {
                int scheduled = rescheduleNotifications(reminder);
                totalScheduled += scheduled;
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to reschedule notifications for reminder: " + reminder.getReminderId(), e);
            }
        }
        
        Log.d(TAG, "✅ Rescheduled " + totalScheduled + " total notifications for " + activeReminders.size() + " reminders");
        return totalScheduled;
    }
    
    /**
     * Validate if reminder is eligible for notification scheduling
     * Requirements: 6.4
     * 
     * @param reminder The reminder to validate
     * @return true if reminder can have notifications scheduled
     */
    private boolean isValidForScheduling(Reminder reminder) {
        if (reminder == null) {
            return false;
        }
        
        // Must be active
        if (!reminder.isActive()) {
            Log.d(TAG, "📴 Reminder is inactive: " + reminder.getReminderId());
            return false;
        }
        
        // Must not be expired
        if (reminder.isExpired()) {
            Log.d(TAG, "⏰ Reminder is expired: " + reminder.getReminderId());
            return false;
        }
        
        // Must not be completed
        if (reminder.isCompleted()) {
            Log.d(TAG, "✅ Reminder is already completed: " + reminder.getReminderId());
            return false;
        }
        
        // Must have valid frequency
        if (reminder.getFrequency() == null || reminder.getFrequency().trim().isEmpty()) {
            Log.d(TAG, "❓ Reminder has invalid frequency: " + reminder.getReminderId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate all notification times for a reminder based on frequency and deadline
     * 
     * @param reminder The reminder to calculate times for
     * @return List of notification timestamps
     */
    private List<Long> calculateNotificationTimes(Reminder reminder) {
        List<Long> times = new ArrayList<>();
        
        long currentTime = System.currentTimeMillis();
        long startTime = Math.max(currentTime, reminder.getReminderTime());
        long endTime = reminder.hasDeadline() ? reminder.getDeadline() : (currentTime + (365L * 24 * 60 * 60 * 1000)); // 1 year if no deadline
        
        String frequency = reminder.getFrequency().toLowerCase();
        
        switch (frequency) {
            case "once":
                if (startTime <= endTime && startTime > currentTime) {
                    times.add(startTime);
                }
                break;
                
            case "daily":
                addDailyNotifications(times, startTime, endTime, currentTime);
                break;
                
            case "weekly":
                addWeeklyNotifications(times, startTime, endTime, currentTime);
                break;
                
            case "monthly":
                addMonthlyNotifications(times, startTime, endTime, currentTime);
                break;
                
            default:
                Log.w(TAG, "⚠️ Unknown frequency: " + frequency + " for reminder: " + reminder.getReminderId());
                break;
        }
        
        // Limit to reasonable number of notifications (max 365 for daily reminders)
        if (times.size() > 365) {
            Log.w(TAG, "⚠️ Too many notifications (" + times.size() + "), limiting to 365");
            times = times.subList(0, 365);
        }
        
        Log.d(TAG, "📊 Calculated " + times.size() + " notification times for " + frequency + " reminder");
        return times;
    }
    
    /**
     * Add daily notification times
     */
    private void addDailyNotifications(List<Long> times, long startTime, long endTime, long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        
        while (calendar.getTimeInMillis() <= endTime) {
            long notificationTime = calendar.getTimeInMillis();
            
            if (notificationTime > currentTime) {
                times.add(notificationTime);
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    
    /**
     * Add weekly notification times
     */
    private void addWeeklyNotifications(List<Long> times, long startTime, long endTime, long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        
        while (calendar.getTimeInMillis() <= endTime) {
            long notificationTime = calendar.getTimeInMillis();
            
            if (notificationTime > currentTime) {
                times.add(notificationTime);
            }
            
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
    }
    
    /**
     * Add monthly notification times
     */
    private void addMonthlyNotifications(List<Long> times, long startTime, long endTime, long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        
        while (calendar.getTimeInMillis() <= endTime) {
            long notificationTime = calendar.getTimeInMillis();
            
            if (notificationTime > currentTime) {
                times.add(notificationTime);
            }
            
            calendar.add(Calendar.MONTH, 1);
        }
    }
    
    /**
     * Schedule a single notification
     * 
     * @param reminder The reminder
     * @param notificationTime When to show the notification
     * @param sequenceNumber Sequence number for unique identification
     * @return true if scheduled successfully
     */
    private boolean scheduleNotification(Reminder reminder, long notificationTime, int sequenceNumber) {
        try {
            PendingIntent pendingIntent = createNotificationPendingIntent(reminder.getReminderId(), sequenceNumber);
            
            if (pendingIntent == null) {
                Log.e(TAG, "❌ Failed to create PendingIntent for notification");
                return false;
            }
            
            // Add reminder data to intent
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(ACTION_REMINDER_NOTIFICATION);
            intent.putExtra("reminder_id", reminder.getReminderId());
            intent.putExtra("reminder_title", reminder.getTitle());
            intent.putExtra("reminder_description", reminder.getDescription());
            intent.putExtra("sequence_number", sequenceNumber);
            intent.putExtra("scheduled_time", notificationTime);
            
            // Use exact alarm for precise timing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
            }
            
            Log.d(TAG, "⏰ Scheduled notification #" + sequenceNumber + " for " + 
                  new java.util.Date(notificationTime) + " (reminder: " + reminder.getTitle() + ")");
            
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Permission denied for scheduling exact alarms", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to schedule notification", e);
            return false;
        }
    }
    
    /**
     * Create PendingIntent for notification
     * 
     * @param reminderId The reminder ID
     * @param sequenceNumber Sequence number for uniqueness
     * @return PendingIntent or null if creation failed
     */
    private PendingIntent createNotificationPendingIntent(String reminderId, int sequenceNumber) {
        try {
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(ACTION_REMINDER_NOTIFICATION);
            intent.putExtra("reminder_id", reminderId);
            intent.putExtra("sequence_number", sequenceNumber);
            
            // Create unique request code
            int requestCode = (reminderId + "_" + sequenceNumber).hashCode();
            
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create PendingIntent", e);
            return null;
        }
    }
    
    /**
     * Check if exact alarm permission is available (Android 12+)
     * 
     * @return true if exact alarms can be scheduled
     */
    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Always available on older versions
    }
    
    /**
     * Get information about scheduled notifications for debugging
     * 
     * @param reminderId The reminder ID
     */
    public void logScheduledNotifications(String reminderId) {
        Log.d(TAG, "📊 Notification scheduling info for reminder: " + reminderId);
        Log.d(TAG, "  - Can schedule exact alarms: " + canScheduleExactAlarms());
        Log.d(TAG, "  - AlarmManager available: " + (alarmManager != null));
    }
}