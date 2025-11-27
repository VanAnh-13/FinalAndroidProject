package com.example.healthylifehub.base;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.healthylifehub.utils.network.NetworkMonitor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * BaseViewModel is an abstract base class for all ViewModels in the Base application.
 * This class provides a standardized approach to ViewModel implementation with
 * asynchronous task execution, loading state management, and proper resource cleanup.
 * 
 * The ViewModel is designed to store and manage UI-related data in a lifecycle conscious way,
 * surviving configuration changes and providing a clean separation between UI and business logic.
 */
public class BaseViewModel extends AndroidViewModel {

    public BaseViewModel(Application application) {
        super(application);
        this.networkMonitor = NetworkMonitor.getInstance(application);
    }

    /** MutableLiveData for managing loading state across the application */
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    
    /** MutableLiveData for managing network connectivity state */
    private final MutableLiveData<Boolean> _isNetworkAvailable = new MutableLiveData<>(true);
    
    /** MutableLiveData for managing network error messages */
    private final MutableLiveData<String> _networkError = new MutableLiveData<>();
    
    /** ExecutorService for handling asynchronous operations in background threads */
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /** Network monitor for checking connectivity */
    protected final NetworkMonitor networkMonitor;

    /**
     * Gets the loading state as LiveData for observation by UI components.
     * This allows UI components to react to loading state changes and
     * display appropriate loading indicators.
     * 
     * @return LiveData containing the current loading state
     */
    public LiveData<Boolean> getLoading() {
        return _loading;
    }
    
    /**
     * Gets the network availability state as LiveData for observation by UI components.
     * This allows UI components to react to network changes and disable/enable features.
     * 
     * @return LiveData containing the current network availability state
     */
    public LiveData<Boolean> getNetworkAvailable() {
        return _isNetworkAvailable;
    }
    
    /**
     * Gets network error messages as LiveData for observation by UI components.
     * This allows UI components to display appropriate network error messages.
     * 
     * @return LiveData containing network error messages
     */
    public LiveData<String> getNetworkError() {
        return _networkError;
    }
    
    /**
     * Gets the network connectivity state from NetworkMonitor as LiveData.
     * This provides real-time network status updates.
     * 
     * @return LiveData containing the current network connectivity state
     */
    public LiveData<Boolean> getNetworkConnectivity() {
        return networkMonitor.isConnected();
    }

    /**
     * Executes an asynchronous task with comprehensive error handling and loading state management.
     * This method provides a standardized way to execute repository operations or other
     * asynchronous tasks while automatically managing loading states and handling results.
     * 
     * @param <T> The type of data expected from the operation
     * @param request A Supplier function that returns a DataState containing the operation result
     * @param onSuccess A Consumer function to handle successful operation results
     * @param onError A Consumer function to handle operation errors
     * @param showLoading Boolean flag indicating whether to show loading state during execution
     */
    protected <T> void executeTask(
            Supplier<DataState<T>> request,
            Consumer<T> onSuccess,
            Consumer<Exception> onError,
            boolean showLoading
    ) {
        CompletableFuture.supplyAsync(() -> {
            // Show loading indicator if requested
            if (showLoading) {
                showLoading();
            }
            
            // Execute the actual request
            return request.get();
        }, executorService).thenAccept(response -> {
            // Handle the response based on its type
            if (response.isSuccess()) {
                // Extract data from successful response and call success callback
                onSuccess.accept(response.getData());
                hideLoading();
            } else if (response.isError()) {
                // Call error callback with error message
                onError.accept(new Exception(response.getMessage()));
                hideLoading();
            }
        });
    }

    /**
     * Executes an asynchronous task with default loading state management.
     * This is a convenience method that calls executeTask with showLoading set to true.
     * 
     * @param <T> The type of data expected from the operation
     * @param request A Supplier function that returns a DataState containing the operation result
     * @param onSuccess A Consumer function to handle successful operation results
     * @param onError A Consumer function to handle operation errors
     */
    protected <T> void executeTask(
            Supplier<DataState<T>> request,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        executeTask(request, onSuccess, onError, true);
    }
    
    /**
     * Executes a network-dependent task with proper offline handling.
     * This method checks network connectivity before executing the task.
     * If offline, it shows an appropriate error message and disables network-dependent features.
     * 
     * @param <T> The type of data expected from the operation
     * @param networkRequest A Supplier function that returns a DataState containing the network operation result
     * @param onSuccess A Consumer function to handle successful operation results
     * @param onError A Consumer function to handle operation errors
     * @param offlineMessage Custom message to show when offline
     * @param showLoading Boolean flag indicating whether to show loading state during execution
     */
    protected <T> void executeNetworkTask(
            Supplier<DataState<T>> networkRequest,
            Consumer<T> onSuccess,
            Consumer<Exception> onError,
            String offlineMessage,
            boolean showLoading
    ) {
        CompletableFuture.supplyAsync(() -> {
            // Show loading indicator if requested
            if (showLoading) {
                showLoading();
            }
            
            // Check network connectivity first
            if (!networkMonitor.isCurrentlyConnected()) {
                _isNetworkAvailable.postValue(false);
                String message = offlineMessage != null ? 
                    offlineMessage : "Không có kết nối mạng. Vui lòng kiểm tra và thử lại.";
                _networkError.postValue(message);
                return DataState.<T>error(message);
            }
            
            _isNetworkAvailable.postValue(true);
            _networkError.postValue(null);
            
            // Execute the network request
            return networkRequest.get();
        }, executorService).thenAccept(response -> {
            // Handle the response based on its type
            if (response.isSuccess()) {
                // Extract data from successful response and call success callback
                onSuccess.accept(response.getData());
                hideLoading();
            } else if (response.isError()) {
                // Handle network-specific errors
                String errorMessage = response.getMessage();
                if (errorMessage != null && (
                    errorMessage.contains("network") || 
                    errorMessage.contains("timeout") ||
                    errorMessage.contains("connection") ||
                    errorMessage.contains("host"))) {
                    _networkError.postValue("Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.");
                } else {
                    _networkError.postValue(errorMessage);
                }
                
                // Call error callback with error message
                onError.accept(new Exception(response.getMessage()));
                hideLoading();
            }
        });
    }
    
    /**
     * Executes a network-dependent task with default offline message and loading state.
     * 
     * @param <T> The type of data expected from the operation
     * @param networkRequest A Supplier function that returns a DataState containing the network operation result
     * @param onSuccess A Consumer function to handle successful operation results
     * @param onError A Consumer function to handle operation errors
     */
    protected <T> void executeNetworkTask(
            Supplier<DataState<T>> networkRequest,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        executeNetworkTask(networkRequest, onSuccess, onError, null, true);
    }
    
    /**
     * Check if network is currently available
     * @return true if network is available, false otherwise
     */
    protected boolean isNetworkAvailable() {
        return networkMonitor.isCurrentlyConnected();
    }
    
    /**
     * Check if currently on WiFi (for large operations)
     * @return true if on WiFi, false otherwise
     */
    protected boolean isOnWiFi() {
        return networkMonitor.isOnWiFi();
    }
    
    /**
     * Check if on metered connection (cellular)
     * @return true if on metered connection, false otherwise
     */
    protected boolean isOnMeteredConnection() {
        return networkMonitor.isMeteredConnection();
    }
    
    /**
     * Clear network error message
     */
    protected void clearNetworkError() {
        _networkError.postValue(null);
    }

    /**
     * Shows the loading indicator by updating the loading LiveData.
     * This method posts a true value to the loading LiveData,
     * which can be observed by UI components to display loading states.
     */
    public void showLoading() {
        _loading.postValue(true);
    }

    /**
     * Hides the loading indicator by updating the loading LiveData.
     * This method posts a false value to the loading LiveData,
     * which can be observed by UI components to hide loading states.
     */
    public void hideLoading() {
        _loading.postValue(false);
    }

    /**
     * Called when this ViewModel is no longer used and will be destroyed.
     * This method ensures proper cleanup of resources, particularly
     * shutting down the ExecutorService to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // Shutdown the executor service to prevent memory leaks
        executorService.shutdown();
    }
}