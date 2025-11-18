# Requirements Document

## Introduction

Sửa lỗi biểu đồ huyết áp không hiển thị và các thông số thống kê (trung bình, cao nhất, thấp nhất) không được tính toán đúng trong ứng dụng HealthyLife Hub. Vấn đề xảy ra do huyết áp có cấu trúc dữ liệu đặc biệt với hai giá trị: tâm thu (systolic) và tâm trương (diastolic), khác với các chỉ số khác chỉ có một giá trị đơn.

## Requirements

### Requirement 1

**User Story:** Là người dùng, tôi muốn xem biểu đồ huyết áp hiển thị chính xác theo thời gian (ngày, tuần, tháng, năm), để tôi có thể theo dõi xu hướng huyết áp của mình.

#### Acceptance Criteria

1. WHEN người dùng chọn xem biểu đồ huyết áp THEN hệ thống SHALL hiển thị biểu đồ với cả hai đường: tâm thu và tâm trương
2. WHEN người dùng chọn khoảng thời gian (ngày, tuần, tháng, năm) THEN hệ thống SHALL hiển thị dữ liệu huyết áp tương ứng với khoảng thời gian đó
3. WHEN có dữ liệu huyết áp THEN biểu đồ SHALL hiển thị hai đường riêng biệt với màu sắc khác nhau cho tâm thu và tâm trương
4. WHEN không có dữ liệu huyết áp THEN hệ thống SHALL hiển thị thông báo "Chưa có dữ liệu huyết áp"

### Requirement 2

**User Story:** Là người dùng, tôi muốn xem các thông số thống kê huyết áp (trung bình, cao nhất, thấp nhất) được tính toán chính xác, để tôi có thể đánh giá tình trạng sức khỏe của mình.

#### Acceptance Criteria

1. WHEN có dữ liệu huyết áp THEN hệ thống SHALL tính toán và hiển thị giá trị trung bình cho cả tâm thu và tâm trương
2. WHEN có dữ liệu huyết áp THEN hệ thống SHALL tính toán và hiển thị giá trị cao nhất cho cả tâm thu và tâm trương
3. WHEN có dữ liệu huyết áp THEN hệ thống SHALL tính toán và hiển thị giá trị thấp nhất cho cả tâm thu và tâm trương
4. WHEN không có dữ liệu huyết áp THEN các thông số thống kê SHALL hiển thị "N/A" hoặc ẩn đi
5. WHEN dữ liệu huyết áp được cập nhật THEN các thông số thống kê SHALL được tính toán lại tự động

### Requirement 3

**User Story:** Là người dùng, tôi muốn dữ liệu huyết áp được parse và xử lý chính xác từ cơ sở dữ liệu, để đảm bảo tính chính xác của biểu đồ và thống kê.

#### Acceptance Criteria

1. WHEN hệ thống đọc dữ liệu huyết áp từ Firestore THEN hệ thống SHALL parse chính xác cả giá trị tâm thu và tâm trương
2. WHEN dữ liệu huyết áp có format "120/80" THEN hệ thống SHALL tách thành systolic=120 và diastolic=80
3. IF dữ liệu huyết áp không đúng format THEN hệ thống SHALL bỏ qua record đó và ghi log lỗi
4. WHEN xử lý dữ liệu cho biểu đồ THEN hệ thống SHALL tạo hai series dữ liệu riêng biệt cho tâm thu và tâm trương
5. WHEN tính toán thống kê THEN hệ thống SHALL xử lý riêng biệt cho từng loại giá trị (tâm thu/tâm trương)

### Requirement 4

**User Story:** Là người dùng, tôi muốn giao diện biểu đồ huyết áp có legend và nhãn rõ ràng, để tôi có thể phân biệt được đường tâm thu và tâm trương.

#### Acceptance Criteria

1. WHEN biểu đồ huyết áp được hiển thị THEN hệ thống SHALL hiển thị legend với nhãn "Tâm thu" và "Tâm trương"
2. WHEN biểu đồ huyết áp được hiển thị THEN đường tâm thu SHALL có màu đỏ và đường tâm trương SHALL có màu xanh
3. WHEN người dùng touch vào điểm dữ liệu THEN hệ thống SHALL hiển thị tooltip với giá trị cụ thể và thời gian
4. WHEN biểu đồ được zoom hoặc pan THEN cả hai đường SHALL được đồng bộ di chuyển