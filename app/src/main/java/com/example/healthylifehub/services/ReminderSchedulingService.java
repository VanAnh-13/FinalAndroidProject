package com.example.healthylifehub.services;

import android.content.Context;
import android.util.Log;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.utils.notification.NotificationScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for managing reminder notification scheduling lifecycle
 * Provides high-level operations for scheduling, updating, and managing reminder notifications
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
public class ReminderSchedulingService {
    
    private static final String TAG = "ReminderSchedulingService";
    
    private final Context context;
    private final NotificationScheduler scheduler;
    private final AppDatabase database;
    private final ExecutorService executorService;
    
    // Singleton instance
    private static ReminderSchedulingService instance;
    
    private ReminderSchedulingService(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = new NotificationScheduler(this.context);
        this.database = AppDatabase.getInstance(this.context);
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Get singleton instance of ReminderSchedulingService
     * 
     * @param context Application context
     * @return Service instance
     */
    public static synchronized ReminderSchedulingService getInstance(Context context) {
        if (instance == null) {
            instance = new ReminderSchedulingService(context);
        }
        return instance;
    }
    
    /**
     * Schedule notifications for a new or updated reminder
     * Requirements: 6.1, 6.2
     * 
     * @param reminder The reminder to schedule notifications for
     * @param callback Callback for result
     */
    public void scheduleReminderNotifications(Reminder reminder, SchedulingCallback callback) {
        if (reminder == null) {
            if (callback != null) {
                callback.onError("Reminder is null");
            }
            return;
        }
        
        Log.d(TAG, "📅 Scheduling notifications for reminder: " + reminder.getTitle());
        
        executorService.execute(() -> {
            try {
                int scheduledCount = scheduler.scheduleNotifications(reminder);
                
                if (callback != null) {
                    callback.onSuccess(scheduledCount);
                }
                
                Log.d(TAG, "✅ Successfully scheduled " + scheduledCount + " notifications for: " + reminder.getTitle());
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to schedule notifications for reminder: " + reminder.getReminderId(), e);
                
                if (callback != null) {
                    callback.onError("Failed to schedule notifications: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Cancel notifications for a reminder (when deactivated or deleted)
     * Requirements: 6.2
     * 
     * @param reminderId The reminder ID to cancel notifications for
     * @param callback Callback for result
     */
    public void cancelReminderNotifications(String reminderId, SchedulingCallback callback) {
        if (reminderId == null || reminderId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Invalid reminder ID");
            }
            return;
        }
        
        Log.d(TAG, "🚫 Cancelling notifications for reminder: " + reminderId);
        
        executorService.execute(() -> {
            try {
                scheduler.cancelNotifications(reminderId);
                
                if (callback != null) {
                    callback.onSuccess(0); // No count for cancellation
                }
                
                Log.d(TAG, "✅ Successfully cancelled notifications for reminder: " + reminderId);
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to cancel notifications for reminder: " + reminderId, e);
                
                if (callback != null) {
                    callback.onError("Failed to cancel notifications: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Reschedule notifications for an updated reminder
     * Requirements: 6.2, 6.3
     * 
     * @param reminder The updated reminder
     * @param callback Callback for result
     */
    public void rescheduleReminderNotifications(Reminder reminder, SchedulingCallback callback) {
        if (reminder == null) {
            if (callback != null) {
                callback.onError("Reminder is null");
            }
            return;
        }
        
        Log.d(TAG, "🔄 Rescheduling notifications for reminder: " + reminder.getTitle());
        
        executorService.execute(() -> {
            try {
                int scheduledCount = scheduler.rescheduleNotifications(reminder);
                
                if (callback != null) {
                    callback.onSuccess(scheduledCount);
                }
                
                Log.d(TAG, "✅ Successfully rescheduled " + scheduledCount + " notifications for: " + reminder.getTitle());
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to reschedule notifications for reminder: " + reminder.getReminderId(), e);
                
                if (callback != null) {
                    callback.onError("Failed to reschedule notifications: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Reschedule notifications for all active reminders (e.g., on app restart)
     * Requirements: 6.5
     * 
     * @param callback Callback for result
     */
    public void rescheduleAllActiveReminders(SchedulingCallback callback) {
        Log.d(TAG, "🔄 Rescheduling notifications for all active reminders");
        
        executorService.execute(() -> {
            try {
                if (database == null) {
                    throw new IllegalStateException("Database not available");
                }
                
                ReminderDao reminderDao = database.reminderDao();
                List<Reminder> activeReminders = reminderDao.getAllActiveReminders();
                
                if (activeReminders == null || activeReminders.isEmpty()) {
                    Log.d(TAG, "📭 No active reminders found to reschedule");
                    
                    if (callback != null) {
                        callback.onSuccess(0);
                    }
                    return;
                }
                
                int totalScheduled = scheduler.rescheduleAllNotifications(activeReminders);
                
                if (callback != null) {
                    callback.onSuccess(totalScheduled);
                }
                
                Log.d(TAG, "✅ Successfully rescheduled " + totalScheduled + " notifications for " + 
                          activeReminders.size() + " active reminders");
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to reschedule all active reminders", e);
                
                if (callback != null) {
                    callback.onError("Failed to reschedule all reminders: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handle reminder creation - schedule notifications if needed
     * Requirements: 6.1
     * 
     * @param reminder The newly created reminder
     * @param callback Callback for result
     */
    public void onReminderCreated(Reminder reminder, SchedulingCallback callback) {
        Log.d(TAG, "➕ Handling new reminder creation: " + (reminder != null ? reminder.getTitle() : "null"));
        scheduleReminderNotifications(reminder, callback);
    }
    
    /**
     * Handle reminder update - reschedule notifications
     * Requirements: 6.2, 6.3
     * 
     * @param reminder The updated reminder
     * @param callback Callback for result
     */
    public void onReminderUpdated(Reminder reminder, SchedulingCallback callback) {
        Log.d(TAG, "✏️ Handling reminder update: " + (reminder != null ? reminder.getTitle() : "null"));
        rescheduleReminderNotifications(reminder, callback);
    }
    
    /**
     * Handle reminder deletion - cancel notifications
     * Requirements: 6.2
     * 
     * @param reminderId The deleted reminder ID
     * @param callback Callback for result
     */
    public void onReminderDeleted(String reminderId, SchedulingCallback callback) {
        Log.d(TAG, "🗑️ Handling reminder deletion: " + reminderId);
        cancelReminderNotifications(reminderId, callback);
    }
    
    /**
     * Handle reminder deactivation - cancel notifications
     * Requirements: 6.2
     * 
     * @param reminderId The deactivated reminder ID
     * @param callback Callback for result
     */
    public void onReminderDeactivated(String reminderId, SchedulingCallback callback) {
        Log.d(TAG, "📴 Handling reminder deactivation: " + reminderId);
        cancelReminderNotifications(reminderId, callback);
    }
    
    /**
     * Check if exact alarm scheduling is available
     * 
     * @return true if exact alarms can be scheduled
     */
    public boolean canScheduleExactAlarms() {
        return scheduler.canScheduleExactAlarms();
    }
    
    /**
     * Get scheduling information for debugging
     * 
     * @param reminderId The reminder ID to get info for
     */
    public void logSchedulingInfo(String reminderId) {
        scheduler.logScheduledNotifications(reminderId);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * Callback interface for scheduling operations
     */
    public interface SchedulingCallback {
        /**
         * Called when scheduling operation succeeds
         * 
         * @param count Number of notifications scheduled (0 for cancellation operations)
         */
        void onSuccess(int count);
        
        /**
         * Called when scheduling operation fails
         * 
         * @param error Error message
         */
        void onError(String error);
    }
}