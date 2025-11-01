package com.example.healthylifehub.base;

/**
 * DataState represents the state of data operations (loading, success, error).
 * This class implements the Result pattern for type-safe handling of async operations.
 * 
 * @param <T> The type of data contained in successful operations
 */
public class DataState<T> {

    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final T data;
    private final String message;

    private DataState(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> DataState<T> loading() {
        return new DataState<>(Status.LOADING, null, null);
    }

    public static <T> DataState<T> success(T data) {
        return new DataState<>(Status.SUCCESS, data, null);
    }

    public static <T> DataState<T> error(String message) {
        return new DataState<>(Status.ERROR, null, message);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}