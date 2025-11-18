package com.example.healthylifehub.utils;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.workers.DeadlineCleanupWorker;
import com.example.healthylifehub.workers.DeadlineWarningWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages deadline-related functionality for reminders
 * Requirements: 2.4, 2.5, 7.6
 */
public class DeadlineManager {
    
    private static final String TAG = "DeadlineManager";
    
    // Work tags for deadline management
    private static final String WORK_TAG_DEADLINE_CLEANUP = "deadline_cleanup";
    private static final String WORK_TAG_DEADLINE_WARNING = "deadline_warning";
    
    // Work names for unique periodic work
    private static final String WORK_NAME_CLEANUP = "deadline_cleanup_periodic";
    private static final String WORK_NAME_WARNING = "deadline_warning_periodic";
    
    private final Context context;
    private final WorkManager workManager;
    private final ReminderDao reminderDao;
    
    public DeadlineManager(Context context) {
        this.context = context.getApplicationContext();
        this.workManager = WorkManager.getInstance(context);
        AppDatabase database = AppDatabase.getInstance(context);
        this.reminderDao = database.reminderDao();
    }
    
    /**
     * Initialize deadline management system
     * Sets up periodic workers for cleanup and warnings
     */
    public void initialize() {
        Log.d(TAG, "🚀 Initializing deadline management system");
        
        try {
            setupPeriodicCleanup();
            setupPeriodicWarnings();
            
            // Run immediate cleanup and warning check
            runImmediateCleanup();
            runImmediateWarningCheck();
            
            Log.d(TAG, "✅ Deadline management system initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize deadline management system", e);
        }
    }
    
    /**
     * Set up periodic cleanup worker to run daily
     * Requirements: 2.4
     */
    private void setupPeriodicCleanup() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();
        
        PeriodicWorkRequest cleanupWork = new PeriodicWorkRequest.Builder(
                DeadlineCleanupWorker.class, 
                24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(WORK_TAG_DEADLINE_CLEANUP)
                .build();
        
        workManager.enqueueUniquePeriodicWork(
                WORK_NAME_CLEANUP,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWork
        );
        
        Log.d(TAG, "📅 Scheduled periodic deadline cleanup (every 24 hours)");
    }
    
    /**
     * Set up periodic warning worker to run every 6 hours
     * Requirements: 2.5
     */
    private void setupPeriodicWarnings() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();
        
        PeriodicWorkRequest warningWork = new PeriodicWorkRequest.Builder(
                DeadlineWarningWorker.class, 
                6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(WORK_TAG_DEADLINE_WARNING)
                .build();
        
        workManager.enqueueUniquePeriodicWork(
                WORK_NAME_WARNING,
                ExistingPeriodicWorkPolicy.KEEP,
                warningWork
        );
        
        Log.d(TAG, "⚠️ Scheduled periodic deadline warnings (every 6 hours)");
    }
    
    /**
     * Run immediate cleanup of expired reminders
     * Requirements: 2.4
     */
    public void runImmediateCleanup() {
        OneTimeWorkRequest cleanupWork = new OneTimeWorkRequest.Builder(DeadlineCleanupWorker.class)
                .addTag(WORK_TAG_DEADLINE_CLEANUP)
                .build();
        
        workManager.enqueue(cleanupWork);
        Log.d(TAG, "🧹 Scheduled immediate deadline cleanup");
    }
    
    /**
     * Run immediate warning check for approaching deadlines
     * Requirements: 2.5
     */
    public void runImmediateWarningCheck() {
        OneTimeWorkRequest warningWork = new OneTimeWorkRequest.Builder(DeadlineWarningWorker.class)
                .addTag(WORK_TAG_DEADLINE_WARNING)
                .build();
        
        workManager.enqueue(warningWork);
        Log.d(TAG, "⚠️ Scheduled immediate deadline warning check");
    }
    
    /**
     * Deactivate expired reminders and calculate final progress
     * Requirements: 2.4, 7.6
     * 
     * @param userId The user ID to process reminders for
     * @return Number of reminders deactivated
     */
    public int deactivateExpiredReminders(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Invalid user ID for deadline cleanup");
            return 0;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            List<Reminder> expiredReminders = reminderDao.getExpiredReminders(userId, currentTime);
            
            if (expiredReminders.isEmpty()) {
                Log.d(TAG, "📭 No expired reminders found for user: " + userId);
                return 0;
            }
            
            int deactivatedCount = 0;
            
            for (Reminder reminder : expiredReminders) {
                if (reminder.isActive()) {
                    // Calculate final progress
                    float finalProgress = reminder.getProgressPercentage();
                    
                    // Deactivate the reminder
                    reminder.setActive(false);
                    reminder.setUpdatedAt(currentTime);
                    
                    reminderDao.update(reminder);
                    deactivatedCount++;
                    
                    Log.d(TAG, "🔒 Deactivated expired reminder: " + reminder.getTitle() + 
                              " (Final progress: " + String.format("%.1f", finalProgress) + "%)");
                }
            }
            
            Log.d(TAG, "✅ Deactivated " + deactivatedCount + " expired reminders for user: " + userId);
            return deactivatedCount;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to deactivate expired reminders for user: " + userId, e);
            return 0;
        }
    }
    
    /**
     * Get reminders approaching deadline (within 3 days)
     * Requirements: 2.5
     * 
     * @param userId The user ID
     * @return List of reminders approaching deadline
     */
    public List<Reminder> getApproachingDeadlineReminders(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            long threeDaysFromNow = currentTime + (3 * 24 * 60 * 60 * 1000L);
            
            List<Reminder> approachingReminders = reminderDao.getRemindersExpiringSoon(
                    userId, currentTime, threeDaysFromNow);
            
            Log.d(TAG, "📅 Found " + approachingReminders.size() + 
                      " reminders approaching deadline for user: " + userId);
            
            return approachingReminders;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to get approaching deadline reminders for user: " + userId, e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Extend deadline for a reminder
     * Requirements: 2.4 (deadline extension functionality)
     * 
     * @param reminderId The reminder ID
     * @param newDeadline The new deadline timestamp
     * @return true if deadline was extended successfully
     */
    public boolean extendDeadline(String reminderId, long newDeadline) {
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Invalid reminder ID for deadline extension");
            return false;
        }
        
        if (newDeadline <= System.currentTimeMillis()) {
            Log.w(TAG, "⚠️ New deadline must be in the future");
            return false;
        }
        
        try {
            Reminder reminder = reminderDao.getReminderById(reminderId);
            if (reminder == null) {
                Log.w(TAG, "⚠️ Reminder not found: " + reminderId);
                return false;
            }
            
            long oldDeadline = reminder.getDeadline() != null ? reminder.getDeadline() : 0;
            
            // Update deadline and recalculate total expected
            reminder.setDeadline(newDeadline);
            reminder.setUpdatedAt(System.currentTimeMillis());
            
            // Recalculate total expected based on new deadline
            ProgressCalculator progressCalculator = new ProgressCalculator();
            int newTotalExpected = progressCalculator.calculateTotalExpected(reminder);
            reminder.setTotalExpected(newTotalExpected);
            
            reminderDao.update(reminder);
            
            Log.d(TAG, "✅ Extended deadline for reminder: " + reminder.getTitle() + 
                      " from " + new java.util.Date(oldDeadline) + 
                      " to " + new java.util.Date(newDeadline) +
                      " (New total expected: " + newTotalExpected + ")");
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to extend deadline for reminder: " + reminderId, e);
            return false;
        }
    }
    
    /**
     * Check if a reminder needs deadline warning
     * Requirements: 2.5
     * 
     * @param reminder The reminder to check
     * @return true if warning should be shown
     */
    public boolean needsDeadlineWarning(Reminder reminder) {
        if (reminder == null || !reminder.isActive() || !reminder.hasDeadline()) {
            return false;
        }
        
        if (reminder.isExpired() || reminder.isCompleted()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long threeDaysFromNow = currentTime + (3 * 24 * 60 * 60 * 1000L);
        
        return reminder.getDeadline() <= threeDaysFromNow;
    }
    
    /**
     * Get deadline status text for UI display
     * Requirements: 7.6
     * 
     * @param reminder The reminder
     * @return Status text describing deadline state
     */
    public String getDeadlineStatusText(Reminder reminder) {
        if (reminder == null || !reminder.hasDeadline()) {
            return "Không giới hạn thời gian";
        }
        
        long currentTime = System.currentTimeMillis();
        long deadline = reminder.getDeadline();
        long timeRemaining = deadline - currentTime;
        
        if (timeRemaining <= 0) {
            // Expired
            float finalProgress = reminder.getProgressPercentage();
            return String.format("Đã hết hạn (%.1f%% hoàn thành)", finalProgress);
        } else if (timeRemaining <= 24 * 60 * 60 * 1000L) {
            // Less than 1 day
            long hoursRemaining = timeRemaining / (60 * 60 * 1000L);
            return String.format("Còn %d giờ", hoursRemaining);
        } else if (timeRemaining <= 3 * 24 * 60 * 60 * 1000L) {
            // Less than 3 days
            long daysRemaining = timeRemaining / (24 * 60 * 60 * 1000L);
            return String.format("Còn %d ngày", daysRemaining);
        } else {
            // More than 3 days
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return "Hết hạn: " + sdf.format(new java.util.Date(deadline));
        }
    }
    
    /**
     * Get deadline warning color for UI
     * Requirements: 7.6
     * 
     * @param reminder The reminder
     * @return Color resource ID for deadline status
     */
    public int getDeadlineWarningColor(Reminder reminder) {
        if (reminder == null || !reminder.hasDeadline()) {
            return android.R.color.darker_gray;
        }
        
        long currentTime = System.currentTimeMillis();
        long deadline = reminder.getDeadline();
        long timeRemaining = deadline - currentTime;
        
        if (timeRemaining <= 0) {
            // Expired - red
            return android.R.color.holo_red_dark;
        } else if (timeRemaining <= 24 * 60 * 60 * 1000L) {
            // Less than 1 day - red
            return android.R.color.holo_red_light;
        } else if (timeRemaining <= 3 * 24 * 60 * 60 * 1000L) {
            // Less than 3 days - orange/yellow
            return android.R.color.holo_orange_light;
        } else {
            // More than 3 days - normal
            return android.R.color.darker_gray;
        }
    }
    
    /**
     * Cancel all deadline management work
     * Used for cleanup when app is uninstalled or user logs out
     */
    public void cancelAllWork() {
        try {
            workManager.cancelAllWorkByTag(WORK_TAG_DEADLINE_CLEANUP);
            workManager.cancelAllWorkByTag(WORK_TAG_DEADLINE_WARNING);
            workManager.cancelUniqueWork(WORK_NAME_CLEANUP);
            workManager.cancelUniqueWork(WORK_NAME_WARNING);
            
            Log.d(TAG, "🚫 Cancelled all deadline management work");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to cancel deadline management work", e);
        }
    }
}