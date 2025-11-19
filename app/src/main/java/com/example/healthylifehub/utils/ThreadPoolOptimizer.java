package com.example.healthylifehub.utils;

import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Utility for optimizing thread pool configurations based on benchmarks and monitoring.
 * Provides recommendations for pool sizes and helps tune performance.
 * 
 * This class integrates benchmarking and monitoring to provide:
 * - Optimal pool size recommendations
 * - Performance analysis
 * - Configuration tuning suggestions
 * - Runtime optimization advice
 */
public class ThreadPoolOptimizer {
    
    private static final String TAG = "ThreadPoolOptimizer";
    
    /**
     * Optimization recommendation containing suggested configuration changes.
     */
    public static class OptimizationRecommendation {
        public final String poolName;
        public final int currentSize;
        public final int recommendedSize;
        public final String reason;
        public final double expectedImprovement;
        
        public OptimizationRecommendation(String poolName, int currentSize,
                                         int recommendedSize, String reason,
                                         double expectedImprovement) {
            this.poolName = poolName;
            this.currentSize = currentSize;
            this.recommendedSize = recommendedSize;
            this.reason = reason;
            this.expectedImprovement = expectedImprovement;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Recommendation[pool=%s, current=%d, recommended=%d, reason=%s, improvement=%.1f%%]",
                poolName, currentSize, recommendedSize, reason, expectedImprovement
            );
        }
        
        public String getReport() {
            StringBuilder report = new StringBuilder();
            report.append("\n========================================\n");
            report.append(String.format("Pool: %s\n", poolName));
            report.append("========================================\n");
            report.append(String.format("Current Size: %d threads\n", currentSize));
            report.append(String.format("Recommended Size: %d threads\n", recommendedSize));
            report.append(String.format("Reason: %s\n", reason));
            report.append(String.format("Expected Improvement: %.1f%%\n", expectedImprovement));
            report.append("========================================\n");
            return report.toString();
        }
    }
    
    /**
     * Run comprehensive optimization analysis for IO thread pool.
     * Performs benchmarks and provides recommendations.
     * 
     * @param numTasks Number of tasks for benchmark
     * @param taskDurationMs Duration of each IO task
     * @return Optimization recommendation
     */
    public static OptimizationRecommendation optimizeIOThreadPool(int numTasks, int taskDurationMs) {
        Log.i(TAG, "Running IO thread pool optimization...");
        
        // Run benchmarks
        List<ThreadPoolBenchmark.BenchmarkResult> results = 
            ThreadPoolBenchmark.benchmarkIOThreadPool(numTasks, taskDurationMs);
        
        // Get recommended size
        int recommendedSize = ThreadPoolBenchmark.getRecommendedIOPoolSize(results);
        
        // Calculate expected improvement
        ThreadPoolBenchmark.BenchmarkResult best = results.stream()
            .filter(r -> r.poolSize == recommendedSize)
            .findFirst()
            .orElse(null);
        
        ThreadPoolBenchmark.BenchmarkResult current = results.stream()
            .filter(r -> r.poolSize == 4) // Current default is 4
            .findFirst()
            .orElse(null);
        
        double improvement = 0.0;
        String reason = "Optimal based on benchmark results";
        
        if (best != null && current != null && best != current) {
            improvement = ((double)(current.totalDurationMs - best.totalDurationMs) / 
                          current.totalDurationMs) * 100.0;
            
            if (best.poolSize > current.poolSize) {
                reason = String.format(
                    "Increasing pool size reduces queue wait time (max queue: %d -> %d)",
                    current.maxQueueSize, best.maxQueueSize
                );
            } else {
                reason = String.format(
                    "Decreasing pool size improves efficiency without performance loss " +
                    "(avg active threads: %.1f vs %.1f)",
                    current.avgActiveThreads, best.avgActiveThreads
                );
            }
        } else if (best != null && current != null) {
            reason = "Current configuration is already optimal";
        }
        
        OptimizationRecommendation recommendation = new OptimizationRecommendation(
            "IO", 4, recommendedSize, reason, improvement
        );
        
        Log.i(TAG, recommendation.getReport());
        
        return recommendation;
    }
    
    /**
     * Run comprehensive optimization analysis for Compute thread pool.
     * Performs benchmarks and provides recommendations.
     * 
     * @param numTasks Number of tasks for benchmark
     * @param computeIterations Number of compute iterations per task
     * @return Optimization recommendation
     */
    public static OptimizationRecommendation optimizeComputeThreadPool(int numTasks, 
                                                                       int computeIterations) {
        Log.i(TAG, "Running Compute thread pool optimization...");
        
        // Run benchmarks
        List<ThreadPoolBenchmark.BenchmarkResult> results = 
            ThreadPoolBenchmark.benchmarkComputeThreadPool(numTasks, computeIterations);
        
        // Get recommended size
        int recommendedSize = ThreadPoolBenchmark.getRecommendedComputePoolSize(results);
        
        // Get CPU core count for context
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        // Calculate expected improvement
        ThreadPoolBenchmark.BenchmarkResult best = results.stream()
            .filter(r -> r.poolSize == recommendedSize)
            .findFirst()
            .orElse(null);
        
        ThreadPoolBenchmark.BenchmarkResult current = results.stream()
            .filter(r -> r.poolSize == 2) // Current default is 2
            .findFirst()
            .orElse(null);
        
        double improvement = 0.0;
        String reason = String.format(
            "Optimal based on benchmark results (CPU cores: %d)", cpuCores
        );
        
        if (best != null && current != null && best != current) {
            improvement = ((double)(current.totalDurationMs - best.totalDurationMs) / 
                          current.totalDurationMs) * 100.0;
            
            if (best.poolSize > current.poolSize) {
                reason = String.format(
                    "Device has %d CPU cores - increasing pool size improves parallelism " +
                    "(avg active threads: %.1f -> %.1f)",
                    cpuCores, current.avgActiveThreads, best.avgActiveThreads
                );
            } else {
                reason = String.format(
                    "Smaller pool size reduces context switching overhead " +
                    "(avg task duration: %dms vs %dms)",
                    best.avgTaskDurationMs, current.avgTaskDurationMs
                );
            }
        } else if (best != null && current != null) {
            reason = String.format(
                "Current configuration is already optimal for %d CPU cores", cpuCores
            );
        }
        
        OptimizationRecommendation recommendation = new OptimizationRecommendation(
            "Compute", 2, recommendedSize, reason, improvement
        );
        
        Log.i(TAG, recommendation.getReport());
        
        return recommendation;
    }
    
    /**
     * Analyze runtime metrics and provide optimization recommendations.
     * 
     * @param poolName Name of the pool to analyze
     * @param metrics Current pool metrics from monitoring
     * @return Optimization recommendation or null if no changes needed
     */
    public static OptimizationRecommendation analyzeRuntimeMetrics(
            String poolName, ThreadPoolMonitor.PoolMetrics metrics) {
        
        if (metrics == null) {
            return null;
        }
        
        int currentSize = metrics.maximumPoolSize;
        int recommendedSize = currentSize;
        String reason = "No changes needed";
        double expectedImprovement = 0.0;
        
        // Check for saturation
        if (metrics.isSaturated() && metrics.hasLargeQueue()) {
            recommendedSize = Math.min(currentSize + 2, currentSize * 2);
            reason = String.format(
                "Pool is saturated with large queue (%d tasks) - increase size to reduce wait time",
                metrics.queueSize
            );
            expectedImprovement = 20.0; // Estimated
        }
        // Check for low utilization
        else if (metrics.getUtilization() < 20.0 && metrics.taskCount > 100) {
            recommendedSize = Math.max(currentSize - 1, 1);
            reason = String.format(
                "Low utilization (%.1f%%) - decrease size to reduce resource overhead",
                metrics.getUtilization()
            );
            expectedImprovement = 5.0; // Estimated
        }
        // Check for moderate queue with high utilization
        else if (metrics.queueSize > 5 && metrics.getUtilization() > 80.0) {
            recommendedSize = currentSize + 1;
            reason = String.format(
                "High utilization (%.1f%%) with growing queue (%d tasks) - slight increase recommended",
                metrics.getUtilization(), metrics.queueSize
            );
            expectedImprovement = 10.0; // Estimated
        }
        
        if (recommendedSize == currentSize) {
            return null; // No recommendation needed
        }
        
        OptimizationRecommendation recommendation = new OptimizationRecommendation(
            poolName, currentSize, recommendedSize, reason, expectedImprovement
        );
        
        Log.i(TAG, recommendation.getReport());
        
        return recommendation;
    }
    
    /**
     * Get optimal pool size based on device capabilities.
     * 
     * @param poolType Type of pool ("IO" or "Compute")
     * @return Recommended pool size
     */
    public static int getOptimalPoolSize(String poolType) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        if ("IO".equalsIgnoreCase(poolType)) {
            // IO-bound: can use more threads than CPU cores
            // Rule of thumb: 2x CPU cores, minimum 4, maximum 8
            return Math.max(4, Math.min(8, cpuCores * 2));
        } else if ("Compute".equalsIgnoreCase(poolType)) {
            // CPU-bound: should match CPU cores
            // Rule of thumb: CPU cores, minimum 2, maximum 4
            return Math.max(2, Math.min(4, cpuCores));
        }
        
        return 4; // Default fallback
    }
    
    /**
     * Log device information relevant to thread pool optimization.
     */
    public static void logDeviceInfo() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // MB
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024); // MB
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024); // MB
        
        Log.i(TAG, "\n========================================");
        Log.i(TAG, "Device Information");
        Log.i(TAG, "========================================");
        Log.i(TAG, String.format("CPU Cores: %d", cpuCores));
        Log.i(TAG, String.format("Max Memory: %d MB", maxMemory));
        Log.i(TAG, String.format("Total Memory: %d MB", totalMemory));
        Log.i(TAG, String.format("Free Memory: %d MB", freeMemory));
        Log.i(TAG, String.format("Recommended IO Pool Size: %d", getOptimalPoolSize("IO")));
        Log.i(TAG, String.format("Recommended Compute Pool Size: %d", getOptimalPoolSize("Compute")));
        Log.i(TAG, "========================================\n");
    }
}
