package com.example.healthylifehub.base;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
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
    }

    /** MutableLiveData for managing loading state across the application */
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    
    /** ExecutorService for handling asynchronous operations in background threads */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

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