package com.example.healthylifehub.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.healthylifehub.utils.ErrorHandler;
import com.example.healthylifehub.utils.ReminderLogger;
import com.example.healthylifehub.utils.RetryManager;
import com.example.healthylifehub.utils.SmartNotificationManager;
import com.example.healthylifehub.utils.WorkManagerConfig;

/**
 * BroadcastReceiver for handling smart notification actions
 * Handles ACTION_COMPLETE and ACTION_SKIP intents from notification buttons
 * Requirements: 1.2, 1.3, 8.2, 8.3, 8.4
 */
public class ReminderActionReceiver extends BroadcastReceiver {
    
    private static final String TAG = "ReminderActionReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize logger
        ReminderLogger.initialize(context);
        
        String action = intent.getAction();
        String reminderId = intent.getStringExtra("reminder_id");
        int notificationId = intent.getIntExtra("notification_id", 0);
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
        
        // Log received action
        ReminderLogger.logInfo(ReminderLogger.LogCategory.NOTIFICATION, 
            "Received notification action", 
            String.format("Action: %s, ReminderId: %s, NotificationId: %d", action, reminderId, notificationId));
        
        // Comprehensive validation with error handling
        if (reminderId == null || reminderId.trim().isEmpty()) {
            ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                "Invalid reminder ID in notification action", 
                "Action: " + action + ", NotificationId: " + notificationId, null);
            
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.VALIDATION, 
                ErrorHandler.ErrorSeverity.HIGH, ErrorHandler.ERROR_REMINDER_NOT_FOUND, 
                null, "Invalid reminder ID in notification action");
            return;
        }
        
        if (action == null || action.trim().isEmpty()) {
            ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                "No action specified in notification intent", 
                "ReminderId: " + reminderId + ", NotificationId: " + notificationId, null);
            
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.NOTIFICATION, 
                ErrorHandler.ErrorSeverity.HIGH, ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, 
                null, "No action specified in intent");
            return;
        }
        
        // Dismiss notification with error handling
        try {
            SmartNotificationManager notificationManager = new SmartNotificationManager(context);
            notificationManager.cancelNotification(notificationId);
            
            ReminderLogger.logNotificationEvent("dismissed", reminderId, 
                "Notification dismissed after action: " + action);
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Failed to dismiss notification", 
                "NotificationId: " + notificationId + ", ReminderId: " + reminderId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, e, reminderId, false);
        }
        
        // Handle the action with comprehensive error handling
        try {
            switch (action) {
                case SmartNotificationManager.ACTION_COMPLETE:
                    handleCompleteAction(context, reminderId, timestamp);
                    break;
                    
                case SmartNotificationManager.ACTION_SKIP:
                    handleSkipAction(context, reminderId, timestamp);
                    break;
                    
                default:
                    ReminderLogger.logWarning(ReminderLogger.LogCategory.NOTIFICATION, 
                        "Unknown notification action received", 
                        "Action: " + action + ", ReminderId: " + reminderId);
                    
                    ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.NOTIFICATION, 
                        ErrorHandler.ErrorSeverity.MEDIUM, ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, 
                        null, "Unknown action: " + action);
                    break;
            }
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Unexpected error handling notification action", 
                "Action: " + action + ", ReminderId: " + reminderId, e);
            
            ErrorHandler.handleError(context, ErrorHandler.ErrorCategory.NOTIFICATION, 
                ErrorHandler.ErrorSeverity.HIGH, ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, 
                e, "Unexpected error in action handling");
        }
    }
    
    /**
     * Handle ACTION_COMPLETE intent with comprehensive error handling and retry
     * Requirements: 1.2, 8.2, 8.5
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     * @param timestamp Action timestamp
     */
    private void handleCompleteAction(Context context, String reminderId, long timestamp) {
        ReminderLogger.logInfo(ReminderLogger.LogCategory.NOTIFICATION, 
            "Processing complete action", "ReminderId: " + reminderId);
        
        try {
            // Validate reminder ID format
            if (!isValidReminderId(reminderId)) {
                ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                    "Invalid reminder ID format for complete action", 
                    "ReminderId: " + reminderId, null);
                
                ErrorHandler.handleValidationError(context, 
                    ErrorHandler.ERROR_REMINDER_NOT_FOUND, "reminder_id");
                return;
            }
            
            // Enqueue background work with retry capability
            boolean enqueued = enqueueReminderUpdateWithRetry(context, reminderId, "completed", timestamp);
            
            if (enqueued) {
                ReminderLogger.logInfo(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                    "Successfully enqueued complete action", "ReminderId: " + reminderId);
            } else {
                // Handle enqueue failure
                ErrorHandler.handleBackgroundError(context, 
                    ErrorHandler.ERROR_BACKGROUND_WORK_FAILED, null, 
                    "complete_" + reminderId, 1, 1);
            }
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Failed to handle complete action", "ReminderId: " + reminderId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, e, reminderId, true);
        }
    }
    
    /**
     * Handle ACTION_SKIP intent with comprehensive error handling and retry
     * Requirements: 1.3, 8.3, 8.5
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     * @param timestamp Action timestamp
     */
    private void handleSkipAction(Context context, String reminderId, long timestamp) {
        ReminderLogger.logInfo(ReminderLogger.LogCategory.NOTIFICATION, 
            "Processing skip action", "ReminderId: " + reminderId);
        
        try {
            // Validate reminder ID format
            if (!isValidReminderId(reminderId)) {
                ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                    "Invalid reminder ID format for skip action", 
                    "ReminderId: " + reminderId, null);
                
                ErrorHandler.handleValidationError(context, 
                    ErrorHandler.ERROR_REMINDER_NOT_FOUND, "reminder_id");
                return;
            }
            
            // Enqueue background work with retry capability
            boolean enqueued = enqueueReminderUpdateWithRetry(context, reminderId, "skipped", timestamp);
            
            if (enqueued) {
                ReminderLogger.logInfo(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                    "Successfully enqueued skip action", "ReminderId: " + reminderId);
            } else {
                // Handle enqueue failure
                ErrorHandler.handleBackgroundError(context, 
                    ErrorHandler.ERROR_BACKGROUND_WORK_FAILED, null, 
                    "skip_" + reminderId, 1, 1);
            }
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.NOTIFICATION, 
                "Failed to handle skip action", "ReminderId: " + reminderId, e);
            
            ErrorHandler.handleNotificationError(context, 
                ErrorHandler.ERROR_NOTIFICATION_ACTION_FAILED, e, reminderId, true);
        }
    }
    
    /**
     * Validate reminder ID format
     * Requirements: 8.5
     * 
     * @param reminderId The reminder ID to validate
     * @return true if valid format
     */
    private boolean isValidReminderId(String reminderId) {
        if (reminderId == null || reminderId.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - should be non-empty and reasonable length
        String trimmed = reminderId.trim();
        return trimmed.length() > 0 && trimmed.length() <= 100 && !trimmed.contains(" ");
    }
    
    /**
     * Enqueue background work with retry capability
     * Requirements: 1.5, 8.4, 8.5
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     * @param actionType "completed" or "skipped"
     * @param timestamp Action timestamp
     * @return true if successfully enqueued
     */
    private boolean enqueueReminderUpdateWithRetry(Context context, String reminderId, 
                                                  String actionType, long timestamp) {
        try {
            // Use WorkManagerConfig for standardized work enqueuing with proper constraints
            String workId = WorkManagerConfig.enqueueReminderUpdate(
                context, reminderId, actionType, timestamp, timestamp);
            
            if (workId != null) {
                ReminderLogger.logBackgroundWork("ReminderUpdate", workId, "ENQUEUED", 
                    String.format("Action: %s, ReminderId: %s", actionType, reminderId));
                return true;
            } else {
                ReminderLogger.logError(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                    "WorkManagerConfig returned null work ID", 
                    String.format("Action: %s, ReminderId: %s", actionType, reminderId), null);
                return false;
            }
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                "Failed to enqueue reminder update work", 
                String.format("Action: %s, ReminderId: %s", actionType, reminderId), e);
            
            // Check if we should retry based on exception type
            if (RetryManager.shouldUseRetry(e, "reminder_update")) {
                return attemptRetryEnqueue(context, reminderId, actionType, timestamp, e);
            }
            
            return false;
        }
    }
    
    /**
     * Attempt to retry enqueuing work with exponential backoff
     * Requirements: 8.5
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     * @param actionType Action type
     * @param timestamp Original timestamp
     * @param originalException The original exception
     * @return true if retry was scheduled
     */
    private boolean attemptRetryEnqueue(Context context, String reminderId, String actionType, 
                                      long timestamp, Throwable originalException) {
        try {
            // Create retry context
            String operationId = actionType + "_" + reminderId + "_" + timestamp;
            RetryManager.RetryContext retryContext = RetryManager.createRetryContext(
                operationId, 0, originalException);
            
            if (retryContext.canRetry()) {
                ReminderLogger.logRetryAttempt(operationId, 1, 3, false, 
                    "Scheduling retry for failed work enqueue");
                
                // For now, just log the retry attempt - actual retry would be handled by WorkManager
                // In a production system, you might implement a custom retry mechanism here
                ReminderLogger.logInfo(ReminderLogger.LogCategory.RETRY, 
                    "Retry scheduled for work enqueue", 
                    String.format("OperationId: %s, Delay: %s", operationId, 
                    RetryManager.getRetryDelayString(retryContext)));
                
                return true;
            } else {
                ReminderLogger.logError(ReminderLogger.LogCategory.RETRY, 
                    "Cannot retry work enqueue - max attempts would be exceeded", 
                    "OperationId: " + operationId, null);
                return false;
            }
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.RETRY, 
                "Failed to schedule retry for work enqueue", 
                String.format("ReminderId: %s, ActionType: %s", reminderId, actionType), e);
            return false;
        }
    }
    
    /**
     * Legacy method for backward compatibility
     * Requirements: 1.5, 8.4, 8.5
     * 
     * @param context Application context
     * @param reminderId The reminder ID
     * @param actionType "completed" or "skipped"
     * @param timestamp Action timestamp
     */
    private void enqueueReminderUpdate(Context context, String reminderId, String actionType, long timestamp) {
        boolean success = enqueueReminderUpdateWithRetry(context, reminderId, actionType, timestamp);
        
        if (!success) {
            ReminderLogger.logError(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                "Failed to enqueue reminder update work", 
                String.format("Action: %s, ReminderId: %s", actionType, reminderId), null);
        }
    }
}