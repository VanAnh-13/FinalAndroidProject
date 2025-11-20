# Hướng dẫn Sử dụng Hệ thống Quản lý Thông báo

## Cho Developer

### 1. Xem Lịch sử Thông báo

Thêm button hoặc menu item để mở NotificationHistoryActivity:

```java
// Trong MainActivity hoặc ProfileFragment
findViewById(R.id.btnNotificationHistory).setOnClickListener(v -> {
    Intent intent = new Intent(this, NotificationHistoryActivity.class);
    startActivity(intent);
});
```

### 2. Gửi Thông báo Custom

```java
// Khởi tạo manager
NotificationHistoryManager manager = new NotificationHistoryManager(context);

// Gửi system notification
manager.saveSystemNotification(
    "Cập nhật mới", 
    "Phiên bản 2.0 đã có sẵn!"
);

// Gửi promotional notification
manager.savePromotionalNotification(
    "Tính năng mới", 
    "Thử tính năng xuất báo cáo PDF ngay!"
);
```

### 3. Test Promotional Notifications

Để test ngay không cần đợi 3 ngày:

```java
// Trigger one-time promotional notification
OneTimeWorkRequest testWork = new OneTimeWorkRequest.Builder(
    PromotionalNotificationWorker.class
).build();

WorkManager.getInstance(context).enqueue(testWork);
```

### 4. Tắt Promotional Notifications

```java
// Trong settings hoặc khi user opt-out
WorkManagerConfig.cancelPromotionalNotifications(context);
```

### 5. Cleanup Old Notifications

```java
// Tự động cleanup notifications >30 ngày
NotificationHistoryManager manager = new NotificationHistoryManager(context);
manager.cleanOldNotifications();
```

## Cho User

### Xem Thông báo
1. Mở app HealthyLife Hub
2. Vào Profile → Lịch sử thông báo
3. Chọn tab "Tất cả" hoặc "Chưa đọc"

### Đánh dấu Đã đọc
- Click vào bất kỳ thông báo nào để đánh dấu đã đọc
- Chấm xanh sẽ biến mất khi đã đọc

### Loại Thông báo
- 🔔 **Reminder**: Nhắc nhở uống thuốc, đo chỉ số
- ⭐ **Promotional**: Giới thiệu tính năng, tips sử dụng
- ℹ️ **System**: Thông báo hệ thống, cập nhật

## Tùy chỉnh

### Thay đổi Chu kỳ Promotional Notifications

Trong `WorkManagerConfig.java`:

```java
// Thay đổi từ 3 ngày sang 7 ngày
androidx.work.PeriodicWorkRequest workRequest = 
    new androidx.work.PeriodicWorkRequest.Builder(
        PromotionalNotificationWorker.class,
        7, TimeUnit.DAYS,  // Thay đổi ở đây
        1, TimeUnit.HOURS
    )
    .setConstraints(constraints)
    .addTag(TAG_PROMOTIONAL)
    .build();
```

### Thêm Promotional Messages

Trong `PromotionalNotificationWorker.java`:

```java
private static final String[][] PROMOTIONAL_MESSAGES = {
    // Thêm messages mới vào đây
    {"🎉 Title mới", "Content mới"},
    {"💡 Mẹo hay", "Nội dung mẹo"},
    // ...
};
```

### Thay đổi Cleanup Period

Trong `NotificationHistoryManager.java`:

```java
// Thay đổi từ 30 ngày sang 60 ngày
long sixtyDaysAgo = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000);
database.notificationHistoryDao().deleteOldNotifications(userId, sixtyDaysAgo);
```

## Troubleshooting

### Không nhận được Promotional Notifications
1. Kiểm tra WorkManager đã được initialize:
```java
boolean initialized = WorkManagerInitializer.isInitialized(context);
Log.d("Debug", "WorkManager initialized: " + initialized);
```

2. Kiểm tra work status:
```java
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWorkLiveData("promotional_notifications")
    .observe(this, workInfos -> {
        for (WorkInfo info : workInfos) {
            Log.d("Debug", "Work state: " + info.getState());
        }
    });
```

### Notifications không lưu vào History
1. Kiểm tra database migration đã chạy:
```java
AppDatabase db = AppDatabase.getInstance(context);
// Check version
Log.d("Debug", "DB version: " + db.getOpenHelper().getReadableDatabase().getVersion());
```

2. Kiểm tra userId:
```java
FirebaseAuth auth = FirebaseAuth.getInstance();
String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
Log.d("Debug", "User ID: " + userId);
```

### Empty State luôn hiển thị
1. Kiểm tra LiveData observer:
```java
viewModel.getNotifications().observe(this, notifications -> {
    Log.d("Debug", "Notifications count: " + (notifications != null ? notifications.size() : 0));
});
```

2. Kiểm tra query:
```java
// Trong ViewModel
String userId = getCurrentUserId();
Log.d("Debug", "Querying for userId: " + userId);
```

## Best Practices

1. **Luôn lưu notification vào history** khi gửi notification
2. **Cleanup định kỳ** để tránh database bloat
3. **Respect user preferences** - cho phép tắt promotional notifications
4. **Test thoroughly** trước khi release
5. **Monitor WorkManager** status trong production
6. **Handle edge cases** như user logout, app reinstall

## Performance Tips

1. Sử dụng DiffUtil cho RecyclerView
2. Database operations trên background thread
3. LiveData cho reactive updates
4. Indices trên các column thường query
5. Batch operations khi có thể
6. Lazy loading cho danh sách dài

## Security

1. Validate userId trước khi query
2. Không lưu sensitive data trong notifications
3. Cleanup khi user logout
4. Encrypt notification content nếu cần
5. Rate limiting cho promotional notifications
