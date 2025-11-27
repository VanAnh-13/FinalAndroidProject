package com.example.healthylifehub.utils.notification;

import android.content.Context;
import android.util.Log;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.model.NotificationHistory;
import com.example.healthylifehub.data.model.Reminder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager để lưu và quản lý lịch sử thông báo
 */
public class NotificationHistoryManager {
    
    private static final String TAG = "NotificationHistoryMgr";
    private final Context context;
    private final AppDatabase database;
    private final ExecutorService executor;
    
    public NotificationHistoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Lưu thông báo reminder vào lịch sử
     */
    public void saveReminderNotification(Reminder reminder, int notificationId) {
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId == null) return;
                
                NotificationHistory history = new NotificationHistory(
                    "notif_" + notificationId + "_" + System.currentTimeMillis(),
                    userId,
                    reminder.getTitle(),
                    reminder.getDescription() != null ? reminder.getDescription() : "Nhắc nhở sức khỏe",
                    NotificationHistory.TYPE_REMINDER,
                    System.currentTimeMillis()
                );
                history.setRelatedId(reminder.getReminderId());
                
                database.notificationHistoryDao().insert(history);
                Log.d(TAG, "✅ Saved reminder notification to history");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving notification history", e);
            }
        });
    }
    
    /**
     * Lưu thông báo quảng cáo vào lịch sử
     */
    public void savePromotionalNotification(String title, String message) {
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId == null) return;
                
                NotificationHistory history = new NotificationHistory(
                    "promo_" + UUID.randomUUID().toString(),
                    userId,
                    title,
                    message,
                    NotificationHistory.TYPE_PROMOTIONAL,
                    System.currentTimeMillis()
                );
                
                database.notificationHistoryDao().insert(history);
                Log.d(TAG, "✅ Saved promotional notification to history");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving promotional notification", e);
            }
        });
    }
    
    /**
     * Lưu thông báo hệ thống vào lịch sử
     */
    public void saveSystemNotification(String title, String message) {
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId == null) return;
                
                NotificationHistory history = new NotificationHistory(
                    "system_" + UUID.randomUUID().toString(),
                    userId,
                    title,
                    message,
                    NotificationHistory.TYPE_SYSTEM,
                    System.currentTimeMillis()
                );
                
                database.notificationHistoryDao().insert(history);
                Log.d(TAG, "✅ Saved system notification to history");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving system notification", e);
            }
        });
    }
    
    /**
     * Cập nhật action đã thực hiện trên notification
     */
    public void updateNotificationAction(String notificationId, String action) {
        executor.execute(() -> {
            try {
                // Tìm notification theo related ID hoặc notification ID
                // Đơn giản hóa: chỉ mark as read
                database.notificationHistoryDao().markAsRead(notificationId);
                Log.d(TAG, "✅ Updated notification action: " + action);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating notification action", e);
            }
        });
    }
    
    /**
     * Xóa thông báo cũ (> 30 ngày)
     */
    public void cleanOldNotifications() {
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                if (userId == null) return;
                
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                database.notificationHistoryDao().deleteOldNotifications(userId, thirtyDaysAgo);
                Log.d(TAG, "✅ Cleaned old notifications");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error cleaning old notifications", e);
            }
        });
    }
    
    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}

