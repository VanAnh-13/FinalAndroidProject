#!/bin/bash

# Script to push bug report with images to GitHub
# Usage: ./push-bug-report.sh

echo "🐛 Pushing Bug Report to GitHub..."
echo ""

# Add all bug report files
echo "📁 Adding files to Git..."
git add assets/bug-report/

# Show what will be committed
echo ""
echo "📋 Files to be committed:"
git status --short assets/bug-report/

# Confirm before commit
echo ""
read -p "Continue with commit? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]
then
    # Commit
    echo "💾 Committing..."
    git commit -m "docs: Add bug report with screenshots

- Bug #01: Nav Tab không đồng bộ với Sidebar
- Bug #02: Chỉ số huyết áp bị lỗi (with screenshot)
- Bug #03: Xuất file báo cáo bị lỗi (with screenshot)
- Bug #04: Lỗi layout nhắc nhở (with screenshot)

Includes 3 screenshot images for bugs #02, #03, #04"

    # Push
    echo "🚀 Pushing to GitHub..."
    git push origin main
    
    echo ""
    echo "✅ Done! Check your GitHub repository to verify images display correctly."
    echo "📍 URL: https://github.com/YOUR_USERNAME/YOUR_REPO/tree/main/assets/bug-report"
else
    echo "❌ Cancelled"
    exit 1
fi
