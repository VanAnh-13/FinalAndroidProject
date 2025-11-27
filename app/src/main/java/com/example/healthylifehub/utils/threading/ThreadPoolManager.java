package com.example.healthylifehub.utils.threading;

import android.util.Log;

import com.example.healthylifehub.base.BaseRepository;

/**
 * Central manager for thread pool monitoring and optimization.
 * Provides a simple API to set up and manage thread pool monitoring across the application.
 * 
 * Usage in Application class:
 * <pre>
 * public class HealthyLifeHubApplication extends Application {
 *     @Override
 *     public void onCreate() {
 *         super.onCreate();
 *         
 *         // Enable thread pool monitoring in debug builds
 *         if (BuildConfig.DEBUG) {
 *             ThreadPoolManager.enableMonitoring(10000); // Monitor every 10 seconds
 *         }
 *     }
 * }
 * </pre>
 */
public class ThreadPoolManager {
    
    private static final String TAG = "ThreadPoolManager";
    private static ThreadPoolMonitor monitor;
    private static boolean isInitialized = false;
    
    /**
     * Initialize thread pool monitoring for the application.
     * Registers the shared IO and Compute thread pools from BaseRepository.
     */
    public static synchronized void initialize() {
        if (isInitialized) {
            Log.w(TAG, "ThreadPoolManager already initialized");
            return;
        }
        
        monitor = new ThreadPoolMonitor();
        
        // Register shared thread pools from BaseRepository
        monitor.registerPool("IO", BaseRepository.getSharedIoExecutor());
        monitor.registerPool("Compute", BaseRepository.getSharedComputeExecutor());
        
        isInitialized = true;
        Log.i(TAG, "ThreadPoolManager initialized");
    }
    
    /**
     * Enable continuous monitoring of thread pools.
     * 
     * @param intervalMs Interval between metric samples in milliseconds
     */
    public static void enableMonitoring(long intervalMs) {
        if (!isInitialized) {
            initialize();
        }
        
        if (monitor != null && !monitor.isMonitoring()) {
            monitor.startMonitoring(intervalMs);
            Log.i(TAG, "Thread pool monitoring enabled");
        }
    }
    
    /**
     * Enable monitoring with default interval (10 seconds).
     */
    public static void enableMonitoring() {
        enableMonitoring(10000);
    }
    
    /**
     * Disable continuous monitoring of thread pools.
     */
    public static void disableMonitoring() {
        if (monitor != null && monitor.isMonitoring()) {
            monitor.stopMonitoring();
            Log.i(TAG, "Thread pool monitoring disabled");
        }
    }
    
    /**
     * Get current metrics for a specific pool.
     * 
     * @param poolName Name of the pool ("IO" or "Compute")
     * @return Current metrics or null if not available
     */
    public static ThreadPoolMonitor.PoolMetrics getCurrentMetrics(String poolName) {
        if (!isInitialized) {
            initialize();
        }
        return monitor != null ? monitor.getCurrentMetrics(poolName) : null;
    }
    
    /**
     * Log current metrics for all pools.
     */
    public static void logCurrentMetrics() {
        if (!isInitialized) {
            initialize();
        }
        
        if (monitor != null) {
            ThreadPoolMonitor.PoolMetrics ioMetrics = monitor.getCurrentMetrics("IO");
            ThreadPoolMonitor.PoolMetrics computeMetrics = monitor.getCurrentMetrics("Compute");
            
            if (ioMetrics != null) {
                Log.i(TAG, "IO Pool: " + ioMetrics.toString());
            }
            
            if (computeMetrics != null) {
                Log.i(TAG, "Compute Pool: " + computeMetrics.toString());
            }
        }
    }
    
    /**
     * Run benchmarks and get optimization recommendations.
     * This is a blocking operation and should be run on a background thread.
     * 
     * @return Array of recommendations [IO, Compute]
     */
    public static ThreadPoolOptimizer.OptimizationRecommendation[] runOptimization() {
        Log.i(TAG, "Running thread pool optimization...");
        
        // Log device info
        ThreadPoolOptimizer.logDeviceInfo();
        
        // Run benchmarks
        ThreadPoolOptimizer.OptimizationRecommendation ioRec = 
            ThreadPoolOptimizer.optimizeIOThreadPool(50, 100);
        
        ThreadPoolOptimizer.OptimizationRecommendation computeRec = 
            ThreadPoolOptimizer.optimizeComputeThreadPool(50, 100000);
        
        return new ThreadPoolOptimizer.OptimizationRecommendation[] { ioRec, computeRec };
    }
    
    /**
     * Analyze current runtime metrics and get recommendations.
     * 
     * @return Array of recommendations [IO, Compute] (may contain nulls if no changes needed)
     */
    public static ThreadPoolOptimizer.OptimizationRecommendation[] analyzeCurrentPerformance() {
        if (!isInitialized) {
            initialize();
        }
        
        if (monitor == null) {
            return new ThreadPoolOptimizer.OptimizationRecommendation[0];
        }
        
        ThreadPoolMonitor.PoolMetrics ioMetrics = monitor.getCurrentMetrics("IO");
        ThreadPoolMonitor.PoolMetrics computeMetrics = monitor.getCurrentMetrics("Compute");
        
        ThreadPoolOptimizer.OptimizationRecommendation ioRec = 
            ThreadPoolOptimizer.analyzeRuntimeMetrics("IO", ioMetrics);
        
        ThreadPoolOptimizer.OptimizationRecommendation computeRec = 
            ThreadPoolOptimizer.analyzeRuntimeMetrics("Compute", computeMetrics);
        
        return new ThreadPoolOptimizer.OptimizationRecommendation[] { ioRec, computeRec };
    }
    
    /**
     * Check if monitoring is currently active.
     * 
     * @return true if monitoring is active
     */
    public static boolean isMonitoring() {
        return monitor != null && monitor.isMonitoring();
    }
    
    /**
     * Get the monitor instance for advanced usage.
     * 
     * @return ThreadPoolMonitor instance or null if not initialized
     */
    public static ThreadPoolMonitor getMonitor() {
        if (!isInitialized) {
            initialize();
        }
        return monitor;
    }
}
