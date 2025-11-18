# Fix Large File Error - GitHub Push

## ❌ Problem

```
error: File assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4 is 113.43 MB
error: this exceeds GitHub's file size limit of 100.00 MB
```

## 🔍 Root Cause

File video 113MB đã được commit trong history, vượt quá giới hạn 100MB của GitHub.

## ✅ Solutions

### Option 1: Remove file và recommit (RECOMMENDED)

```bash
# 1. Reset commit cuối (đã làm)
git reset --soft HEAD~1

# 2. Remove file khỏi staging
git reset HEAD "assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4"

# 3. Delete file physically (nếu còn)
rm "assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4"

# 4. Add .gitignore (đã tạo)
git add .gitignore

# 5. Commit lại WITHOUT video file
git add .
git commit -m "added features: search, filter metrics, smart notify reminders, export report (bug), improve UX, policy & app info

Note: Large video file excluded (>100MB GitHub limit)"

# 6. Push
git push origin develop
```

### Option 2: Use Git LFS (nếu cần keep video)

```bash
# 1. Install Git LFS
git lfs install

# 2. Track mp4 files
git lfs track "*.mp4"

# 3. Add .gitattributes
git add .gitattributes

# 4. Add video file
git add "assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4"

# 5. Commit
git commit -m "Add video with Git LFS"

# 6. Push
git push origin develop
```

### Option 3: Upload video elsewhere

**Recommended platforms:**
- YouTube (unlisted)
- Google Drive
- Dropbox
- GitHub Releases (for releases only)

**Then link in markdown:**
```markdown
**Video minh chứng:**
[Watch video](https://youtube.com/watch?v=...)
```

## 🚀 Quick Fix (Recommended)

```bash
# Đơn giản nhất: Không commit video file
git add .
git commit -m "added features without large video file"
git push origin develop
```

## 📝 Update bug-report.md

Thay video file bằng link external:

```markdown
### Bug #01: Nav Tab không đồng bộ với Sidebar

**Video minh chứng:**
[Watch on YouTube](https://youtube.com/...)
# Hoặc
[Download from Google Drive](https://drive.google.com/...)
```

## ⚠️ If Already Pushed

Nếu đã push commit có large file lên remote:

```bash
# 1. Remove from history
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch 'assets/bug-report/bug-report-18.11/Recording 2025-11-14 134644.mp4'" \
  --prune-empty --tag-name-filter cat -- --all

# 2. Force push
git push origin develop --force

# ⚠️ WARNING: Force push sẽ rewrite history!
# Chỉ làm nếu bạn là người duy nhất work trên branch này
```

## 📊 File Size Limits

| Platform | Limit | Notes |
|----------|-------|-------|
| GitHub | 100 MB | Hard limit |
| Git LFS | 2 GB | Per file |
| GitHub Releases | 2 GB | Per file |

## ✅ Current Status

- ✅ .gitignore updated (exclude *.mp4)
- ✅ Commit reset (ready to recommit)
- ⏳ Need to: Commit without video file
- ⏳ Need to: Push to GitHub

## 🎯 Next Steps

1. **Commit without video:**
   ```bash
   git add .
   git commit -m "added features (excluding large video)"
   git push origin develop
   ```

2. **Upload video separately:**
   - Upload to YouTube/Drive
   - Update bug-report.md with link

3. **Verify:**
   - Check GitHub repo
   - Verify images display correctly
