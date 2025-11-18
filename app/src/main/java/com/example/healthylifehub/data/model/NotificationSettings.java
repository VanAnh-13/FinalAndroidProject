package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.healthylifehub.data.local.converter.DateConverter;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NotificationSettings - Model for user notification preferences
 * Follows project pattern: Room Entity + Firestore sync
 */
@Data
@NoArgsConstructor
@Entity(tableName = "notification_settings")
@TypeConverters(DateConverter.class)
public class NotificationSettings {
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String userId;
    
    // Reminder Settings
    private boolean remindersEnabled;
    private boolean reminderSound;
    private boolean reminderVibration;
    private int reminderVolumeLevel; // 0-100
    private String reminderSoundUri; // Custom sound URI
    private String reminderVibrationPattern; // "default", "gentle", "strong", "custom"
    
    // Health Alert Settings
    private boolean healthAlertsEnabled;
    private boolean criticalAlertsEnabled;
    private boolean anomalyAlertsEnabled;
    private boolean healthAlertSound;
    private boolean healthAlertVibration;
    
    // Suggestion Settings
    private boolean suggestionsEnabled;
    private boolean weeklyReportsEnabled;
    private boolean monthlyReportsEnabled;
    private boolean smartSuggestionsEnabled;
    
    // Quiet Hours
    private boolean quietHoursEnabled;
    private String quietStartTime; // "22:00"
    private String quietEndTime;   // "07:00"
    
    // Advanced Settings
    private boolean bundleNotifications;
    private int maxNotificationsPerDay; // 0 = unlimited
    private boolean showOnLockScreen;
    private boolean showInStatusBar;
    
    // Sync metadata
    private Date lastSyncedAt;
    private boolean needsSync;
    private Date updatedAt;
    
    /**
     * Default settings for new users
     */
    @Ignore
    public static NotificationSettings getDefaultSettings(String userId) {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(userId);
        
        // Reminders: Enabled by default
        settings.setRemindersEnabled(true);
        settings.setReminderSound(true);
        settings.setReminderVibration(true);
        settings.setReminderVolumeLevel(80);
        settings.setReminderSoundUri("default"); // Use system default sound
        settings.setReminderVibrationPattern("default"); // Use default vibration pattern
        
        // Health Alerts: Enabled for critical only
        settings.setHealthAlertsEnabled(true);
        settings.setCriticalAlertsEnabled(true);
        settings.setAnomalyAlertsEnabled(false);
        settings.setHealthAlertSound(true);
        settings.setHealthAlertVibration(true);
        
        // Suggestions: Enabled but less intrusive
        settings.setSuggestionsEnabled(true);
        settings.setWeeklyReportsEnabled(true);
        settings.setMonthlyReportsEnabled(true);
        settings.setSmartSuggestionsEnabled(false); // User can opt-in
        
        // Quiet Hours: 22:00 - 07:00
        settings.setQuietHoursEnabled(true);
        settings.setQuietStartTime("22:00");
        settings.setQuietEndTime("07:00");
        
        // Advanced: Conservative defaults
        settings.setBundleNotifications(true);
        settings.setMaxNotificationsPerDay(20);
        settings.setShowOnLockScreen(true);
        settings.setShowInStatusBar(true);
        
        // Sync metadata
        settings.setLastSyncedAt(new Date());
        settings.setNeedsSync(true);
        settings.setUpdatedAt(new Date());
        
        return settings;
    }
    
    /**
     * Check if notifications are allowed at current time
     */
    @Ignore
    public boolean isNotificationAllowedNow() {
        if (!quietHoursEnabled || quietStartTime == null || quietEndTime == null) {
            return true;
        }
        
        try {
            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(java.util.Calendar.MINUTE);
            int currentTimeInMinutes = currentHour * 60 + currentMinute;
            
            String[] startParts = quietStartTime.split(":");
            String[] endParts = quietEndTime.split(":");
            
            int startTimeInMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endTimeInMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
            
            // Handle overnight quiet hours (e.g., 22:00 - 07:00)
            if (startTimeInMinutes > endTimeInMinutes) {
                return currentTimeInMinutes < startTimeInMinutes && currentTimeInMinutes > endTimeInMinutes;
            } else {
                return currentTimeInMinutes < startTimeInMinutes || currentTimeInMinutes > endTimeInMinutes;
            }
            
        } catch (Exception e) {
            // If parsing fails, allow notifications
            return true;
        }
    }
    
    /**
     * Check if reminder notifications are enabled
     */
    @Ignore
    public boolean areRemindersAllowed() {
        return remindersEnabled && isNotificationAllowedNow();
    }
    
    /**
     * Check if health alert notifications are enabled
     */
    @Ignore
    public boolean areHealthAlertsAllowed() {
        return healthAlertsEnabled && isNotificationAllowedNow();
    }
    
    /**
     * Check if suggestion notifications are enabled
     */
    @Ignore
    public boolean areSuggestionsAllowed() {
        return suggestionsEnabled && isNotificationAllowedNow();
    }
}