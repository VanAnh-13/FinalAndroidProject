# Test Hệ thống Quản lý Thông báo

## ✅ Build Status
- **Compilation**: SUCCESS
- **No Errors**: Confirmed
- **Warnings**: Only Room constructor warnings (expected, không ảnh hưởng)

## 🧪 Test Cases

### 1. Test Database Migration
```java
// Trong MainActivity onCreate() hoặc test class
AppDatabase db = AppDatabase.getInstance(this);
int version = db.getOpenHelper().getReadableDatabase().getVersion();
Log.d("Test", "Database version: " + version); // Should be 11

// Test DAO
NotificationHistoryDao dao = db.notificationHistoryDao();
Log.d("Test", "NotificationHistoryDao created: " + (dao != null));
```

### 2. Test Save Notification
```java
// Test lưu notification
NotificationHistoryManager manager = new NotificationHistoryManager(this);

// Test promotional
manager.savePromotionalNotification(
    "Test Promo", 
    "This is a test promotional notification"
);

// Test system
manager.saveSystemNotification(
    "Test System", 
    "This is a test system notification"
);

Log.d("Test", "Notifications saved successfully");
```

### 3. Test Open NotificationHistoryActivity
```java
// Thêm button test trong MainActivity
Button btnTest = findViewById(R.id.btnTestNotifications);
btnTest.setOnClickListener(v -> {
    Intent intent = new Intent(this, NotificationHistoryActivity.class);
    startActivity(intent);
});
```

### 4. Test Promotional Worker
```java
// Trigger immediate test
OneTimeWorkRequest testWork = new OneTimeWorkRequest.Builder(
    PromotionalNotificationWorker.class
).build();

WorkManager.getInstance(this).enqueue(testWork);

// Check work status
WorkManager.getInstance(this)
    .getWorkInfoByIdLiveData(testWork.getId())
    .observe(this, workInfo -> {
        if (workInfo != null) {
            Log.d("Test", "Work state: " + workInfo.getState());
        }
    });
```

### 5. Test Query Notifications
```java
// Trong Activity với ViewModel
NotificationHistoryViewModel viewModel = new ViewModelProvider(this)
    .get(NotificationHistoryViewModel.class);

viewModel.getNotifications().observe(this, notifications -> {
    Log.d("Test", "Notifications count: " + 
        (notifications != null ? notifications.size() : 0));
    
    if (notifications != null) {
        for (NotificationHistory notif : notifications) {
            Log.d("Test", "- " + notif.getTitle() + ": " + notif.getMessage());
        }
    }
});
```

## 🔍 Debugging Steps

### Nếu app crash khi mở:
1. Check logcat:
```bash
adb logcat | grep -i "healthylifehub\|fatal\|exception"
```

2. Check database version:
```java
Log.d("Debug", "DB version: " + 
    AppDatabase.getInstance(this).getOpenHelper()
        .getReadableDatabase().getVersion());
```

3. Check migration:
```java
// Nếu crash do migration, có thể force recreate:
// Trong AppDatabase.getInstance():
.fallbackToDestructiveMigration() // Đã có sẵn
```

### Nếu không thấy notifications:
1. Check userId:
```java
FirebaseAuth auth = FirebaseAuth.getInstance();
String userId = auth.getCurrentUser() != null ? 
    auth.getCurrentUser().getUid() : null;
Log.d("Debug", "Current userId: " + userId);
```

2. Check DAO query:
```java
AppDatabase db = AppDatabase.getInstance(this);
new Thread(() -> {
    List<NotificationHistory> all = db.notificationHistoryDao()
        .getAllNotifications(userId).getValue();
    Log.d("Debug", "Total notifications: " + 
        (all != null ? all.size() : 0));
}).start();
```

### Nếu promotional notifications không gửi:
1. Check WorkManager:
```java
WorkManager.getInstance(this)
    .getWorkInfosForUniqueWorkLiveData("promotional_notifications")
    .observe(this, workInfos -> {
        for (WorkInfo info : workInfos) {
            Log.d("Debug", "Promo work state: " + info.getState());
        }
    });
```

2. Trigger manual test:
```java
// Gửi ngay không đợi 3 ngày
OneTimeWorkRequest testWork = new OneTimeWorkRequest.Builder(
    PromotionalNotificationWorker.class
).build();
WorkManager.getInstance(this).enqueue(testWork);
```

## 📱 Manual Testing

### Step 1: Install App
```bash
./gradlew installDebug
```

### Step 2: Open App
- Login với tài khoản test
- Navigate to Profile hoặc Settings

### Step 3: Add Test Button
Thêm button trong ProfileFragment hoặc MainActivity:
```xml
<Button
    android:id="@+id/btnNotificationHistory"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Lịch sử thông báo"
    android:layout_margin="16dp" />
```

```java
findViewById(R.id.btnNotificationHistory).setOnClickListener(v -> {
    startActivity(new Intent(this, NotificationHistoryActivity.class));
});
```

### Step 4: Test Features
1. ✅ Mở NotificationHistoryActivity
2. ✅ Xem empty state (nếu chưa có notifications)
3. ✅ Trigger test notification
4. ✅ Xem notification xuất hiện trong list
5. ✅ Click notification → mark as read
6. ✅ Switch tabs (Tất cả / Chưa đọc)
7. ✅ Test promotional worker

## 🎯 Expected Results

### Database
- ✅ Version = 11
- ✅ notification_history table exists
- ✅ Indices created

### UI
- ✅ NotificationHistoryActivity opens without crash
- ✅ Empty state shows when no notifications
- ✅ Notifications display with correct icon
- ✅ Unread indicator shows/hides correctly
- ✅ Time formatting works (vừa xong, 5 phút trước, etc.)
- ✅ Tabs switch correctly

### Background
- ✅ PromotionalNotificationWorker scheduled
- ✅ Notifications saved to history automatically
- ✅ WorkManager running

## 🐛 Known Issues & Solutions

### Issue 1: App crashes on startup
**Solution**: Check AppInitializer is called in Application.onCreate()

### Issue 2: Notifications not saving
**Solution**: Check userId is not null, check database migration

### Issue 3: Empty list always shows
**Solution**: Check LiveData observer, check query with correct userId

### Issue 4: Icons not showing
**Solution**: All icons exist (ic_notifications_active, ic_star, ic_check_circle, ic_close)

### Issue 5: Promotional notifications not working
**Solution**: Check WorkManager initialized, check constraints (battery not low)

## ✅ Verification Checklist

- [x] Build successful
- [x] No compilation errors
- [x] All files created
- [x] Database migration added
- [x] DAOs implemented
- [x] ViewModels implemented
- [x] Adapters implemented
- [x] Layouts created
- [x] Icons exist
- [x] AndroidManifest updated
- [x] AppInitializer integrated
- [x] Documentation complete

## 🚀 Ready for Testing!

Hệ thống đã sẵn sàng để test. Chỉ cần:
1. Install app
2. Add navigation button
3. Test các features
4. Report any issues

Nếu có lỗi runtime, hãy cung cấp logcat output để debug.
