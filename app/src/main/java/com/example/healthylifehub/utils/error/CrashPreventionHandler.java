package com.example.healthylifehub.utils.error;

import android.content.Context;
import android.util.Log;
import com.example.healthylifehub.R;
import com.example.healthylifehub.utils.ui.UserFeedbackManager;

/**
 * Comprehensive crash prevention handler
 */
public class CrashPreventionHandler {
    
    private static final String TAG = "CrashPrevention";
    
    public static <T> T safeExecute(Context context, SafeOperation<T> operation, T defaultValue) {
        try {
            return operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Safe execution failed", e);
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                ErrorHandler.ErrorSeverity.MEDIUM, ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, "Safe execution");
            return defaultValue;
        }
    }
    
    public static void safeExecute(Context context, SafeVoidOperation operation) {
        try {
            operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Safe void execution failed", e);
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                ErrorHandler.ErrorSeverity.MEDIUM, ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, "Safe void execution");
        }
    }
    
    public static void safeUIExecute(Context context, SafeVoidOperation operation, String userMessage) {
        try {
            operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Safe UI execution failed", e);
            if (userMessage != null && !userMessage.isEmpty()) {
                UserFeedbackManager.showError(context, userMessage);
            } else {
                UserFeedbackManager.showError(context, context.getString(R.string.error_unexpected));
            }
        }
    }
    
    public static <T> T safeDatabaseExecute(Context context, SafeOperation<T> operation, T defaultValue, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Database operation failed: " + operationName, e);
            ErrorHandler.handleDatabaseError(context, ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, operationName, "unknown");
            UserFeedbackManager.showError(context, context.getString(R.string.error_database_operation));
            return defaultValue;
        }
    }
    
    public static <T> T safeNetworkExecute(Context context, SafeOperation<T> operation, T defaultValue, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Network operation failed: " + operationName, e);
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.NETWORK, 
                ErrorHandler.ErrorSeverity.MEDIUM, ErrorHandler.ERROR_DATABASE_CONNECTION_FAILED, e, "Network: " + operationName);
            UserFeedbackManager.showError(context, context.getString(R.string.error_network_unavailable));
            return defaultValue;
        }
    }
    
    public static <T> T safeNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    public static String safeString(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    public static <T> T safeArrayAccess(T[] array, int index, T defaultValue) {
        if (array == null || index < 0 || index >= array.length) return defaultValue;
        return array[index];
    }
    
    public static <T> T safeListAccess(java.util.List<T> list, int index, T defaultValue) {
        if (list == null || index < 0 || index >= list.size()) return defaultValue;
        return list.get(index);
    }
    
    public static int safeParseInt(String value, int defaultValue) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultValue; }
    }
    
    public static double safeParseDouble(String value, double defaultValue) {
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return defaultValue; }
    }
    
    public static long safeParseLong(String value, long defaultValue) {
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return defaultValue; }
    }
    
    public interface SafeOperation<T> { T execute() throws Exception; }
    public interface SafeVoidOperation { void execute() throws Exception; }
    
    public static void setupGlobalExceptionHandler(Context context) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            Log.e(TAG, "Uncaught exception in thread " + thread.getName(), exception);
            System.exit(1);
        });
    }
}
