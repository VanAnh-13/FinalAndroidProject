# Hướng Dẫn Thiết Lập MCP cho Android Studio - HOÀN CHỈNH

## ✅ Đã Kiểm Tra

### 1. Python
- ✅ Python 3.14.0 đã cài đặt
- ✅ Đường dẫn: Có trong PATH

### 2. MCP SDK
- ✅ MCP 1.21.1 đã cài đặt
- ✅ Package: `mcp`

### 3. Config File
- ✅ File: `.kiro/settings/mcp.json`
- ✅ Server name: `android-studio`
- ✅ Project path: `F:/Document/Android/FinalAndroidProject`

### 4. MCP Server
- ✅ File: `mcp-server/android_studio_mcp_server.py`
- ✅ Test script: `mcp-server/test_server.py`

## 🔧 Cấu Hình Hiện Tại

```json
{
  "mcpServers": {
    "android-studio": {
      "command": "python",
      "args": [
        "mcp-server/android_studio_mcp_server.py",
        "F:/Document/Android/FinalAndroidProject"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "read_file",
        "list_files",
        "get_project_structure",
        "analyze_dependencies",
        "get_build_status",
        "search_code"
      ]
    }
  }
}
```

## 🚀 Cách Sử Dụng

### 1. Restart Kiro IDE
MCP server sẽ tự động kết nối khi Kiro khởi động lại.

**Cách restart:**
- Đóng Kiro IDE hoàn toàn
- Mở lại Kiro IDE
- Server sẽ tự động start

### 2. Kiểm Tra Kết Nối

Trong Kiro IDE, mở **MCP Server View**:
- Vào menu: View → MCP Servers
- Hoặc: Ctrl+Shift+P → "MCP: Show Servers"

Bạn sẽ thấy:
- ✅ `android-studio` - Status: **Running**

### 3. Test MCP Tools

Hỏi Kiro trong chat:

**Test 1: Đọc file**
```
Hãy đọc file MainActivity.java cho tôi
```

**Test 2: Xem cấu trúc project**
```
Cho tôi xem cấu trúc thư mục của dự án
```

**Test 3: Tìm kiếm code**
```
Tìm tất cả các class sử dụng Firebase trong dự án
```

**Test 4: Chạy Gradle**
```
Hãy build dự án Android của tôi
```

**Test 5: Xem dependencies**
```
Phân tích dependencies trong build.gradle
```

## 🛠️ Các Tools Có Sẵn

### 1. File Operations
- `read_file` - Đọc nội dung file
- `write_file` - Ghi nội dung vào file
- `list_files` - Liệt kê files trong thư mục

### 2. Build & Gradle
- `run_gradle_task` - Chạy Gradle tasks
- `get_build_status` - Kiểm tra trạng thái build
- `analyze_dependencies` - Phân tích dependencies

### 3. Code Analysis
- `search_code` - Tìm kiếm code patterns
- `get_project_structure` - Xem cấu trúc project

### 4. Testing & Debugging
- `run_tests` - Chạy unit tests
- `get_logcat` - Xem Android logcat

## 🔍 Troubleshooting

### Vấn đề 1: Server không kết nối

**Kiểm tra:**
```powershell
# Test Python
python --version

# Test MCP
pip list | Select-String "mcp"

# Test server trực tiếp
python mcp-server/android_studio_mcp_server.py "F:/Document/Android/FinalAndroidProject"
```

**Giải pháp:**
1. Restart Kiro IDE
2. Kiểm tra logs trong Kiro
3. Xem MCP Server view

### Vấn đề 2: Tools không hoạt động

**Kiểm tra config:**
- File: `.kiro/settings/mcp.json`
- Đảm bảo `disabled: false`
- Đảm bảo đường dẫn project đúng

**Giải pháp:**
1. Sửa config nếu cần
2. Restart Kiro IDE
3. Reconnect server từ MCP Server view

### Vấn đề 3: Permission denied

**Giải pháp:**
- Thêm tools vào `autoApprove` trong config
- Hoặc approve manually khi Kiro hỏi

## 📝 Auto-Approve Tools

Các tools sau đã được auto-approve (không cần xác nhận):
- ✅ read_file
- ✅ list_files
- ✅ get_project_structure
- ✅ analyze_dependencies
- ✅ get_build_status
- ✅ search_code

Các tools khác sẽ cần xác nhận:
- ⚠️ write_file
- ⚠️ run_gradle_task
- ⚠️ run_tests

## 🎯 Ví Dụ Sử Dụng

### Ví dụ 1: Đọc và phân tích code
```
Hãy đọc file MainActivity.java và cho tôi biết:
1. Có bao nhiêu methods?
2. Có sử dụng Firebase không?
3. Có lỗi gì không?
```

### Ví dụ 2: Build project
```
Hãy build dự án Android của tôi với task assembleDebug
```

### Ví dụ 3: Tìm bugs
```
Tìm tất cả các TODO comments trong dự án
```

### Ví dụ 4: Refactor code
```
Tìm tất cả các class có tên chứa "Activity" 
và cho tôi biết class nào chưa implement onDestroy()
```

## 🔄 Reconnect Server

Nếu server bị disconnect:

**Cách 1: Từ MCP Server View**
1. Mở MCP Server view
2. Click vào server "android-studio"
3. Click "Reconnect"

**Cách 2: Restart Kiro**
1. Đóng Kiro IDE
2. Mở lại
3. Server tự động reconnect

**Cách 3: Từ Command Palette**
1. Ctrl+Shift+P
2. Gõ: "MCP: Reconnect Server"
3. Chọn "android-studio"

## ✅ Checklist Setup

- [x] Python đã cài đặt (3.14.0)
- [x] MCP SDK đã cài đặt (1.21.1)
- [x] Config file đã tạo (`.kiro/settings/mcp.json`)
- [x] MCP server file tồn tại (`mcp-server/android_studio_mcp_server.py`)
- [x] Auto-approve tools đã config
- [ ] **Restart Kiro IDE** ← BẠN CẦN LÀM BƯỚC NÀY
- [ ] **Kiểm tra MCP Server view** ← SAU KHI RESTART
- [ ] **Test một vài tools** ← ĐỂ XÁC NHẬN HOẠT ĐỘNG

## 🎉 Kết Luận

MCP server đã được cài đặt và cấu hình xong!

**Bước tiếp theo:**
1. **Restart Kiro IDE** (quan trọng!)
2. Kiểm tra MCP Server view
3. Test bằng cách hỏi Kiro đọc file hoặc build project

Nếu có vấn đề, check logs trong Kiro hoặc chạy test script:
```powershell
python mcp-server/test_server.py
```

Good luck! 🚀
