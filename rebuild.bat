@echo off
REM Rebuild script to fix Lombok annotation processing

echo Cleaning build...
call gradlew.bat clean

echo Building project...
call gradlew.bat build

echo.
echo Build complete! 
echo If you still see errors in IDE:
echo 1. File > Invalidate Caches > Invalidate and Restart
echo 2. Or: File > Invalidate Caches > Just Clear Cache
echo 3. Then rebuild project in IDE
