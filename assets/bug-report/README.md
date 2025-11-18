# Bug Reports - HealthyLife Hub

## 📁 Cấu trúc thư mục

```
assets/bug-report/
├── README.md (file này)
└── bug-report-18.11/
    ├── bug-report.md          # File báo cáo bugs
    ├── Screenshot 2025-11-18 235535.png  # Bug #02: Huyết áp
    ├── Screenshot 2025-11-18 235653.png  # Bug #03: Xuất báo cáo
    └── Screenshot 2025-11-19 001338.png  # Bug #04: Layout nhắc nhở
```

## 🚀 Hướng dẫn push lên GitHub

### Bước 1: Add files vào Git

```bash
# Add toàn bộ thư mục bug-report
git add assets/bug-report/

# Hoặc add từng file cụ thể
git add assets/bug-report/bug-report-18.11/bug-report.md
git add "assets/bug-report/bug-report-18.11/Screenshot 2025-11-18 235535.png"
git add "assets/bug-report/bug-report-18.11/Screenshot 2025-11-18 235653.png"
git add "assets/bug-report/bug-report-18.11/Screenshot 2025-11-19 001338.png"
```

### Bước 2: Commit

```bash
git commit -m "docs: Add bug report with screenshots for bugs #02, #03, #04"
```

### Bước 3: Push lên GitHub

```bash
git push origin main
# Hoặc nếu branch khác
git push origin <branch-name>
```

## 📸 Cách images hiển thị trên GitHub

Khi push lên GitHub, images sẽ tự động hiển thị trong file markdown vì:

1. **Relative Path**: Dùng `./filename.png` để link đến file trong cùng thư mục
2. **URL Encoding**: Spaces trong tên file được encode thành `%20`
3. **GitHub Rendering**: GitHub tự động render markdown và hiển thị images

### Ví dụ trong markdown:

```markdown
![Alt text](./Screenshot%202025-11-18%20235535.png)
```

Sẽ hiển thị như:

![Bug huyết áp](./bug-report-18.11/Screenshot%202025-11-18%20235535.png)

## ✅ Checklist trước khi push

- [ ] Tất cả files đã được add vào Git
- [ ] File paths trong markdown đúng (relative path)
- [ ] Tên files có spaces đã được encode `%20`
- [ ] Commit message rõ ràng
- [ ] Test xem images có hiển thị trên GitHub không

## 🔍 Verify sau khi push

1. Vào repository trên GitHub
2. Navigate đến `assets/bug-report/bug-report-18.11/`
3. Click vào `bug-report.md`
4. Kiểm tra images có hiển thị không

## 📝 Lưu ý

- **File size**: GitHub có giới hạn 100MB/file. Images hiện tại đều < 1MB nên OK
- **Git LFS**: Không cần dùng Git LFS cho images nhỏ này
- **Binary files**: Git sẽ track images như binary files
- **.gitignore**: Đảm bảo thư mục `assets/` không bị ignore

## 🐛 Troubleshooting

### Images không hiển thị trên GitHub?

1. **Check file path**: Đảm bảo relative path đúng
2. **Check file name**: Spaces phải encode thành `%20`
3. **Check file exists**: Verify file đã được push lên
4. **Check permissions**: File phải có quyền public trong repo

### Example fix:

```markdown
# ❌ Sai
![Bug](Screenshot 2025-11-18 235535.png)

# ✅ Đúng
![Bug](./Screenshot%202025-11-18%20235535.png)
```

## 📞 Support

Nếu gặp vấn đề, check:
- [GitHub Markdown Guide](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)
- [GitHub Images in Markdown](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax#images)
