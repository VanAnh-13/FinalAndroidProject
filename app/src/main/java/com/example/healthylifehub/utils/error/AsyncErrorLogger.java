package com.example.healthylifehub.utils.error;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized error logging utility for asynchronous operations.
 */
public class AsyncErrorLogger {
    
    private static final String TAG = "AsyncError";
    
    public static void logError(String operationType, Throwable throwable, Map<String, String> context) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Async operation failed: ").append(operationType);
        
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey("operation_type", operationType);
        crashlytics.setCustomKey("error_category", "async_operation");
        
        if (context != null && !context.isEmpty()) {
            logMessage.append(" | Context: ");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                crashlytics.setCustomKey(entry.getKey(), entry.getValue());
            }
            logMessage.setLength(logMessage.length() - 2);
        }
        
        Log.e(TAG, logMessage.toString(), throwable);
        crashlytics.recordException(throwable);
    }
    
    public static void logError(String operationType, Throwable throwable, String userId) {
        Map<String, String> context = new HashMap<>();
        if (userId != null) {
            context.put("userId", userId);
            FirebaseCrashlytics.getInstance().setUserId(userId);
        }
        logError(operationType, throwable, context);
    }
    
    public static void logError(String operationType, Throwable throwable) {
        logError(operationType, throwable, (Map<String, String>) null);
    }
    
    public static ContextBuilder context() {
        return new ContextBuilder();
    }
    
    public static class ContextBuilder {
        private final Map<String, String> context = new HashMap<>();
        
        public ContextBuilder put(String key, String value) {
            if (key != null && value != null) {
                context.put(key, value);
            }
            return this;
        }
        
        public void log(String operationType, Throwable throwable) {
            AsyncErrorLogger.logError(operationType, throwable, context);
        }
        
        public Map<String, String> build() {
            return new HashMap<>(context);
        }
    }
    
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
    
    public static void logDebug(String operationType, String message) {
        Log.d(TAG, "Async operation: " + operationType + " | " + message);
    }
}
