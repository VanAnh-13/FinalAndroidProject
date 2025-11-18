package com.example.healthylifehub;

import android.app.Application;

import com.example.healthylifehub.utils.ApplicationContextProvider;
import com.example.healthylifehub.utils.NotificationHelper;
import com.example.healthylifehub.utils.WorkManagerInitializer;

/**
 * Application class for HealthyLife Hub
 * Initializes global application context and dependencies
 */
public class HealthyLifeHubApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize application context provider for repositories
        ApplicationContextProvider.init(this);
        
        // Initialize WorkManager for background processing
        WorkManagerInitializer.initialize(this);
        
        // Create notification channel for reminders
        NotificationHelper.createNotificationChannel(this);
    }
}
