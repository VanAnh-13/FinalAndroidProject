@echo off
echo Starting MCP Server for HealthyLife Hub Android Project...
echo.

REM Kiểm tra Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js không được tìm thấy. Vui lòng cài đặt Node.js trước.
    echo Tải về tại: https://nodejs.org/
    pause
    exit /b 1
)

echo Node.js đã được cài đặt.
echo.

REM Khởi động MCP Server
echo Đang khởi động MCP Server...
npx -y @modelcontextprotocol/server-filesystem@latest

pause