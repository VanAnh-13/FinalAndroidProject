# Hướng dẫn thiết lập MCP Server cho HealthyLife Hub

## Giới thiệu
MCP (Model Context Protocol) server cho phép Claude Desktop kết nối trực tiếp với dự án Android HealthyLife Hub của bạn, giúp phân tích code, đề xuất cải tiến và hỗ trợ phát triển.

## Yêu cầu hệ thống
- Node.js (phiên bản 18 trở lên)
- Claude Desktop app
- Android Studio
- Git (khuyến nghị)

## Các bước thiết lập

### Bước 1: Cài đặt Node.js
1. Tải về Node.js từ: https://nodejs.org/
2. Cài đặt phiên bản LTS
3. Kiểm tra bằng lệnh: `node --version` và `npm --version`

### Bước 2: Cài đặt Claude Desktop
1. Tải về Claude Desktop từ: https://claude.ai/desktop
2. Cài đặt và đăng nhập tài khoản

### Bước 3: Cấu hình MCP Server

#### Cách 1: Sử dụng file cấu hình có sẵn
1. Copy nội dung file `claude-desktop-config.json` trong thư mục này
2. Dán vào file cấu hình Claude Desktop:
   - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Linux**: `~/.config/Claude/claude_desktop_config.json`

#### Cách 2: Cấu hình thủ công
Thêm đoạn code sau vào file cấu hình Claude Desktop:

```json
{
  "mcpServers": {
    "healthylifehub-filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem@latest",
        "ĐƯỜNG_DẪN_ĐẾN_THU_MUC_DU_AN"
      ],
      "env": {
        "DEBUG": "mcp*"
      }
    }
  }
}
```

**Lưu ý**: Thay `ĐƯỜNG_DẪN_ĐẾN_THU_MUC_DU_AN` bằng đường dẫn tuyệt đối đến thư mục dự án HealthyLife Hub.

### Bước 4: Khởi động và kiểm tra

1. **Khởi động nhanh** (chỉ để test):
   - Chạy file `start-mcp-server.bat` 
   - Hoặc chạy lệnh: `npx -y @modelcontextprotocol/server-filesystem@latest`

2. **Khởi động lại Claude Desktop**:
   - Đóng hoàn toàn Claude Desktop
   - Mở lại ứng dụng

3. **Kiểm tra kết nối**:
   - Mở Claude Desktop
   - Tạo conversation mới
   - Gõ lệnh để test: "Hãy liệt kê các file trong dự án Android"

## Các tính năng có sẵn

### 1. Filesystem Server
- Đọc/ghi file source code
- Phân tích cấu trúc project
- Tìm kiếm trong code
- Chỉnh sửa file

### 2. Git Server (tùy chọn)
- Xem lịch sử commit
- Phân tích thay đổi
- Quản lý branch

## Khắc phục sự cố

### Lỗi "Node.js not found"
- Cài đặt Node.js từ nodejs.org
- Khởi động lại terminal/command prompt

### Lỗi "MCP server not responding"
- Kiểm tra đường dẫn đến thư mục dự án
- Đảm bảo quyền truy cập thư mục
- Khởi động lại Claude Desktop

### Lỗi quyền truy cập
- Chạy terminal với quyền Administrator (Windows)
- Sử dụng `sudo` (macOS/Linux)

## Sử dụng hiệu quả

### Các lệnh hữu ích với Claude:
1. "Phân tích cấu trúc dự án Android này"
2. "Tìm các vấn đề trong code Java/Kotlin"
3. "Đề xuất cải tiến architecture"
4. "Kiểm tra security issues"
5. "Tối ưu performance"

### Workflow khuyến nghị:
1. Mở Android Studio để development
2. Sử dụng Claude Desktop để code review và consultation
3. Áp dụng suggestions từ Claude vào Android Studio

## Lưu ý bảo mật
- MCP server chỉ hoạt động local
- Không chia sẻ dữ liệu qua internet
- Kiểm tra quyền truy cập file

## Hỗ trợ
Nếu gặp vấn đề, hãy kiểm tra:
1. Console log của Claude Desktop
2. Terminal output khi chạy MCP server
3. File log trong thư mục dự án

---
**Tạo bởi**: MCP Setup Assistant cho HealthyLife Hub Android Project
**Cập nhật**: {{current_date}}