package com.example.healthylifehub.patterns.factory;

import androidx.core.app.NotificationCompat;
import com.example.healthylifehub.R;

/**
 * Factory Pattern - Achievement Notification (Concrete Product)
 */
public class AchievementNotification extends BaseNotification {
    
    public AchievementNotification(String message) {
        this.message = message;
        configure();
    }

    @Override
    public void configure() {
        this.title = "🎉 Thành tựu";
        this.channelId = "achievement_channel";
        this.priority = NotificationCompat.PRIORITY_DEFAULT;
        this.iconResId = R.drawable.ic_star;
    }
}
