@echo off
echo ========================================
echo Android Studio MCP Server Setup
echo ========================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python is not installed or not in PATH
    echo Please install Python 3.8 or higher from https://www.python.org/
    pause
    exit /b 1
)

echo Installing MCP SDK...
python -m pip install --upgrade pip
python -m pip install -r requirements.txt

if errorlevel 1 (
    echo.
    echo Error: Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo ========================================
echo Setup completed successfully!
echo ========================================
echo.
echo The MCP server is now configured in .kiro/settings/mcp.json
echo.
echo To use the server:
echo 1. Restart Kiro IDE
echo 2. The server will automatically connect
echo 3. You can now use Android Studio tools in Kiro
echo.
echo Available tools:
echo - read_file: Read files from the project
echo - write_file: Write files to the project
echo - list_files: List files in directories
echo - run_gradle_task: Run Gradle tasks
echo - search_code: Search for code patterns
echo - get_project_structure: Get project structure
echo - analyze_dependencies: Analyze dependencies
echo - get_build_status: Get build status
echo - run_tests: Run unit tests
echo - get_logcat: Get Android logcat output
echo.
pause
