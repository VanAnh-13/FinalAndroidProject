package com.example.healthylifehub.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.healthylifehub.utils.NotificationHelper;

public class ReminderReceiver extends BroadcastReceiver {
    
    private static final String TAG = "ReminderReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");
        
        String reminderId = intent.getStringExtra("reminderId");
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        int notificationId = intent.getIntExtra("notificationId", 0);
        
        if (reminderId == null || title == null) {
            Log.e(TAG, "Missing reminder data in intent");
            return;
        }
        
        Log.d(TAG, "Showing notification for: " + title);
        NotificationHelper.showReminderNotification(
            context,
            reminderId,
            title,
            description,
            notificationId
        );
    }
}
