# PowerShell script to push bug report with images to GitHub
# Usage: .\push-bug-report.ps1

Write-Host "🐛 Pushing Bug Report to GitHub..." -ForegroundColor Cyan
Write-Host ""

# Add all bug report files
Write-Host "📁 Adding files to Git..." -ForegroundColor Yellow
git add assets/bug-report/

# Show what will be committed
Write-Host ""
Write-Host "📋 Files to be committed:" -ForegroundColor Yellow
git status --short assets/bug-report/

# Confirm before commit
Write-Host ""
$confirm = Read-Host "Continue with commit? (y/n)"

if ($confirm -eq 'y' -or $confirm -eq 'Y') {
    # Commit
    Write-Host "💾 Committing..." -ForegroundColor Yellow
    git commit -m "docs: Add bug report with screenshots

- Bug #01: Nav Tab không đồng bộ với Sidebar
- Bug #02: Chỉ số huyết áp bị lỗi (with screenshot)
- Bug #03: Xuất file báo cáo bị lỗi (with screenshot)
- Bug #04: Lỗi layout nhắc nhở (with screenshot)

Includes 3 screenshot images for bugs #02, #03, #04"

    # Push
    Write-Host "🚀 Pushing to GitHub..." -ForegroundColor Yellow
    git push origin main
    
    Write-Host ""
    Write-Host "✅ Done! Check your GitHub repository to verify images display correctly." -ForegroundColor Green
    Write-Host "📍 URL: https://github.com/YOUR_USERNAME/YOUR_REPO/tree/main/assets/bug-report" -ForegroundColor Cyan
} else {
    Write-Host "❌ Cancelled" -ForegroundColor Red
    exit 1
}
