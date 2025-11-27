package com.example.healthylifehub.patterns.factory;

import androidx.core.app.NotificationCompat;
import com.example.healthylifehub.R;

/**
 * Factory Pattern - Sync Notification (Concrete Product)
 */
public class SyncNotification extends BaseNotification {
    
    public SyncNotification(String message) {
        this.message = message;
        configure();
    }

    @Override
    public void configure() {
        this.title = "Đồng bộ dữ liệu";
        this.channelId = "sync_channel";
        this.priority = NotificationCompat.PRIORITY_LOW;
        this.iconResId = R.drawable.ic_sync;
    }
}
