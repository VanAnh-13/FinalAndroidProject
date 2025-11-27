package com.example.healthylifehub.base;

import androidx.core.content.ContextCompat;

import com.example.healthylifehub.utils.app.ApplicationContextProvider;
import com.example.healthylifehub.utils.error.AsyncErrorLogger;
import com.example.healthylifehub.utils.performance.AsyncPerformanceLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import android.content.Context;
import com.example.healthylifehub.utils.network.NetworkMonitor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * BaseRepository is an abstract base class for all repository implementations in the Base application.
 * This class provides a standardized approach to data operations with asynchronous execution,
 * error handling, and result wrapping using the DataState pattern.
 * 
 * The repository pattern helps separate data access logic from business logic,
 * providing a clean API for data operations while handling threading and error management.
 */
public abstract class BaseRepository {

    /** Shared IO thread pool for all repositories - handles I/O-bound operations */
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4);
    
    /** Shared compute thread pool for all repositories - handles CPU-intensive operations */
    private static final ExecutorService COMPUTE_EXECUTOR = Executors.newFixedThreadPool(2);

    /** Main thread executor for UI updates */
    private static final Executor MAIN_EXECUTOR = ContextCompat.getMainExecutor(
        ApplicationContextProvider.getContext()
    );

    /** Legacy executor service for backward compatibility */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /** Network monitor for checking connectivity */
    protected NetworkMonitor networkMonitor;

    /**
     * Constructor with context for network monitoring
     */
    public BaseRepository(Context context) {
        this.networkMonitor = NetworkMonitor.getInstance(context);
    }

    /**
     * Default constructor for backward compatibility
     */
    public BaseRepository() {
        this.networkMonitor = null;
    }

    /**
     * Get the shared IO thread pool executor.
     * Use this for I/O-bound operations like network requests, database queries, file operations.
     *
     * @return ExecutorService with 4 threads optimized for I/O operations
     */
    protected ExecutorService getIoExecutor() {
        return IO_EXECUTOR;
    }

    /**
     * Get the shared compute thread pool executor.
     * Use this for CPU-intensive operations like data processing, statistics calculation, chart rendering.
     *
     * @return ExecutorService with 2 threads optimized for compute operations
     */
    protected ExecutorService getComputeExecutor() {
        return COMPUTE_EXECUTOR;
    }

    /**
     * Get the main thread executor.
     * Use this for UI updates and LiveData posting.
     *
     * @return Executor that runs on the main/UI thread
     */
    protected Executor getMainExecutor() {
        return MAIN_EXECUTOR;
    }

    /**
     * Execute a task asynchronously on the IO thread pool.
     * This is a convenience method for simple async operations.
     * Includes automatic error logging for debugging.
     *
     * @param <T> The type of result expected from the task
     * @param task The task to execute
     * @return CompletableFuture containing the result
     */
    protected <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, IO_EXECUTOR)
        .exceptionally(throwable -> {
            // Log error with context
            AsyncErrorLogger.logError("executeAsync", throwable);
            throw new CompletionException(throwable);
        });
    }

    /**
     * Execute a task asynchronously on the IO thread pool with operation context.
     * This version allows specifying the operation type for better error logging.
     * Includes performance tracking.
     *
     * @param <T> The type of result expected from the task
     * @param task The task to execute
     * @param operationType The type of operation for logging (e.g., "fetch_metrics", "save_user")
     * @return CompletableFuture containing the result
     */
    protected <T> CompletableFuture<T> executeAsync(Callable<T> task, String operationType) {
        // Start performance tracking
        String operationId = AsyncPerformanceLogger.operation(operationType).start();

        return CompletableFuture.supplyAsync(() -> {
            try {
                T result = task.call();
                // Log successful completion
                AsyncPerformanceLogger.logEnd(operationId, operationType, true);
                return result;
            } catch (Exception e) {
                // Log failed completion
                AsyncPerformanceLogger.logEnd(operationId, operationType, false);
                throw new CompletionException(e);
            }
        }, IO_EXECUTOR)
        .exceptionally(throwable -> {
            // Log error with operation type
            AsyncErrorLogger.logError(operationType, throwable);
            throw new CompletionException(throwable);
        });
    }

    /**
     * Execute a task asynchronously with full context for error logging.
     * Includes performance tracking with user context.
     *
     * @param <T> The type of result expected from the task
     * @param task The task to execute
     * @param operationType The type of operation for logging
     * @param userId The user ID associated with the operation
     * @return CompletableFuture containing the result
     */
    protected <T> CompletableFuture<T> executeAsyncWithContext(
            Callable<T> task, String operationType, String userId) {
        // Start performance tracking with user context
        String operationId = AsyncPerformanceLogger.operation(operationType)
            .withContext("userId", userId)
            .start();

        return CompletableFuture.supplyAsync(() -> {
            try {
                T result = task.call();
                // Log successful completion
                AsyncPerformanceLogger.logEnd(operationId, operationType, true);
                return result;
            } catch (Exception e) {
                // Log failed completion
                AsyncPerformanceLogger.logEnd(operationId, operationType, false);
                throw new CompletionException(e);
            }
        }, IO_EXECUTOR)
        .exceptionally(throwable -> {
            // Log error with user context
            AsyncErrorLogger.logError(operationType, throwable, userId);
            throw new CompletionException(throwable);
        });
    }

    /**
     * Execute multiple tasks in parallel on the IO thread pool.
     * All tasks will run concurrently and results will be collected.
     * Includes automatic error logging for each failed task.
     *
     * @param <T> The type of result expected from each task
     * @param tasks List of tasks to execute in parallel
     * @return CompletableFuture containing list of results in the same order as tasks
     */
    protected <T> CompletableFuture<List<T>> executeParallel(List<Callable<T>> tasks) {
        List<CompletableFuture<T>> futures = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            final int taskIndex = i;
            Callable<T> task = tasks.get(i);
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, IO_EXECUTOR)
            .exceptionally(throwable -> {
                // Log error with task index
                AsyncErrorLogger.context()
                    .put("taskIndex", String.valueOf(taskIndex))
                    .put("totalTasks", String.valueOf(tasks.size()))
                    .log("executeParallel", throwable);
                throw new CompletionException(throwable);
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<T> results = new ArrayList<>();
                for (CompletableFuture<T> future : futures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }
                return results;
            })
            .exceptionally(throwable -> {
                // Log overall parallel execution failure
                AsyncErrorLogger.logError("executeParallel_completion", throwable);
                throw new CompletionException(throwable);
            });
    }

    /**
     * Execute multiple tasks in parallel with operation context.
     *
     * @param <T> The type of result expected from each task
     * @param tasks List of tasks to execute in parallel
     * @param operationType The type of operation for logging
     * @return CompletableFuture containing list of results in the same order as tasks
     */
    protected <T> CompletableFuture<List<T>> executeParallel(
            List<Callable<T>> tasks, String operationType) {
        List<CompletableFuture<T>> futures = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            final int taskIndex = i;
            Callable<T> task = tasks.get(i);
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, IO_EXECUTOR)
            .exceptionally(throwable -> {
                // Log error with operation type and task index
                AsyncErrorLogger.context()
                    .put("operationType", operationType)
                    .put("taskIndex", String.valueOf(taskIndex))
                    .put("totalTasks", String.valueOf(tasks.size()))
                    .log(operationType + "_task", throwable);
                throw new CompletionException(throwable);
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<T> results = new ArrayList<>();
                for (CompletableFuture<T> future : futures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }
                return results;
            })
            .exceptionally(throwable -> {
                // Log overall parallel execution failure
                AsyncErrorLogger.logError(operationType + "_completion", throwable);
                throw new CompletionException(throwable);
            });
    }

    /**
     * Executes a data operation asynchronously and wraps the result in a DataState.
     * This method provides a standardized way to handle asynchronous data operations
     * with proper error handling and result wrapping.
     * 
     * The method uses CompletableFuture to execute the request on a background thread,
     * automatically catching exceptions and wrapping results in appropriate DataState objects.
     * 
     * @param <T> The type of data expected from the operation
     * @param request A Supplier function that performs the actual data operation
     * @return A CompletableFuture containing a DataState with either success data or error information
     */
    protected <T> CompletableFuture<DataState<T>> getResult(Supplier<T> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Execute the data operation
                T result = request.get();
                
                // Wrap successful result in DataState
                return DataState.success(result);
            } catch (Exception exception) {
                // Log the error
                AsyncErrorLogger.logError("getResult", exception);

                // Wrap any exception in DataState
                return DataState.error(exception.getMessage());
            }
        }, executorService);
    }

    /**
     * Executes a data operation asynchronously with operation context for better error logging.
     *
     * @param <T> The type of data expected from the operation
     * @param request A Supplier function that performs the actual data operation
     * @param operationType The type of operation for logging
     * @return A CompletableFuture containing a DataState with either success data or error information
     */
    protected <T> CompletableFuture<DataState<T>> getResult(Supplier<T> request, String operationType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Execute the data operation
                T result = request.get();

                // Wrap successful result in DataState
                return DataState.success(result);
            } catch (Exception exception) {
                // Log the error with operation type
                AsyncErrorLogger.logError(operationType, exception);

                // Wrap any exception in DataState
                return DataState.error(exception.getMessage());
            }
        }, executorService);
    }

    /**
     * Executes a data operation asynchronously with full context for error logging.
     *
     * @param <T> The type of data expected from the operation
     * @param request A Supplier function that performs the actual data operation
     * @param operationType The type of operation for logging
     * @param userId The user ID associated with the operation
     * @return A CompletableFuture containing a DataState with either success data or error information
     */
    protected <T> CompletableFuture<DataState<T>> getResultWithContext(
            Supplier<T> request, String operationType, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Execute the data operation
                T result = request.get();

                // Wrap successful result in DataState
                return DataState.success(result);
            } catch (Exception exception) {
                // Log the error with user context
                AsyncErrorLogger.logError(operationType, exception, userId);

                // Wrap any exception in DataState
                return DataState.error(exception.getMessage());
            }
        }, executorService);
    }

    /**
     * Executes a network-dependent operation with proper offline handling.
     * This method checks network connectivity before executing the operation.
     * If offline, it returns an appropriate error state.
     *
     * @param <T> The type of data expected from the operation
     * @param networkRequest A Supplier function that performs the network operation
     * @param offlineMessage Custom message to show when offline
     * @return A CompletableFuture containing a DataState with either success data or error information
     */
    protected <T> CompletableFuture<DataState<T>> getNetworkResult(
            Supplier<T> networkRequest,
            String offlineMessage) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check network connectivity first
                if (networkMonitor != null && !networkMonitor.isCurrentlyConnected()) {
                    return DataState.error(offlineMessage != null ?
                        offlineMessage : "Không có kết nối mạng. Vui lòng kiểm tra và thử lại.");
                }

                // Execute the network operation
                T result = networkRequest.get();

                // Wrap successful result in DataState
                return DataState.success(result);
            } catch (Exception exception) {
                // Handle network-specific exceptions
                String errorMessage = exception.getMessage();
                if (errorMessage != null && (
                    errorMessage.contains("network") ||
                    errorMessage.contains("timeout") ||
                    errorMessage.contains("connection") ||
                    errorMessage.contains("host"))) {
                    return DataState.error("Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.");
                }

                // Wrap any other exception in DataState
                return DataState.error(errorMessage != null ? errorMessage : "Đã xảy ra lỗi không xác định");
            }
        }, executorService);
    }

    /**
     * Executes a network-dependent operation with default offline message.
     *
     * @param <T> The type of data expected from the operation
     * @param networkRequest A Supplier function that performs the network operation
     * @return A CompletableFuture containing a DataState with either success data or error information
     */
    protected <T> CompletableFuture<DataState<T>> getNetworkResult(Supplier<T> networkRequest) {
        return getNetworkResult(networkRequest, null);
    }

    /**
     * Check if network is currently available
     * @return true if network is available, false otherwise
     */
    protected boolean isNetworkAvailable() {
        return networkMonitor != null && networkMonitor.isCurrentlyConnected();
    }

    /**
     * Check if currently on WiFi (for large operations)
     * @return true if on WiFi, false otherwise
     */
    protected boolean isOnWiFi() {
        return networkMonitor != null && networkMonitor.isOnWiFi();
    }

    /**
     * Check if on metered connection (cellular)
     * @return true if on metered connection, false otherwise
     */
    protected boolean isOnMeteredConnection() {
        return networkMonitor != null && networkMonitor.isMeteredConnection();
    }

    /**
     * Log thread pool metrics for monitoring performance.
     * Call this periodically to track thread pool health.
     */
    protected void logThreadPoolMetrics() {
        AsyncPerformanceLogger.logThreadPoolMetrics("IO", IO_EXECUTOR);
        AsyncPerformanceLogger.logThreadPoolMetrics("Compute", COMPUTE_EXECUTOR);
    }

    /**
     * Log thread pool metrics for a specific executor.
     *
     * @param poolName Name of the thread pool
     * @param executor The executor to monitor
     */
    protected void logThreadPoolMetrics(String poolName, ExecutorService executor) {
        AsyncPerformanceLogger.logThreadPoolMetrics(poolName, executor);
    }

    /**
     * Get the shared IO executor for external monitoring.
     *
     * @return The IO ExecutorService
     */
    public static ExecutorService getSharedIoExecutor() {
        return IO_EXECUTOR;
    }

    /**
     * Get the shared compute executor for external monitoring.
     *
     * @return The compute ExecutorService
     */
    public static ExecutorService getSharedComputeExecutor() {
        return COMPUTE_EXECUTOR;
    }
}