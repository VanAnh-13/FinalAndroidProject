# HealthyLife Hub - Bug Report

## Mục đích
Repository này dùng để theo dõi và báo cáo các lỗi (bugs) trong dự án **HealthyLife Hub**. Mỗi bug được mô tả chi tiết kèm theo hình ảnh hoặc video minh chứng để dễ dàng tái hiện và khắc phục.

---

## Danh sách Bug

### Bug #1: Nav Tab không đồng bộ với Sidebar

**Trạng thái:** 🔴 Chưa khắc phục

**Mô tả:**
Nav tab phía dưới app không navigate theo sidebar. Khi người dùng chọn một mục trong sidebar, nav tab ở phía dưới màn hình không cập nhật trạng thái tương ứng, gây ra trải nghiệm không nhất quán trong navigation.

**Môi trường:**
- Platform: Mobile App
- Version: [Chưa cập nhật]
- Device: [Chưa cập nhật]

**Video minh chứng:**

https://github.com/user-attachments/assets/20251118-1606-56.1014639.mp4

**Các bước tái hiện:**
1. Mở ứng dụng HealthyLife Hub
2. Sử dụng sidebar để chuyển đổi giữa các màn hình
3. Quan sát nav tab phía dưới không thay đổi trạng thái active

**Kết quả mong đợi:**
Nav tab phía dưới nên highlight đúng mục tương ứng khi navigation thông qua sidebar.

**Kết quả thực tế:**
Nav tab không đồng bộ với sidebar, không cập nhật trạng thái active.

**Độ ưu tiên:** High

**Ngày phát hiện:** 18/11/2025

---

### Bug #02: Chỉ số huyết áp bị lỗi

**Trạng thái:** 🔴 Chưa khắc phục

**Mô tả:**
Cái chỉ số về huyết áp bị lỗi không tính toán được các chỉ số hiện tại, trung bình, cao nhất, thấp nhất, đang mặc định là 0. Chắc vì nó có 2 loại chỉ số là tâm thu và tâm trương nên nó không convert được ra số hay j đó.

**Môi trường:**
- Platform: Mobile App (Android)
- Version: [Chưa cập nhật]
- Device: [Chưa cập nhật]

**Hình ảnh minh chứng:**

![Bug huyết áp - Screenshot 1](./Screenshot%202025-11-18%20235535.png)

**Kết quả mong đợi:**
Hiển thị đúng các chỉ số: hiện tại, trung bình, cao nhất, thấp nhất dựa trên dữ liệu tâm thu/tâm trương.

**Kết quả thực tế:**
Tất cả các chỉ số đều hiển thị là 0.

**Độ ưu tiên:** High

**Ngày phát hiện:** 18/11/2025

---

### Bug #03: Xuất file báo cáo bị lỗi

**Trạng thái:** 🔴 Chưa khắc phục

**Mô tả:**
Chức năng xuất file báo cáo (PDF/Excel) gặp lỗi, không thể tạo hoặc lưu file báo cáo.

**Môi trường:**
- Platform: Mobile App (Android)
- Version: [Chưa cập nhật]
- Device: [Chưa cập nhật]

**Hình ảnh minh chứng:**

![Bug xuất báo cáo - Screenshot 2](./Screenshot%202025-11-18%20235653.png)

**Kết quả mong đợi:**
File báo cáo được tạo và lưu thành công vào thiết bị.

**Kết quả thực tế:**
Lỗi khi xuất file, không thể hoàn thành.

**Độ ưu tiên:** Medium

**Ngày phát hiện:** 18/11/2025

---

### Bug #04: Lỗi layout nhắc nhở 

**Trạng thái:** 🔴 Chưa khắc phục

**Mô tả:**
Layout của màn hình nhắc nhở bị lỗi hiển thị, các thành phần UI không được sắp xếp đúng vị trí hoặc bị chồng lên nhau.

**Môi trường:**
- Platform: Mobile App (Android)
- Version: [Chưa cập nhật]
- Device: [Chưa cập nhật]

**Hình ảnh minh chứng:**

![Bug layout nhắc nhở - Screenshot 3](./Screenshot%202025-11-19%20001338.png)

**Kết quả mong đợi:**
Layout hiển thị đúng, các thành phần UI được sắp xếp hợp lý.

**Kết quả thực tế:**
Layout bị lỗi, UI không hiển thị đúng.

**Độ ưu tiên:** Medium

**Ngày phát hiện:** 19/11/2025



---



## Template cho Bug mới

### Bug #[Số thứ tự]: [Tiêu đề ngắn gọn]

**Trạng thái:** 🔴 Chưa khắc phục / 🟡 Đang xử lý / 🟢 Đã khắc phục

**Mô tả:**
[Mô tả chi tiết về lỗi]

**Môi trường:**
- Platform:
- Version:
- Device:

**Hình ảnh/Video minh chứng:**
[Đính kèm ảnh hoặc video]

**Các bước tái hiện:**
1. [Bước 1]
2. [Bước 2]
3. [Bước 3]

**Kết quả mong đợi:**
[Mô tả hành vi đúng]

**Kết quả thực tế:**
[Mô tả hành vi sai]

**Độ ưu tiên:** Low / Medium / High / Critical

**Ngày phát hiện:** [DD/MM/YYYY]

---

## Hướng dẫn sử dụng

1. **Báo cáo bug mới:** Copy template bên trên và điền đầy đủ thông tin
2. **Cập nhật trạng thái:** Thay đổi emoji trạng thái khi bug được xử lý
3. **Đính kèm media:** Upload hình ảnh/video vào thư mục `/assets/bug-reports/` và link vào báo cáo
4. **Đánh số thứ tự:** Tiếp tục đánh số bug từ số cuối cùng + 1

## Thống kê

- 🔴 Chưa khắc phục: 4
- 🟡 Đang xử lý: 0
- 🟢 Đã khắc phục: 0
- **Tổng số bug:** 4

---

## Ghi chú về Images

Các file ảnh minh chứng được lưu trong cùng thư mục với file bug-report.md:
- `Screenshot 2025-11-18 235535.png` - Bug #02: Chỉ số huyết áp
- `Screenshot 2025-11-18 235653.png` - Bug #03: Xuất báo cáo
- `Screenshot 2025-11-19 001338.png` - Bug #04: Layout nhắc nhở

**Lưu ý khi push lên GitHub:**
- Đảm bảo push cả thư mục `assets/bug-report/bug-report-18.11/` với tất cả files
- Images sẽ hiển thị tự động trên GitHub khi dùng relative path
- Format: `![Alt text](./filename.png)` hoặc `![Alt text](filename.png)`
