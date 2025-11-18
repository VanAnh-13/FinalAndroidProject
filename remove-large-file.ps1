# Quick fix: Remove large file from Git history
# This uses git filter-repo (faster than filter-branch)

Write-Host "🔧 Removing large file from Git history..." -ForegroundColor Yellow
Write-Host ""

# Option 1: Simple - just remove the file path from all commits
$filePath = "assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4"

Write-Host "Removing: $filePath" -ForegroundColor Cyan

# Use git filter-branch (built-in, no install needed)
git filter-branch --force --index-filter `
  "git rm --cached --ignore-unmatch '$filePath'" `
  --prune-empty --tag-name-filter cat -- --all

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ File removed from history!" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  Now you need to force push:" -ForegroundColor Yellow
    Write-Host "   git push origin develop --force" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "⚠️  WARNING: This will rewrite history!" -ForegroundColor Red
    Write-Host "   Only do this if you're the only one working on this branch" -ForegroundColor Red
} else {
    Write-Host ""
    Write-Host "❌ Failed to remove file" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative: Delete the file manually and create new branch" -ForegroundColor Yellow
    Write-Host "   git checkout -b develop-clean" -ForegroundColor Cyan
    Write-Host "   git push origin develop-clean" -ForegroundColor Cyan
}
