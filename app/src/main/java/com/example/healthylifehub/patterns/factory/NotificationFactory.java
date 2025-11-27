package com.example.healthylifehub.patterns.factory;

/**
 * Factory Pattern - Notification Factory
 * Creates appropriate notification objects based on type
 * 
 * Usage:
 * BaseNotification notification = NotificationFactory.create(NotificationType.ALERT, "Huyết áp cao!");
 */
public class NotificationFactory {

    private NotificationFactory() {
        // Private constructor to prevent instantiation
    }

    public static BaseNotification create(NotificationType type, String message) {
        switch (type) {
            case REMINDER:
                return new ReminderNotification(message);
            case ALERT:
                return new AlertNotification(message);
            case ACHIEVEMENT:
                return new AchievementNotification(message);
            case SYNC:
                return new SyncNotification(message);
            case INFO:
            default:
                return new InfoNotification(message);
        }
    }

    /**
     * Create notification with custom title
     */
    public static BaseNotification create(NotificationType type, String title, String message) {
        BaseNotification notification = create(type, message);
        notification.setTitle(title);
        return notification;
    }
}
