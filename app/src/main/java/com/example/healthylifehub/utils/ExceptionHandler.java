package com.example.healthylifehub.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * ExceptionHandler provides centralized exception handling for the HealthyLife Hub application.
 * It categorizes exceptions and provides user-friendly error messages in Vietnamese.
 * 
 * Features:
 * - Network exception handling
 * - Firebase-specific exception handling
 * - User-friendly error messages
 * - Logging for debugging
 * - Toast notifications for users
 */
public class ExceptionHandler {
    
    private static final String TAG = "ExceptionHandler";
    
    /**
     * Exception categories for better error handling
     */
    public enum ExceptionCategory {
        NETWORK_ERROR,
        FIREBASE_AUTH_ERROR,
        FIREBASE_FIRESTORE_ERROR,
        VALIDATION_ERROR,
        PERMISSION_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * Exception result containing category and user message
     */
    public static class ExceptionResult {
        private final ExceptionCategory category;
        private final String userMessage;
        private final String technicalMessage;
        private final boolean shouldRetry;
        
        public ExceptionResult(ExceptionCategory category, String userMessage, String technicalMessage, boolean shouldRetry) {
            this.category = category;
            this.userMessage = userMessage;
            this.technicalMessage = technicalMessage;
            this.shouldRetry = shouldRetry;
        }
        
        public ExceptionCategory getCategory() { return category; }
        public String getUserMessage() { return userMessage; }
        public String getTechnicalMessage() { return technicalMessage; }
        public boolean shouldRetry() { return shouldRetry; }
    }
    
    /**
     * Handle exception and return user-friendly result
     * @param exception The exception to handle
     * @return ExceptionResult with category and messages
     */
    public static ExceptionResult handleException(Exception exception) {
        if (exception == null) {
            return new ExceptionResult(
                ExceptionCategory.UNKNOWN_ERROR,
                "Đã xảy ra lỗi không xác định",
                "Null exception",
                false
            );
        }
        
        Log.e(TAG, "Handling exception: " + exception.getClass().getSimpleName(), exception);
        
        // Network exceptions
        if (isNetworkException(exception)) {
            return handleNetworkException(exception);
        }
        
        // Firebase Auth exceptions
        if (exception instanceof FirebaseAuthException) {
            return handleFirebaseAuthException((FirebaseAuthException) exception);
        }
        
        // Firebase Firestore exceptions
        if (exception instanceof FirebaseFirestoreException) {
            return handleFirebaseFirestoreException((FirebaseFirestoreException) exception);
        }
        
        // Firebase general exceptions
        if (exception instanceof FirebaseException) {
            return handleFirebaseException((FirebaseException) exception);
        }
        
        // Validation exceptions
        if (isValidationException(exception)) {
            return handleValidationException(exception);
        }
        
        // Default unknown error
        return new ExceptionResult(
            ExceptionCategory.UNKNOWN_ERROR,
            "Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.",
            exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName(),
            true
        );
    }
    
    /**
     * Handle exception and show toast to user
     * @param context Context for showing toast
     * @param exception The exception to handle
     * @return ExceptionResult for further processing
     */
    public static ExceptionResult handleExceptionWithToast(Context context, Exception exception) {
        ExceptionResult result = handleException(exception);
        
        if (context != null) {
            String toastMessage = result.getUserMessage();
            if (result.shouldRetry()) {
                toastMessage += " (Có thể thử lại)";
            }
            
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
        }
        
        return result;
    }
    
    /**
     * Check if exception is network-related
     */
    private static boolean isNetworkException(Exception exception) {
        return exception instanceof ConnectException ||
               exception instanceof UnknownHostException ||
               exception instanceof SocketTimeoutException ||
               exception instanceof TimeoutException ||
               exception instanceof FirebaseNetworkException ||
               (exception.getMessage() != null && (
                   exception.getMessage().toLowerCase().contains("network") ||
                   exception.getMessage().toLowerCase().contains("connection") ||
                   exception.getMessage().toLowerCase().contains("timeout") ||
                   exception.getMessage().toLowerCase().contains("host")
               ));
    }
    
    /**
     * Handle network exceptions
     */
    private static ExceptionResult handleNetworkException(Exception exception) {
        String userMessage;
        boolean shouldRetry = true;
        
        if (exception instanceof ConnectException || exception instanceof UnknownHostException) {
            userMessage = "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.";
        } else if (exception instanceof SocketTimeoutException || exception instanceof TimeoutException) {
            userMessage = "Kết nối bị timeout. Vui lòng thử lại.";
        } else if (exception instanceof FirebaseNetworkException) {
            userMessage = "Lỗi kết nối Firebase. Vui lòng kiểm tra mạng và thử lại.";
        } else {
            userMessage = "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.";
        }
        
        return new ExceptionResult(
            ExceptionCategory.NETWORK_ERROR,
            userMessage,
            exception.getMessage(),
            shouldRetry
        );
    }
    
    /**
     * Handle Firebase Auth exceptions
     */
    private static ExceptionResult handleFirebaseAuthException(FirebaseAuthException exception) {
        String userMessage;
        boolean shouldRetry = false;
        
        String errorCode = exception.getErrorCode();
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                userMessage = "Email không hợp lệ. Vui lòng kiểm tra lại.";
                break;
            case "ERROR_WRONG_PASSWORD":
                userMessage = "Mật khẩu không đúng. Vui lòng thử lại.";
                break;
            case "ERROR_USER_NOT_FOUND":
                userMessage = "Không tìm thấy tài khoản với email này.";
                break;
            case "ERROR_USER_DISABLED":
                userMessage = "Tài khoản đã bị vô hiệu hóa.";
                break;
            case "ERROR_TOO_MANY_REQUESTS":
                userMessage = "Quá nhiều yêu cầu. Vui lòng thử lại sau.";
                shouldRetry = true;
                break;
            case "ERROR_OPERATION_NOT_ALLOWED":
                userMessage = "Phương thức đăng nhập này không được phép.";
                break;
            case "ERROR_EMAIL_ALREADY_IN_USE":
                userMessage = "Email này đã được sử dụng cho tài khoản khác.";
                break;
            case "ERROR_WEAK_PASSWORD":
                userMessage = "Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn.";
                break;
            case "ERROR_NETWORK_REQUEST_FAILED":
                userMessage = "Lỗi kết nối mạng. Vui lòng thử lại.";
                shouldRetry = true;
                break;
            default:
                userMessage = "Lỗi xác thực: " + exception.getMessage();
                shouldRetry = true;
                break;
        }
        
        return new ExceptionResult(
            ExceptionCategory.FIREBASE_AUTH_ERROR,
            userMessage,
            errorCode + ": " + exception.getMessage(),
            shouldRetry
        );
    }
    
    /**
     * Handle Firebase Firestore exceptions
     */
    private static ExceptionResult handleFirebaseFirestoreException(FirebaseFirestoreException exception) {
        String userMessage;
        boolean shouldRetry = true;
        
        FirebaseFirestoreException.Code code = exception.getCode();
        switch (code) {
            case PERMISSION_DENIED:
                userMessage = "Không có quyền truy cập dữ liệu này.";
                shouldRetry = false;
                break;
            case NOT_FOUND:
                userMessage = "Không tìm thấy dữ liệu yêu cầu.";
                shouldRetry = false;
                break;
            case ALREADY_EXISTS:
                userMessage = "Dữ liệu đã tồn tại.";
                shouldRetry = false;
                break;
            case RESOURCE_EXHAUSTED:
                userMessage = "Đã vượt quá giới hạn sử dụng. Vui lòng thử lại sau.";
                break;
            case FAILED_PRECONDITION:
                userMessage = "Điều kiện không thỏa mãn để thực hiện thao tác.";
                shouldRetry = false;
                break;
            case ABORTED:
                userMessage = "Thao tác bị hủy bỏ. Vui lòng thử lại.";
                break;
            case OUT_OF_RANGE:
                userMessage = "Dữ liệu nằm ngoài phạm vi cho phép.";
                shouldRetry = false;
                break;
            case UNIMPLEMENTED:
                userMessage = "Tính năng này chưa được hỗ trợ.";
                shouldRetry = false;
                break;
            case INTERNAL:
                userMessage = "Lỗi hệ thống nội bộ. Vui lòng thử lại sau.";
                break;
            case UNAVAILABLE:
                userMessage = "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại.";
                break;
            case DATA_LOSS:
                userMessage = "Mất dữ liệu không thể khôi phục.";
                shouldRetry = false;
                break;
            case UNAUTHENTICATED:
                userMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
                shouldRetry = false;
                break;
            default:
                userMessage = "Lỗi cơ sở dữ liệu: " + exception.getMessage();
                break;
        }
        
        return new ExceptionResult(
            ExceptionCategory.FIREBASE_FIRESTORE_ERROR,
            userMessage,
            code.name() + ": " + exception.getMessage(),
            shouldRetry
        );
    }
    
    /**
     * Handle general Firebase exceptions
     */
    private static ExceptionResult handleFirebaseException(FirebaseException exception) {
        String userMessage;
        boolean shouldRetry = true;
        
        if (exception instanceof FirebaseTooManyRequestsException) {
            userMessage = "Quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.";
        } else if (exception instanceof FirebaseNetworkException) {
            userMessage = "Lỗi kết nối Firebase. Vui lòng kiểm tra mạng.";
        } else {
            userMessage = "Lỗi Firebase: " + exception.getMessage();
        }
        
        return new ExceptionResult(
            ExceptionCategory.FIREBASE_FIRESTORE_ERROR,
            userMessage,
            exception.getMessage(),
            shouldRetry
        );
    }
    
    /**
     * Check if exception is validation-related
     */
    private static boolean isValidationException(Exception exception) {
        String message = exception.getMessage();
        return message != null && (
            message.toLowerCase().contains("validation") ||
            message.toLowerCase().contains("invalid") ||
            message.toLowerCase().contains("required") ||
            message.toLowerCase().contains("format")
        );
    }
    
    /**
     * Handle validation exceptions
     */
    private static ExceptionResult handleValidationException(Exception exception) {
        return new ExceptionResult(
            ExceptionCategory.VALIDATION_ERROR,
            "Dữ liệu không hợp lệ: " + exception.getMessage(),
            exception.getMessage(),
            false
        );
    }
    
    /**
     * Log exception for debugging
     * @param tag Log tag
     * @param message Log message
     * @param exception Exception to log
     */
    public static void logException(String tag, String message, Exception exception) {
        Log.e(tag, message, exception);
        
        // In production, you might want to send this to crash reporting service
        // like Firebase Crashlytics
        // FirebaseCrashlytics.getInstance().recordException(exception);
    }
    
    /**
     * Check if exception should trigger retry mechanism
     * @param exception Exception to check
     * @return true if should retry, false otherwise
     */
    public static boolean shouldRetry(Exception exception) {
        ExceptionResult result = handleException(exception);
        return result.shouldRetry();
    }
    
    /**
     * Get user-friendly message for exception
     * @param exception Exception to get message for
     * @return User-friendly message in Vietnamese
     */
    public static String getUserMessage(Exception exception) {
        ExceptionResult result = handleException(exception);
        return result.getUserMessage();
    }
    
    /**
     * Get exception category
     * @param exception Exception to categorize
     * @return Exception category
     */
    public static ExceptionCategory getCategory(Exception exception) {
        ExceptionResult result = handleException(exception);
        return result.getCategory();
    }
}