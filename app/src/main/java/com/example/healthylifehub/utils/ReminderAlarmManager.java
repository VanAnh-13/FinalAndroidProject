package com.example.healthylifehub.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.receivers.ReminderReceiver;

import java.util.Calendar;

public class ReminderAlarmManager {
    
    private static final String TAG = "ReminderAlarmManager";
    
    public static void scheduleReminder(Context context, Reminder reminder) {
        if (reminder == null || reminder.getReminderId() == null) {
            Log.e(TAG, "Invalid reminder data");
            return;
        }
        
        // Check permission for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, 
                android.Manifest.permission.SCHEDULE_EXACT_ALARM) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Missing SCHEDULE_EXACT_ALARM permission, using inexact alarm");
                // Fall back to setAndAllowWhileIdle() instead of setExactAndAllowWhileIdle()
            }
        }
        
        long reminderTime = reminder.getReminderTime();
        long currentTime = System.currentTimeMillis();
        
        if (reminderTime <= currentTime) {
            Log.w(TAG, "Reminder time is in the past, scheduling for tomorrow");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(reminderTime);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            reminderTime = calendar.getTimeInMillis();
        }
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }
        
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminderId", reminder.getReminderId());
        intent.putExtra("title", reminder.getTitle());
        intent.putExtra("description", reminder.getDescription());
        intent.putExtra("notificationId", reminder.getReminderId().hashCode());
        
        int requestCode = reminder.getReminderId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            );
        }
        
        Log.d(TAG, "Alarm scheduled for: " + reminder.getTitle() + " at " + reminderTime);
        
        if ("daily".equals(reminder.getFrequency())) {
            scheduleRepeatingReminder(context, reminder, AlarmManager.INTERVAL_DAY);
        } else if ("weekly".equals(reminder.getFrequency())) {
            scheduleRepeatingReminder(context, reminder, AlarmManager.INTERVAL_DAY * 7);
        } else if ("monthly".equals(reminder.getFrequency())) {
            scheduleRepeatingReminder(context, reminder, AlarmManager.INTERVAL_DAY * 30);
        }
    }
    
    private static void scheduleRepeatingReminder(Context context, Reminder reminder, long interval) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminderId", reminder.getReminderId());
        intent.putExtra("title", reminder.getTitle());
        intent.putExtra("description", reminder.getDescription());
        intent.putExtra("notificationId", reminder.getReminderId().hashCode());
        
        int requestCode = reminder.getReminderId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            reminder.getReminderTime(),
            interval,
            pendingIntent
        );
        
        Log.d(TAG, "Repeating alarm scheduled with interval: " + interval);
    }
    
    public static void cancelReminder(Context context, String reminderId) {
        if (reminderId == null) {
            Log.e(TAG, "ReminderId is null");
            return;
        }
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        
        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = reminderId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        
        Log.d(TAG, "Alarm cancelled for reminderId: " + reminderId);
    }
    
    public static void rescheduleReminder(Context context, Reminder reminder, long newTime) {
        cancelReminder(context, reminder.getReminderId());
        
        reminder.setReminderTime(newTime);
        scheduleReminder(context, reminder);
        
        Log.d(TAG, "Reminder rescheduled to: " + newTime);
    }
    
    /**
     * Schedule one-time alarm (for snooze functionality)
     * @param context Application context
     * @param intent Intent to broadcast
     * @param triggerTime When to trigger the alarm
     * @param requestCode Unique request code
     */
    public static void scheduleOneTimeAlarm(Context context, Intent intent, long triggerTime, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
        }
        
        Log.d(TAG, "One-time alarm scheduled for: " + new java.util.Date(triggerTime));
    }
}
