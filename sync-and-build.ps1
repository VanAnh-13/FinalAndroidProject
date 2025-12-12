# Script để sync và build project Base

Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "🔄 SYNC AND BUILD PROJECT" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean
Write-Host "🧹 Cleaning project..." -ForegroundColor Yellow
.\gradlew clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Clean failed!" -ForegroundColor Red
    pause
    exit
}
Write-Host "✅ Clean successful" -ForegroundColor Green
Write-Host ""

# Step 2: Sync Gradle
Write-Host "🔄 Syncing Gradle..." -ForegroundColor Yellow
.\gradlew --refresh-dependencies
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Sync had warnings, but continuing..." -ForegroundColor Yellow
}
Write-Host "✅ Sync completed" -ForegroundColor Green
Write-Host ""

# Step 3: Build Debug APK
Write-Host "🔨 Building Debug APK..." -ForegroundColor Yellow
.\gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Write-Host "📖 Check the error messages above" -ForegroundColor Yellow
    pause
    exit
}
Write-Host "✅ Build successful!" -ForegroundColor Green
Write-Host ""

# Step 4: Show APK location
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "🎉 BUILD COMPLETE!" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""
Write-Host "📦 APK Location:" -ForegroundColor Yellow
Write-Host "   app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
Write-Host ""
Write-Host "📱 Next steps:" -ForegroundColor Yellow
Write-Host "   1. Install APK: adb install app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
Write-Host "   2. Or run from Android Studio" -ForegroundColor White
Write-Host "   3. Login with: nguyenvana@gmail.com / 123456" -ForegroundColor White
Write-Host ""
Write-Host "📖 See TEST_LOGIN.md for more details" -ForegroundColor Yellow
Write-Host ""

pause
