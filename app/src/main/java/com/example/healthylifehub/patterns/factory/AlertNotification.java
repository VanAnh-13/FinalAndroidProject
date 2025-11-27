package com.example.healthylifehub.patterns.factory;

import androidx.core.app.NotificationCompat;
import com.example.healthylifehub.R;

/**
 * Factory Pattern - Alert Notification (Concrete Product)
 * For abnormal health metrics warnings
 */
public class AlertNotification extends BaseNotification {
    
    public AlertNotification(String message) {
        this.message = message;
        configure();
    }

    @Override
    public void configure() {
        this.title = "⚠️ Cảnh báo sức khỏe";
        this.channelId = "alert_channel";
        this.priority = NotificationCompat.PRIORITY_MAX;
        this.iconResId = R.drawable.ic_warning;
    }
}
