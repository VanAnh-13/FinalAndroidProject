# Hệ thống Quản lý Thông báo

## Tổng quan
Hệ thống quản lý thông báo hoàn chỉnh cho HealthyLife Hub, bao gồm:
- Lưu trữ lịch sử tất cả thông báo đã gửi
- Hiển thị danh sách thông báo với filter (tất cả/chưa đọc)
- Thông báo quảng cáo định kỳ (mỗi 3 ngày)
- Tích hợp với hệ thống reminder hiện có

## Kiến trúc

### 1. Database Layer
**NotificationHistory Model** (`data/model/NotificationHistory.java`)
- Lưu trữ thông tin thông báo: title, message, type, timestamp
- Hỗ trợ 3 loại: REMINDER, PROMOTIONAL, SYSTEM
- Tracking trạng thái đọc/chưa đọc và action đã thực hiện

**NotificationHistoryDao** (`data/local/dao/NotificationHistoryDao.java`)
- CRUD operations cho notification history
- Query theo user, filter theo read/unread
- Auto-cleanup notifications cũ (>30 ngày)

### 2. Business Logic Layer
**NotificationHistoryManager** (`utils/NotificationHistoryManager.java`)
- Centralized manager để lưu notifications
- Methods:
  - `saveReminderNotification()` - Lưu reminder notification
  - `savePromotionalNotification()` - Lưu promotional notification
  - `saveSystemNotification()` - Lưu system notification
  - `cleanOldNotifications()` - Xóa notifications cũ

### 3. UI Layer
**NotificationHistoryActivity** (`ui/notifications/NotificationHistoryActivity.java`)
- Hiển thị danh sách notifications
- TabLayout: Tất cả / Chưa đọc
- RecyclerView với adapter tối ưu

**NotificationHistoryAdapter** (`ui/notifications/NotificationHistoryAdapter.java`)
- DiffUtil cho performance tốt
- Hiển thị icon theo loại notification
- Unread indicator (chấm xanh)
- Format thời gian relative (vừa xong, 5 phút trước, etc.)

**NotificationHistoryViewModel** (`ui/notifications/NotificationHistoryViewModel.java`)
- LiveData cho reactive UI
- Filter logic (all/unread)
- Mark as read functionality

### 4. Background Workers
**PromotionalNotificationWorker** (`workers/PromotionalNotificationWorker.java`)
- Periodic worker chạy mỗi 3 ngày
- 8 promotional messages khác nhau (random)
- Tự động lưu vào notification history
- Constraints: battery not low

## Tích hợp

### SmartNotificationManager
Đã được cập nhật để tự động lưu notification history khi gửi reminder notification:
```java
// Save to notification history after successful send
historyManager.saveReminderNotification(reminder, notificationId);
```

### AppDatabase
- Version tăng từ 10 lên 11
- Thêm NotificationHistory entity
- Migration 10->11 tạo bảng notification_history với indices

### AppInitializer
Setup promotional notifications khi app khởi động:
```java
WorkManagerConfig.setupPromotionalNotifications(context);
```

## Sử dụng

### Mở Notification History
```java
Intent intent = new Intent(context, NotificationHistoryActivity.class);
startActivity(intent);
```

### Lưu Custom Notification
```java
NotificationHistoryManager manager = new NotificationHistoryManager(context);
manager.saveSystemNotification("Title", "Message");
```

### Cancel Promotional Notifications
```java
WorkManagerConfig.cancelPromotionalNotifications(context);
```

## UI Components

### Layout Files
- `activity_notification_history.xml` - Main activity layout với TabLayout
- `item_notification_history.xml` - RecyclerView item với icon, title, message, time
- `circle_blue.xml` - Unread indicator drawable

### Features
- ✅ Hiển thị icon khác nhau theo loại notification
- ✅ Unread indicator (chấm xanh)
- ✅ Relative time formatting
- ✅ Mark as read khi click
- ✅ Filter: All / Unread
- ✅ Empty state message
- ✅ Material Design

## Promotional Messages
8 messages được rotate ngẫu nhiên:
1. Theo dõi sức khỏe mỗi ngày
2. Đạt mục tiêu sức khỏe
3. Xem báo cáo sức khỏe
4. Nhắc nhở thông minh
5. Quản lý sức khỏe hiệu quả
6. Uống thuốc đúng giờ
7. Theo dõi tiến độ
8. Chăm sóc sức khỏe tốt hơn

## Performance
- DiffUtil cho RecyclerView updates
- ExecutorService cho database operations
- LiveData cho reactive updates
- Indices trên userId, timestamp, isRead
- Auto-cleanup notifications >30 ngày

## Testing
Để test promotional notifications ngay:
```java
// Trigger immediately for testing
OneTimeWorkRequest testWork = new OneTimeWorkRequest.Builder(PromotionalNotificationWorker.class)
    .build();
WorkManager.getInstance(context).enqueue(testWork);
```

## Notes
- Promotional notifications chỉ gửi khi battery not low
- Flex interval: 1 hour (có thể delay trong khoảng 1h)
- ExistingPeriodicWorkPolicy.KEEP để tránh duplicate
- Tất cả notifications đều được lưu vào history tự động
