package com.example.healthylifehub.utils.retry;

import android.content.Context;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.healthylifehub.utils.error.ErrorHandler;

import java.util.concurrent.TimeUnit;

/**
 * Retry mechanism utility for failed background operations
 * Implements exponential backoff and intelligent retry strategies
 * Requirements: 8.5
 */
public class RetryManager {
    
    private static final String TAG = "RetryManager";
    
    // Retry configuration constants
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_DELAY_MS = 1000; // 1 second
    private static final long MAX_BACKOFF_DELAY_MS = 30000; // 30 seconds
    
    // Retry strategies
    public enum RetryStrategy {
        IMMEDIATE,      // Retry immediately
        LINEAR,         // Linear backoff (1s, 2s, 3s, ...)
        EXPONENTIAL,    // Exponential backoff (1s, 2s, 4s, 8s, ...)
        FIBONACCI,      // Fibonacci backoff (1s, 1s, 2s, 3s, 5s, ...)
        CUSTOM          // Custom backoff defined by caller
    }
    
    // Retry context for tracking retry attempts
    public static class RetryContext {
        public final String operationId;
        public final int currentAttempt;
        public final int maxAttempts;
        public final RetryStrategy strategy;
        public final long lastAttemptTime;
        public final Throwable lastException;
        
        public RetryContext(String operationId, int currentAttempt, int maxAttempts,
                          RetryStrategy strategy, long lastAttemptTime, Throwable lastException) {
            this.operationId = operationId;
            this.currentAttempt = currentAttempt;
            this.maxAttempts = maxAttempts;
            this.strategy = strategy;
            this.lastAttemptTime = lastAttemptTime;
            this.lastException = lastException;
        }
        
        public boolean canRetry() {
            return currentAttempt < maxAttempts;
        }
        
        public boolean isLastAttempt() {
            return currentAttempt >= maxAttempts - 1;
        }
        
        public int getRemainingAttempts() {
            return Math.max(0, maxAttempts - currentAttempt);
        }
    }
    
    /**
     * Calculate delay for next retry attempt based on strategy
     * 
     * @param attempt Current attempt number (0-based)
     * @param strategy Retry strategy to use
     * @param customDelayMs Custom delay for CUSTOM strategy
     * @return Delay in milliseconds for next retry
     */
    public static long calculateRetryDelay(int attempt, RetryStrategy strategy, long customDelayMs) {
        long delay;
        
        switch (strategy) {
            case IMMEDIATE:
                delay = 0;
                break;
                
            case LINEAR:
                delay = INITIAL_BACKOFF_DELAY_MS * (attempt + 1);
                break;
                
            case EXPONENTIAL:
                delay = INITIAL_BACKOFF_DELAY_MS * (long) Math.pow(2, attempt);
                break;
                
            case FIBONACCI:
                delay = INITIAL_BACKOFF_DELAY_MS * fibonacci(attempt + 1);
                break;
                
            case CUSTOM:
                delay = customDelayMs;
                break;
                
            default:
                delay = INITIAL_BACKOFF_DELAY_MS;
                break;
        }
        
        // Cap the delay at maximum backoff
        return Math.min(delay, MAX_BACKOFF_DELAY_MS);
    }
    
    /**
     * Calculate Fibonacci number for Fibonacci backoff strategy
     */
    private static long fibonacci(int n) {
        if (n <= 1) return n;
        
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }
    
    /**
     * Determine if an exception is retryable
     * 
     * @param exception The exception to check
     * @return true if the operation should be retried
     */
    public static boolean isRetryableException(Throwable exception) {
        if (exception == null) {
            return false;
        }
        
        String exceptionName = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        // Network-related exceptions are usually retryable
        if (exceptionName.contains("Network") || exceptionName.contains("Connection") ||
            exceptionName.contains("Timeout") || exceptionName.contains("Socket")) {
            return true;
        }
        
        // Database lock exceptions are retryable
        if (message != null && (message.contains("database is locked") || 
                               message.contains("SQLITE_BUSY") ||
                               message.contains("transaction"))) {
            return true;
        }
        
        // OutOfMemoryError is not retryable
        if (exception instanceof OutOfMemoryError) {
            return false;
        }
        
        // SecurityException is not retryable
        if (exception instanceof SecurityException) {
            return false;
        }
        
        // IllegalArgumentException is usually not retryable
        if (exception instanceof IllegalArgumentException) {
            return false;
        }
        
        // NullPointerException is usually not retryable
        if (exception instanceof NullPointerException) {
            return false;
        }
        
        // Default to retryable for unknown exceptions
        return true;
    }
    
    /**
     * Create retry context for an operation
     * 
     * @param operationId Unique identifier for the operation
     * @param currentAttempt Current attempt number (0-based)
     * @param exception Exception that caused the failure
     * @return RetryContext object
     */
    public static RetryContext createRetryContext(String operationId, int currentAttempt, Throwable exception) {
        return new RetryContext(
            operationId,
            currentAttempt,
            MAX_RETRY_ATTEMPTS,
            RetryStrategy.EXPONENTIAL,
            System.currentTimeMillis(),
            exception
        );
    }
    
    /**
     * Create retry context with custom configuration
     */
    public static RetryContext createRetryContext(String operationId, int currentAttempt, 
                                                int maxAttempts, RetryStrategy strategy, Throwable exception) {
        return new RetryContext(
            operationId,
            currentAttempt,
            maxAttempts,
            strategy,
            System.currentTimeMillis(),
            exception
        );
    }
    
    /**
     * Schedule retry for WorkManager operation
     * 
     * @param context Application context
     * @param workerClass Worker class to retry
     * @param inputData Input data for the worker
     * @param retryContext Retry context information
     * @return Work request ID if scheduled, null if max retries exceeded
     */
    public static String scheduleRetry(Context context, Class<? extends androidx.work.Worker> workerClass,
                                     Data inputData, RetryContext retryContext) {
        
        if (!retryContext.canRetry()) {
            Log.w(TAG, "Max retry attempts exceeded for operation: " + retryContext.operationId);
            
            // Handle max retries exceeded
            ErrorHandler.handleBackgroundError(
                context,
                ErrorHandler.ERROR_RETRY_LIMIT_EXCEEDED,
                retryContext.lastException,
                retryContext.operationId,
                retryContext.currentAttempt,
                retryContext.maxAttempts
            );
            
            return null;
        }
        
        // Calculate delay for next attempt
        long delay = calculateRetryDelay(retryContext.currentAttempt, retryContext.strategy, 0);
        
        // Create constraints for retry
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();
        
        // Add retry attempt information to input data
        Data retryInputData = new Data.Builder()
            .putAll(inputData)
            .putInt("retry_attempt", retryContext.currentAttempt + 1)
            .putString("operation_id", retryContext.operationId)
            .putLong("original_timestamp", retryContext.lastAttemptTime)
            .build();
        
        // Create work request with backoff policy
        OneTimeWorkRequest retryRequest = new OneTimeWorkRequest.Builder(workerClass)
            .setInputData(retryInputData)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_DELAY_MS, TimeUnit.MILLISECONDS)
            .addTag("retry_" + retryContext.operationId)
            .build();
        
        // Enqueue the retry work
        WorkManager.getInstance(context).enqueue(retryRequest);
        
        Log.d(TAG, String.format("Scheduled retry for operation %s: attempt %d/%d, delay %dms",
            retryContext.operationId, retryContext.currentAttempt + 1, retryContext.maxAttempts, delay));
        
        return retryRequest.getId().toString();
    }
    
    /**
     * Cancel all retry attempts for an operation
     * 
     * @param context Application context
     * @param operationId Operation ID to cancel retries for
     */
    public static void cancelRetries(Context context, String operationId) {
        WorkManager.getInstance(context).cancelAllWorkByTag("retry_" + operationId);
        Log.d(TAG, "Cancelled all retry attempts for operation: " + operationId);
    }
    
    /**
     * Get retry delay for display purposes
     * 
     * @param retryContext Retry context
     * @return Human-readable delay string
     */
    public static String getRetryDelayString(RetryContext retryContext) {
        if (!retryContext.canRetry()) {
            return "No more retries";
        }
        
        long delay = calculateRetryDelay(retryContext.currentAttempt, retryContext.strategy, 0);
        
        if (delay < 1000) {
            return "Immediately";
        } else if (delay < 60000) {
            return (delay / 1000) + " seconds";
        } else {
            return (delay / 60000) + " minutes";
        }
    }
    
    /**
     * Log retry attempt information
     * 
     * @param retryContext Retry context
     * @param success Whether the retry was successful
     */
    public static void logRetryAttempt(RetryContext retryContext, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Retry attempt %d/%d for operation %s: %s",
            retryContext.currentAttempt + 1, retryContext.maxAttempts, retryContext.operationId, status);
        
        if (success) {
            Log.i(TAG, "✅ " + message);
        } else {
            Log.w(TAG, "❌ " + message);
            if (retryContext.lastException != null) {
                Log.w(TAG, "Exception: " + retryContext.lastException.getMessage());
            }
        }
    }
    
    /**
     * Check if operation should use retry mechanism based on error type
     * 
     * @param exception The exception that occurred
     * @param operationType Type of operation (for context)
     * @return true if retry mechanism should be used
     */
    public static boolean shouldUseRetry(Throwable exception, String operationType) {
        // Always use retry for retryable exceptions
        if (isRetryableException(exception)) {
            return true;
        }
        
        // Use retry for critical operations even with non-retryable exceptions
        if ("reminder_update".equals(operationType) || "notification_send".equals(operationType)) {
            return true;
        }
        
        return false;
    }
}
