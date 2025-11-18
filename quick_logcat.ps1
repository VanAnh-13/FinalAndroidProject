# Quick logcat check for HealthyLifeHub
$adb = "C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Clearing old logs..." -ForegroundColor Cyan
& $adb logcat -c

Write-Host "Starting app..." -ForegroundColor Cyan
& $adb shell am start -n com.example.healthylifehub/.MainActivity

Write-Host "Capturing logs for 5 seconds..." -ForegroundColor Yellow
Start-Sleep -Seconds 1

# Capture 5 seconds of logs
$job = Start-Job -ScriptBlock {
    param($adbPath)
    & $adbPath logcat -d -v time
} -ArgumentList $adb

Wait-Job $job -Timeout 5 | Out-Null
$logs = Receive-Job $job
Stop-Job $job -ErrorAction SilentlyContinue
Remove-Job $job -ErrorAction SilentlyContinue

# Save to file
$logs | Out-File -FilePath "logcat_output.txt" -Encoding UTF8

# Filter and display errors
Write-Host "`n=== ERRORS AND CRASHES ===" -ForegroundColor Red
$logs | Select-String -Pattern "FATAL|AndroidRuntime|CRASH|Exception" | Select-Object -First 50

Write-Host "`n=== APP LOGS ===" -ForegroundColor Green
$logs | Select-String -Pattern "healthylifehub" | Select-Object -First 30

Write-Host "`nFull logs saved to: logcat_output.txt" -ForegroundColor Cyan
