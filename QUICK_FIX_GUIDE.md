# Quick Fix Guide - Notification System

## 🔥 Common Issues & Instant Fixes

### 1. App Crashes on Startup

**Symptom**: App crashes ngay khi mở

**Quick Fix**:
```java
// Trong HealthyLifeHubApplication.java
// Comment dòng này tạm thời:
// AppInitializer.initialize(this);

// Hoặc wrap trong try-catch:
try {
    AppInitializer.initialize(this);
} catch (Exception e) {
    Log.e("App", "Init failed", e);
}
```

### 2. Database Migration Error

**Symptom**: "Migration didn't properly handle" hoặc crash khi access database

**Quick Fix**:
```java
// Trong AppDatabase.java, dòng fallbackToDestructiveMigration() đã có
// Nếu vẫn lỗi, uninstall app và install lại:
adb uninstall com.example.healthylifehub
./gradlew installDebug
```

### 3. NotificationHistoryActivity Not Found

**Symptom**: "Unable to find explicit activity class"

**Quick Fix**:
```bash
# Rebuild project
./gradlew clean
./gradlew :app:assembleDebug
```

### 4. Icons Not Found

**Symptom**: "Resource not found: ic_star" hoặc similar

**Quick Fix**: Tạo icon placeholder
```xml
<!-- app/src/main/res/drawable/ic_star.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFD700"
        android:pathData="M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"/>
</vector>
```

### 5. Promotional Notifications Not Working

**Symptom**: Không nhận được promotional notifications

**Quick Fix**: Test ngay lập tức
```java
// Thêm vào MainActivity hoặc test button
OneTimeWorkRequest testWork = new OneTimeWorkRequest.Builder(
    PromotionalNotificationWorker.class
).build();
WorkManager.getInstance(this).enqueue(testWork);

Toast.makeText(this, "Test notification queued", Toast.LENGTH_SHORT).show();
```

### 6. Empty List Always Shows

**Symptom**: Luôn hiển thị "Chưa có thông báo nào"

**Quick Fix**: Thêm test data
```java
// Trong MainActivity hoặc test button
NotificationHistoryManager manager = new NotificationHistoryManager(this);
manager.savePromotionalNotification("Test 1", "Message 1");
manager.savePromotionalNotification("Test 2", "Message 2");
manager.saveSystemNotification("Test 3", "Message 3");

Toast.makeText(this, "Test notifications added", Toast.LENGTH_SHORT).show();
```

### 7. User Not Logged In

**Symptom**: userId is null, notifications không lưu

**Quick Fix**:
```java
// Check trong NotificationHistoryManager
private String getCurrentUserId() {
    FirebaseAuth auth = FirebaseAuth.getInstance();
    if (auth.getCurrentUser() == null) {
        // Fallback to local user ID
        return "local_user_" + android.os.Build.SERIAL;
    }
    return auth.getCurrentUser().getUid();
}
```

### 8. WorkManager Not Initialized

**Symptom**: "WorkManager is not initialized properly"

**Quick Fix**:
```java
// Trong HealthyLifeHubApplication.java
// Đảm bảo WorkManagerInitializer.initialize() được gọi TRƯỚC AppInitializer
WorkManagerInitializer.initialize(this);
AppInitializer.initialize(this);
```

## 🛠️ Emergency Rollback

Nếu hệ thống notification gây crash không fix được:

### Option 1: Disable Promotional Notifications
```java
// Trong AppInitializer.java, comment dòng:
// WorkManagerConfig.setupPromotionalNotifications(context);
```

### Option 2: Disable Notification History Saving
```java
// Trong SmartNotificationManager.java, comment dòng:
// historyManager.saveReminderNotification(reminder, notificationId);
```

### Option 3: Skip Migration
```java
// Trong AppDatabase.java, tạm thời giảm version:
version = 10,  // Rollback to previous version
```

## 🔧 Debug Commands

### Check Database
```bash
adb shell "run-as com.example.healthylifehub ls -la databases/"
adb shell "run-as com.example.healthylifehub sqlite3 databases/healthylife_hub.db 'SELECT * FROM notification_history LIMIT 5;'"
```

### Check WorkManager
```bash
adb shell "dumpsys jobscheduler | grep healthylifehub"
```

### Clear App Data
```bash
adb shell pm clear com.example.healthylifehub
```

### View Logcat
```bash
adb logcat | grep -i "healthylifehub\|notification\|fatal"
```

## 📞 Get Help

Nếu vẫn gặp vấn đề, cung cấp:
1. Logcat output (adb logcat)
2. Build output (./gradlew assembleDebug)
3. Database version (check trong code)
4. Android version của device
5. Steps to reproduce

## ✅ Verification After Fix

Sau khi fix, verify:
```java
// Test 1: Database
AppDatabase db = AppDatabase.getInstance(this);
Log.d("Verify", "DB version: " + db.getOpenHelper().getReadableDatabase().getVersion());

// Test 2: DAO
NotificationHistoryDao dao = db.notificationHistoryDao();
Log.d("Verify", "DAO exists: " + (dao != null));

// Test 3: Save notification
NotificationHistoryManager manager = new NotificationHistoryManager(this);
manager.saveSystemNotification("Verify Test", "If you see this, it works!");

// Test 4: Open activity
startActivity(new Intent(this, NotificationHistoryActivity.class));
```

## 🎯 Success Indicators

Hệ thống hoạt động tốt khi:
- ✅ App mở không crash
- ✅ Database version = 11
- ✅ NotificationHistoryActivity mở được
- ✅ Có thể lưu và xem notifications
- ✅ Tabs switch được
- ✅ Icons hiển thị đúng
- ✅ Time formatting đúng
- ✅ Mark as read hoạt động

Nếu tất cả đều OK → Hệ thống sẵn sàng! 🎉
