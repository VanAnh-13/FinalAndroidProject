package com.example.healthylifehub.patterns.factory;

import androidx.core.app.NotificationCompat;
import com.example.healthylifehub.R;

/**
 * Factory Pattern - Reminder Notification (Concrete Product)
 */
public class ReminderNotification extends BaseNotification {
    
    public ReminderNotification(String message) {
        this.message = message;
        configure();
    }

    @Override
    public void configure() {
        this.title = "Nhắc nhở";
        this.channelId = "reminder_channel";
        this.priority = NotificationCompat.PRIORITY_HIGH;
        this.iconResId = R.drawable.ic_reminder;
    }
}
