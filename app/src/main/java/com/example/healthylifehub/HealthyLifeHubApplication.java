package com.example.healthylifehub;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.healthylifehub.utils.ApplicationContextProvider;
import com.example.healthylifehub.utils.NotificationHelper;
import com.example.healthylifehub.utils.WorkManagerInitializer;

/**
 * Application class for HealthyLife Hub
 * Initializes global application context and dependencies
 */
public class HealthyLifeHubApplication extends Application {
    
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LANGUAGE = "language";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply theme mode (must be done before any UI is created)
        applyThemeMode();
        
        // Apply language settings
        applyLanguageSettings();
        
        // Initialize application context provider for repositories
        ApplicationContextProvider.init(this);
        
        // Initialize WorkManager for background processing
        WorkManagerInitializer.initialize(this);
        
        // Create notification channel for reminders
        NotificationHelper.createNotificationChannel(this);
    }
    
    /**
     * Apply the saved theme mode or default to system
     */
    private void applyThemeMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * Apply the saved language or default to system language
     */
    private void applyLanguageSettings() {
        com.example.healthylifehub.utils.LocaleHelper.applyLanguage(this);
    }
    
    /**
     * Save theme mode preference
     */
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    
    /**
     * Get current theme mode
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
    
    /**
     * Save language preference
     */
    public static void setLanguage(Context context, String languageCode) {
        com.example.healthylifehub.utils.LocaleHelper.setLanguage(context, languageCode);
    }
    
    /**
     * Get current language
     */
    public static String getLanguage(Context context) {
        return com.example.healthylifehub.utils.LocaleHelper.getLanguage(context);
    }
}
