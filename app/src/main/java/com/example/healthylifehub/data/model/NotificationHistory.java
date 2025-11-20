package com.example.healthylifehub.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * NotificationHistory model - Lưu lịch sử tất cả thông báo đã gửi
 */
@Entity(tableName = "notification_history")
public class NotificationHistory {
    
    @PrimaryKey
    @NonNull
    private String notificationId;
    
    private String userId;
    private String title;
    private String message;
    private String type; // REMINDER, PROMOTIONAL, SYSTEM
    private String relatedId; // reminder_id nếu là reminder notification
    private long timestamp;
    private boolean isRead;
    private String actionTaken; // COMPLETED, SKIPPED, DISMISSED, null
    
    public NotificationHistory() {
        this.notificationId = "";
    }
    
    public NotificationHistory(@NonNull String notificationId, String userId, String title, 
                              String message, String type, long timestamp) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
    }
    
    // Getters and Setters
    @NonNull
    public String getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(@NonNull String notificationId) {
        this.notificationId = notificationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getRelatedId() {
        return relatedId;
    }
    
    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public String getActionTaken() {
        return actionTaken;
    }
    
    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }
    
    // Constants
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_PROMOTIONAL = "PROMOTIONAL";
    public static final String TYPE_SYSTEM = "SYSTEM";
    
    public static final String ACTION_COMPLETED = "COMPLETED";
    public static final String ACTION_SKIPPED = "SKIPPED";
    public static final String ACTION_DISMISSED = "DISMISSED";
}
