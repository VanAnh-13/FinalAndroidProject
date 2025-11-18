package com.example.healthylifehub.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.healthylifehub.workers.DeadlineExtensionWorker;

/**
 * BroadcastReceiver for handling deadline extension actions from notifications
 * Requirements: 2.4 (deadline extension functionality)
 */
public class DeadlineExtensionReceiver extends BroadcastReceiver {
    
    private static final String TAG = "DeadlineExtensionReceiver";
    
    // Actions
    public static final String ACTION_EXTEND_DEADLINE = "EXTEND_DEADLINE";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String reminderId = intent.getStringExtra("reminder_id");
        
        Log.d(TAG, "📨 Received deadline extension action: " + action + " for reminder: " + reminderId);
        
        if (reminderId == null || reminderId.trim().isEmpty()) {
            Log.e(TAG, "❌ Invalid reminder ID in deadline extension action");
            return;
        }
        
        if (ACTION_EXTEND_DEADLINE.equals(action)) {
            handleExtendDeadline(context, reminderId);
        } else {
            Log.w(TAG, "⚠️ Unknown action: " + action);
        }
    }
    
    /**
     * Handle deadline extension request
     * 
     * @param context The context
     * @param reminderId The reminder ID to extend deadline for
     */
    private void handleExtendDeadline(Context context, String reminderId) {
        try {
            Log.d(TAG, "⏰ Processing deadline extension for reminder: " + reminderId);
            
            // Default extension: 7 days
            long extensionDays = 7;
            long extensionMillis = extensionDays * 24 * 60 * 60 * 1000L;
            long newDeadline = System.currentTimeMillis() + extensionMillis;
            
            // Create work data
            Data inputData = new Data.Builder()
                    .putString("reminder_id", reminderId)
                    .putLong("new_deadline", newDeadline)
                    .putLong("extension_days", extensionDays)
                    .build();
            
            // Enqueue deadline extension work
            OneTimeWorkRequest extensionWork = new OneTimeWorkRequest.Builder(DeadlineExtensionWorker.class)
                    .setInputData(inputData)
                    .addTag("deadline_extension")
                    .build();
            
            WorkManager.getInstance(context).enqueue(extensionWork);
            
            Log.d(TAG, "✅ Enqueued deadline extension work for reminder: " + reminderId + 
                      " (Extension: " + extensionDays + " days)");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to handle deadline extension", e);
        }
    }
}