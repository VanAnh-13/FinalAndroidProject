# Bug Fix Plan

## Issues Identified

### 1. ❌ Tên user không hiển thị trong tab cá nhân
**Root Cause:** ProfileFragment đang load từ FirebaseAuth.displayName, nhưng có thể null
**Fix:** 
- Đã có fallback loadDisplayNameFromFirestore()
- Cần verify Firebase Auth profile được update đúng
- Add onResume() để reload khi quay lại

### 2. ❌ Cache không sync khi edit user
**Root Cause:** DashboardFragment cache userName, không reload khi quay lại
**Fix:**
- Add onResume() trong DashboardFragment
- Reload userName từ Firebase khi fragment visible
- Clear cache khi user update profile

### 3. ❌ Crash khi nhấn vào reminder
**Root Cause:** ReminderDetailActivity tìm views không tồn tại (cvProgress, tvProgressText, etc.)
**Fix:**
- Comment out code tìm views chưa có trong layout
- Hoặc thêm null checks trước khi dùng

### 4. ❌ Biểu đồ không hiển thị
**Root Cause:** Code đã bị comment out (lineChartBloodPressure)
**Fix:**
- Cần thêm LineChart vào fragment_dashboard.xml
- Hoặc giữ nguyên commented nếu chưa ready

### 5. ❌ Reminders chỉ hiển thị 3 bản ghi
**Root Cause:** Adapter hoặc query limit
**Fix:**
- Check RemindersAdapter logic
- Check DAO query có LIMIT không
- Sắp xếp theo reminderTime

## Priority Fixes

### HIGH (Fix ngay)
1. Fix crash ReminderDetailActivity - null pointer
2. Fix user name display trong Profile
3. Fix cache sync Dashboard

### MEDIUM  
4. Fix reminders limit 3 records
5. Sort reminders by setup time

### LOW
6. Biểu đồ (đã comment, không urgent)
