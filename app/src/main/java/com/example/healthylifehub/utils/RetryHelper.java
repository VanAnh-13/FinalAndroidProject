package com.example.healthylifehub.utils;

import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * RetryHelper provides utilities for retrying failed async operations with exponential backoff.
 * 
 * This is useful for handling transient failures like:
 * - Network timeouts
 * - Temporary server errors
 * - Database lock conflicts
 * 
 * The exponential backoff strategy increases delay between retries to avoid overwhelming the system.
 */
public class RetryHelper {
    
    private static final String TAG = "RetryHelper";
    
    /**
     * Execute a task with retry logic and exponential backoff.
     * 
     * @param <T> The type of result expected from the task
     * @param task The task to execute
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds before first retry
     * @param executor ExecutorService to run the task on
     * @return CompletableFuture containing the result
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            Supplier<T> task,
            int maxRetries,
            long initialDelayMs,
            ExecutorService executor) {
        
        return executeWithRetryInternal(task, maxRetries, initialDelayMs, 0, executor);
    }
    
    /**
     * Internal method for recursive retry logic
     */
    private static <T> CompletableFuture<T> executeWithRetryInternal(
            Supplier<T> task,
            int maxRetries,
            long delayMs,
            int attemptNumber,
            ExecutorService executor) {
        
        return CompletableFuture.supplyAsync(task, executor)
            .exceptionally(throwable -> {
                if (maxRetries > 0) {
                    Log.w(TAG, "Attempt " + (attemptNumber + 1) + " failed, retrying in " + delayMs + "ms", throwable);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                    
                    // Exponential backoff: double the delay for next retry
                    return executeWithRetryInternal(
                        task,
                        maxRetries - 1,
                        delayMs * 2,
                        attemptNumber + 1,
                        executor
                    ).join();
                } else {
                    Log.e(TAG, "All retry attempts exhausted", throwable);
                    throw new CompletionException(throwable);
                }
            });
    }
    
    /**
     * Execute a task with default retry settings (3 retries, 1 second initial delay)
     * 
     * @param <T> The type of result expected from the task
     * @param task The task to execute
     * @param executor ExecutorService to run the task on
     * @return CompletableFuture containing the result
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            Supplier<T> task,
            ExecutorService executor) {
        return executeWithRetry(task, 3, 1000, executor);
    }
    
    /**
     * Check if an exception is retryable (transient error)
     * 
     * @param throwable The exception to check
     * @return true if the exception is retryable
     */
    public static boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        
        // Common retryable error patterns
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("unavailable") ||
               message.contains("503") ||
               message.contains("504");
    }
    
    /**
     * Execute a task with retry only for retryable exceptions
     * 
     * @param <T> The type of result expected from the task
     * @param task The task to execute
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param executor ExecutorService to run the task on
     * @return CompletableFuture containing the result
     */
    public static <T> CompletableFuture<T> executeWithSmartRetry(
            Supplier<T> task,
            int maxRetries,
            long initialDelayMs,
            ExecutorService executor) {
        
        return CompletableFuture.supplyAsync(task, executor)
            .exceptionally(throwable -> {
                if (maxRetries > 0 && isRetryable(throwable)) {
                    Log.w(TAG, "Retryable error detected, retrying in " + initialDelayMs + "ms", throwable);
                    
                    try {
                        Thread.sleep(initialDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                    
                    return executeWithSmartRetry(
                        task,
                        maxRetries - 1,
                        initialDelayMs * 2,
                        executor
                    ).join();
                } else {
                    if (!isRetryable(throwable)) {
                        Log.e(TAG, "Non-retryable error, failing immediately", throwable);
                    } else {
                        Log.e(TAG, "Max retries reached", throwable);
                    }
                    throw new CompletionException(throwable);
                }
            });
    }
}
