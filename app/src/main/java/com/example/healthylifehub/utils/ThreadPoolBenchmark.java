package com.example.healthylifehub.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for benchmarking thread pool configurations.
 * Tests different pool sizes to determine optimal configuration for IO and compute operations.
 * 
 * This class helps identify the best thread pool size by:
 * - Measuring task completion times
 * - Monitoring queue lengths and wait times
 * - Tracking thread utilization
 * - Comparing performance across different pool sizes
 */
public class ThreadPoolBenchmark {
    
    private static final String TAG = "ThreadPoolBenchmark";
    
    /**
     * Benchmark result containing performance metrics for a specific pool configuration.
     */
    public static class BenchmarkResult {
        public final int poolSize;
        public final long totalDurationMs;
        public final long avgTaskDurationMs;
        public final long maxTaskDurationMs;
        public final long minTaskDurationMs;
        public final int maxQueueSize;
        public final double avgQueueSize;
        public final int maxActiveThreads;
        public final double avgActiveThreads;
        public final long totalWaitTimeMs;
        public final int tasksCompleted;
        
        public BenchmarkResult(int poolSize, long totalDurationMs, long avgTaskDurationMs,
                             long maxTaskDurationMs, long minTaskDurationMs,
                             int maxQueueSize, double avgQueueSize,
                             int maxActiveThreads, double avgActiveThreads,
                             long totalWaitTimeMs, int tasksCompleted) {
            this.poolSize = poolSize;
            this.totalDurationMs = totalDurationMs;
            this.avgTaskDurationMs = avgTaskDurationMs;
            this.maxTaskDurationMs = maxTaskDurationMs;
            this.minTaskDurationMs = minTaskDurationMs;
            this.maxQueueSize = maxQueueSize;
            this.avgQueueSize = avgQueueSize;
            this.maxActiveThreads = maxActiveThreads;
            this.avgActiveThreads = avgActiveThreads;
            this.totalWaitTimeMs = totalWaitTimeMs;
            this.tasksCompleted = tasksCompleted;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BenchmarkResult[poolSize=%d, totalDuration=%dms, avgTask=%dms, " +
                "maxTask=%dms, minTask=%dms, maxQueue=%d, avgQueue=%.2f, " +
                "maxActive=%d, avgActive=%.2f, totalWait=%dms, completed=%d]",
                poolSize, totalDurationMs, avgTaskDurationMs, maxTaskDurationMs,
                minTaskDurationMs, maxQueueSize, avgQueueSize, maxActiveThreads,
                avgActiveThreads, totalWaitTimeMs, tasksCompleted
            );
        }
        
        /**
         * Get a formatted report string for logging.
         */
        public String getReport() {
            StringBuilder report = new StringBuilder();
            report.append("\n========================================\n");
            report.append(String.format("Thread Pool Size: %d threads\n", poolSize));
            report.append("========================================\n");
            report.append(String.format("Total Duration: %d ms\n", totalDurationMs));
            report.append(String.format("Tasks Completed: %d\n", tasksCompleted));
            report.append(String.format("Avg Task Duration: %d ms\n", avgTaskDurationMs));
            report.append(String.format("Max Task Duration: %d ms\n", maxTaskDurationMs));
            report.append(String.format("Min Task Duration: %d ms\n", minTaskDurationMs));
            report.append(String.format("Max Queue Size: %d\n", maxQueueSize));
            report.append(String.format("Avg Queue Size: %.2f\n", avgQueueSize));
            report.append(String.format("Max Active Threads: %d\n", maxActiveThreads));
            report.append(String.format("Avg Active Threads: %.2f\n", avgActiveThreads));
            report.append(String.format("Total Wait Time: %d ms\n", totalWaitTimeMs));
            report.append("========================================\n");
            return report.toString();
        }
    }
    
    /**
     * Benchmark IO thread pool with different sizes.
     * Tests 2, 4, and 8 thread configurations with IO-bound workload.
     * 
     * @param numTasks Number of tasks to execute in the benchmark
     * @param taskDurationMs Simulated duration of each IO task
     * @return List of benchmark results for each pool size
     */
    public static List<BenchmarkResult> benchmarkIOThreadPool(int numTasks, int taskDurationMs) {
        Log.i(TAG, "Starting IO thread pool benchmark...");
        Log.i(TAG, String.format("Tasks: %d, Task Duration: %dms", numTasks, taskDurationMs));
        
        List<BenchmarkResult> results = new ArrayList<>();
        
        // Test with 2 threads
        results.add(runBenchmark("IO-2", 2, numTasks, taskDurationMs, true));
        
        // Test with 4 threads
        results.add(runBenchmark("IO-4", 4, numTasks, taskDurationMs, true));
        
        // Test with 8 threads
        results.add(runBenchmark("IO-8", 8, numTasks, taskDurationMs, true));
        
        // Log comparison
        logComparison("IO Thread Pool", results);
        
        return results;
    }
    
    /**
     * Benchmark compute thread pool with different sizes.
     * Tests 1, 2, and 4 thread configurations with CPU-bound workload.
     * 
     * @param numTasks Number of tasks to execute in the benchmark
     * @param computeIterations Number of iterations for CPU-intensive work
     * @return List of benchmark results for each pool size
     */
    public static List<BenchmarkResult> benchmarkComputeThreadPool(int numTasks, int computeIterations) {
        Log.i(TAG, "Starting Compute thread pool benchmark...");
        Log.i(TAG, String.format("Tasks: %d, Compute Iterations: %d", numTasks, computeIterations));
        
        List<BenchmarkResult> results = new ArrayList<>();
        
        // Test with 1 thread
        results.add(runBenchmark("Compute-1", 1, numTasks, computeIterations, false));
        
        // Test with 2 threads
        results.add(runBenchmark("Compute-2", 2, numTasks, computeIterations, false));
        
        // Test with 4 threads
        results.add(runBenchmark("Compute-4", 4, numTasks, computeIterations, false));
        
        // Log comparison
        logComparison("Compute Thread Pool", results);
        
        return results;
    }
    
    /**
     * Run a single benchmark with specified pool size and workload.
     */
    private static BenchmarkResult runBenchmark(String poolName, int poolSize, 
                                               int numTasks, int workloadParam, 
                                               boolean isIOBound) {
        Log.i(TAG, String.format("Running benchmark: %s", poolName));
        
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
        
        // Metrics tracking
        List<Long> taskDurations = new ArrayList<>();
        List<Integer> queueSizes = new ArrayList<>();
        List<Integer> activeThreadCounts = new ArrayList<>();
        AtomicInteger completedTasks = new AtomicInteger(0);
        
        long benchmarkStart = System.currentTimeMillis();
        
        // Create and submit tasks
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                long taskStart = System.currentTimeMillis();
                
                // Simulate workload
                if (isIOBound) {
                    // IO-bound: simulate network/disk operation with sleep
                    try {
                        Thread.sleep(workloadParam);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // CPU-bound: perform actual computation
                    performCPUWork(workloadParam);
                }
                
                long taskEnd = System.currentTimeMillis();
                long taskDuration = taskEnd - taskStart;
                
                synchronized (taskDurations) {
                    taskDurations.add(taskDuration);
                }
                
                completedTasks.incrementAndGet();
            }, executor);
            
            futures.add(future);
            
            // Sample metrics periodically
            if (i % 10 == 0) {
                queueSizes.add(threadPool.getQueue().size());
                activeThreadCounts.add(threadPool.getActiveCount());
            }
        }
        
        // Wait for all tasks to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Benchmark failed: " + e.getMessage());
        }
        
        long benchmarkEnd = System.currentTimeMillis();
        long totalDuration = benchmarkEnd - benchmarkStart;
        
        // Calculate metrics
        long avgTaskDuration = taskDurations.stream()
            .mapToLong(Long::longValue)
            .sum() / Math.max(1, taskDurations.size());
        
        long maxTaskDuration = taskDurations.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        long minTaskDuration = taskDurations.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);
        
        int maxQueueSize = queueSizes.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        
        double avgQueueSize = queueSizes.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        
        int maxActiveThreads = activeThreadCounts.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        
        double avgActiveThreads = activeThreadCounts.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        
        // Estimate total wait time (time tasks spent in queue)
        long totalWaitTime = (avgTaskDuration * numTasks) - totalDuration;
        if (totalWaitTime < 0) totalWaitTime = 0;
        
        // Shutdown executor
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        BenchmarkResult result = new BenchmarkResult(
            poolSize, totalDuration, avgTaskDuration, maxTaskDuration, minTaskDuration,
            maxQueueSize, avgQueueSize, maxActiveThreads, avgActiveThreads,
            totalWaitTime, completedTasks.get()
        );
        
        Log.i(TAG, result.getReport());
        
        return result;
    }
    
    /**
     * Perform CPU-intensive work for compute benchmarking.
     */
    private static void performCPUWork(int iterations) {
        double result = 0.0;
        for (int i = 0; i < iterations; i++) {
            result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
        }
        // Prevent optimization
        if (result == Double.MAX_VALUE) {
            Log.v(TAG, "Unlikely result: " + result);
        }
    }
    
    /**
     * Log comparison of benchmark results.
     */
    private static void logComparison(String benchmarkType, List<BenchmarkResult> results) {
        Log.i(TAG, "\n\n========================================");
        Log.i(TAG, benchmarkType + " Benchmark Comparison");
        Log.i(TAG, "========================================");
        
        // Find best result (lowest total duration)
        BenchmarkResult best = results.stream()
            .min((r1, r2) -> Long.compare(r1.totalDurationMs, r2.totalDurationMs))
            .orElse(null);
        
        for (BenchmarkResult result : results) {
            String marker = (result == best) ? " *** BEST ***" : "";
            Log.i(TAG, String.format(
                "Pool Size: %d | Duration: %dms | Avg Task: %dms | Max Queue: %d | Avg Active: %.2f%s",
                result.poolSize, result.totalDurationMs, result.avgTaskDurationMs,
                result.maxQueueSize, result.avgActiveThreads, marker
            ));
        }
        
        if (best != null) {
            Log.i(TAG, String.format("\nRecommended pool size: %d threads", best.poolSize));
            Log.i(TAG, String.format("Performance gain vs others: %.1f%% faster on average",
                calculateAverageImprovement(best, results)));
        }
        
        Log.i(TAG, "========================================\n\n");
    }
    
    /**
     * Calculate average performance improvement of best result vs others.
     */
    private static double calculateAverageImprovement(BenchmarkResult best, List<BenchmarkResult> all) {
        if (all.size() <= 1) return 0.0;
        
        double totalImprovement = 0.0;
        int count = 0;
        
        for (BenchmarkResult result : all) {
            if (result != best) {
                double improvement = ((double)(result.totalDurationMs - best.totalDurationMs) / 
                                     result.totalDurationMs) * 100.0;
                totalImprovement += improvement;
                count++;
            }
        }
        
        return count > 0 ? totalImprovement / count : 0.0;
    }
    
    /**
     * Get recommended IO pool size based on benchmark results.
     * 
     * @param results Benchmark results from benchmarkIOThreadPool
     * @return Recommended pool size
     */
    public static int getRecommendedIOPoolSize(List<BenchmarkResult> results) {
        return results.stream()
            .min((r1, r2) -> Long.compare(r1.totalDurationMs, r2.totalDurationMs))
            .map(r -> r.poolSize)
            .orElse(4); // Default to 4 if no results
    }
    
    /**
     * Get recommended compute pool size based on benchmark results.
     * 
     * @param results Benchmark results from benchmarkComputeThreadPool
     * @return Recommended pool size
     */
    public static int getRecommendedComputePoolSize(List<BenchmarkResult> results) {
        return results.stream()
            .min((r1, r2) -> Long.compare(r1.totalDurationMs, r2.totalDurationMs))
            .map(r -> r.poolSize)
            .orElse(2); // Default to 2 if no results
    }
}
