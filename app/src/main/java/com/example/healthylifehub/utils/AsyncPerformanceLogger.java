package com.example.healthylifehub.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Performance logging utility for asynchronous operations.
 * Tracks execution times, thread pool metrics, and operation durations.
 * 
 * This utility helps monitor and optimize async performance by logging:
 * - Operation start and end times
 * - Duration of async operations
 * - Thread pool queue sizes and active thread counts
 * - Performance metrics for analysis
 */
public class AsyncPerformanceLogger {
    
    private static final String TAG = "AsyncPerformance";
    
    // Store operation start times
    private static final Map<String, Long> operationStartTimes = new ConcurrentHashMap<>();
    
    /**
     * Log the start of an async operation.
     * 
     * @param operationId Unique identifier for this operation instance
     * @param operationType Type of operation (e.g., "login", "sync", "analysis")
     */
    public static void logStart(String operationId, String operationType) {
        long startTime = System.currentTimeMillis();
        operationStartTimes.put(operationId, startTime);
        
        Log.d(TAG, String.format("START | Operation: %s | ID: %s | Time: %d",
            operationType, operationId, startTime));
    }
    
    /**
     * Log the start of an async operation with context.
     * 
     * @param operationId Unique identifier for this operation instance
     * @param operationType Type of operation
     * @param context Additional context information
     */
    public static void logStart(String operationId, String operationType, Map<String, String> context) {
        long startTime = System.currentTimeMillis();
        operationStartTimes.put(operationId, startTime);
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("START | Operation: %s | ID: %s | Time: %d",
            operationType, operationId, startTime));
        
        if (context != null && !context.isEmpty()) {
            logMessage.append(" | Context: ");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            logMessage.setLength(logMessage.length() - 2);
        }
        
        Log.d(TAG, logMessage.toString());
    }
    
    /**
     * Log the end of an async operation and calculate duration.
     * 
     * @param operationId Unique identifier for this operation instance
     * @param operationType Type of operation
     */
    public static void logEnd(String operationId, String operationType) {
        long endTime = System.currentTimeMillis();
        Long startTime = operationStartTimes.remove(operationId);
        
        if (startTime != null) {
            long duration = endTime - startTime;
            
            Log.d(TAG, String.format("END | Operation: %s | ID: %s | Duration: %dms",
                operationType, operationId, duration));
            
            // Log warning if operation took too long
            if (duration > 5000) {
                Log.w(TAG, String.format("SLOW OPERATION | %s took %dms (>5s)",
                    operationType, duration));
            }
        } else {
            Log.w(TAG, String.format("END | Operation: %s | ID: %s | No start time found",
                operationType, operationId));
        }
    }
    
    /**
     * Log the end of an async operation with result status.
     * 
     * @param operationId Unique identifier for this operation instance
     * @param operationType Type of operation
     * @param success Whether the operation succeeded
     */
    public static void logEnd(String operationId, String operationType, boolean success) {
        long endTime = System.currentTimeMillis();
        Long startTime = operationStartTimes.remove(operationId);
        
        if (startTime != null) {
            long duration = endTime - startTime;
            String status = success ? "SUCCESS" : "FAILED";
            
            Log.d(TAG, String.format("END | Operation: %s | ID: %s | Status: %s | Duration: %dms",
                operationType, operationId, status, duration));
            
            // Log warning if operation took too long
            if (duration > 5000) {
                Log.w(TAG, String.format("SLOW OPERATION | %s took %dms (>5s)",
                    operationType, duration));
            }
        } else {
            Log.w(TAG, String.format("END | Operation: %s | ID: %s | No start time found",
                operationType, operationId));
        }
    }
    
    /**
     * Log thread pool metrics for monitoring.
     * 
     * @param poolName Name of the thread pool (e.g., "IO", "Compute")
     * @param executor The ExecutorService to monitor
     */
    public static void logThreadPoolMetrics(String poolName, ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
            
            int activeCount = threadPool.getActiveCount();
            int poolSize = threadPool.getPoolSize();
            int corePoolSize = threadPool.getCorePoolSize();
            int maximumPoolSize = threadPool.getMaximumPoolSize();
            long completedTaskCount = threadPool.getCompletedTaskCount();
            long taskCount = threadPool.getTaskCount();
            int queueSize = threadPool.getQueue().size();
            
            Log.d(TAG, String.format(
                "THREAD POOL | Name: %s | Active: %d/%d | Core: %d | Max: %d | Queue: %d | Completed: %d/%d",
                poolName, activeCount, poolSize, corePoolSize, maximumPoolSize, 
                queueSize, completedTaskCount, taskCount));
            
            // Log warning if queue is getting large
            if (queueSize > 10) {
                Log.w(TAG, String.format("LARGE QUEUE | %s thread pool has %d tasks queued",
                    poolName, queueSize));
            }
            
            // Log warning if all threads are active
            if (activeCount >= maximumPoolSize) {
                Log.w(TAG, String.format("POOL SATURATED | %s thread pool is at maximum capacity",
                    poolName));
            }
        } else {
            Log.d(TAG, String.format("THREAD POOL | Name: %s | Type: %s (metrics not available)",
                poolName, executor.getClass().getSimpleName()));
        }
    }
    
    /**
     * Create a performance context builder for fluent API usage.
     * 
     * Example:
     * <pre>
     * String opId = AsyncPerformanceLogger.operation("user_login")
     *     .withContext("userId", userId)
     *     .withContext("method", "email")
     *     .start();
     * // ... perform operation ...
     * AsyncPerformanceLogger.end(opId, "user_login", true);
     * </pre>
     * 
     * @param operationType The type of operation
     * @return A new OperationBuilder instance
     */
    public static OperationBuilder operation(String operationType) {
        return new OperationBuilder(operationType);
    }
    
    /**
     * Builder class for creating performance-tracked operations with fluent API.
     */
    public static class OperationBuilder {
        private final String operationType;
        private final String operationId;
        private final Map<String, String> context = new HashMap<>();
        
        private OperationBuilder(String operationType) {
            this.operationType = operationType;
            this.operationId = generateOperationId(operationType);
        }
        
        /**
         * Add a context key-value pair.
         * 
         * @param key The context key
         * @param value The context value
         * @return This builder for chaining
         */
        public OperationBuilder withContext(String key, String value) {
            if (key != null && value != null) {
                context.put(key, value);
            }
            return this;
        }
        
        /**
         * Start the operation and begin tracking performance.
         * 
         * @return The operation ID for use with logEnd()
         */
        public String start() {
            logStart(operationId, operationType, context);
            return operationId;
        }
        
        /**
         * Get the operation ID without starting (for manual start).
         * 
         * @return The operation ID
         */
        public String getOperationId() {
            return operationId;
        }
        
        /**
         * Get the operation type.
         * 
         * @return The operation type
         */
        public String getOperationType() {
            return operationType;
        }
    }
    
    /**
     * Generate a unique operation ID.
     * 
     * @param operationType The type of operation
     * @return A unique operation ID
     */
    private static String generateOperationId(String operationType) {
        return operationType + "_" + System.currentTimeMillis() + "_" + 
               Thread.currentThread().getId();
    }
    
    /**
     * Log a performance metric.
     * 
     * @param metricName Name of the metric
     * @param value Value of the metric
     * @param unit Unit of measurement
     */
    public static void logMetric(String metricName, long value, String unit) {
        Log.d(TAG, String.format("METRIC | %s: %d %s", metricName, value, unit));
    }
    
    /**
     * Log a performance metric with context.
     * 
     * @param metricName Name of the metric
     * @param value Value of the metric
     * @param unit Unit of measurement
     * @param context Additional context
     */
    public static void logMetric(String metricName, long value, String unit, Map<String, String> context) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("METRIC | %s: %d %s", metricName, value, unit));
        
        if (context != null && !context.isEmpty()) {
            logMessage.append(" | Context: ");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            logMessage.setLength(logMessage.length() - 2);
        }
        
        Log.d(TAG, logMessage.toString());
    }
    
    /**
     * Clear all stored operation start times.
     * Useful for cleanup or testing.
     */
    public static void clear() {
        operationStartTimes.clear();
    }
}
