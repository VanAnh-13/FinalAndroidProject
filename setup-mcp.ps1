# Script thiết lập MCP Server cho HealthyLife Hub Android Project
# Chạy với quyền Administrator

param(
    [switch]$Install,
    [switch]$Configure,
    [switch]$Start
)

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Check-NodeJS {
    Write-ColorOutput "Đang kiểm tra Node.js..." "Yellow"
    try {
        $nodeVersion = node --version 2>$null
        if ($nodeVersion) {
            Write-ColorOutput "✓ Node.js đã cài đặt: $nodeVersion" "Green"
            return $true
        }
    }
    catch {
        Write-ColorOutput "✗ Node.js chưa được cài đặt" "Red"
        Write-ColorOutput "Vui lòng tải về và cài đặt từ: https://nodejs.org/" "Yellow"
        return $false
    }
}

function Install-MCPPackages {
    Write-ColorOutput "Đang cài đặt MCP packages..." "Yellow"
    
    # Cài đặt filesystem server
    npm install -g @modelcontextprotocol/server-filesystem@latest
    
    # Cài đặt git server (tùy chọn)
    npm install -g @modelcontextprotocol/server-git@latest
    
    Write-ColorOutput "✓ Đã cài đặt xong MCP packages" "Green"
}

function Setup-ClaudeConfig {
    $configPath = "$env:APPDATA\Claude\claude_desktop_config.json"
    $projectPath = (Get-Location).Path
    
    Write-ColorOutput "Đang thiết lập cấu hình Claude Desktop..." "Yellow"
    
    # Tạo thư mục nếu chưa tồn tại
    $configDir = Split-Path $configPath
    if (!(Test-Path $configDir)) {
        New-Item -ItemType Directory -Path $configDir -Force
    }
    
    # Tạo cấu hình MCP
    $config = @{
        mcpServers = @{
            "healthylifehub-filesystem" = @{
                command = "npx"
                args = @(
                    "-y",
                    "@modelcontextprotocol/server-filesystem@latest",
                    $projectPath
                )
                env = @{
                    DEBUG = "mcp*"
                }
            }
            "healthylifehub-git" = @{
                command = "npx"
                args = @(
                    "-y", 
                    "@modelcontextprotocol/server-git@latest",
                    $projectPath
                )
            }
        }
    }
    
    # Lưu cấu hình
    $config | ConvertTo-Json -Depth 10 | Out-File -FilePath $configPath -Encoding UTF8
    
    Write-ColorOutput "✓ Đã thiết lập cấu hình tại: $configPath" "Green"
    Write-ColorOutput "✓ Project path: $projectPath" "Green"
}

function Start-MCPServer {
    Write-ColorOutput "Đang khởi động MCP Server..." "Yellow"
    Write-ColorOutput "Nhấn Ctrl+C để dừng server" "Cyan"
    
    try {
        npx -y @modelcontextprotocol/server-filesystem@latest .
    }
    catch {
        Write-ColorOutput "Lỗi khi khởi động MCP Server: $_" "Red"
    }
}

# Main script
Write-ColorOutput "=== MCP Server Setup cho HealthyLife Hub ===" "Cyan"
Write-ColorOutput ""

if ($Install -or (!$Configure -and !$Start)) {
    if (Check-NodeJS) {
        Install-MCPPackages
        Setup-ClaudeConfig
        
        Write-ColorOutput ""
        Write-ColorOutput "=== Thiết lập hoàn tất! ===" "Green"
        Write-ColorOutput "Bước tiếp theo:" "Yellow"
        Write-ColorOutput "1. Khởi động lại Claude Desktop" "White"
        Write-ColorOutput "2. Chạy lệnh: .\setup-mcp.ps1 -Start" "White"
        Write-ColorOutput "3. Hoặc chạy: .\start-mcp-server.bat" "White"
    }
}

if ($Configure) {
    Setup-ClaudeConfig
}

if ($Start) {
    if (Check-NodeJS) {
        Start-MCPServer
    }
}

Write-ColorOutput ""
Write-ColorOutput "Để xem hướng dẫn chi tiết: Get-Content MCP_SETUP_GUIDE.md" "Cyan"