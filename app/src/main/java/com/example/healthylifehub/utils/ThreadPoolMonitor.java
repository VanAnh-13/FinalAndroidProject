package com.example.healthylifehub.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Continuous monitoring utility for thread pools.
 * Tracks and logs thread pool metrics at regular intervals.
 * 
 * This monitor helps identify performance issues by tracking:
 * - Active thread count
 * - Queue size and growth
 * - Completed task count
 * - Pool saturation
 * - Task throughput
 * 
 * Usage:
 * <pre>
 * ThreadPoolMonitor monitor = new ThreadPoolMonitor();
 * monitor.registerPool("IO", ioExecutor);
 * monitor.registerPool("Compute", computeExecutor);
 * monitor.startMonitoring(5000); // Monitor every 5 seconds
 * // ... later ...
 * monitor.stopMonitoring();
 * </pre>
 */
public class ThreadPoolMonitor {
    
    private static final String TAG = "ThreadPoolMonitor";
    private static final long DEFAULT_INTERVAL_MS = 10000; // 10 seconds
    
    private final Map<String, MonitoredPool> monitoredPools = new ConcurrentHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private Runnable monitoringRunnable;
    private long intervalMs = DEFAULT_INTERVAL_MS;
    
    /**
     * Container for a monitored thread pool and its metrics.
     */
    private static class MonitoredPool {
        final String name;
        final ExecutorService executor;
        long lastCompletedTaskCount;
        long lastSampleTime;
        int maxQueueSize;
        int maxActiveThreads;
        long totalSamples;
        
        MonitoredPool(String name, ExecutorService executor) {
            this.name = name;
            this.executor = executor;
            this.lastCompletedTaskCount = 0;
            this.lastSampleTime = System.currentTimeMillis();
            this.maxQueueSize = 0;
            this.maxActiveThreads = 0;
            this.totalSamples = 0;
        }
    }
    
    /**
     * Snapshot of thread pool metrics at a point in time.
     */
    public static class PoolMetrics {
        public final String poolName;
        public final int activeThreads;
        public final int poolSize;
        public final int corePoolSize;
        public final int maximumPoolSize;
        public final int queueSize;
        public final long completedTaskCount;
        public final long taskCount;
        public final double tasksPerSecond;
        public final long timestamp;
        
        public PoolMetrics(String poolName, int activeThreads, int poolSize,
                          int corePoolSize, int maximumPoolSize, int queueSize,
                          long completedTaskCount, long taskCount,
                          double tasksPerSecond, long timestamp) {
            this.poolName = poolName;
            this.activeThreads = activeThreads;
            this.poolSize = poolSize;
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.queueSize = queueSize;
            this.completedTaskCount = completedTaskCount;
            this.taskCount = taskCount;
            this.tasksPerSecond = tasksPerSecond;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PoolMetrics[name=%s, active=%d/%d, core=%d, max=%d, queue=%d, " +
                "completed=%d/%d, throughput=%.2f tasks/s]",
                poolName, activeThreads, poolSize, corePoolSize, maximumPoolSize,
                queueSize, completedTaskCount, taskCount, tasksPerSecond
            );
        }
        
        /**
         * Check if the pool is saturated (all threads active).
         */
        public boolean isSaturated() {
            return activeThreads >= maximumPoolSize;
        }
        
        /**
         * Check if the queue is growing large.
         */
        public boolean hasLargeQueue() {
            return queueSize > 10;
        }
        
        /**
         * Get pool utilization percentage.
         */
        public double getUtilization() {
            return maximumPoolSize > 0 ? 
                (double) activeThreads / maximumPoolSize * 100.0 : 0.0;
        }
    }
    
    /**
     * Register a thread pool for monitoring.
     * 
     * @param name Descriptive name for the pool (e.g., "IO", "Compute")
     * @param executor The ExecutorService to monitor
     */
    public void registerPool(String name, ExecutorService executor) {
        if (executor == null) {
            Log.w(TAG, "Cannot register null executor: " + name);
            return;
        }
        
        monitoredPools.put(name, new MonitoredPool(name, executor));
        Log.i(TAG, "Registered thread pool for monitoring: " + name);
    }
    
    /**
     * Unregister a thread pool from monitoring.
     * 
     * @param name Name of the pool to unregister
     */
    public void unregisterPool(String name) {
        monitoredPools.remove(name);
        Log.i(TAG, "Unregistered thread pool from monitoring: " + name);
    }
    
    /**
     * Start monitoring all registered pools.
     * Metrics will be logged at the default interval (10 seconds).
     */
    public void startMonitoring() {
        startMonitoring(DEFAULT_INTERVAL_MS);
    }
    
    /**
     * Start monitoring all registered pools with custom interval.
     * 
     * @param intervalMs Interval between metric samples in milliseconds
     */
    public void startMonitoring(long intervalMs) {
        if (isMonitoring.get()) {
            Log.w(TAG, "Monitoring already started");
            return;
        }
        
        this.intervalMs = intervalMs;
        isMonitoring.set(true);
        
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring.get()) {
                    sampleAllPools();
                    handler.postDelayed(this, ThreadPoolMonitor.this.intervalMs);
                }
            }
        };
        
        handler.post(monitoringRunnable);
        Log.i(TAG, String.format("Started monitoring %d pools (interval: %dms)",
            monitoredPools.size(), intervalMs));
    }
    
    /**
     * Stop monitoring all pools.
     */
    public void stopMonitoring() {
        if (!isMonitoring.get()) {
            Log.w(TAG, "Monitoring not started");
            return;
        }
        
        isMonitoring.set(false);
        if (monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
        }
        
        Log.i(TAG, "Stopped monitoring");
        
        // Log final summary
        logSummary();
    }
    
    /**
     * Sample metrics from all registered pools.
     */
    private void sampleAllPools() {
        for (MonitoredPool pool : monitoredPools.values()) {
            PoolMetrics metrics = samplePool(pool);
            if (metrics != null) {
                logMetrics(metrics);
                updatePoolStats(pool, metrics);
            }
        }
    }
    
    /**
     * Sample metrics from a single pool.
     */
    private PoolMetrics samplePool(MonitoredPool pool) {
        if (!(pool.executor instanceof ThreadPoolExecutor)) {
            return null;
        }
        
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) pool.executor;
        long currentTime = System.currentTimeMillis();
        
        int activeThreads = threadPool.getActiveCount();
        int poolSize = threadPool.getPoolSize();
        int corePoolSize = threadPool.getCorePoolSize();
        int maximumPoolSize = threadPool.getMaximumPoolSize();
        int queueSize = threadPool.getQueue().size();
        long completedTaskCount = threadPool.getCompletedTaskCount();
        long taskCount = threadPool.getTaskCount();
        
        // Calculate throughput (tasks per second)
        long timeDelta = currentTime - pool.lastSampleTime;
        long taskDelta = completedTaskCount - pool.lastCompletedTaskCount;
        double tasksPerSecond = timeDelta > 0 ? 
            (taskDelta * 1000.0) / timeDelta : 0.0;
        
        return new PoolMetrics(
            pool.name, activeThreads, poolSize, corePoolSize, maximumPoolSize,
            queueSize, completedTaskCount, taskCount, tasksPerSecond, currentTime
        );
    }
    
    /**
     * Update pool statistics based on current metrics.
     */
    private void updatePoolStats(MonitoredPool pool, PoolMetrics metrics) {
        pool.lastCompletedTaskCount = metrics.completedTaskCount;
        pool.lastSampleTime = metrics.timestamp;
        pool.totalSamples++;
        
        if (metrics.queueSize > pool.maxQueueSize) {
            pool.maxQueueSize = metrics.queueSize;
        }
        
        if (metrics.activeThreads > pool.maxActiveThreads) {
            pool.maxActiveThreads = metrics.activeThreads;
        }
    }
    
    /**
     * Log metrics for a pool.
     */
    private void logMetrics(PoolMetrics metrics) {
        Log.d(TAG, String.format(
            "POOL[%s] | Active: %d/%d (%.1f%%) | Queue: %d | Completed: %d/%d | Throughput: %.2f tasks/s",
            metrics.poolName, metrics.activeThreads, metrics.maximumPoolSize,
            metrics.getUtilization(), metrics.queueSize, metrics.completedTaskCount,
            metrics.taskCount, metrics.tasksPerSecond
        ));
        
        // Log warnings for potential issues
        if (metrics.isSaturated()) {
            Log.w(TAG, String.format(
                "POOL[%s] SATURATED | All %d threads are active - consider increasing pool size",
                metrics.poolName, metrics.maximumPoolSize
            ));
        }
        
        if (metrics.hasLargeQueue()) {
            Log.w(TAG, String.format(
                "POOL[%s] LARGE QUEUE | %d tasks queued - pool may be undersized",
                metrics.poolName, metrics.queueSize
            ));
        }
        
        // Log info for low utilization
        if (metrics.getUtilization() < 20.0 && metrics.taskCount > 10) {
            Log.i(TAG, String.format(
                "POOL[%s] LOW UTILIZATION | Only %.1f%% utilized - pool may be oversized",
                metrics.poolName, metrics.getUtilization()
            ));
        }
    }
    
    /**
     * Log summary statistics for all pools.
     */
    private void logSummary() {
        Log.i(TAG, "\n========================================");
        Log.i(TAG, "Thread Pool Monitoring Summary");
        Log.i(TAG, "========================================");
        
        for (MonitoredPool pool : monitoredPools.values()) {
            if (pool.executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor threadPool = (ThreadPoolExecutor) pool.executor;
                
                Log.i(TAG, String.format(
                    "Pool: %s | Samples: %d | Max Queue: %d | Max Active: %d | Total Completed: %d",
                    pool.name, pool.totalSamples, pool.maxQueueSize,
                    pool.maxActiveThreads, threadPool.getCompletedTaskCount()
                ));
            }
        }
        
        Log.i(TAG, "========================================\n");
    }
    
    /**
     * Get current metrics for a specific pool.
     * 
     * @param poolName Name of the pool
     * @return Current metrics or null if pool not found
     */
    public PoolMetrics getCurrentMetrics(String poolName) {
        MonitoredPool pool = monitoredPools.get(poolName);
        if (pool == null) {
            return null;
        }
        return samplePool(pool);
    }
    
    /**
     * Get current metrics for all registered pools.
     * 
     * @return Map of pool names to their current metrics
     */
    public Map<String, PoolMetrics> getAllCurrentMetrics() {
        Map<String, PoolMetrics> allMetrics = new ConcurrentHashMap<>();
        
        for (MonitoredPool pool : monitoredPools.values()) {
            PoolMetrics metrics = samplePool(pool);
            if (metrics != null) {
                allMetrics.put(pool.name, metrics);
            }
        }
        
        return allMetrics;
    }
    
    /**
     * Check if monitoring is currently active.
     * 
     * @return true if monitoring is active
     */
    public boolean isMonitoring() {
        return isMonitoring.get();
    }
    
    /**
     * Get the number of registered pools.
     * 
     * @return Number of pools being monitored
     */
    public int getPoolCount() {
        return monitoredPools.size();
    }
}
