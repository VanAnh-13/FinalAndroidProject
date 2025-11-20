package com.example.healthylifehub.utils;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized error logging utility for asynchronous operations.
 * Provides consistent error logging with context information across all CompletableFuture chains.
 * 
 * This utility helps track and debug async errors by including:
 * - Operation type (login, sync, analysis, etc.)
 * - User context (user ID when available)
 * - Error details and stack traces
 * - Timestamp information
 */
public class AsyncErrorLogger {
    
    private static final String TAG = "AsyncError";
    
    /**
     * Log an error from a CompletableFuture chain with context information.
     * Also records the exception in Firebase Crashlytics with custom keys for debugging.
     * 
     * @param operationType The type of operation that failed (e.g., "login", "sync", "analysis")
     * @param throwable The exception that occurred
     * @param context Additional context information (userId, metricType, etc.)
     */
    public static void logError(String operationType, Throwable throwable, Map<String, String> context) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Async operation failed: ").append(operationType);
        
        // Get Crashlytics instance
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        
        // Set operation type as custom key
        crashlytics.setCustomKey("operation_type", operationType);
        crashlytics.setCustomKey("error_category", "async_operation");
        
        if (context != null && !context.isEmpty()) {
            logMessage.append(" | Context: ");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                
                // Add each context entry as a custom key in Crashlytics
                crashlytics.setCustomKey(entry.getKey(), entry.getValue());
            }
            // Remove trailing comma and space
            logMessage.setLength(logMessage.length() - 2);
        }
        
        // Log to Android logcat
        Log.e(TAG, logMessage.toString(), throwable);
        
        // Record exception in Firebase Crashlytics
        crashlytics.recordException(throwable);
    }
    
    /**
     * Log an error with user ID context.
     * Sets the user identifier in Crashlytics for better tracking.
     * 
     * @param operationType The type of operation that failed
     * @param throwable The exception that occurred
     * @param userId The user ID associated with the operation
     */
    public static void logError(String operationType, Throwable throwable, String userId) {
        Map<String, String> context = new HashMap<>();
        if (userId != null) {
            context.put("userId", userId);
            // Set user identifier in Crashlytics
            FirebaseCrashlytics.getInstance().setUserId(userId);
        }
        logError(operationType, throwable, context);
    }
    
    /**
     * Log an error without additional context.
     * 
     * @param operationType The type of operation that failed
     * @param throwable The exception that occurred
     */
    public static void logError(String operationType, Throwable throwable) {
        logError(operationType, throwable, (Map<String, String>) null);
    }
    
    /**
     * Create a context map builder for fluent API usage.
     * 
     * Example:
     * <pre>
     * AsyncErrorLogger.context()
     *     .put("userId", userId)
     *     .put("metricType", "blood_pressure")
     *     .log("metric_analysis", throwable);
     * </pre>
     * 
     * @return A new ContextBuilder instance
     */
    public static ContextBuilder context() {
        return new ContextBuilder();
    }
    
    /**
     * Builder class for creating context maps with fluent API.
     */
    public static class ContextBuilder {
        private final Map<String, String> context = new HashMap<>();
        
        /**
         * Add a context key-value pair.
         * 
         * @param key The context key
         * @param value The context value
         * @return This builder for chaining
         */
        public ContextBuilder put(String key, String value) {
            if (key != null && value != null) {
                context.put(key, value);
            }
            return this;
        }
        
        /**
         * Log the error with the built context.
         * 
         * @param operationType The type of operation that failed
         * @param throwable The exception that occurred
         */
        public void log(String operationType, Throwable throwable) {
            AsyncErrorLogger.logError(operationType, throwable, context);
        }
        
        /**
         * Get the built context map.
         * 
         * @return The context map
         */
        public Map<String, String> build() {
            return new HashMap<>(context);
        }
    }
    
    /**
     * Log a warning for async operations (non-fatal issues).
     * 
     * @param operationType The type of operation
     * @param message The warning message
     * @param context Additional context information
     */
    public static void logWarning(String operationType, String message, Map<String, String> context) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Async operation warning: ").append(operationType);
        logMessage.append(" | Message: ").append(message);
        
        if (context != null && !context.isEmpty()) {
            logMessage.append(" | Context: ");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            logMessage.setLength(logMessage.length() - 2);
        }
        
        Log.w(TAG, logMessage.toString());
    }
    
    /**
     * Log debug information for async operations.
     * 
     * @param operationType The type of operation
     * @param message The debug message
     */
    public static void logDebug(String operationType, String message) {
        Log.d(TAG, "Async operation: " + operationType + " | " + message);
    }
}
