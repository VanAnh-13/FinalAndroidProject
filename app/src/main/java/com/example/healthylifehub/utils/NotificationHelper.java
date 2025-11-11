package com.example.healthylifehub.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.healthylifehub.R;
import com.example.healthylifehub.ui.reminders.detail.ReminderDetailActivity;

public class NotificationHelper {
    
    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Nhắc nhở sức khỏe";
    private static final String CHANNEL_DESC = "Thông báo nhắc nhở uống thuốc và kiểm tra sức khỏe";
    
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.enableLights(true);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    public static void showReminderNotification(
            Context context,
            String reminderId,
            String title,
            String description,
            int notificationId
    ) {
        createNotificationChannel(context);
        
        Intent intent = new Intent(context, ReminderDetailActivity.class);
        intent.putExtra("reminderId", reminderId);
        intent.putExtra("reminderTitle", title);
        intent.putExtra("reminderDescription", description);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle(title)
            .setContentText(description != null && !description.isEmpty() ? description : "Đã đến giờ nhắc nhở!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(new long[]{0, 500, 200, 500});
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
    
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }
}
