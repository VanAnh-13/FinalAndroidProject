package com.example.healthylifehub.utils.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Comprehensive logging system for debugging notification and background issues
 * Provides structured logging with different levels and persistent storage
 * Requirements: 8.5
 */
public class ReminderLogger {
    
    private static final String TAG = "ReminderLogger";
    private static final String PREFS_NAME = "ReminderLoggerPrefs";
    private static final String KEY_LOG_LEVEL = "log_level";
    private static final String KEY_MAX_LOG_ENTRIES = "max_log_entries";
    private static final String KEY_LOG_TO_FILE = "log_to_file";
    
    // Log levels
    public enum LogLevel {
        VERBOSE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4);
        
        private final int level;
        
        LogLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    // Log categories for better organization
    public enum LogCategory {
        NOTIFICATION("NOTIFICATION"),
        BACKGROUND_WORK("BACKGROUND"),
        DATABASE("DATABASE"),
        VALIDATION("VALIDATION"),
        RETRY("RETRY"),
        GENERAL("GENERAL");
        
        private final String tag;
        
        LogCategory(String tag) {
            this.tag = tag;
        }
        
        public String getTag() {
            return tag;
        }
    }
    
    // Configuration
    private static LogLevel currentLogLevel = LogLevel.DEBUG;
    private static int maxLogEntries = 1000;
    private static boolean logToFile = true;
    private static Context appContext;
    
    // Date formatter for log entries
    private static final SimpleDateFormat dateFormat = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    
    /**
     * Initialize the logger with application context
     * 
     * @param context Application context
     */
    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
        loadConfiguration();
        
        // Log initialization
        logInfo(LogCategory.GENERAL, "ReminderLogger initialized", null);
    }
    
    /**
     * Load configuration from SharedPreferences
     */
    private static void loadConfiguration() {
        if (appContext == null) return;
        
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        int levelOrdinal = prefs.getInt(KEY_LOG_LEVEL, LogLevel.DEBUG.ordinal());
        currentLogLevel = LogLevel.values()[levelOrdinal];
        
        maxLogEntries = prefs.getInt(KEY_MAX_LOG_ENTRIES, 1000);
        logToFile = prefs.getBoolean(KEY_LOG_TO_FILE, true);
    }
    
    /**
     * Save configuration to SharedPreferences
     */
    private static void saveConfiguration() {
        if (appContext == null) return;
        
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putInt(KEY_LOG_LEVEL, currentLogLevel.ordinal());
        editor.putInt(KEY_MAX_LOG_ENTRIES, maxLogEntries);
        editor.putBoolean(KEY_LOG_TO_FILE, logToFile);
        
        editor.apply();
    }
    
    /**
     * Set log level
     * 
     * @param level New log level
     */
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
        saveConfiguration();
        logInfo(LogCategory.GENERAL, "Log level changed to: " + level.name(), null);
    }
    
    /**
     * Set maximum log entries
     * 
     * @param maxEntries Maximum number of log entries to keep
     */
    public static void setMaxLogEntries(int maxEntries) {
        maxLogEntries = maxEntries;
        saveConfiguration();
        logInfo(LogCategory.GENERAL, "Max log entries changed to: " + maxEntries, null);
    }
    
    /**
     * Enable or disable file logging
     * 
     * @param enabled Whether to log to file
     */
    public static void setFileLoggingEnabled(boolean enabled) {
        logToFile = enabled;
        saveConfiguration();
        logInfo(LogCategory.GENERAL, "File logging " + (enabled ? "enabled" : "disabled"), null);
    }
    
    /**
     * Log verbose message
     */
    public static void logVerbose(LogCategory category, String message, String details) {
        log(LogLevel.VERBOSE, category, message, details, null);
    }
    
    /**
     * Log debug message
     */
    public static void logDebug(LogCategory category, String message, String details) {
        log(LogLevel.DEBUG, category, message, details, null);
    }
    
    /**
     * Log info message
     */
    public static void logInfo(LogCategory category, String message, String details) {
        log(LogLevel.INFO, category, message, details, null);
    }
    
    /**
     * Log warning message
     */
    public static void logWarning(LogCategory category, String message, String details) {
        log(LogLevel.WARN, category, message, details, null);
    }
    
    /**
     * Log error message
     */
    public static void logError(LogCategory category, String message, String details, Throwable exception) {
        log(LogLevel.ERROR, category, message, details, exception);
    }
    
    /**
     * Log notification event
     */
    public static void logNotificationEvent(String event, String reminderId, String details) {
        String message = String.format("Notification %s for reminder %s", event, reminderId);
        logInfo(LogCategory.NOTIFICATION, message, details);
    }
    
    /**
     * Log background work event
     */
    public static void logBackgroundWork(String workType, String workId, String status, String details) {
        String message = String.format("Background work %s [%s]: %s", workType, workId, status);
        logInfo(LogCategory.BACKGROUND_WORK, message, details);
    }
    
    /**
     * Log database operation
     */
    public static void logDatabaseOperation(String operation, String table, String entityId, 
                                          boolean success, String details) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Database %s on %s [%s]: %s", operation, table, entityId, status);
        
        if (success) {
            logDebug(LogCategory.DATABASE, message, details);
        } else {
            logError(LogCategory.DATABASE, message, details, null);
        }
    }
    
    /**
     * Log validation result
     */
    public static void logValidation(String field, boolean isValid, String errorMessage) {
        String status = isValid ? "VALID" : "INVALID";
        String message = String.format("Validation for %s: %s", field, status);
        
        if (isValid) {
            logDebug(LogCategory.VALIDATION, message, null);
        } else {
            logWarning(LogCategory.VALIDATION, message, errorMessage);
        }
    }
    
    /**
     * Log retry attempt
     */
    public static void logRetryAttempt(String operationId, int attempt, int maxAttempts, 
                                     boolean success, String details) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Retry %d/%d for %s: %s", attempt, maxAttempts, operationId, status);
        
        if (success) {
            logInfo(LogCategory.RETRY, message, details);
        } else {
            logWarning(LogCategory.RETRY, message, details);
        }
    }
    
    /**
     * Core logging method
     */
    private static void log(LogLevel level, LogCategory category, String message, 
                          String details, Throwable exception) {
        
        // Check if we should log this level
        if (level.getLevel() < currentLogLevel.getLevel()) {
            return;
        }
        
        // Create log entry
        LogEntry entry = new LogEntry(level, category, message, details, exception);
        
        // Log to Android Log
        logToAndroidLog(entry);
        
        // Log to file if enabled
        if (logToFile && appContext != null) {
            logToFile(entry);
        }
        
        // Store in memory for retrieval
        storeLogEntry(entry);
    }
    
    /**
     * Log to Android Log system
     */
    private static void logToAndroidLog(LogEntry entry) {
        String tag = TAG + "_" + entry.category.getTag();
        String logMessage = formatLogMessage(entry);
        
        switch (entry.level) {
            case VERBOSE:
                Log.v(tag, logMessage, entry.exception);
                break;
            case DEBUG:
                Log.d(tag, logMessage, entry.exception);
                break;
            case INFO:
                Log.i(tag, logMessage, entry.exception);
                break;
            case WARN:
                Log.w(tag, logMessage, entry.exception);
                break;
            case ERROR:
                Log.e(tag, logMessage, entry.exception);
                break;
        }
    }
    
    /**
     * Log to file
     */
    private static void logToFile(LogEntry entry) {
        try {
            File logFile = getLogFile();
            if (logFile == null) return;
            
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(formatLogEntryForFile(entry));
            writer.append("\n");
            writer.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    /**
     * Store log entry in memory
     */
    private static void storeLogEntry(LogEntry entry) {
        if (appContext == null) return;
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences("log_entries", Context.MODE_PRIVATE);
            String existingLogs = prefs.getString("entries", "[]");
            
            JSONArray logArray = new JSONArray(existingLogs);
            logArray.put(entry.toJSON());
            
            // Limit the number of stored entries
            while (logArray.length() > maxLogEntries) {
                logArray.remove(0);
            }
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("entries", logArray.toString());
            editor.apply();
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to store log entry", e);
        }
    }
    
    /**
     * Get log file
     */
    private static File getLogFile() {
        if (appContext == null) return null;
        
        File logDir = new File(appContext.getFilesDir(), "logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            return null;
        }
        
        String fileName = "reminder_log_" + 
            new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date()) + ".txt";
        
        return new File(logDir, fileName);
    }
    
    /**
     * Format log message for display
     */
    private static String formatLogMessage(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.message);
        
        if (entry.details != null && !entry.details.trim().isEmpty()) {
            sb.append(" | ").append(entry.details);
        }
        
        return sb.toString();
    }
    
    /**
     * Format log entry for file output
     */
    private static String formatLogEntryForFile(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(entry.timestamp)));
        sb.append(" [").append(entry.level.name()).append("]");
        sb.append(" [").append(entry.category.getTag()).append("]");
        sb.append(" ").append(entry.message);
        
        if (entry.details != null && !entry.details.trim().isEmpty()) {
            sb.append(" | ").append(entry.details);
        }
        
        if (entry.exception != null) {
            sb.append(" | Exception: ").append(entry.exception.toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Get recent log entries
     * 
     * @param count Number of entries to retrieve
     * @return List of recent log entries
     */
    public static List<LogEntry> getRecentLogEntries(int count) {
        List<LogEntry> entries = new ArrayList<>();
        
        if (appContext == null) return entries;
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences("log_entries", Context.MODE_PRIVATE);
            String existingLogs = prefs.getString("entries", "[]");
            
            JSONArray logArray = new JSONArray(existingLogs);
            int startIndex = Math.max(0, logArray.length() - count);
            
            for (int i = startIndex; i < logArray.length(); i++) {
                JSONObject logObj = logArray.getJSONObject(i);
                entries.add(LogEntry.fromJSON(logObj));
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to retrieve log entries", e);
        }
        
        return entries;
    }
    
    /**
     * Clear all log entries
     */
    public static void clearLogs() {
        if (appContext == null) return;
        
        // Clear memory logs
        SharedPreferences prefs = appContext.getSharedPreferences("log_entries", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("entries", "[]");
        editor.apply();
        
        // Clear log files
        File logDir = new File(appContext.getFilesDir(), "logs");
        if (logDir.exists()) {
            File[] logFiles = logDir.listFiles();
            if (logFiles != null) {
                for (File file : logFiles) {
                    file.delete();
                }
            }
        }
        
        logInfo(LogCategory.GENERAL, "All logs cleared", null);
    }
    
    /**
     * Log entry class
     */
    public static class LogEntry {
        public final long timestamp;
        public final LogLevel level;
        public final LogCategory category;
        public final String message;
        public final String details;
        public final Throwable exception;
        
        public LogEntry(LogLevel level, LogCategory category, String message, 
                       String details, Throwable exception) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.category = category;
            this.message = message;
            this.details = details;
            this.exception = exception;
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("timestamp", timestamp);
            obj.put("level", level.name());
            obj.put("category", category.name());
            obj.put("message", message);
            obj.put("details", details);
            if (exception != null) {
                obj.put("exception", exception.toString());
            }
            return obj;
        }
        
        public static LogEntry fromJSON(JSONObject obj) throws JSONException {
            LogLevel level = LogLevel.valueOf(obj.getString("level"));
            LogCategory category = LogCategory.valueOf(obj.getString("category"));
            String message = obj.getString("message");
            String details = obj.optString("details", null);
            
            LogEntry entry = new LogEntry(level, category, message, details, null);
            // Note: We can't reconstruct the original exception from JSON
            return entry;
        }
        
        public String getFormattedTimestamp() {
            return dateFormat.format(new Date(timestamp));
        }
    }
}
