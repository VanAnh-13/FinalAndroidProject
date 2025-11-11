package com.example.healthylifehub.data.model;

import lombok.Data;

@Data
public class NotificationItem {
    public enum NotificationType {
        MEDICATION,
        ACTIVITY,
        APPOINTMENT,
        INSIGHT,
        WARNING,
        GENERAL
    }

    private String id; // Firestore document ID
    private String title;
    private String message;
    private String time;
    private boolean isRead;
    private NotificationType type;
    private boolean isHighPriority;

    public NotificationItem(String title, String message, String time, boolean isRead, NotificationType type) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = isRead;
        this.type = type;
        this.isHighPriority = false;
    }

    public NotificationItem(String title, String message, String time, boolean isRead, NotificationType type, boolean isHighPriority) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = isRead;
        this.type = type;
        this.isHighPriority = isHighPriority;
    }

    // Lombok generates getters/setters
}
