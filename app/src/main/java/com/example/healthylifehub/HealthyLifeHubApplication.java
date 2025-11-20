package com.example.healthylifehub;

import android.app.Application;

import com.example.healthylifehub.utils.ApplicationContextProvider;
import com.example.healthylifehub.utils.NotificationHelper;

/**
 * Application class for HealthyLife Hub
 * Initializes global application context and dependencies
 */
public class HealthyLifeHubApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Initialize application context provider for repositories
            ApplicationContextProvider.init(this);
            
            // Create notification channel for reminders
            NotificationHelper.createNotificationChannel(this);
            
            // Initialize app features (WorkManager is auto-initialized by AndroidX)
            initializeAppFeatures();
            
        } catch (Exception e) {
            android.util.Log.e("HealthyLifeHub", "Initialization error", e);
        }
    }
    
    private void initializeAppFeatures() {
        try {
            // WorkManager is already initialized automatically by AndroidX
            // No need to call WorkManagerInitializer.initialize()
            
            // Setup deadline management
            com.example.healthylifehub.utils.DeadlineManager deadlineManager = 
                new com.example.healthylifehub.utils.DeadlineManager(this);
            deadlineManager.initialize();
            
            // Setup promotional notifications
            com.example.healthylifehub.utils.WorkManagerConfig.setupPromotionalNotifications(this);
            
            // Reschedule reminders will be done when user logs in
            
        } catch (Exception e) {
            android.util.Log.e("HealthyLifeHub", "Feature initialization error", e);
        }
    }
}
