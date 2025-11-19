# Microsoft 365 Redirect Feature

## Tổng quan
Tính năng này tự động redirect người dùng đến Microsoft 365 Online khi thiết bị không có ứng dụng Excel để mở file báo cáo.

## Chức năng

### Khi người dùng mở file Excel:
1. **Có ứng dụng Excel**: Mở file trực tiếp bằng ứng dụng
2. **Không có ứng dụng Excel**: Hiển thị dialog với 3 tùy chọn:
   - **Mở trên Microsoft 365**: Redirect đến Excel Online (https://www.office.com/launch/excel)
   - **Tải Excel App**: Mở Google Play Store để tải Microsoft Excel
   - **Chia sẻ**: Chia sẻ file để mở bằng ứng dụng khác

## Files đã thay đổi

### 1. ReportManager.java
**Đường dẫn**: `app/src/main/java/com/example/healthylifehub/utils/report/ReportManager.java`

**Các phương thức mới**:
- `isExcelFile(File file)` - Kiểm tra xem file có phải Excel không
- `showMicrosoft365RedirectDialog(File file)` - Hiển thị dialog tùy chọn
- `openInMicrosoft365Online()` - Mở Microsoft 365 Online trong trình duyệt
- `openPlayStoreForExcel()` - Mở Google Play Store để tải Excel app
- `showFileOpenChooser(Intent openIntent)` - Hiển thị chooser để chọn ứng dụng

**Phương thức đã cập nhật**:
- `openReport(File file)` - Thêm logic kiểm tra và redirect
- `shareReport(File file)` - Sử dụng string resources

### 2. String Resources

#### values/strings.xml (Tiếng Việt)
```xml
<string name="open_excel_file">Mở file Excel</string>
<string name="no_excel_app_message">Không tìm thấy ứng dụng Excel trên thiết bị.\n\nBạn có muốn:\n1. Mở file trên Microsoft 365 Online (Excel Online)\n2. Tải ứng dụng Excel từ Google Play Store\n3. Chia sẻ file để mở bằng ứng dụng khác</string>
<string name="open_microsoft_365">Mở trên Microsoft 365</string>
<string name="download_excel_app">Tải Excel App</string>
<string name="share">Chia sẻ</string>
<string name="opening_microsoft_365">Đang mở Microsoft 365 Online.\nBạn có thể tải file lên để xem và chỉnh sửa.</string>
<string name="cannot_open_browser">Không thể mở trình duyệt. Vui lòng truy cập: https://www.office.com</string>
<string name="cannot_open_play_store">Không thể mở Google Play Store</string>
<string name="share_report">Chia sẻ báo cáo</string>
<string name="error_sharing_report">Lỗi khi chia sẻ: %s</string>
<string name="error_file_path">Lỗi đường dẫn file: %s</string>
<string name="error_no_app_to_open">Không tìm thấy ứng dụng để mở file</string>
<string name="error_cannot_open_file">Không thể mở file: %s</string>
<string name="no_app_for_pdf">Không có ứng dụng để mở file. Vui lòng tải xuống từ Google Play Store:\n- PDF: Adobe Reader, Google PDF Viewer</string>
```

#### values-en/strings.xml (English)
```xml
<string name="open_excel_file">Open Excel File</string>
<string name="no_excel_app_message">No Excel app found on device.\n\nWould you like to:\n1. Open file on Microsoft 365 Online (Excel Online)\n2. Download Excel app from Google Play Store\n3. Share file to open with another app</string>
<string name="open_microsoft_365">Open on Microsoft 365</string>
<string name="download_excel_app">Download Excel App</string>
<string name="share">Share</string>
<string name="opening_microsoft_365">Opening Microsoft 365 Online.\nYou can upload the file to view and edit.</string>
<string name="cannot_open_browser">Cannot open browser. Please visit: https://www.office.com</string>
<string name="cannot_open_play_store">Cannot open Google Play Store</string>
<string name="share_report">Share Report</string>
<string name="error_sharing_report">Error sharing: %s</string>
<string name="error_file_path">File path error: %s</string>
<string name="error_no_app_to_open">No app found to open file</string>
<string name="error_cannot_open_file">Cannot open file: %s</string>
<string name="no_app_for_pdf">No app to open file. Please download from Google Play Store:\n- PDF: Adobe Reader, Google PDF Viewer</string>
```

## Luồng hoạt động

```
User clicks "Open Report"
        ↓
Check if Excel app exists
        ↓
    ┌───┴───┐
    │       │
   YES     NO
    │       │
    │       └→ Is Excel file?
    │              ↓
    │          ┌───┴───┐
    │          │       │
    │         YES     NO
    │          │       │
    │          │       └→ Show file chooser
    │          │
    │          └→ Show Microsoft 365 Dialog
    │                  ↓
    │              User selects:
    │              1. Open on Microsoft 365
    │              2. Download Excel App
    │              3. Share file
    │
    └→ Open file directly
```

## Hướng dẫn sử dụng

### Cho người dùng:
1. Tạo báo cáo Excel trong app
2. Nhấn "Mở" để xem báo cáo
3. Nếu không có Excel app:
   - Chọn "Mở trên Microsoft 365" để xem online
   - Chọn "Tải Excel App" để cài đặt Microsoft Excel
   - Chọn "Chia sẻ" để gửi file qua email/app khác

### Cho developer:
```java
// Sử dụng ReportManager để mở file
ReportManager reportManager = new ReportManager(context);
reportManager.openReport(excelFile);

// Tự động xử lý:
// - Mở trực tiếp nếu có app
// - Hiển thị dialog Microsoft 365 nếu không có app
```

## Lợi ích

1. **Trải nghiệm người dùng tốt hơn**: Không bị stuck khi không có Excel app
2. **Đa ngôn ngữ**: Tất cả messages theo ngôn ngữ hệ thống
3. **Linh hoạt**: Người dùng có nhiều lựa chọn
4. **Dễ bảo trì**: Sử dụng string resources thay vì hardcode

## Testing

### Test Case 1: Có Excel app
1. Cài đặt Microsoft Excel hoặc Google Sheets
2. Tạo báo cáo Excel
3. Nhấn "Mở"
4. **Kết quả**: File mở trực tiếp trong Excel app

### Test Case 2: Không có Excel app
1. Gỡ cài đặt tất cả Excel apps
2. Tạo báo cáo Excel
3. Nhấn "Mở"
4. **Kết quả**: Hiển thị dialog với 3 tùy chọn

### Test Case 3: Chọn Microsoft 365
1. Thực hiện Test Case 2
2. Chọn "Mở trên Microsoft 365"
3. **Kết quả**: Trình duyệt mở https://www.office.com/launch/excel

### Test Case 4: Chọn Download Excel
1. Thực hiện Test Case 2
2. Chọn "Tải Excel App"
3. **Kết quả**: Google Play Store mở trang Microsoft Excel

### Test Case 5: Chọn Share
1. Thực hiện Test Case 2
2. Chọn "Chia sẻ"
3. **Kết quả**: Hiển thị share sheet để chia sẻ file

### Test Case 6: Đa ngôn ngữ
1. Đổi ngôn ngữ hệ thống sang English
2. Thực hiện Test Case 2
3. **Kết quả**: Dialog hiển thị bằng tiếng Anh

## Ghi chú kỹ thuật

- Sử dụng `Intent.ACTION_VIEW` để mở file
- Sử dụng `FileProvider` để chia sẻ file an toàn
- Kiểm tra `resolveActivity()` để xác định có app hay không
- Tất cả Toast messages sử dụng string resources
- Hỗ trợ cả `.xlsx` và `.xls` files
- Graceful fallback khi không thể mở trình duyệt

## Links tham khảo

- Microsoft 365 Online: https://www.office.com
- Excel Online: https://www.office.com/launch/excel
- Microsoft Excel App: https://play.google.com/store/apps/details?id=com.microsoft.office.excel
