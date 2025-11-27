package com.example.healthylifehub.utils.error;

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
 * ExceptionHandler provides centralized exception handling.
 */
public class ExceptionHandler {
    
    private static final String TAG = "ExceptionHandler";
    
    public enum ExceptionCategory {
        NETWORK_ERROR, FIREBASE_AUTH_ERROR, FIREBASE_FIRESTORE_ERROR, VALIDATION_ERROR, PERMISSION_ERROR, UNKNOWN_ERROR
    }
    
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
    
    public static ExceptionResult handleException(Exception exception) {
        if (exception == null) {
            return new ExceptionResult(ExceptionCategory.UNKNOWN_ERROR, "Đã xảy ra lỗi không xác định", "Null exception", false);
        }
        
        Log.e(TAG, "Handling exception: " + exception.getClass().getSimpleName(), exception);
        
        if (isNetworkException(exception)) return handleNetworkException(exception);
        if (exception instanceof FirebaseAuthException) return handleFirebaseAuthException((FirebaseAuthException) exception);
        if (exception instanceof FirebaseFirestoreException) return handleFirebaseFirestoreException((FirebaseFirestoreException) exception);
        if (exception instanceof FirebaseException) return handleFirebaseException((FirebaseException) exception);
        
        return new ExceptionResult(ExceptionCategory.UNKNOWN_ERROR, "Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.",
            exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName(), true);
    }
    
    public static ExceptionResult handleExceptionWithToast(Context context, Exception exception) {
        ExceptionResult result = handleException(exception);
        if (context != null) {
            String toastMessage = result.getUserMessage();
            if (result.shouldRetry()) toastMessage += " (Có thể thử lại)";
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
        }
        return result;
    }
    
    private static boolean isNetworkException(Exception exception) {
        return exception instanceof ConnectException || exception instanceof UnknownHostException ||
               exception instanceof SocketTimeoutException || exception instanceof TimeoutException ||
               exception instanceof FirebaseNetworkException;
    }
    
    private static ExceptionResult handleNetworkException(Exception exception) {
        String userMessage;
        if (exception instanceof ConnectException || exception instanceof UnknownHostException) {
            userMessage = "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.";
        } else if (exception instanceof SocketTimeoutException || exception instanceof TimeoutException) {
            userMessage = "Kết nối bị timeout. Vui lòng thử lại.";
        } else {
            userMessage = "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.";
        }
        return new ExceptionResult(ExceptionCategory.NETWORK_ERROR, userMessage, exception.getMessage(), true);
    }
    
    private static ExceptionResult handleFirebaseAuthException(FirebaseAuthException exception) {
        String userMessage;
        boolean shouldRetry = false;
        String errorCode = exception.getErrorCode();
        
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL": userMessage = "Email không hợp lệ."; break;
            case "ERROR_WRONG_PASSWORD": userMessage = "Mật khẩu không đúng."; break;
            case "ERROR_USER_NOT_FOUND": userMessage = "Không tìm thấy tài khoản."; break;
            case "ERROR_USER_DISABLED": userMessage = "Tài khoản đã bị vô hiệu hóa."; break;
            case "ERROR_TOO_MANY_REQUESTS": userMessage = "Quá nhiều yêu cầu. Vui lòng thử lại sau."; shouldRetry = true; break;
            case "ERROR_EMAIL_ALREADY_IN_USE": userMessage = "Email đã được sử dụng."; break;
            case "ERROR_WEAK_PASSWORD": userMessage = "Mật khẩu quá yếu."; break;
            default: userMessage = "Lỗi xác thực: " + exception.getMessage(); shouldRetry = true; break;
        }
        return new ExceptionResult(ExceptionCategory.FIREBASE_AUTH_ERROR, userMessage, errorCode + ": " + exception.getMessage(), shouldRetry);
    }
    
    private static ExceptionResult handleFirebaseFirestoreException(FirebaseFirestoreException exception) {
        String userMessage;
        boolean shouldRetry = true;
        FirebaseFirestoreException.Code code = exception.getCode();
        
        switch (code) {
            case PERMISSION_DENIED: userMessage = "Không có quyền truy cập."; shouldRetry = false; break;
            case NOT_FOUND: userMessage = "Không tìm thấy dữ liệu."; shouldRetry = false; break;
            case ALREADY_EXISTS: userMessage = "Dữ liệu đã tồn tại."; shouldRetry = false; break;
            case UNAVAILABLE: userMessage = "Dịch vụ tạm thời không khả dụng."; break;
            case UNAUTHENTICATED: userMessage = "Phiên đăng nhập đã hết hạn."; shouldRetry = false; break;
            default: userMessage = "Lỗi cơ sở dữ liệu: " + exception.getMessage(); break;
        }
        return new ExceptionResult(ExceptionCategory.FIREBASE_FIRESTORE_ERROR, userMessage, code.name() + ": " + exception.getMessage(), shouldRetry);
    }
    
    private static ExceptionResult handleFirebaseException(FirebaseException exception) {
        String userMessage;
        if (exception instanceof FirebaseTooManyRequestsException) {
            userMessage = "Quá nhiều yêu cầu. Vui lòng thử lại sau.";
        } else {
            userMessage = "Lỗi Firebase: " + exception.getMessage();
        }
        return new ExceptionResult(ExceptionCategory.FIREBASE_FIRESTORE_ERROR, userMessage, exception.getMessage(), true);
    }
    
    public static String getUserMessage(Exception exception) {
        return handleException(exception).getUserMessage();
    }
    
    public static boolean shouldRetry(Exception exception) {
        return handleException(exception).shouldRetry();
    }
}
