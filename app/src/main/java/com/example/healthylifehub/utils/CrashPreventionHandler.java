package com.example.healthylifehub.utils;

import android.content.Context;
import android.util.Log;
import com.example.healthylifehub.R;
import com.example.healthylifehub.utils.ui.UserFeedbackManager;

/**
 * Comprehensive crash prevention handler
 * Provides graceful error handling to prevent app crashes
 */
public class CrashPreventionHandler {
    
    private static final String TAG = "CrashPrevention";
    
    /**
     * Safe operation executor that prevents crashes
     */
    public static <T> T safeExecute(Context context, SafeOperation<T> operation, T defaultValue) {
        try {
            return operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Safe execution failed", e);
            
            // Handle the error gracefully
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                                   ErrorHandler.ErrorSeverity.MEDIUM, 
                                   ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, 
                                   "Safe execution wrapper");
            
            return defaultValue;
        }
    }
    
    /**
     * Safe operation executor without return value
     */
    public static void safeExecute(Context context, SafeVoidOperation operation) {
        try {
            operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Safe void execution failed", e);
            
            // Handle the error gracefully
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                                   ErrorHandler.ErrorSeverity.MEDIUM, 
                                   ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, 
                                   "Safe void execution wrapper");
        }
    }
    
    /**
     * Safe UI operation executor
     */
    public static void safeUIExecute(Context context, SafeVoidOperation operation, 
                                   String userMessage) {
        try {
            operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Safe UI execution failed", e);
            
            // Show user-friendly message
            if (userMessage != null && !userMessage.isEmpty()) {
                UserFeedbackManager.showError(context, userMessage);
            } else {
                UserFeedbackManager.showError(context, 
                    context.getString(R.string.error_unexpected));
            }
            
            // Log the error
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                                   ErrorHandler.ErrorSeverity.LOW, 
                                   ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, 
                                   "Safe UI execution wrapper");
        }
    }
    
    /**
     * Safe database operation executor
     */
    public static <T> T safeDatabaseExecute(Context context, SafeOperation<T> operation, 
                                          T defaultValue, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Database operation failed: " + operationName, e);
            
            // Handle database error specifically
            ErrorHandler.handleDatabaseError(context, 
                                           ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, 
                                           e, operationName, "unknown");
            
            // Show user-friendly message
            UserFeedbackManager.showError(context, 
                context.getString(R.string.error_database_operation));
            
            return defaultValue;
        }
    }
    
    /**
     * Safe network operation executor
     */
    public static <T> T safeNetworkExecute(Context context, SafeOperation<T> operation, 
                                         T defaultValue, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            Log.e(TAG, "Network operation failed: " + operationName, e);
            
            // Handle network error specifically
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.NETWORK, 
                                   ErrorHandler.ErrorSeverity.MEDIUM, 
                                   ErrorHandler.ERROR_DATABASE_CONNECTION_FAILED, e, 
                                   "Network operation: " + operationName);
            
            // Show user-friendly message
            UserFeedbackManager.showError(context, 
                context.getString(R.string.error_network_unavailable));
            
            return defaultValue;
        }
    }
    
    /**
     * Safe file operation executor
     */
    public static <T> T safeFileExecute(Context context, SafeOperation<T> operation, 
                                      T defaultValue, String fileName) {
        try {
            return operation.execute();
        } catch (SecurityException e) {
            Log.e(TAG, "File permission denied: " + fileName, e);
            
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.PERMISSION, 
                                   ErrorHandler.ErrorSeverity.MEDIUM, 
                                   ErrorHandler.ERROR_NOTIFICATION_PERMISSION_DENIED, e, 
                                   "File operation: " + fileName);
            
            UserFeedbackManager.showError(context, 
                context.getString(R.string.error_permission_required));
            
            return defaultValue;
        } catch (Exception e) {
            Log.e(TAG, "File operation failed: " + fileName, e);
            
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                                   ErrorHandler.ErrorSeverity.MEDIUM, 
                                   ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, e, 
                                   "File operation: " + fileName);
            
            UserFeedbackManager.showError(context, "Lỗi thao tác với tệp: " + fileName);
            
            return defaultValue;
        }
    }
    
    /**
     * Safe parsing operation executor
     */
    public static <T> T safeParseExecute(Context context, SafeOperation<T> operation, 
                                       T defaultValue, String dataType) {
        try {
            return operation.execute();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number parsing failed for: " + dataType, e);
            
            ErrorHandler.handleValidationError(context, 
                                             ErrorHandler.ERROR_INVALID_TIME, dataType);
            
            UserFeedbackManager.showError(context, 
                "Dữ liệu " + dataType + " không hợp lệ");
            
            return defaultValue;
        } catch (Exception e) {
            Log.e(TAG, "Parsing failed for: " + dataType, e);
            
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.VALIDATION, 
                                   ErrorHandler.ErrorSeverity.LOW, 
                                   ErrorHandler.ERROR_INVALID_TIME, e, 
                                   "Parsing: " + dataType);
            
            UserFeedbackManager.showError(context, 
                "Không thể xử lý dữ liệu " + dataType);
            
            return defaultValue;
        }
    }
    
    /**
     * Safe null check with default value
     */
    public static <T> T safeNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Safe string operation
     */
    public static String safeString(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    /**
     * Safe array access
     */
    public static <T> T safeArrayAccess(T[] array, int index, T defaultValue) {
        if (array == null || index < 0 || index >= array.length) {
            return defaultValue;
        }
        return array[index];
    }
    
    /**
     * Safe list access
     */
    public static <T> T safeListAccess(java.util.List<T> list, int index, T defaultValue) {
        if (list == null || index < 0 || index >= list.size()) {
            return defaultValue;
        }
        return list.get(index);
    }
    
    /**
     * Safe integer parsing
     */
    public static int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safe double parsing
     */
    public static double safeParseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safe long parsing
     */
    public static long safeParseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Interface for safe operations with return value
     */
    public interface SafeOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Interface for safe operations without return value
     */
    public interface SafeVoidOperation {
        void execute() throws Exception;
    }
    
    /**
     * Global exception handler setup
     */
    public static void setupGlobalExceptionHandler(Context context) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            Log.e(TAG, "Uncaught exception in thread " + thread.getName(), exception);
            
            // Log to crash reporting
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.GENERAL, 
                                   ErrorHandler.ErrorSeverity.CRITICAL, 
                                   ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, exception, 
                                   "Uncaught exception in thread: " + thread.getName());
            
            // In production, you might want to restart the app or show a crash dialog
            // For now, we'll let the system handle it
            System.exit(1);
        });
    }
}