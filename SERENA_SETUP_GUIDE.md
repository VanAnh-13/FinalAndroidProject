# 🚀 SERENA + MCP SETUP COMPLETE

## ✅ Đã Setup

### 1. **Android Studio MCP Server**
- **Project:** `F:/Document/Android/FinalAndroidProject`
- **Status:** ✅ Đang chạy
- **Tools:** 10 tools (read_file, write_file, run_gradle, etc.)

### 2. **Serena MCP Server** 
- **Project:** `F:/Document/TASC/Backend/springfood-microservice`
- **Status:** ⏳ Cần cài đặt
- **Tools:** Code analysis, refactoring, symbol search

---

## 📦 Cài Đặt Serena

### Bước 1: Cài đặt uv (nếu chưa có)
```powershell
# Kiểm tra xem đã có uv chưa
uv --version

# Nếu chưa có, cài đặt:
pip install uv
```

### Bước 2: Cài đặt Serena MCP Server
```powershell
uvx mcp-server-serena
```

### Bước 3: Restart Kiro
- Đóng và mở lại Kiro
- Hoặc reload MCP servers từ Command Palette

---

## 🎯 Sử Dụng

### Với Android Project:
```
"Đọc MainActivity.java"
"Tìm tất cả Activities"
"Build debug APK"
"Xem logcat"
```

### Với Spring Boot Project:
```
"Phân tích cấu trúc Spring Boot project"
"Tìm tất cả Controllers"
"Xem Service classes"
"Refactor code"
```

---

## 🔧 Kiểm Tra Kết Nối

### Test Android MCP:
```
Tôi: "Liệt kê các Activity trong Android project"
```

### Test Serena:
```
Tôi: "Phân tích cấu trúc Spring Boot microservice"
```

---

## 📝 MCP Config Location

**File:** `.kiro/settings/mcp.json`

```json
{
  "mcpServers": {
    "android-studio": {
      "command": "python",
      "args": ["mcp-server/android_studio_mcp_server.py", "..."],
      "disabled": false
    },
    "serena": {
      "command": "uvx",
      "args": ["mcp-server-serena"],
      "env": {
        "SERENA_PROJECT_ROOT": "F:/Document/TASC/Backend/springfood-microservice"
      },
      "disabled": false
    }
  }
}
```

---

## 🛠️ Troubleshooting

### Serena không kết nối?
1. Kiểm tra uv đã cài đặt: `uv --version`
2. Kiểm tra path đúng: `F:/Document/TASC/Backend/springfood-microservice`
3. Restart Kiro
4. Xem MCP logs trong Kiro

### Android MCP không hoạt động?
1. Kiểm tra Python: `python --version`
2. Kiểm tra MCP SDK: `pip show mcp`
3. Restart MCP server từ Kiro

---

## 🎉 Tính Năng

### Android Studio MCP:
- ✅ Read/Write files
- ✅ Run Gradle tasks
- ✅ Search code
- ✅ Get logcat
- ✅ Analyze dependencies
- ✅ Build APK

### Serena MCP:
- ✅ Code analysis
- ✅ Symbol search
- ✅ Refactoring
- ✅ Find references
- ✅ Rename symbols
- ✅ Code navigation
- ✅ Pattern search

---

## 📚 Tài Liệu

- **Serena:** https://github.com/serena-ai/mcp-server-serena
- **MCP Protocol:** https://modelcontextprotocol.io
- **Kiro MCP:** Xem trong Kiro settings

---

**Setup by:** Kiro AI Assistant  
**Date:** 18/11/2025
