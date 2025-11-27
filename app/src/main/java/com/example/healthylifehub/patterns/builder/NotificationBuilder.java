package com.example.healthylifehub.patterns.builder;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.core.app.NotificationCompat;

import com.example.healthylifehub.R;

/**
 * Builder Pattern - Notification Builder
 * Simplified wrapper around NotificationCompat.Builder
 */
public class NotificationBuilder {
    private final Context context;
    private String channelId = "default_channel";
    private String title;
    private String content;
    private String bigText;
    private int smallIcon = R.drawable.ic_notifications;
    private Bitmap largeIcon;
    private PendingIntent contentIntent;
    private PendingIntent deleteIntent;
    private boolean autoCancel = true;
    private int priority = NotificationCompat.PRIORITY_DEFAULT;
    private String category;
    private long[] vibrationPattern;
    private boolean showWhen = true;
    private String groupKey;

    public NotificationBuilder(Context context) {
        this.context = context;
    }

    public NotificationBuilder setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public NotificationBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public NotificationBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public NotificationBuilder setBigText(String bigText) {
        this.bigText = bigText;
        return this;
    }

    public NotificationBuilder setSmallIcon(int smallIcon) {
        this.smallIcon = smallIcon;
        return this;
    }

    public NotificationBuilder setLargeIcon(Bitmap largeIcon) {
        this.largeIcon = largeIcon;
        return this;
    }

    public NotificationBuilder setContentIntent(PendingIntent intent) {
        this.contentIntent = intent;
        return this;
    }

    public NotificationBuilder setDeleteIntent(PendingIntent intent) {
        this.deleteIntent = intent;
        return this;
    }

    public NotificationBuilder setAutoCancel(boolean autoCancel) {
        this.autoCancel = autoCancel;
        return this;
    }

    public NotificationBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public NotificationBuilder setCategory(String category) {
        this.category = category;
        return this;
    }

    public NotificationBuilder setVibrationPattern(long[] pattern) {
        this.vibrationPattern = pattern;
        return this;
    }

    public NotificationBuilder setShowWhen(boolean showWhen) {
        this.showWhen = showWhen;
        return this;
    }

    public NotificationBuilder setGroupKey(String groupKey) {
        this.groupKey = groupKey;
        return this;
    }

    public Notification build() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(autoCancel)
                .setPriority(priority)
                .setShowWhen(showWhen);

        if (bigText != null) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        }
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }
        if (deleteIntent != null) {
            builder.setDeleteIntent(deleteIntent);
        }
        if (category != null) {
            builder.setCategory(category);
        }
        if (vibrationPattern != null) {
            builder.setVibrate(vibrationPattern);
        }
        if (groupKey != null) {
            builder.setGroup(groupKey);
        }

        return builder.build();
    }
}
