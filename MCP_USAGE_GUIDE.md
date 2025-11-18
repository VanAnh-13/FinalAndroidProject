# Hướng Dẫn Sử Dụng MCP Server cho Android Studio

## ✅ Setup Hoàn Tất

MCP server đã được cấu hình và đang hoạt động cho project Android này.

## 🛠️ Các Tools Có Sẵn

### 1. **read_file** - Đọc file
```
Đọc nội dung file từ project
Ví dụ: Đọc MainActivity.java
```

### 2. **write_file** - Ghi file
```
Tạo hoặc ghi đè nội dung file
Ví dụ: Tạo class mới
```

### 3. **list_files** - Liệt kê files
```
Liệt kê files theo pattern
Ví dụ: Tìm tất cả *.java files
```

### 4. **run_gradle_task** - Chạy Gradle task
```
Build, clean, test project
Ví dụ: assembleDebug, clean, test
```

### 5. **search_code** - Tìm kiếm code
```
Tìm kiếm pattern trong code
Ví dụ: Tìm tất cả "onCreate" methods
```

### 6. **get_project_structure** - Xem cấu trúc project
```
Hiển thị cây thư mục project
```

### 7. **analyze_dependencies** - Phân tích dependencies
```
Xem các thư viện đang dùng
```

### 8. **get_build_status** - Kiểm tra build status
```
Xem trạng thái build và APK files
```

### 9. **run_tests** - Chạy tests
```
Chạy unit tests
```

### 10. **get_logcat** - Xem Android logs
```
Đọc logcat từ device/emulator
```

## 🚀 Cách Sử Dụng

Bạn có thể yêu cầu Kiro thực hiện các tác vụ như:

- "Đọc file MainActivity.java"
- "Tìm tất cả các Activity trong project"
- "Build debug APK"
- "Xem cấu trúc project"
- "Tìm kiếm code có chứa 'Reminder'"
- "Chạy tests"

## 📝 Lưu Ý

- MCP server tự động kết nối khi Kiro khởi động
- Các tools đã được auto-approve nên không cần xác nhận
- Project root: `F:/Document/Android/FinalAndroidProject`

## 🔧 Cấu Hình

File cấu hình: `.kiro/settings/mcp.json`

Server đang chạy với Python 3.14 và MCP SDK 1.21.1
