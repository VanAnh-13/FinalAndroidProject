package com.example.healthylifehub.utils.network;

import android.content.Context;
import android.view.View;
import com.example.healthylifehub.R;
import com.example.healthylifehub.utils.error.ErrorHandler;
import com.example.healthylifehub.utils.ui.UserFeedbackManager;
import com.google.android.material.snackbar.Snackbar;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;

/**
 * Comprehensive network error handler with retry mechanisms
 */
public class NetworkErrorHandler {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;
    
    public interface RetryCallback { void onRetry(); }
    public interface NetworkOperationCallback { void onSuccess(); void onError(Exception error); }
    public interface NetworkOperation { void execute() throws Exception; }
    
    public static void handleNetworkError(Context context, View rootView, Exception error, RetryCallback retryCallback) {
        String errorMessage = getNetworkErrorMessage(context, error);
        
        if (rootView != null) {
            Snackbar snackbar = Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG);
            if (retryCallback != null && isRetryableError(error)) {
                snackbar.setAction(context.getString(R.string.retry), v -> {
                    UserFeedbackManager.showInfo(context, context.getString(R.string.retry_in_progress));
                    retryCallback.onRetry();
                });
            }
            snackbar.show();
        } else {
            UserFeedbackManager.showError(context, errorMessage);
        }
        
        ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.NETWORK, 
                                ErrorHandler.ErrorSeverity.MEDIUM, 
                                getNetworkErrorCode(error), error, null);
    }
    
    public static void executeWithRetry(Context context, View rootView, NetworkOperation operation, NetworkOperationCallback callback) {
        executeWithRetry(context, rootView, operation, callback, 0);
    }
    
    private static void executeWithRetry(Context context, View rootView, NetworkOperation operation, NetworkOperationCallback callback, int attemptCount) {
        try {
            operation.execute();
            if (callback != null) callback.onSuccess();
        } catch (Exception error) {
            if (attemptCount < MAX_RETRY_ATTEMPTS && isRetryableError(error)) {
                if (attemptCount > 0) UserFeedbackManager.showInfo(context, context.getString(R.string.retry_in_progress));
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> executeWithRetry(context, rootView, operation, callback, attemptCount + 1), RETRY_DELAY_MS);
            } else {
                if (attemptCount >= MAX_RETRY_ATTEMPTS) UserFeedbackManager.showError(context, context.getString(R.string.max_retries_reached));
                handleNetworkError(context, rootView, error, () -> executeWithRetry(context, rootView, operation, callback, 0));
                if (callback != null) callback.onError(error);
            }
        }
    }
    
    private static String getNetworkErrorMessage(Context context, Exception error) {
        if (error instanceof UnknownHostException) return context.getString(R.string.error_network_unavailable);
        else if (error instanceof SocketTimeoutException) return "Kết nối quá chậm. Vui lòng thử lại.";
        else if (error instanceof ConnectException) return "Không thể kết nối đến máy chủ.";
        else if (error instanceof IOException) return context.getString(R.string.network_error_message);
        else return context.getString(R.string.error_unexpected);
    }
    
    private static int getNetworkErrorCode(Exception error) {
        if (error instanceof UnknownHostException || error instanceof ConnectException) return ErrorHandler.ERROR_DATABASE_CONNECTION_FAILED;
        else if (error instanceof SocketTimeoutException) return ErrorHandler.ERROR_BACKGROUND_WORK_FAILED;
        else return ErrorHandler.ERROR_DATABASE_OPERATION_FAILED;
    }
    
    private static boolean isRetryableError(Exception error) {
        return error instanceof IOException || error instanceof SocketTimeoutException || error instanceof ConnectException;
    }
    
    public static void showNetworkStatus(Context context, View rootView, boolean isConnected) {
        if (isConnected) UserFeedbackManager.showSuccess(context, "Kết nối mạng đã được khôi phục");
        else UserFeedbackManager.showNetworkError(rootView, null);
    }
}
