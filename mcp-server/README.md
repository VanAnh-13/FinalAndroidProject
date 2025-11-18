# Android Studio MCP Server

MCP (Model Context Protocol) server cho phép Kiro IDE tương tác với dự án Android Studio của bạn.

## Tính năng

### 🔧 Quản lý File
- **read_file**: Đọc nội dung file từ dự án
- **write_file**: Ghi nội dung vào file
- **list_files**: Liệt kê files trong thư mục

### 🏗️ Build & Gradle
- **run_gradle_task**: Chạy Gradle tasks (build, clean, test, v.v.)
- **get_build_status**: Kiểm tra trạng thái build
- **analyze_dependencies**: Phân tích dependencies trong build.gradle

### 🔍 Code Analysis
- **search_code**: Tìm kiếm code patterns trong dự án
- **get_project_structure**: Xem cấu trúc thư mục dự án

### 🧪 Testing
- **run_tests**: Chạy unit tests
- **get_logcat**: Xem Android logcat output

## Cài đặt

### Yêu cầu
- Python 3.8 trở lên
- pip (Python package manager)
- Android SDK (cho logcat và adb commands)

### Các bước cài đặt

1. **Chạy script setup**:
   ```bash
   cd mcp-server
   setup.bat
   ```

2. **Restart Kiro IDE**:
   - Đóng và mở lại Kiro IDE
   - Server sẽ tự động kết nối

3. **Kiểm tra kết nối**:
   - Mở Kiro IDE
   - Vào MCP Server view (nếu có)
   - Kiểm tra xem "android-studio" server có status "running"

## Sử dụng

### Trong Kiro IDE

Sau khi cài đặt, bạn có thể sử dụng các tools trong chat với Kiro:

**Ví dụ 1: Đọc file**
```
Hãy đọc file MainActivity.java cho tôi
```

**Ví dụ 2: Chạy build**
```
Hãy build dự án Android của tôi
```

**Ví dụ 3: Tìm kiếm code**
```
Tìm tất cả các class sử dụng Firebase trong dự án
```

**Ví dụ 4: Chạy tests**
```
Chạy tất cả unit tests trong dự án
```

**Ví dụ 5: Xem cấu trúc dự án**
```
Cho tôi xem cấu trúc thư mục của dự án
```

### Tools chi tiết

#### read_file
Đọc nội dung của một file.

**Parameters:**
- `path` (string, required): Đường dẫn tương đối từ project root

**Example:**
```json
{
  "path": "app/src/main/java/com/example/healthylifehub/MainActivity.java"
}
```

#### write_file
Ghi nội dung vào file.

**Parameters:**
- `path` (string, required): Đường dẫn tương đối từ project root
- `content` (string, required): Nội dung cần ghi

**Example:**
```json
{
  "path": "app/src/main/java/com/example/Test.java",
  "content": "public class Test { }"
}
```

#### list_files
Liệt kê files trong thư mục.

**Parameters:**
- `path` (string, optional): Đường dẫn thư mục (default: ".")
- `pattern` (string, optional): Pattern để filter files (default: "*")

**Example:**
```json
{
  "path": "app/src/main/java",
  "pattern": "*.java"
}
```

#### run_gradle_task
Chạy Gradle task.

**Parameters:**
- `task` (string, required): Tên task (e.g., "assembleDebug", "clean", "test")
- `args` (array, optional): Arguments bổ sung

**Example:**
```json
{
  "task": "assembleDebug",
  "args": ["--stacktrace"]
}
```

#### search_code
Tìm kiếm code patterns.

**Parameters:**
- `query` (string, required): Regex pattern để tìm
- `file_pattern` (string, optional): Pattern của files cần search (default: "*.java")
- `path` (string, optional): Thư mục để search (default: "app/src")

**Example:**
```json
{
  "query": "Firebase.*",
  "file_pattern": "*.java",
  "path": "app/src/main"
}
```

#### get_project_structure
Xem cấu trúc thư mục dự án.

**Parameters:**
- `depth` (integer, optional): Độ sâu tối đa (default: 3)

**Example:**
```json
{
  "depth": 4
}
```

#### analyze_dependencies
Phân tích dependencies từ build.gradle.

**Parameters:** None

#### get_build_status
Kiểm tra trạng thái build hiện tại.

**Parameters:** None

#### run_tests
Chạy unit tests.

**Parameters:**
- `test_class` (string, optional): Class test cụ thể
- `test_method` (string, optional): Method test cụ thể

**Example:**
```json
{
  "test_class": "com.example.healthylifehub.ExampleUnitTest",
  "test_method": "addition_isCorrect"
}
```

#### get_logcat
Xem Android logcat output.

**Parameters:**
- `filter` (string, optional): Logcat filter (default: "*:I")
- `lines` (integer, optional): Số dòng cần lấy (default: 100)

**Example:**
```json
{
  "filter": "ActivityManager:I *:S",
  "lines": 50
}
```

## Troubleshooting

### Server không kết nối
1. Kiểm tra Python đã cài đặt: `python --version`
2. Kiểm tra MCP SDK đã cài: `pip list | findstr mcp`
3. Xem logs trong Kiro IDE
4. Restart Kiro IDE

### Gradle tasks không chạy
1. Kiểm tra gradlew.bat có trong project root
2. Kiểm tra quyền execute của gradlew.bat
3. Thử chạy manual: `./gradlew assembleDebug`

### Logcat không hoạt động
1. Kiểm tra adb đã cài: `adb version`
2. Kiểm tra device đã kết nối: `adb devices`
3. Thêm Android SDK platform-tools vào PATH

## Cấu hình nâng cao

### Thay đổi project path
Edit `.kiro/settings/mcp.json`:
```json
{
  "mcpServers": {
    "android-studio": {
      "args": [
        "mcp-server/android_studio_mcp_server.py",
        "YOUR_PROJECT_PATH_HERE"
      ]
    }
  }
}
```

### Auto-approve thêm tools
Thêm tool names vào `autoApprove` array trong config:
```json
{
  "autoApprove": [
    "read_file",
    "write_file",
    "run_gradle_task"
  ]
}
```

### Disable server
Set `disabled: true` trong config:
```json
{
  "disabled": true
}
```

## Phát triển

### Thêm tool mới
1. Thêm Tool definition trong `list_tools()`
2. Implement handler trong `call_tool()`
3. Thêm method implementation
4. Update README

### Debug
Chạy server trực tiếp để debug:
```bash
python android_studio_mcp_server.py "F:/Document/Android/FinalAndroidProject"
```

## License
MIT License - Free to use and modify
