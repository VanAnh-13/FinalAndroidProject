package com.example.healthylifehub.base;

import android.content.Context;
import com.example.healthylifehub.utils.NetworkMonitor;
import java.util.concurrent.CompletableFuture;
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
public class BaseRepository {
    
    /** ExecutorService for handling asynchronous operations in background threads */
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
}