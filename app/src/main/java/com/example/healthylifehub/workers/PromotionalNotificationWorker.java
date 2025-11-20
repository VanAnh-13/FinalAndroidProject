package com.example.healthylifehub.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.MainActivity;
import com.example.healthylifehub.R;
import com.example.healthylifehub.utils.NotificationHistoryManager;

import java.util.Random;

/**
 * Worker gửi thông báo quảng cáo định kỳ
 */
public class PromotionalNotificationWorker extends Worker {
    
    private static final String TAG = "PromoNotificationWorker";
    private static final String CHANNEL_ID = "promotional_channel";
    private static final String CHANNEL_NAME = "Thông báo khuyến mãi";
    
    // Các thông báo quảng cáo mẫu
    private static final String[][] PROMOTIONAL_MESSAGES = {
        {"💪 Theo dõi sức khỏe mỗi ngày", "Hãy ghi lại chỉ số sức khỏe của bạn hôm nay để theo dõi tiến trình!"},
        {"🎯 Đạt mục tiêu sức khỏe", "Thiết lập nhắc nhở để không bỏ lỡ bất kỳ liều thuốc nào!"},
        {"📊 Xem báo cáo sức khỏe", "Kiểm tra xu hướng sức khỏe của bạn trong tuần qua"},
        {"⏰ Nhắc nhở thông minh", "Để ứng dụng giúp bạn nhớ uống thuốc đúng giờ mỗi ngày"},
        {"🏥 Quản lý sức khỏe hiệu quả", "Lưu trữ hồ sơ y tế và theo dõi các chỉ số quan trọng"},
        {"💊 Uống thuốc đúng giờ", "Thiết lập lịch nhắc nhở để không bao giờ quên uống thuốc"},
        {"📈 Theo dõi tiến độ", "Xem biểu đồ sức khỏe và phân tích xu hướng của bạn"},
        {"🌟 Chăm sóc sức khỏe tốt hơn", "Sử dụng tính năng xuất báo cáo để chia sẻ với bác sĩ"}
    };
    
    public PromotionalNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "🔔 Starting promotional notification worker");
            
            createNotificationChannel();
            sendPromotionalNotification();
            
            Log.d(TAG, "✅ Promotional notification sent successfully");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending promotional notification", e);
            return Result.failure();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Thông báo về tính năng và mẹo sử dụng ứng dụng");
            
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private void sendPromotionalNotification() {
        // Chọn ngẫu nhiên một thông báo
        Random random = new Random();
        String[] message = PROMOTIONAL_MESSAGES[random.nextInt(PROMOTIONAL_MESSAGES.length)];
        String title = message[0];
        String content = message[1];
        
        // Tạo intent mở app
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            getApplicationContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_active)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        // Send notification
        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        if (manager != null) {
            int notificationId = (int) System.currentTimeMillis();
            manager.notify(notificationId, builder.build());
            
            // Lưu vào lịch sử
            NotificationHistoryManager historyManager = new NotificationHistoryManager(getApplicationContext());
            historyManager.savePromotionalNotification(title, content);
        }
    }
}
