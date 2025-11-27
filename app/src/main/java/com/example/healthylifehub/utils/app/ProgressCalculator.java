package com.example.healthylifehub.utils.app;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for calculating reminder progress and related metrics
 * Supports different reminder frequencies and deadline-based calculations
 */
public class ProgressCalculator {
    
    // Constants for time calculations
    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
    private static final long MILLIS_PER_WEEK = TimeUnit.DAYS.toMillis(7);
    private static final long MILLIS_PER_MONTH = TimeUnit.DAYS.toMillis(30); // Approximate
    
    // Progress thresholds for color coding
    private static final float LOW_PROGRESS_THRESHOLD = 50f;
    private static final float MEDIUM_PROGRESS_THRESHOLD = 80f;
    
    /**
     * Calculate total expected reminder count based on frequency and deadline
     * 
     * @param reminder The reminder object containing frequency, creation time, and deadline
     * @return Total expected reminder count, or 0 if no deadline is set
     */
    public static int calculateTotalExpected(Reminder reminder) {
        if (reminder == null || reminder.getDeadline() == null) {
            return 0; // Unlimited reminders - no deadline set
        }
        
        long startTime = reminder.getCreatedAt();
        long endTime = reminder.getDeadline();
        String frequency = reminder.getFrequency();
        
        // Validate time range
        if (endTime <= startTime) {
            return 0;
        }
        
        return calculateTotalExpectedByFrequency(startTime, endTime, frequency);
    }
    
    /**
     * Calculate total expected count based on frequency and time range
     * 
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds  
     * @param frequency Reminder frequency ("once", "daily", "weekly", "monthly")
     * @return Total expected reminder count
     */
    public static int calculateTotalExpectedByFrequency(long startTime, long endTime, String frequency) {
        if (endTime <= startTime || frequency == null) {
            return 0;
        }
        
        long timeDiff = endTime - startTime;
        
        switch (frequency.toLowerCase()) {
            case "once":
                return 1;
                
            case "daily":
                // Calculate number of days between start and end (inclusive)
                return (int) (timeDiff / MILLIS_PER_DAY) + 1;
                
            case "weekly":
                // Calculate number of weeks between start and end (inclusive)
                return (int) (timeDiff / MILLIS_PER_WEEK) + 1;
                
            case "monthly":
                // Use Calendar for more accurate monthly calculations
                return calculateMonthlyCount(startTime, endTime);
                
            default:
                // Unknown frequency, treat as once
                return 1;
        }
    }
    
    /**
     * Calculate monthly reminder count using Calendar for accuracy
     * 
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Number of months between dates (inclusive)
     */
    private static int calculateMonthlyCount(long startTime, long endTime) {
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        
        startCal.setTimeInMillis(startTime);
        endCal.setTimeInMillis(endTime);
        
        int startYear = startCal.get(Calendar.YEAR);
        int startMonth = startCal.get(Calendar.MONTH);
        int endYear = endCal.get(Calendar.YEAR);
        int endMonth = endCal.get(Calendar.MONTH);
        
        // Calculate total months difference
        int monthsDiff = (endYear - startYear) * 12 + (endMonth - startMonth);
        
        // Add 1 to make it inclusive (start month counts)
        return monthsDiff + 1;
    }
    
    /**
     * Calculate progress percentage
     * 
     * @param completedCount Number of completed reminders
     * @param totalExpected Total expected reminder count
     * @return Progress percentage (0-100), capped at 100%
     */
    public static float calculateProgress(int completedCount, int totalExpected) {
        if (totalExpected <= 0) {
            return 0f;
        }
        
        // Ensure completed count is not negative
        int validCompletedCount = Math.max(0, completedCount);
        
        // Calculate percentage and cap at 100%
        float progress = (validCompletedCount * 100f) / totalExpected;
        return Math.min(100f, progress);
    }
    
    /**
     * Get progress bar color resource ID based on progress percentage
     * 
     * @param progress Progress percentage (0-100)
     * @return Color resource ID for progress bar
     */
    public static int getProgressColor(float progress) {
        if (progress < LOW_PROGRESS_THRESHOLD) {
            return R.color.error_red; // Red for low progress (< 50%)
        } else if (progress < MEDIUM_PROGRESS_THRESHOLD) {
            return R.color.warning_orange; // Orange for medium progress (50-79%)
        } else {
            return R.color.success_green; // Green for high progress (80%+)
        }
    }
    
    /**
     * Get progress status text based on completion state
     * 
     * @param reminder The reminder object
     * @return Formatted progress text
     */
    public static String getProgressText(Reminder reminder) {
        if (reminder == null) {
            return "";
        }
        
        if (!reminder.hasDeadline()) {
            return "Không giới hạn thời gian";
        }
        
        int completed = reminder.getCompletedCount();
        int total = reminder.getTotalExpected();
        float progress = reminder.getProgressPercentage();
        
        if (reminder.isCompleted()) {
            return "Hoàn thành!";
        } else if (reminder.isExpired()) {
            return String.format("%d/%d hoàn thành (%.0f%%) - Hết hạn", completed, total, progress);
        } else {
            return String.format("%d/%d hoàn thành (%.0f%%)", completed, total, progress);
        }
    }
    
    /**
     * Check if reminder is approaching deadline (within 3 days)
     * 
     * @param reminder The reminder object
     * @return true if deadline is within 3 days, false otherwise
     */
    public static boolean isApproachingDeadline(Reminder reminder) {
        if (reminder == null || !reminder.hasDeadline()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long deadline = reminder.getDeadline();
        long threeDaysInMillis = 3 * MILLIS_PER_DAY;
        
        return (deadline - currentTime) <= threeDaysInMillis && deadline > currentTime;
    }
    
    /**
     * Get days remaining until deadline
     * 
     * @param reminder The reminder object
     * @return Days remaining, or -1 if no deadline or already expired
     */
    public static int getDaysUntilDeadline(Reminder reminder) {
        if (reminder == null || !reminder.hasDeadline()) {
            return -1;
        }
        
        long currentTime = System.currentTimeMillis();
        long deadline = reminder.getDeadline();
        
        if (deadline <= currentTime) {
            return 0; // Expired
        }
        
        long timeDiff = deadline - currentTime;
        return (int) (timeDiff / MILLIS_PER_DAY);
    }
}
