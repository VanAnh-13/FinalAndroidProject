# Serena MCP Server Fix Guide

## Problem
```
× No solution found when resolving tool dependencies:
╰─▶ Because mcp-server-serena was not found in the package registry
```

## Root Cause
Package name `mcp-server-serena` không tồn tại trong PyPI registry. Serena có thể:
1. Chưa được publish
2. Có tên package khác
3. Cần cài đặt từ source

## Solutions

### Option 1: Sử dụng NPX (Đã thử)
```json
{
  "serena": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-serena"],
    "env": {
      "SERENA_PROJECT_ROOT": "F:/Document/TASC/Backend/springfood-microservice"
    }
  }
}
```

### Option 2: Disable Serena tạm thời
```json
{
  "serena": {
    "disabled": true
  }
}
```

### Option 3: Dùng Filesystem MCP thay thế
```json
{
  "filesystem": {
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-filesystem", "F:/Document/TASC/Backend/springfood-microservice"],
    "disabled": false,
    "autoApprove": ["read_file", "list_directory", "search_files"]
  }
}
```

### Option 4: Cài đặt Serena từ source (nếu có repo)
```bash
# Clone repo
git clone https://github.com/your-org/serena-mcp-server
cd serena-mcp-server

# Install
pip install -e .

# Hoặc
npm install -g .
```

Sau đó config:
```json
{
  "serena": {
    "command": "serena-mcp",
    "args": [],
    "env": {
      "SERENA_PROJECT_ROOT": "F:/Document/TASC/Backend/springfood-microservice"
    }
  }
}
```

### Option 5: Kiểm tra tên package chính xác
```bash
# Search PyPI
pip search serena

# Search NPM
npm search serena mcp

# Hoặc check MCP registry
# https://github.com/modelcontextprotocol/servers
```

## Recommended Action

**Tạm thời disable Serena** và dùng filesystem server:

```json
{
  "mcpServers": {
    "android-studio": {
      "command": "python",
      "args": ["mcp-server/android_studio_mcp_server.py", "F:/Document/Android/FinalAndroidProject"],
      "disabled": false
    },
    "serena": {
      "disabled": true,
      "command": "uvx",
      "args": ["mcp-server-serena"]
    },
    "springfood-filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "F:/Document/TASC/Backend/springfood-microservice"],
      "disabled": false,
      "autoApprove": ["read_file", "list_directory", "search_files"]
    }
  }
}
```

## Testing
Sau khi update config:
1. Restart Kiro IDE
2. Hoặc reconnect MCP servers từ MCP Server view
3. Check logs để verify connection

## Notes
- Serena có thể là custom/internal tool chưa public
- Filesystem server cung cấp tương tự functionality
- Android Studio MCP đang hoạt động tốt
