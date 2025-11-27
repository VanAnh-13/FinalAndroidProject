package com.example.healthylifehub.patterns.singleton;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Singleton Pattern - Application Configuration
 * Thread-safe singleton for app-wide configuration
 */
public class AppConfig {
    private static volatile AppConfig instance;
    private final SharedPreferences prefs;
    
    // Config keys
    private static final String PREF_NAME = "app_config";
    private static final String KEY_SYNC_ENABLED = "sync_enabled";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SYNC_INTERVAL = "sync_interval";
    private static final String KEY_DATA_RETENTION_DAYS = "data_retention_days";

    private AppConfig(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppConfig getInstance(Context context) {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig(context);
                }
            }
        }
        return instance;
    }

    // Sync settings
    public boolean isSyncEnabled() {
        return prefs.getBoolean(KEY_SYNC_ENABLED, true);
    }

    public void setSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply();
    }

    public int getSyncIntervalMinutes() {
        return prefs.getInt(KEY_SYNC_INTERVAL, 15);
    }

    public void setSyncIntervalMinutes(int minutes) {
        prefs.edit().putInt(KEY_SYNC_INTERVAL, minutes).apply();
    }

    // Notification settings
    public boolean isNotificationEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    // Language settings
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "vi");
    }

    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    // Theme settings
    public String getTheme() {
        return prefs.getString(KEY_THEME, "system");
    }

    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    // Data retention
    public int getDataRetentionDays() {
        return prefs.getInt(KEY_DATA_RETENTION_DAYS, 365);
    }

    public void setDataRetentionDays(int days) {
        prefs.edit().putInt(KEY_DATA_RETENTION_DAYS, days).apply();
    }

    // Reset to defaults
    public void resetToDefaults() {
        prefs.edit()
                .putBoolean(KEY_SYNC_ENABLED, true)
                .putBoolean(KEY_NOTIFICATION_ENABLED, true)
                .putString(KEY_LANGUAGE, "vi")
                .putString(KEY_THEME, "system")
                .putInt(KEY_SYNC_INTERVAL, 15)
                .putInt(KEY_DATA_RETENTION_DAYS, 365)
                .apply();
    }
}
