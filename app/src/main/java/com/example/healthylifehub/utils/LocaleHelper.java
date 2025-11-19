package com.example.healthylifehub.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * LocaleHelper - Utility class for managing app localization
 * Handles language switching and persists user language preference
 */
public class LocaleHelper {
    
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANGUAGE_SYSTEM = "system";
    
    /**
     * Apply saved language preference to context
     * Call this in attachBaseContext() of activities
     */
    public static Context applyLanguage(Context context) {
        String languageCode = getLanguage(context);
        
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            // Use system language
            return context;
        }
        
        return setLocale(context, languageCode);
    }
    
    /**
     * Set locale for the context
     */
    public static Context setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }
    
    /**
     * Save language preference
     */
    public static void setLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }
    
    /**
     * Get saved language preference
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM);
    }
    
    /**
     * Get current system language code
     */
    public static String getSystemLanguage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else {
            return context.getResources().getConfiguration().locale.getLanguage();
        }
    }
    
    /**
     * Get display name for language code
     */
    public static String getLanguageDisplayName(String languageCode, Context context) {
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            return "System Default";
        }
        
        Locale locale = new Locale(languageCode);
        return locale.getDisplayName(locale);
    }
    
    /**
     * Check if a language is supported by the app
     */
    public static boolean isLanguageSupported(String languageCode) {
        // Add supported language codes here
        return languageCode.equals("en") || 
               languageCode.equals("vi") ||
               languageCode.equals(LANGUAGE_SYSTEM);
    }
}
