package com.example.base.base;

/**
 * DataState is an abstract sealed class that represents the state of data operations in the Base application.
 * This class implements the Result pattern to provide a type-safe way of handling success and error states
 * from asynchronous operations, network requests, or any operation that can succeed or fail.
 * 
 * The DataState pattern helps eliminate null pointer exceptions and provides explicit handling
 * of both success and error scenarios, making the code more robust and predictable.
 * 
 * @param <T> The type of data contained in successful operations
 */
public abstract class DataState<T> {

    /**
     * Success represents a successful data operation with a result.
     * This class encapsulates the successful result of an operation,
     * providing type-safe access to the returned data.
     * 
     * @param <T> The type of data contained in this successful result
     */
    public static class Success<T> extends DataState<T> {
        /** The data returned from the successful operation */
        private final T data;

        /**
         * Constructor for Success state.
         * Creates a new Success instance containing the specified data.
         * 
         * @param data The data result from the successful operation
         */
        public Success(T data) {
            this.data = data;
        }

        /**
         * Gets the data from this successful operation.
         * This method provides access to the actual result data
         * that was returned from the successful operation.
         * 
         * @return The data result from the operation
         */
        public T getData() {
            return data;
        }
    }

    /**
     * Error represents a failed data operation with exception information.
     * This class encapsulates the error state of an operation,
     * providing access to the exception that caused the failure.
     */
    public static class Error extends DataState<Object> {
        /** The exception that caused the operation to fail */
        private final Exception exception;

        /**
         * Constructor for Error state.
         * Creates a new Error instance containing the specified exception.
         * 
         * @param exception The exception that caused the operation failure
         */
        public Error(Exception exception) {
            this.exception = exception;
        }

        /**
         * Gets the exception from this failed operation.
         * This method provides access to the exception that caused
         * the operation to fail, enabling proper error handling.
         * 
         * @return The exception that caused the operation failure
         */
        public Exception getException() {
            return exception;
        }
    }
}