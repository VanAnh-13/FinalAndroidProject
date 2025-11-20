# Reinstall app script - Uninstall old version and install new one

Write-Host "=== Reinstalling HealthyLife Hub ===" -ForegroundColor Cyan

# Find adb
$adb = $null
$possiblePaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:ANDROID_HOME\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe",
    "adb"
)

foreach ($path in $possiblePaths) {
    if (Test-Path $path -ErrorAction SilentlyContinue) {
        $adb = $path
        break
    }
    if ($path -eq "adb") {
        try {
            $null = & adb version 2>&1
            $adb = "adb"
            break
        } catch {}
    }
}

if (-not $adb) {
    Write-Host "❌ ADB not found. Please install Android SDK Platform Tools" -ForegroundColor Red
    Write-Host "Using gradlew installDebug instead..." -ForegroundColor Yellow
    & ./gradlew installDebug
    exit
}

Write-Host "✓ Found ADB: $adb" -ForegroundColor Green

# Check if device is connected
Write-Host "`nChecking for connected devices..." -ForegroundColor Yellow
$devices = & $adb devices
if ($devices -match "device$") {
    Write-Host "✓ Device connected" -ForegroundColor Green
} else {
    Write-Host "❌ No device connected. Please connect a device or start emulator" -ForegroundColor Red
    exit 1
}

# Uninstall old version
Write-Host "`nUninstalling old version..." -ForegroundColor Yellow
& $adb uninstall com.example.healthylifehub 2>&1 | Out-Null
Write-Host "✓ Old version uninstalled (if existed)" -ForegroundColor Green

# Build and install new version
Write-Host "`nBuilding and installing new version..." -ForegroundColor Yellow
& ./gradlew installDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ App reinstalled successfully!" -ForegroundColor Green
    Write-Host "`nYou can now launch the app on your device" -ForegroundColor Cyan
} else {
    Write-Host "`n❌ Installation failed" -ForegroundColor Red
    exit 1
}
