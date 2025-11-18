# Script to check Android logcat for HealthyLifeHub app
$adb = "C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Checking connected devices..." -ForegroundColor Cyan
& $adb devices

Write-Host "`nClearing logcat buffer..." -ForegroundColor Cyan
& $adb logcat -c

Write-Host "`nStarting app..." -ForegroundColor Cyan
& $adb shell am start -n com.example.healthylifehub/.MainActivity

Write-Host "`nWaiting for app to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "`nCapturing logcat (filtering for errors and app logs)..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Yellow

# Filter for app package and errors
& $adb logcat -v time | Select-String -Pattern "healthylifehub|AndroidRuntime|FATAL|ERROR" -Context 0,2
