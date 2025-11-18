package com.example.healthylifehub.utils;

import android.content.Context;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

/**
 * WorkManager initialization utility for smart reminder system
 * Provides proper WorkManager configuration and initialization
 * Requirements: 8.5
 */
public class WorkManagerInitializer {
    
    private static final String TAG = "WorkManagerInitializer";
    
    // WorkManager configuration constants
    private static final int MIN_JOB_SCHEDULER_ID = 1000;
    private static final int MAX_JOB_SCHEDULER_ID = 2000;
    private static final int MAX_SCHEDULER_THREADS = 4;
    
    /**
     * Initialize WorkManager with optimal configuration for reminder system
     * Should be called in Application.onCreate()
     * Requirements: 8.5
     * 
     * @param context Application context
     */
    public static void initialize(Context context) {
        try {
            // Create custom configuration for optimal performance
            Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setJobSchedulerJobIdRange(MIN_JOB_SCHEDULER_ID, MAX_JOB_SCHEDULER_ID)
                // Note: setMaxSchedulerThreads is not available in current WorkManager version
                // Using default thread pool configuration
                .build();
            
            // Initialize WorkManager with custom configuration
            WorkManager.initialize(context, config);
            
            Log.d(TAG, "✅ WorkManager initialized successfully");
            
            // Prune old work entries on startup
            WorkManagerConfig.pruneWork(context);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize WorkManager", e);
        }
    }
    
    /**
     * Check if WorkManager is properly initialized
     * 
     * @param context Application context
     * @return true if WorkManager is initialized
     */
    public static boolean isInitialized(Context context) {
        try {
            WorkManager.getInstance(context);
            return true;
        } catch (IllegalStateException e) {
            Log.w(TAG, "⚠️ WorkManager not initialized", e);
            return false;
        }
    }
    
    /**
     * Get WorkManager configuration info for debugging
     * 
     * @param context Application context
     */
    public static void logConfigurationInfo(Context context) {
        try {
            if (isInitialized(context)) {
                Log.d(TAG, "📊 WorkManager Configuration:");
                Log.d(TAG, "  - Job Scheduler ID Range: " + MIN_JOB_SCHEDULER_ID + " - " + MAX_JOB_SCHEDULER_ID);
                Log.d(TAG, "  - Minimum Logging Level: INFO");
                Log.d(TAG, "  - Using default thread pool configuration");
            } else {
                Log.w(TAG, "⚠️ WorkManager not initialized - cannot log configuration");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to log WorkManager configuration", e);
        }
    }
}