# ========================================
# QUICK DEMO SETUP - Tự động build và populate data
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  QUICK DEMO SETUP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build app
Write-Host "[1/5] Building app..." -ForegroundColor Yellow
.\gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Build successful!" -ForegroundColor Green
Write-Host ""

# Step 2: Install app
Write-Host "[2/5] Installing app..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
adb install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Install failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ App installed!" -ForegroundColor Green
Write-Host ""

# Step 3: Launch app
Write-Host "[3/5] Launching app..." -ForegroundColor Yellow
adb shell am start -n com.example.healthylifehub/.ui.auth.LoginActivity
Start-Sleep -Seconds 3
Write-Host "✅ App launched!" -ForegroundColor Green
Write-Host ""

# Step 4: Wait for user to login
Write-Host "[4/5] Đợi user login..." -ForegroundColor Yellow
Write-Host "👉 Vui lòng login với:" -ForegroundColor Cyan
Write-Host "   Email: nguyenvana@gmail.com" -ForegroundColor White
Write-Host "   Password: 123456" -ForegroundColor White
Write-Host ""
Write-Host "Nhấn ENTER sau khi đã login xong..." -ForegroundColor Yellow
Read-Host

# Step 5: Open PopulateDataActivity
Write-Host "[5/5] Mở PopulateDataActivity..." -ForegroundColor Yellow
adb shell am start -n com.example.healthylifehub/.debug.PopulateDataActivity
Start-Sleep -Seconds 2
Write-Host "✅ PopulateDataActivity đã mở!" -ForegroundColor Green
Write-Host ""

# Final instructions
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  HOÀN THÀNH!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "👉 Bây giờ trên màn hình điện thoại:" -ForegroundColor Yellow
Write-Host "   1. Nhấn nút 'TẠO DATA NGAY'" -ForegroundColor White
Write-Host "   2. Đợi vài giây" -ForegroundColor White
Write-Host "   3. Thấy '✅ HOÀN THÀNH!'" -ForegroundColor White
Write-Host "   4. Quay lại app và xem Health Metrics" -ForegroundColor White
Write-Host ""
Write-Host "📊 Sẽ có ~291 bản ghi health metrics!" -ForegroundColor Green
Write-Host ""
