# Quick Fix: Remove Large File from Git

## ❌ Problem
File video 113MB vẫn trong Git history, không thể push.

## ✅ FASTEST Solution (Recommended)

### Option A: Create New Clean Branch

```bash
# 1. Create new branch from current state (WITHOUT video in working tree)
git checkout -b develop-clean

# 2. Push new branch
git push origin develop-clean

# 3. On GitHub: Make develop-clean the default branch
# Then delete old develop branch

# 4. Rename local branch
git branch -m develop-clean develop
```

**Pros:** 
- ✅ Nhanh nhất (30 giây)
- ✅ Không cần rewrite history
- ✅ An toàn

**Cons:**
- ⚠️ Mất commit history cũ (nhưng code vẫn giữ nguyên)

### Option B: Rewrite History (Slower)

```powershell
# Run the script
.\remove-large-file.ps1

# Then force push
git push origin develop --force
```

**Pros:**
- ✅ Giữ nguyên commit history

**Cons:**
- ⚠️ Mất 5-10 phút
- ⚠️ Cần force push (nguy hiểm nếu có người khác đang work)

## 🚀 RECOMMENDED: Do Option A

```bash
# Quick commands
git checkout -b develop-clean
git push origin develop-clean -u

# Done! Now you can work on develop-clean branch
```

## 📝 Why This Happened

File video được commit trong một commit trước đó. Ngay cả khi xóa file trong commit mới, Git vẫn giữ nó trong history.

## 🎯 Prevention

.gitignore đã được update để ignore *.mp4 files. Từ giờ sẽ không bị nữa.

## ⚡ Execute Now

Choose one:

**A. New Branch (30 seconds):**
```bash
git checkout -b develop-clean
git push origin develop-clean
```

**B. Rewrite History (5-10 minutes):**
```powershell
.\remove-large-file.ps1
git push origin develop --force
```

Pick A if you want to push NOW. Pick B if you want to keep history.
