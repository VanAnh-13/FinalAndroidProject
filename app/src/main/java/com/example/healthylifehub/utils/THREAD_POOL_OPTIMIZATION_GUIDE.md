# Thread Pool Optimization Guide

This guide explains how to use the thread pool optimization and monitoring utilities in the HealthyLife Hub application.

## Overview

The application provides comprehensive thread pool optimization tools:

1. **ThreadPoolBenchmark** - Benchmark different pool sizes to find optimal configuration
2. **ThreadPoolMonitor** - Continuously monitor thread pool metrics at runtime
3. **ThreadPoolOptimizer** - Analyze benchmarks and metrics to provide recommendations
4. **ThreadPoolManager** - Simple API to manage monitoring across the application

## Quick Start

### Enable Monitoring in Application Class

Add this to your `HealthyLifeHubApplication.onCreate()`:

```java
public class HealthyLifeHubApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Enable thread pool monitoring in debug builds
        if (BuildConfig.DEBUG) {
            ThreadPoolManager.enableMonitoring(10000); // Monitor every 10 seconds
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        ThreadPoolManager.disableMonitoring();
    }
}
```

### View Current Metrics

```java
// Get metrics for a specific pool
ThreadPoolMonitor.PoolMetrics ioMetrics = ThreadPoolManager.getCurrentMetrics("IO");
Log.d(TAG, "IO Pool: " + ioMetrics.toString());

// Log all current metrics
ThreadPoolManager.logCurrentMetrics();
```

## Benchmarking

### Run IO Thread Pool Benchmark

```java
// Run on a background thread
new Thread(() -> {
    // Benchmark with 100 tasks, each taking 50ms
    List<ThreadPoolBenchmark.BenchmarkResult> results = 
        ThreadPoolBenchmark.benchmarkIOThreadPool(100, 50);
    
    // Get recommended size
    int recommendedSize = ThreadPoolBenchmark.getRecommendedIOPoolSize(results);
    Log.i(TAG, "Recommended IO pool size: " + recommendedSize);
}).start();
```

### Run Compute Thread Pool Benchmark

```java
// Run on a background thread
new Thread(() -> {
    // Benchmark with 50 tasks, each doing 100,000 iterations
    List<ThreadPoolBenchmark.BenchmarkResult> results = 
        ThreadPoolBenchmark.benchmarkComputeThreadPool(50, 100000);
    
    // Get recommended size
    int recommendedSize = ThreadPoolBenchmark.getRecommendedComputePoolSize(results);
    Log.i(TAG, "Recommended Compute pool size: " + recommendedSize);
}).start();
```

## Optimization

### Run Full Optimization Analysis

```java
// Run on a background thread
new Thread(() -> {
    ThreadPoolOptimizer.OptimizationRecommendation[] recommendations = 
        ThreadPoolManager.runOptimization();
    
    for (ThreadPoolOptimizer.OptimizationRecommendation rec : recommendations) {
        if (rec != null) {
            Log.i(TAG, rec.getReport());
        }
    }
}).start();
```

### Analyze Runtime Performance

```java
// Analyze current metrics and get recommendations
ThreadPoolOptimizer.OptimizationRecommendation[] recommendations = 
    ThreadPoolManager.analyzeCurrentPerformance();

for (ThreadPoolOptimizer.OptimizationRecommendation rec : recommendations) {
    if (rec != null) {
        Log.i(TAG, rec.getReport());
        
        // Apply recommendation if improvement is significant
        if (rec.expectedImprovement > 10.0) {
            Log.w(TAG, "Consider updating " + rec.poolName + 
                  " pool size from " + rec.currentSize + 
                  " to " + rec.recommendedSize);
        }
    }
}
```

## Advanced Usage

### Custom Monitoring

```java
// Create custom monitor
ThreadPoolMonitor monitor = new ThreadPoolMonitor();

// Register custom executor
ExecutorService customExecutor = Executors.newFixedThreadPool(4);
monitor.registerPool("Custom", customExecutor);

// Start monitoring with 5-second interval
monitor.startMonitoring(5000);

// ... later ...
monitor.stopMonitoring();
```

### Manual Metrics Collection

```java
// Get current metrics
ThreadPoolMonitor.PoolMetrics metrics = 
    ThreadPoolManager.getCurrentMetrics("IO");

if (metrics != null) {
    // Check for issues
    if (metrics.isSaturated()) {
        Log.w(TAG, "IO pool is saturated!");
    }
    
    if (metrics.hasLargeQueue()) {
        Log.w(TAG, "IO pool has large queue: " + metrics.queueSize);
    }
    
    // Log utilization
    Log.d(TAG, "IO pool utilization: " + metrics.getUtilization() + "%");
}
```

## Interpreting Results

### Benchmark Results

- **Total Duration**: Time to complete all tasks
- **Avg Task Duration**: Average time per task
- **Max Queue Size**: Maximum number of tasks waiting
- **Avg Active Threads**: Average number of threads working
- **Total Wait Time**: Estimated time tasks spent waiting in queue

**Lower total duration = better performance**

### Pool Metrics

- **Active Threads**: Currently executing tasks
- **Queue Size**: Tasks waiting to execute
- **Utilization**: Percentage of threads in use
- **Throughput**: Tasks completed per second

**Ideal state:**
- Utilization: 60-80%
- Queue Size: < 10
- No saturation warnings

### Optimization Recommendations

Recommendations include:
- Current vs recommended pool size
- Reason for change
- Expected performance improvement

**When to apply:**
- Expected improvement > 10%
- Consistent saturation or large queues
- Low utilization with many tasks

## Best Practices

1. **Run benchmarks during development** to establish baseline configurations
2. **Enable monitoring in debug builds** to catch performance issues early
3. **Analyze metrics periodically** in production to identify optimization opportunities
4. **Test changes thoroughly** before applying to production
5. **Consider device capabilities** - different devices may need different configurations

## Performance Targets

Based on requirements:

- **Dashboard Load** (Req 6.4): Complete within 5 seconds
- **Report Generation** (Req 7.5): Complete within 15 seconds for 30 days of data
- **Metrics Analysis** (Req 4.3): Complete within 3 seconds for 90 days of data
- **Data Sync** (Req 12.5): Complete within 30 seconds for 100 records

Use benchmarking and monitoring to ensure these targets are met.

## Troubleshooting

### High Queue Sizes

**Problem**: Tasks are waiting too long in queue

**Solutions**:
- Increase pool size
- Optimize task execution time
- Reduce task submission rate

### Low Utilization

**Problem**: Threads are idle most of the time

**Solutions**:
- Decrease pool size
- Increase task submission rate
- Consider if pool is needed

### Pool Saturation

**Problem**: All threads are busy, new tasks must wait

**Solutions**:
- Increase pool size
- Optimize task execution
- Use priority queues for important tasks

## Example Output

### Benchmark Output

```
========================================
Thread Pool Size: 4 threads
========================================
Total Duration: 1250 ms
Tasks Completed: 100
Avg Task Duration: 52 ms
Max Task Duration: 65 ms
Min Task Duration: 48 ms
Max Queue Size: 8
Avg Queue Size: 3.50
Max Active Threads: 4
Avg Active Threads: 3.80
Total Wait Time: 2950 ms
========================================
```

### Monitoring Output

```
POOL[IO] | Active: 3/4 (75.0%) | Queue: 2 | Completed: 1523/1525 | Throughput: 12.50 tasks/s
```

### Optimization Recommendation

```
========================================
Pool: IO
========================================
Current Size: 4 threads
Recommended Size: 6 threads
Reason: Increasing pool size reduces queue wait time (max queue: 15 -> 5)
Expected Improvement: 18.5%
========================================
```
