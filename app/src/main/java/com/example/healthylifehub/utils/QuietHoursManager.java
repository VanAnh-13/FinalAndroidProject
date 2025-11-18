package com.example.healthylifehub.utils;

import android.content.Context;
import android.util.Log;

import com.example.healthylifehub.data.model.NotificationSettings;
import com.example.healthylifehub.data.repository.NotificationSettingsRepository;

import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

/**
 * QuietHoursManager - Manages quiet hours functionality for notifications
 * Integrates with NotificationSettings to respect user preferences
 */
public class QuietHoursManager {
    
    private static final String TAG = "QuietHoursManager";
    
    /**
     * Check if current time is within quiet hours
     * @param context Application context
     * @return CompletableFuture<Boolean> true if in quiet hours
     */
    public static CompletableFuture<Boolean> isCurrentlyQuietHours(Context context) {
        NotificationSettingsRepository repository = new NotificationSettingsRepository(context);
        
        return repository.loadNotificationSettings()
            .getValue() != null ? 
            CompletableFuture.completedFuture(isCurrentlyQuietHours(repository.loadNotificationSettings().getValue())) :
            CompletableFuture.completedFuture(false);
    }
    
    /**
     * Check if current time is within quiet hours (synchronous version)
     * @param settings NotificationSettings object
     * @return true if in quiet hours
     */
    public static boolean isCurrentlyQuietHours(NotificationSettings settings) {
        if (settings == null || !settings.isQuietHoursEnabled()) {
            return false;
        }
        
        return !settings.isNotificationAllowedNow();
    }
    
    /**
     * Get next allowed notification time (after quiet hours end)
     * @param settings NotificationSettings object
     * @return Calendar object with next allowed time, or null if no quiet hours
     */
    public static Calendar getNextAllowedNotificationTime(NotificationSettings settings) {
        if (settings == null || !settings.isQuietHoursEnabled()) {
            return null; // No quiet hours, notifications allowed anytime
        }
        
        if (settings.isNotificationAllowedNow()) {
            return null; // Currently allowed, no need to wait
        }
        
        try {
            String[] endParts = settings.getQuietEndTime().split(":");
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);
            
            Calendar nextAllowed = Calendar.getInstance();
            nextAllowed.set(Calendar.HOUR_OF_DAY, endHour);
            nextAllowed.set(Calendar.MINUTE, endMinute);
            nextAllowed.set(Calendar.SECOND, 0);
            nextAllowed.set(Calendar.MILLISECOND, 0);
            
            // If the end time is earlier than current time, it means quiet hours end tomorrow
            Calendar now = Calendar.getInstance();
            if (nextAllowed.before(now)) {
                nextAllowed.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            Log.d(TAG, "Next allowed notification time: " + nextAllowed.getTime());
            return nextAllowed;
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating next allowed notification time", e);
            return null;
        }
    }
    
    /**
     * Calculate delay until quiet hours end
     * @param settings NotificationSettings object
     * @return Delay in milliseconds, or 0 if no delay needed
     */
    public static long getDelayUntilQuietHoursEnd(NotificationSettings settings) {
        Calendar nextAllowed = getNextAllowedNotificationTime(settings);
        if (nextAllowed == null) {
            return 0; // No delay needed
        }
        
        long delay = nextAllowed.getTimeInMillis() - System.currentTimeMillis();
        return Math.max(0, delay);
    }
    
    /**
     * Check if a specific time is within quiet hours
     * @param settings NotificationSettings object
     * @param timeInMillis Time to check
     * @return true if time is within quiet hours
     */
    public static boolean isTimeInQuietHours(NotificationSettings settings, long timeInMillis) {
        if (settings == null || !settings.isQuietHoursEnabled()) {
            return false;
        }
        
        try {
            Calendar checkTime = Calendar.getInstance();
            checkTime.setTimeInMillis(timeInMillis);
            
            int checkHour = checkTime.get(Calendar.HOUR_OF_DAY);
            int checkMinute = checkTime.get(Calendar.MINUTE);
            int checkTimeInMinutes = checkHour * 60 + checkMinute;
            
            String[] startParts = settings.getQuietStartTime().split(":");
            String[] endParts = settings.getQuietEndTime().split(":");
            
            int startTimeInMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endTimeInMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
            
            // Handle overnight quiet hours (e.g., 22:00 - 07:00)
            if (startTimeInMinutes > endTimeInMinutes) {
                return checkTimeInMinutes >= startTimeInMinutes || checkTimeInMinutes < endTimeInMinutes;
            } else {
                return checkTimeInMinutes >= startTimeInMinutes && checkTimeInMinutes < endTimeInMinutes;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if time is in quiet hours", e);
            return false;
        }
    }
    
    /**
     * Get formatted quiet hours display string
     * @param settings NotificationSettings object
     * @return Formatted string like "22:00 - 07:00" or "Tắt"
     */
    public static String getQuietHoursDisplayString(NotificationSettings settings) {
        if (settings == null || !settings.isQuietHoursEnabled()) {
            return "Tắt";
        }
        
        return String.format("%s - %s", 
            settings.getQuietStartTime(), 
            settings.getQuietEndTime());
    }
    
    /**
     * Validate quiet hours time format
     * @param timeString Time string in HH:mm format
     * @return true if valid
     */
    public static boolean isValidTimeFormat(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = timeString.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Get remaining quiet hours duration in minutes
     * @param settings NotificationSettings object
     * @return Minutes remaining in quiet hours, or 0 if not in quiet hours
     */
    public static int getRemainingQuietHoursMinutes(NotificationSettings settings) {
        if (!isCurrentlyQuietHours(settings)) {
            return 0;
        }
        
        Calendar nextAllowed = getNextAllowedNotificationTime(settings);
        if (nextAllowed == null) {
            return 0;
        }
        
        long remainingMillis = nextAllowed.getTimeInMillis() - System.currentTimeMillis();
        return (int) (remainingMillis / (60 * 1000));
    }
    
    /**
     * Log current quiet hours status for debugging
     * @param context Application context
     */
    public static void logQuietHoursStatus(Context context) {
        NotificationSettingsRepository repository = new NotificationSettingsRepository(context);
        // This would need to be async in real implementation
        Log.d(TAG, "📊 Quiet Hours Status:");
        Log.d(TAG, "  - Repository available: " + (repository != null));
        // Additional logging would be added here
    }
}