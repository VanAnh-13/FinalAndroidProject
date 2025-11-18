# Bug Report GitHub Integration Guide

## ✅ Đã hoàn thành

Đã update bug-report.md với:
- ✅ Link 3 images đúng format cho GitHub
- ✅ Mô tả đầy đủ cho bugs #02, #03, #04
- ✅ Cập nhật thống kê: 4 bugs total
- ✅ Tạo README.md hướng dẫn

## 📁 Files Structure

```
assets/bug-report/
├── README.md                              # Hướng dẫn chi tiết
└── bug-report-18.11/
    ├── bug-report.md                      # ✅ Updated với image links
    ├── Screenshot 2025-11-18 235535.png   # Bug #02
    ├── Screenshot 2025-11-18 235653.png   # Bug #03
    └── Screenshot 2025-11-19 001338.png   # Bug #04
```

## 🖼️ Image Links Format

Trong bug-report.md, images được link như sau:

```markdown
![Alt text](./Screenshot%202025-11-18%20235535.png)
```

**Lưu ý:**
- Dùng relative path `./`
- Spaces trong filename → `%20`
- GitHub sẽ tự động render images

## 🚀 Cách Push lên GitHub

### Option 1: Dùng Script (Recommended)

**Windows (PowerShell):**
```powershell
.\push-bug-report.ps1
```

**Linux/Mac (Bash):**
```bash
chmod +x push-bug-report.sh
./push-bug-report.sh
```

### Option 2: Manual Commands

```bash
# 1. Add files
git add assets/bug-report/

# 2. Commit
git commit -m "docs: Add bug report with screenshots"

# 3. Push
git push origin main
```

## ✅ Verify trên GitHub

Sau khi push, check:

1. Navigate đến: `https://github.com/YOUR_USERNAME/YOUR_REPO/tree/main/assets/bug-report/bug-report-18.11`

2. Click vào `bug-report.md`

3. Verify 3 images hiển thị:
   - Bug #02: Chỉ số huyết áp
   - Bug #03: Xuất báo cáo  
   - Bug #04: Layout nhắc nhở

## 📊 Bug Summary

| Bug # | Title | Status | Priority | Screenshot |
|-------|-------|--------|----------|------------|
| #01 | Nav Tab không đồng bộ | 🔴 Chưa fix | High | Video |
| #02 | Chỉ số huyết áp lỗi | 🔴 Chưa fix | High | ✅ Yes |
| #03 | Xuất báo cáo lỗi | 🔴 Chưa fix | Medium | ✅ Yes |
| #04 | Layout nhắc nhở lỗi | 🔴 Chưa fix | Medium | ✅ Yes |

## 🔧 Troubleshooting

### Images không hiển thị?

**Check 1: File path**
```markdown
# ❌ Wrong
![Bug](Screenshot 2025-11-18 235535.png)

# ✅ Correct  
![Bug](./Screenshot%202025-11-18%20235535.png)
```

**Check 2: Files đã push chưa?**
```bash
git ls-files assets/bug-report/
```

**Check 3: GitHub cache**
- Hard refresh: Ctrl+F5
- Hoặc đợi vài phút

## 📝 Notes

- File size: Tất cả images < 1MB → OK
- Git LFS: Không cần
- .gitignore: Đảm bảo `assets/` không bị ignore
- Binary tracking: Git tự động handle

## 🎯 Next Steps

1. Run script hoặc manual push
2. Verify trên GitHub
3. Share link với team
4. Update bug status khi fix

## 📞 Support Links

- [GitHub Markdown Guide](https://docs.github.com/en/get-started/writing-on-github)
- [Images in Markdown](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax#images)
