# Requirements Document

## Introduction

Nâng cấp hệ thống nhắc nhở hiện tại thành hệ thống nhắc nhở thông minh với khả năng tương tác qua notification, theo dõi tiến độ hoàn thành, và quản lý thời hạn. Hệ thống sẽ cho phép người dùng tương tác trực tiếp với notification để đánh dấu hoàn thành hoặc bỏ qua, đồng thời theo dõi tỷ lệ tuân thủ qua thanh tiến trình.

## Requirements

### Requirement 1: Interactive Notification System

**User Story:** Là một người dùng, tôi muốn có thể tương tác trực tiếp với notification nhắc nhở để đánh dấu hoàn thành hoặc bỏ qua mà không cần mở app.

#### Acceptance Criteria

1. WHEN nhắc nhở được kích hoạt THEN hệ thống SHALL gửi notification với 2 action buttons: "Hoàn thành" và "Bỏ qua"
2. WHEN người dùng nhấn "Hoàn thành" trên notification THEN hệ thống SHALL cập nhật trạng thái reminder thành completed và tăng progress
3. WHEN người dùng nhấn "Bỏ qua" trên notification THEN hệ thống SHALL đánh dấu lần nhắc nhở này là skipped và không tăng progress
4. WHEN người dùng tương tác với notification THEN notification SHALL tự động dismiss
5. WHEN action được thực hiện THEN hệ thống SHALL ghi lại timestamp và action type vào database

### Requirement 2: Reminder Deadline Management

**User Story:** Là một người dùng, tôi muốn thiết lập thời hạn cho nhắc nhở để hệ thống có thể tính toán tiến độ hoàn thành chính xác.

#### Acceptance Criteria

1. WHEN tạo hoặc chỉnh sửa reminder THEN hệ thống SHALL hiển thị option để chọn ngày hết hạn (deadline)
2. WHEN người dùng chọn deadline THEN hệ thống SHALL validate deadline phải sau ngày hiện tại
3. WHEN reminder có deadline THEN hệ thống SHALL tính toán tổng số lần nhắc nhở dự kiến từ start date đến deadline
4. WHEN deadline đã qua THEN hệ thống SHALL tự động deactivate reminder và hiển thị final progress
5. WHEN reminder gần hết hạn (còn 3 ngày) THEN hệ thống SHALL hiển thị warning indicator

### Requirement 3: Progress Tracking System

**User Story:** Là một người dùng, tôi muốn xem tiến độ hoàn thành nhắc nhở qua thanh tiến trình để theo dõi sự tuân thủ của mình.

#### Acceptance Criteria

1. WHEN reminder có frequency và deadline THEN hệ thống SHALL tính toán tổng số lần nhắc nhở expected
2. WHEN người dùng complete một lần nhắc nhở THEN hệ thống SHALL tăng completed count và cập nhật progress percentage
3. WHEN hiển thị reminder trong app THEN hệ thống SHALL hiển thị progress bar với percentage và text "X/Y hoàn thành (Z%)"
4. WHEN tính progress THEN công thức SHALL là: (completed_count / total_expected_count) * 100
5. WHEN reminder chưa có deadline THEN progress bar SHALL hiển thị "Không giới hạn thời gian"

### Requirement 4: Reminder History Tracking

**User Story:** Là một người dùng, tôi muốn xem lịch sử các lần nhắc nhở để biết khi nào tôi đã hoàn thành hoặc bỏ qua.

#### Acceptance Criteria

1. WHEN người dùng tương tác với reminder notification THEN hệ thống SHALL tạo ReminderHistory record
2. WHEN tạo history record THEN hệ thống SHALL lưu: reminderId, actionType (completed/skipped), timestamp, scheduledTime
3. WHEN người dùng xem chi tiết reminder THEN hệ thống SHALL hiển thị danh sách history với icon và timestamp
4. WHEN hiển thị history THEN completed actions SHALL có icon xanh, skipped actions SHALL có icon xám
5. WHEN tính progress THEN hệ thống SHALL chỉ đếm completed actions, không đếm skipped actions

### Requirement 5: Enhanced Reminder Data Model

**User Story:** Là một developer, tôi cần mở rộng data model để hỗ trợ các tính năng mới của smart reminder system.

#### Acceptance Criteria

1. WHEN mở rộng Reminder model THEN hệ thống SHALL thêm fields: deadline (Date), totalExpected (int), completedCount (int)
2. WHEN tạo ReminderHistory model THEN hệ thống SHALL có fields: id, reminderId, actionType, timestamp, scheduledTime
3. WHEN tính totalExpected THEN hệ thống SHALL dựa trên frequency và khoảng thời gian từ startDate đến deadline
4. WHEN cập nhật completedCount THEN hệ thống SHALL đếm số lượng ReminderHistory có actionType = "completed"
5. WHEN migrate database THEN hệ thống SHALL preserve existing reminder data và set default values cho fields mới

### Requirement 6: Smart Notification Scheduling

**User Story:** Là một người dùng, tôi muốn hệ thống tự động lên lịch và quản lý các notification nhắc nhở một cách thông minh.

#### Acceptance Criteria

1. WHEN reminder được tạo hoặc cập nhật THEN hệ thống SHALL schedule tất cả notifications từ hiện tại đến deadline
2. WHEN deadline thay đổi THEN hệ thống SHALL cancel old notifications và schedule lại notifications mới
3. WHEN reminder bị deactivate THEN hệ thống SHALL cancel tất cả pending notifications
4. WHEN notification được trigger THEN hệ thống SHALL kiểm tra reminder vẫn active và chưa hết hạn
5. WHEN hệ thống restart THEN tất cả active reminders SHALL được reschedule notifications

### Requirement 7: Progress Visualization

**User Story:** Là một người dùng, tôi muốn thấy progress bar trực quan và thông tin chi tiết về tiến độ hoàn thành.

#### Acceptance Criteria

1. WHEN hiển thị reminder trong RecyclerView THEN progress bar SHALL nằm dưới title và description
2. WHEN progress < 50% THEN progress bar SHALL có màu đỏ nhạt
3. WHEN progress >= 50% và < 80% THEN progress bar SHALL có màu vàng
4. WHEN progress >= 80% THEN progress bar SHALL có màu xanh lá
5. WHEN reminder completed 100% THEN hệ thống SHALL hiển thị celebration icon và "Hoàn thành!" text
6. WHEN reminder expired với progress < 100% THEN hệ thống SHALL hiển thị warning icon và progress final

### Requirement 8: Notification Action Handling

**User Story:** Là một người dùng, tôi muốn actions trên notification được xử lý nhanh chóng và chính xác.

#### Acceptance Criteria

1. WHEN notification action được trigger THEN hệ thống SHALL sử dụng BroadcastReceiver để handle
2. WHEN action "Hoàn thành" được nhấn THEN hệ thống SHALL update database trong background thread
3. WHEN action "Bỏ qua" được nhấn THEN hệ thống SHALL ghi log và update UI nếu app đang mở
4. WHEN database update thành công THEN hệ thống SHALL gửi local broadcast để update UI
5. WHEN database update thất bại THEN hệ thống SHALL retry 3 lần và log error