package com.example.healthylifehub.patterns.factory;

import androidx.core.app.NotificationCompat;
import com.example.healthylifehub.R;

/**
 * Factory Pattern - Info Notification (Concrete Product)
 */
public class InfoNotification extends BaseNotification {
    
    public InfoNotification(String message) {
        this.message = message;
        configure();
    }

    @Override
    public void configure() {
        this.title = "Thông báo";
        this.channelId = "info_channel";
        this.priority = NotificationCompat.PRIORITY_DEFAULT;
        this.iconResId = R.drawable.ic_info;
    }
}
