package com.example.healthylifehub.workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.local.dao.ReminderHistoryDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.ReminderHistory;
import com.example.healthylifehub.utils.error.ErrorHandler;
import com.example.healthylifehub.utils.reminder.ReminderLogger;
import com.example.healthylifehub.utils.retry.RetryManager;

import java.util.UUID;

/**
 * Background worker for updating reminder history and progress
 * Handles database updates for reminder actions from notifications
 * Requirements: 1.5, 8.5, 5.4
 */
public class ReminderUpdateWorker extends Worker {
    
    private static final String TAG = "ReminderUpdateWorker";
    
    // Broadcast action for UI updates
    public static final String ACTION_REMINDER_UPDATED = "com.example.healthylifehub.REMINDER_UPDATED";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_ACTION_TYPE = "action_type";
    
    public ReminderUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        long startTime = System.currentTimeMillis();
        int currentAttempt = getRunAttemptCount() + 1;
        
        // Initialize logger
        ReminderLogger.initialize(getApplicationContext());
        
        // Log work start
        ReminderLogger.logBackgroundWork("ReminderUpdate", getId().toString(), "STARTED", 
            "Attempt: " + currentAttempt);
        
        // Get input data
        String reminderId = getInputData().getString("reminder_id");
        String actionType = getInputData().getString("action_type");
        long timestamp = getInputData().getLong("timestamp", System.currentTimeMillis());
        long scheduledTime = getInputData().getLong("scheduled_time", timestamp);
        String operationId = getInputData().getString("operation_id");
        if (operationId == null) {
            operationId = actionType + "_" + reminderId;
        }
        
        // Comprehensive input validation with error handling
        Result validationResult = validateInputData(reminderId, actionType, operationId, currentAttempt);
        if (validationResult != null) {
            return validationResult;
        }
        
        ReminderLogger.logInfo(ReminderLogger.LogCategory.BACKGROUND_WORK, 
            "Processing reminder action", 
            String.format("Action: %s, ReminderId: %s, Attempt: %d", actionType, reminderId, currentAttempt));
        
        try {
            // Get database instance with comprehensive error handling
            AppDatabase database = getDatabaseWithErrorHandling();
            if (database == null) {
                return handleDatabaseError("Failed to get database instance", null, 
                    reminderId, operationId, currentAttempt);
            }
            
            ReminderHistoryDao historyDao = database.reminderHistoryDao();
            ReminderDao reminderDao = database.reminderDao();
            
            // Verify reminder exists before creating history
            Reminder existingReminder = reminderDao.getReminderById(reminderId);
            if (existingReminder == null) {
                Log.w(TAG, "⚠️ Reminder not found: " + reminderId + " - creating history anyway for audit trail");
            }
            
            // Create and validate history record
            ReminderHistory history = createHistoryRecord(reminderId, actionType, timestamp, scheduledTime);
            
            // Insert history record with transaction safety
            historyDao.insert(history);
            Log.d(TAG, "✅ Created history record: " + history.getId() + " for action: " + actionType);
            
            // Update reminder progress if action is "completed"
            if ("completed".equals(actionType)) {
                updateReminderProgress(reminderDao, historyDao, reminderId);
            }
            
            // Send local broadcast to update UI
            sendUpdateBroadcast(reminderId, actionType);
            
            long executionTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "✅ ReminderUpdateWorker completed successfully in " + executionTime + "ms");
            
            return Result.success();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "❌ ReminderUpdateWorker failed after " + executionTime + "ms", e);
            
            // Enhanced retry logic with exponential backoff
            return shouldRetry() ? Result.retry() : Result.failure();
        }
    }
    
    /**
     * Create a validated history record
     * 
     * @param reminderId The reminder ID
     * @param actionType The action type
     * @param timestamp Action timestamp
     * @param scheduledTime Scheduled timestamp
     * @return Created ReminderHistory object
     */
    private ReminderHistory createHistoryRecord(String reminderId, String actionType, 
                                              long timestamp, long scheduledTime) {
        ReminderHistory history = new ReminderHistory();
        history.setId(UUID.randomUUID().toString());
        history.setReminderId(reminderId);
        history.setActionType(actionType);
        history.setTimestamp(timestamp);
        history.setScheduledTime(scheduledTime);
        
        // Validate the created record
        if (history.getId() == null || history.getReminderId() == null || history.getActionType() == null) {
            throw new IllegalStateException("Invalid history record created");
        }
        
        return history;
    }
    
    /**
     * Determine if work should be retried based on attempt count and error type
     * Requirements: 8.5
     * 
     * @return true if work should be retried
     */
    private boolean shouldRetry() {
        int maxRetries = 3;
        int currentAttempt = getRunAttemptCount();
        
        if (currentAttempt < maxRetries) {
            Log.d(TAG, "🔄 Scheduling retry... Attempt: " + (currentAttempt + 1) + "/" + maxRetries);
            return true;
        } else {
            Log.e(TAG, "🛑 Max retry attempts (" + maxRetries + ") reached, marking as failure");
            return false;
        }
    }
    
    /**
     * Update reminder progress by recalculating completed count
     * Requirements: 5.4
     * 
     * @param reminderDao DAO for reminder operations
     * @param historyDao DAO for history operations
     * @param reminderId The reminder ID to update
     */
    private void updateReminderProgress(ReminderDao reminderDao, ReminderHistoryDao historyDao, String reminderId) {
        try {
            // Get current reminder
            Reminder reminder = reminderDao.getReminderById(reminderId);
            if (reminder == null) {
                Log.w(TAG, "⚠️ Reminder not found: " + reminderId);
                return;
            }
            
            // Get updated completed count from history
            int completedCount = historyDao.getCompletedCountByReminderId(reminderId);
            
            // Update reminder if count changed
            if (reminder.getCompletedCount() != completedCount) {
                reminder.setCompletedCount(completedCount);
                reminder.setUpdatedAt(System.currentTimeMillis());
                
                reminderDao.update(reminder);
                Log.d(TAG, "✅ Updated reminder progress: " + reminderId + " -> " + completedCount + "/" + reminder.getTotalExpected());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to update reminder progress", e);
            throw e; // Re-throw to trigger retry
        }
    }
    
    /**
     * Send local broadcast for UI updates
     * Requirements: 8.4
     * 
     * @param reminderId The reminder ID
     * @param actionType The action type
     */
    private void sendUpdateBroadcast(String reminderId, String actionType) {
        try {
            Intent updateIntent = new Intent(ACTION_REMINDER_UPDATED);
            updateIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
            updateIntent.putExtra(EXTRA_ACTION_TYPE, actionType);
            
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(updateIntent);
            
            ReminderLogger.logInfo(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                "Sent update broadcast", "ReminderId: " + reminderId + ", Action: " + actionType);
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.BACKGROUND_WORK, 
                "Failed to send update broadcast", "ReminderId: " + reminderId, e);
            // Don't throw - this is not critical for the main operation
        }
    }
    
    /**
     * Validate input data with comprehensive error handling
     * Requirements: 8.5
     * 
     * @param reminderId The reminder ID
     * @param actionType The action type
     * @param operationId The operation ID
     * @param currentAttempt Current attempt number
     * @return Result if validation fails, null if validation passes
     */
    private Result validateInputData(String reminderId, String actionType, String operationId, int currentAttempt) {
        // Validate reminder ID
        if (reminderId == null || reminderId.trim().isEmpty()) {
            ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                "Invalid reminder ID in worker", "Attempt: " + currentAttempt, null);
            
            ErrorHandler.handleBackgroundError(getApplicationContext(), 
                ErrorHandler.ERROR_REMINDER_NOT_FOUND, null, 
                operationId, currentAttempt, 3);
            
            return Result.failure();
        }
        
        // Validate action type
        if (actionType == null || (!actionType.equals("completed") && !actionType.equals("skipped"))) {
            ReminderLogger.logError(ReminderLogger.LogCategory.VALIDATION, 
                "Invalid action type in worker", 
                "ActionType: " + actionType + ", Attempt: " + currentAttempt, null);
            
            ErrorHandler.handleBackgroundError(getApplicationContext(), 
                ErrorHandler.ERROR_BACKGROUND_WORK_FAILED, null, 
                operationId, currentAttempt, 3);
            
            return Result.failure();
        }
        
        // Log successful validation
        ReminderLogger.logValidation("worker_input", true, null);
        return null; // Validation passed
    }
    
    /**
     * Get database instance with comprehensive error handling
     * Requirements: 8.5
     * 
     * @return AppDatabase instance or null if failed
     */
    private AppDatabase getDatabaseWithErrorHandling() {
        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            
            if (database == null) {
                ReminderLogger.logError(ReminderLogger.LogCategory.DATABASE, 
                    "Database instance is null", null, null);
            } else {
                ReminderLogger.logDebug(ReminderLogger.LogCategory.DATABASE, 
                    "Successfully obtained database instance", null);
            }
            
            return database;
            
        } catch (Exception e) {
            ReminderLogger.logError(ReminderLogger.LogCategory.DATABASE, 
                "Exception getting database instance", null, e);
            
            ErrorHandler.handleDatabaseError(getApplicationContext(), 
                ErrorHandler.ERROR_DATABASE_CONNECTION_FAILED, e, 
                "getInstance", "ReminderUpdateWorker");
            
            return null;
        }
    }
    
    /**
     * Handle database errors with comprehensive logging and retry logic
     * Requirements: 8.5
     * 
     * @param message Error message
     * @param exception Exception that occurred
     * @param reminderId Reminder ID being processed
     * @param operationId Operation ID
     * @param currentAttempt Current attempt number
     * @return Result indicating retry or failure
     */
    private Result handleDatabaseError(String message, Throwable exception, 
                                     String reminderId, String operationId, int currentAttempt) {
        
        ReminderLogger.logError(ReminderLogger.LogCategory.DATABASE, 
            message, "ReminderId: " + reminderId + ", Attempt: " + currentAttempt, exception);
        
        ErrorHandler.handleDatabaseError(getApplicationContext(), 
            ErrorHandler.ERROR_DATABASE_CONNECTION_FAILED, exception, 
            "worker_operation", reminderId);
        
        // Create retry context to determine if we should retry
        RetryManager.RetryContext retryContext = RetryManager.createRetryContext(
            operationId, currentAttempt - 1, exception);
        
        if (retryContext.canRetry() && RetryManager.isRetryableException(exception)) {
            ReminderLogger.logRetryAttempt(operationId, currentAttempt, 3, false, 
                "Database error - scheduling retry");
            
            return Result.retry();
        } else {
            ReminderLogger.logError(ReminderLogger.LogCategory.RETRY, 
                "Max retries exceeded or non-retryable error", 
                "OperationId: " + operationId, exception);
            
            ErrorHandler.handleBackgroundError(getApplicationContext(), 
                ErrorHandler.ERROR_RETRY_LIMIT_EXCEEDED, exception, 
                operationId, currentAttempt, 3);
            
            return Result.failure();
        }
    }
}