#!/bin/bash

# Fix large file in Git history
echo "🔧 Fixing large file in Git history..."

# Use BFG or git filter-branch to remove large file
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch 'assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4'" \
  --prune-empty --tag-name-filter cat -- --all

echo "✅ Large file removed from history"
echo "⚠️  Now run: git push origin develop --force"
