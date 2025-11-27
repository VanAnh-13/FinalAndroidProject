package com.example.healthylifehub.utils.app;

import android.content.Context;
import android.util.Log;

import com.example.healthylifehub.services.ReminderSchedulingService;
import com.example.healthylifehub.utils.reminder.DeadlineManager;
import com.example.healthylifehub.utils.security.PermissionManager;
import com.example.healthylifehub.utils.workmanager.WorkManagerConfig;
import com.example.healthylifehub.utils.workmanager.WorkManagerInitializer;

/**
 * Application initialization utility for smart reminder system
 * Handles app startup tasks including notification rescheduling
 * Requirements: 6.5
 */
public class AppInitializer {
    
    private static final String TAG = "AppInitializer";
    
    /**
     * Initialize the application and reschedule notifications
     * Should be called in Application.onCreate() or MainActivity.onCreate()
     * Requirements: 6.5
     * 
     * @param context Application context
     */
    public static void initialize(Context context) {
        Log.d(TAG, "🚀 Initializing HealthyLifeHub application");
        
        try {
            // Initialize WorkManager first
            WorkManagerInitializer.initialize(context);
            
            // Initialize deadline management system
            initializeDeadlineManagement(context);
            
            // Setup promotional notifications
            WorkManagerConfig.setupPromotionalNotifications(context);
            
            // Reschedule notifications for all active reminders
            rescheduleNotificationsOnStartup(context);
            
            Log.d(TAG, "✅ Application initialization completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize application", e);
        }
    }
    
    /**
     * Initialize deadline management system
     * Requirements: 2.4, 2.5
     * 
     * @param context Application context
     */
    private static void initializeDeadlineManagement(Context context) {
        Log.d(TAG, "⏰ Initializing deadline management system");
        
        try {
            DeadlineManager deadlineManager = new DeadlineManager(context);
            deadlineManager.initialize();
            
            Log.d(TAG, "✅ Deadline management system initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize deadline management system", e);
        }
    }
    
    /**
     * Reschedule notifications for all active reminders on app startup
     * Requirements: 6.5
     * 
     * @param context Application context
     */
    private static void rescheduleNotificationsOnStartup(Context context) {
        Log.d(TAG, "🔄 Rescheduling notifications on app startup");
        
        try {
            ReminderSchedulingService schedulingService = ReminderSchedulingService.getInstance(context);
            
            schedulingService.rescheduleAllActiveReminders(new ReminderSchedulingService.SchedulingCallback() {
                @Override
                public void onSuccess(int count) {
                    Log.d(TAG, "✅ Successfully rescheduled " + count + " notifications on startup");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Failed to reschedule notifications on startup: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during notification rescheduling on startup", e);
        }
    }
    
    /**
     * Check if the app has necessary permissions for scheduling
     * 
     * @param context Application context
     * @return true if all required permissions are available
     */
    public static boolean checkSchedulingPermissions(Context context) {
        try {
            ReminderSchedulingService schedulingService = ReminderSchedulingService.getInstance(context);
            boolean canScheduleExact = schedulingService.canScheduleExactAlarms();
            boolean notificationsEnabled = PermissionManager.isNotificationPermissionGranted(context);
            
            Log.d(TAG, "📊 Scheduling permissions check:");
            Log.d(TAG, "  - Can schedule exact alarms: " + canScheduleExact);
            Log.d(TAG, "  - Notifications enabled: " + notificationsEnabled);
            
            return canScheduleExact && notificationsEnabled;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking scheduling permissions", e);
            return false;
        }
    }
    
    /**
     * Log application initialization status for debugging
     * 
     * @param context Application context
     */
    public static void logInitializationStatus(Context context) {
        Log.d(TAG, "📊 Application Initialization Status:");
        
        try {
            // WorkManager status
            boolean workManagerInitialized = WorkManagerInitializer.isInitialized(context);
            Log.d(TAG, "  - WorkManager initialized: " + workManagerInitialized);
            
            // Scheduling permissions
            boolean schedulingPermissions = checkSchedulingPermissions(context);
            Log.d(TAG, "  - Scheduling permissions: " + schedulingPermissions);
            
            // Database status
            boolean databaseAvailable = (com.example.healthylifehub.data.local.AppDatabase.getInstance(context) != null);
            Log.d(TAG, "  - Database available: " + databaseAvailable);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error logging initialization status", e);
        }
    }
}
