package com.example.healthylifehub.utils.workmanager;

import android.content.Context;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

/**
 * WorkManager initialization utility for smart reminder system
 */
public class WorkManagerInitializer {
    
    private static final String TAG = "WorkManagerInitializer";
    
    private static final int MIN_JOB_SCHEDULER_ID = 1000;
    private static final int MAX_JOB_SCHEDULER_ID = 2000;
    
    public static void initialize(Context context) {
        try {
            Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setJobSchedulerJobIdRange(MIN_JOB_SCHEDULER_ID, MAX_JOB_SCHEDULER_ID)
                .build();
            
            WorkManager.initialize(context, config);
            
            Log.d(TAG, "✅ WorkManager initialized successfully");
            
            WorkManagerConfig.pruneWork(context);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize WorkManager", e);
        }
    }
    
    public static boolean isInitialized(Context context) {
        try {
            WorkManager.getInstance(context);
            return true;
        } catch (IllegalStateException e) {
            Log.w(TAG, "⚠️ WorkManager not initialized", e);
            return false;
        }
    }
    
    public static void logConfigurationInfo(Context context) {
        try {
            if (isInitialized(context)) {
                Log.d(TAG, "📊 WorkManager Configuration:");
                Log.d(TAG, "  - Job Scheduler ID Range: " + MIN_JOB_SCHEDULER_ID + " - " + MAX_JOB_SCHEDULER_ID);
                Log.d(TAG, "  - Minimum Logging Level: INFO");
            } else {
                Log.w(TAG, "⚠️ WorkManager not initialized - cannot log configuration");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to log WorkManager configuration", e);
        }
    }
}
