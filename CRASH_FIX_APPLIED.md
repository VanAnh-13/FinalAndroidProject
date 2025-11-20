# Crash Fix Applied

## Vấn đề
App crash khi khởi động do database migration hoặc duplicate WorkManager initialization.

## Giải pháp đã áp dụng

### 1. Fixed HealthyLifeHubApplication.java
**Trước:**
```java
AppInitializer.initialize(this); // Gọi WorkManagerInitializer bên trong
```

**Sau:**
```java
try {
    // Initialize WorkManager first (only once)
    WorkManagerInitializer.initialize(this);
    
    // Initialize other features separately
    initializeAppFeatures();
    
} catch (Exception e) {
    Log.e("HealthyLifeHub", "Initialization error", e);
}
```

### 2. Separated Feature Initialization
Tách riêng initialization để dễ debug:
- WorkManager initialization
- Notification channels
- Deadline management
- Promotional notifications
- Reminder rescheduling (khi user login)

### 3. Added Error Handling
Wrap tất cả initialization trong try-catch để app không crash nếu có lỗi.

## Cách Test

### Option 1: Reinstall App (Recommended)
```powershell
# Uninstall old version và install mới
.\reinstall_app.ps1
```

### Option 2: Manual Steps
```powershell
# 1. Uninstall old app
adb uninstall com.example.healthylifehub

# 2. Install new version
./gradlew installDebug

# 3. Launch app
adb shell am start -n com.example.healthylifehub/.ui.onboarding.WelcomeActivity
```

### Option 3: Clear App Data Only
```powershell
# Clear data without uninstall
adb shell pm clear com.example.healthylifehub
```

## Verify Fix

### 1. Check App Starts
App phải mở được màn hình Welcome/Login

### 2. Check Database Version
Sau khi login, check logs:
```java
// Trong MainActivity hoặc DashboardFragment
AppDatabase db = AppDatabase.getInstance(this);
int version = db.getOpenHelper().getReadableDatabase().getVersion();
Log.d("Verify", "Database version: " + version); // Should be 11
```

### 3. Check WorkManager
```java
boolean initialized = WorkManagerInitializer.isInitialized(this);
Log.d("Verify", "WorkManager initialized: " + initialized); // Should be true
```

### 4. Check Notification System
```java
// Test save notification
NotificationHistoryManager manager = new NotificationHistoryManager(this);
manager.saveSystemNotification("Test", "If you see this in logs, it works!");
```

## Nếu Vẫn Crash

### Get Logcat
```powershell
# Clear old logs
adb logcat -c

# Start app and capture crash
adb logcat | Select-String "healthylifehub|FATAL|AndroidRuntime" > crash_log.txt
```

### Common Issues

#### Issue 1: Database Migration Failed
**Solution**: Uninstall app hoàn toàn
```powershell
adb uninstall com.example.healthylifehub
./gradlew installDebug
```

#### Issue 2: WorkManager Already Initialized
**Solution**: Đã fix bằng cách gọi WorkManagerInitializer trực tiếp thay vì qua AppInitializer

#### Issue 3: Missing Dependencies
**Solution**: Clean và rebuild
```powershell
./gradlew clean
./gradlew assembleDebug
```

#### Issue 4: Firebase Not Initialized
**Solution**: Check google-services.json exists
```powershell
Test-Path app/google-services.json
```

## Expected Behavior

### On First Launch
1. ✅ App opens to WelcomeActivity
2. ✅ Database created with version 11
3. ✅ WorkManager initialized
4. ✅ Notification channels created
5. ✅ No crashes

### After Login
1. ✅ Dashboard loads
2. ✅ Reminders rescheduled
3. ✅ Promotional notifications scheduled
4. ✅ Can navigate to NotificationHistoryActivity

### Notification System
1. ✅ Notifications saved to history
2. ✅ Can view notification list
3. ✅ Can filter by read/unread
4. ✅ Promotional notifications work

## Rollback Plan

Nếu fix này gây thêm vấn đề, rollback:

```java
// Trong HealthyLifeHubApplication.java
@Override
public void onCreate() {
    super.onCreate();
    
    // Simple initialization without new features
    ApplicationContextProvider.init(this);
    WorkManagerInitializer.initialize(this);
    NotificationHelper.createNotificationChannel(this);
    
    // Comment out notification system
    // initializeAppFeatures();
}
```

## Status
✅ Fix applied
✅ Build successful
⏳ Waiting for runtime test

Hãy reinstall app và test lại!
