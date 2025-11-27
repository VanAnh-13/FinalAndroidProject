package com.example.healthylifehub.utils.error;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.healthylifehub.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive error handling utility
 */
public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    
    public enum ErrorCategory {
        VALIDATION, NOTIFICATION, DATABASE, NETWORK, PERMISSION, BACKGROUND_PROCESSING, GENERAL
    }
    
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public static final int ERROR_INVALID_DEADLINE = 1001;
    public static final int ERROR_DEADLINE_IN_PAST = 1002;
    public static final int ERROR_EMPTY_TITLE = 1003;
    public static final int ERROR_INVALID_TIME = 1004;
    public static final int ERROR_NOTIFICATION_PERMISSION_DENIED = 2001;
    public static final int ERROR_NOTIFICATION_SEND_FAILED = 2002;
    public static final int ERROR_NOTIFICATION_ACTION_FAILED = 2003;
    public static final int ERROR_DATABASE_CONNECTION_FAILED = 3001;
    public static final int ERROR_DATABASE_OPERATION_FAILED = 3002;
    public static final int ERROR_REMINDER_NOT_FOUND = 3003;
    public static final int ERROR_BACKGROUND_WORK_FAILED = 4001;
    public static final int ERROR_WORK_MANAGER_UNAVAILABLE = 4002;
    public static final int ERROR_RETRY_LIMIT_EXCEEDED = 4003;
    
    private static final Map<Integer, String> ERROR_MESSAGES = new HashMap<>();
    
    static {
        ERROR_MESSAGES.put(ERROR_INVALID_DEADLINE, "Thời hạn không hợp lệ");
        ERROR_MESSAGES.put(ERROR_DEADLINE_IN_PAST, "Thời hạn phải là ngày trong tương lai");
        ERROR_MESSAGES.put(ERROR_EMPTY_TITLE, "Vui lòng nhập tiêu đề nhắc nhở");
        ERROR_MESSAGES.put(ERROR_INVALID_TIME, "Thời gian không hợp lệ");
        ERROR_MESSAGES.put(ERROR_NOTIFICATION_PERMISSION_DENIED, "Cần cấp quyền thông báo để nhắc nhở hoạt động");
        ERROR_MESSAGES.put(ERROR_NOTIFICATION_SEND_FAILED, "Không thể gửi thông báo nhắc nhở");
        ERROR_MESSAGES.put(ERROR_NOTIFICATION_ACTION_FAILED, "Không thể xử lý hành động thông báo");
        ERROR_MESSAGES.put(ERROR_DATABASE_CONNECTION_FAILED, "Không thể kết nối cơ sở dữ liệu");
        ERROR_MESSAGES.put(ERROR_DATABASE_OPERATION_FAILED, "Lỗi khi thao tác với dữ liệu");
        ERROR_MESSAGES.put(ERROR_REMINDER_NOT_FOUND, "Không tìm thấy nhắc nhở");
        ERROR_MESSAGES.put(ERROR_BACKGROUND_WORK_FAILED, "Lỗi xử lý nền");
        ERROR_MESSAGES.put(ERROR_WORK_MANAGER_UNAVAILABLE, "Dịch vụ xử lý nền không khả dụng");
        ERROR_MESSAGES.put(ERROR_RETRY_LIMIT_EXCEEDED, "Đã vượt quá số lần thử lại");
    }
    
    public static void handleError(Context context, ErrorCategory category, ErrorSeverity severity,
                                 int errorCode, Throwable exception, String additionalInfo) {
        String errorMessage = getErrorMessage(errorCode);
        String logMessage = buildLogMessage(category, errorCode, errorMessage, additionalInfo, exception);
        
        switch (severity) {
            case LOW: Log.d(TAG, logMessage); break;
            case MEDIUM: Log.w(TAG, logMessage); break;
            case HIGH: Log.e(TAG, logMessage); break;
            case CRITICAL: Log.wtf(TAG, logMessage); break;
        }
        
        if (severity == ErrorSeverity.MEDIUM || severity == ErrorSeverity.HIGH) {
            showUserFriendlyMessage(context, errorCode, errorMessage);
        }
    }
    
    public static void handleError(Context context, ErrorCategory category, int errorCode, Throwable exception) {
        handleError(context, category, ErrorSeverity.MEDIUM, errorCode, exception, null);
    }
    
    public static void handleValidationError(Context context, int errorCode, String fieldName) {
        handleError(context, ErrorCategory.VALIDATION, ErrorSeverity.LOW, errorCode, null, "Field: " + fieldName);
    }
    
    public static void handleNotificationError(Context context, int errorCode, Throwable exception, 
                                             String reminderId, boolean canRetry) {
        String additionalInfo = "ReminderId: " + reminderId + ", CanRetry: " + canRetry;
        ErrorSeverity severity = canRetry ? ErrorSeverity.MEDIUM : ErrorSeverity.HIGH;
        handleError(context, ErrorCategory.NOTIFICATION, severity, errorCode, exception, additionalInfo);
    }
    
    public static void handleDatabaseError(Context context, int errorCode, Throwable exception, 
                                         String operation, String entityId) {
        String additionalInfo = "Operation: " + operation + ", EntityId: " + entityId;
        handleError(context, ErrorCategory.DATABASE, ErrorSeverity.HIGH, errorCode, exception, additionalInfo);
    }
    
    public static void handleBackgroundError(Context context, int errorCode, Throwable exception,
                                           String workerId, int attemptCount, int maxAttempts) {
        String additionalInfo = String.format("WorkerId: %s, Attempt: %d/%d", workerId, attemptCount, maxAttempts);
        ErrorSeverity severity = attemptCount >= maxAttempts ? ErrorSeverity.HIGH : ErrorSeverity.MEDIUM;
        handleError(context, ErrorCategory.BACKGROUND_PROCESSING, severity, errorCode, exception, additionalInfo);
    }
    
    public static String getErrorMessage(int errorCode) {
        return ERROR_MESSAGES.getOrDefault(errorCode, "Đã xảy ra lỗi không xác định");
    }
    
    private static String buildLogMessage(ErrorCategory category, int errorCode, String errorMessage,
                                        String additionalInfo, Throwable exception) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(category.name()).append("] ");
        sb.append("Error ").append(errorCode).append(": ").append(errorMessage);
        if (additionalInfo != null) sb.append(" | ").append(additionalInfo);
        if (exception != null) sb.append(" | Exception: ").append(exception.getMessage());
        return sb.toString();
    }
    
    private static void showUserFriendlyMessage(Context context, int errorCode, String errorMessage) {
        if (context != null) {
            int duration = isRecoverableError(errorCode) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
            Toast.makeText(context, context.getString(R.string.warning_prefix) + errorMessage, duration).show();
        }
    }
    
    private static boolean isRecoverableError(int errorCode) {
        switch (errorCode) {
            case ERROR_NOTIFICATION_SEND_FAILED:
            case ERROR_DATABASE_CONNECTION_FAILED:
            case ERROR_BACKGROUND_WORK_FAILED:
                return true;
            default:
                return false;
        }
    }
    
    public static String createErrorReport(ErrorCategory category, int errorCode, Throwable exception, String additionalInfo) {
        StringBuilder report = new StringBuilder();
        report.append("=== ERROR REPORT ===\n");
        report.append("Category: ").append(category.name()).append("\n");
        report.append("Error Code: ").append(errorCode).append("\n");
        report.append("Message: ").append(getErrorMessage(errorCode)).append("\n");
        if (additionalInfo != null) report.append("Additional Info: ").append(additionalInfo).append("\n");
        if (exception != null) report.append("Exception: ").append(exception.getMessage()).append("\n");
        return report.toString();
    }
}
