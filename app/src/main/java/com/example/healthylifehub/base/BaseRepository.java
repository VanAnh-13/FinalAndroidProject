package com.example.healthylifehub.base;

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
}