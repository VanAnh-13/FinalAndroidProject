package com.example.healthylifehub.base;

/**
 * Result wrapper class for handling async operation results.
 * Provides a type-safe way to represent success, error, and loading states.
 * 
 * This pattern is useful for:
 * - Wrapping async operation results
 * - Providing consistent error handling
 * - Supporting loading states for UI
 * 
 * @param <T> The type of data contained in the result
 */
public class Result<T> {
    
    private final T data;
    private final Throwable error;
    private final Status status;
    
    /**
     * Status enum representing the state of the result
     */
    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
    
    private Result(T data, Throwable error, Status status) {
        this.data = data;
        this.error = error;
        this.status = status;
    }
    
    /**
     * Create a successful result with data
     * 
     * @param <T> The type of data
     * @param data The data to wrap
     * @return Result with SUCCESS status
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, Status.SUCCESS);
    }
    
    /**
     * Create an error result with throwable
     * 
     * @param <T> The type of data
     * @param error The error that occurred
     * @return Result with ERROR status
     */
    public static <T> Result<T> error(Throwable error) {
        return new Result<>(null, error, Status.ERROR);
    }
    
    /**
     * Create an error result with message
     * 
     * @param <T> The type of data
     * @param message The error message
     * @return Result with ERROR status
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(null, new Exception(message), Status.ERROR);
    }
    
    /**
     * Create a loading result
     * 
     * @param <T> The type of data
     * @return Result with LOADING status
     */
    public static <T> Result<T> loading() {
        return new Result<>(null, null, Status.LOADING);
    }
    
    /**
     * Get the data from the result
     * 
     * @return The data, or null if error or loading
     */
    public T getData() {
        return data;
    }
    
    /**
     * Get the error from the result
     * 
     * @return The error, or null if success or loading
     */
    public Throwable getError() {
        return error;
    }
    
    /**
     * Get the error message
     * 
     * @return The error message, or null if success or loading
     */
    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }
    
    /**
     * Get the status of the result
     * 
     * @return The status (SUCCESS, ERROR, or LOADING)
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Check if the result is successful
     * 
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    /**
     * Check if the result is an error
     * 
     * @return true if status is ERROR
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    /**
     * Check if the result is loading
     * 
     * @return true if status is LOADING
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }
}
