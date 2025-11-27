package com.example.healthylifehub.utils.threading;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Continuous monitoring utility for thread pools.
 * Tracks and logs thread pool metrics at regular intervals.
 */
public class ThreadPoolMonitor {
    
    private static final String TAG = "ThreadPoolMonitor";
    private static final long DEFAULT_INTERVAL_MS = 10000;
    
    private final Map<String, MonitoredPool> monitoredPools = new ConcurrentHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private Runnable monitoringRunnable;
    private long intervalMs = DEFAULT_INTERVAL_MS;
    
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
        
        public boolean isSaturated() {
            return activeThreads >= maximumPoolSize;
        }
        
        public boolean hasLargeQueue() {
            return queueSize > 10;
        }
        
        public double getUtilization() {
            return maximumPoolSize > 0 ? 
                (double) activeThreads / maximumPoolSize * 100.0 : 0.0;
        }
    }
    
    public void registerPool(String name, ExecutorService executor) {
        if (executor == null) {
            Log.w(TAG, "Cannot register null executor: " + name);
            return;
        }
        monitoredPools.put(name, new MonitoredPool(name, executor));
        Log.i(TAG, "Registered thread pool for monitoring: " + name);
    }
    
    public void unregisterPool(String name) {
        monitoredPools.remove(name);
        Log.i(TAG, "Unregistered thread pool from monitoring: " + name);
    }
    
    public void startMonitoring() {
        startMonitoring(DEFAULT_INTERVAL_MS);
    }
    
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
        logSummary();
    }
    
    private void sampleAllPools() {
        for (MonitoredPool pool : monitoredPools.values()) {
            PoolMetrics metrics = samplePool(pool);
            if (metrics != null) {
                logMetrics(metrics);
                updatePoolStats(pool, metrics);
            }
        }
    }
    
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
        
        long timeDelta = currentTime - pool.lastSampleTime;
        long taskDelta = completedTaskCount - pool.lastCompletedTaskCount;
        double tasksPerSecond = timeDelta > 0 ? 
            (taskDelta * 1000.0) / timeDelta : 0.0;
        
        return new PoolMetrics(
            pool.name, activeThreads, poolSize, corePoolSize, maximumPoolSize,
            queueSize, completedTaskCount, taskCount, tasksPerSecond, currentTime
        );
    }
    
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
    
    private void logMetrics(PoolMetrics metrics) {
        Log.d(TAG, String.format(
            "POOL[%s] | Active: %d/%d (%.1f%%) | Queue: %d | Completed: %d/%d | Throughput: %.2f tasks/s",
            metrics.poolName, metrics.activeThreads, metrics.maximumPoolSize,
            metrics.getUtilization(), metrics.queueSize, metrics.completedTaskCount,
            metrics.taskCount, metrics.tasksPerSecond
        ));
        
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
    }
    
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
    
    public PoolMetrics getCurrentMetrics(String poolName) {
        MonitoredPool pool = monitoredPools.get(poolName);
        if (pool == null) {
            return null;
        }
        return samplePool(pool);
    }
    
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
    
    public boolean isMonitoring() {
        return isMonitoring.get();
    }
    
    public int getPoolCount() {
        return monitoredPools.size();
    }
}
