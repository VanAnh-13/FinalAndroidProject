### **CẤU TRÚC DỮ LIỆU NOSQL CHO FIREBASE FIRESTORE - HEALTHYLIFE HUB**

**Tổng quan**

Firebase Firestore là cơ sở dữ liệu NoSQL dạng document-oriented, trong đó dữ liệu được tổ chức thành các **Collections** (tập hợp) chứa các **Documents** (tài liệu). Mỗi document là một đối tượng JSON chứa các cặp key-value.

Dưới đây là thiết kế cấu trúc dữ liệu cho ứng dụng HealthyLife Hub, được tối ưu hóa cho Firebase Firestore với các nguyên tắc:

*   **Denormalization** (phi chuẩn hóa): Lưu trữ dữ liệu trùng lặp để tối ưu tốc độ đọc.
*   **Hierarchical structure** (cấu trúc phân cấp): Sử dụng subcollections cho dữ liệu liên quan.
*   **Scalability** (khả năng mở rộng): Thiết kế để dễ dàng mở rộng khi số lượng người dùng tăng.

---

### **1. Collection: `users`**

Lưu trữ thông tin tài khoản và hồ sơ sức khỏe cá nhân của người dùng.

**Cấu trúc Document**

`users/{userId}`

**Schema**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `userId` | String | ID người dùng (trùng với Firebase Auth UID) | Yes |
| `email` | String | Email đăng ký | Yes |
| `displayName` | String | Tên hiển thị | Yes |
| `photoURL` | String | URL ảnh đại diện | No |
| `role` | String | Vai trò: "user" hoặc "admin" | Yes |
| `createdAt` | Timestamp | Thời điểm tạo tài khoản | Yes |
| `updatedAt` | Timestamp | Thời điểm cập nhật gần nhất | Yes |
| `profile` | Object | Thông tin hồ sơ sức khỏe (xem bên dưới) | No |

**Nested Object: `profile`**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `fullName` | String | Họ và tên đầy đủ | No |
| `dateOfBirth` | Timestamp | Ngày sinh | No |
| `gender` | String | Giới tính: "male", "female", "other" | No |
| `height` | Number | Chiều cao (cm) | No |
| `weight` | Number | Cân nặng (kg) | No |
| `bloodType` | String | Nhóm máu: "A", "B", "AB", "O", "A+", "A-", ... | No |
| `medicalHistory` | String | Tiền sử bệnh án (văn bản tự do) | No |

**Ví dụ Document**

```json
{
  "userId": "user001",
  "email": "nguyenvana@gmail.com",
  "displayName": "Nguyễn Văn A",
  "photoURL": "https://storage.googleapis.com/healthylife-hub/avatars/user001.jpg",
  "role": "user",
  "createdAt": "2024-01-15T08:30:00Z",
  "updatedAt": "2024-10-20T14:22:00Z",
  "profile": {
    "fullName": "Nguyễn Văn A",
    "dateOfBirth": "1985-05-20T00:00:00Z",
    "gender": "male",
    "height": 170,
    "weight": 68,
    "bloodType": "O+",
    "medicalHistory": "Tiền sử tăng huyết áp nhẹ từ năm 2020. Không có dị ứng thuốc."
  }
}
```

---

### **2. Subcollection: `users/{userId}/healthMetrics`**

Lưu trữ các chỉ số sức khỏe mà người dùng đo và ghi lại theo thời gian.

**Cấu trúc Document**

`users/{userId}/healthMetrics/{metricId}`

**Schema**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `metricId` | String | ID tự động của Firestore | Yes |
| `type` | String | Loại chỉ số: "blood\_pressure", "blood\_sugar", "weight", "heart\_rate", "temperature" | Yes |
| `value` | Object | Giá trị đo (cấu trúc khác nhau tùy loại) | Yes |
| `unit` | String | Đơn vị đo | Yes |
| `measuredAt` | Timestamp | Thời điểm đo | Yes |
| `note` | String | Ghi chú thêm | No |
| `createdAt` | Timestamp | Thời điểm tạo bản ghi | Yes |

**Ví dụ Document (Huyết áp)**

```json
{
  "metricId": "metric001",
  "type": "blood_pressure",
  "value": {
    "systolic": 125,
    "diastolic": 82
  },
  "unit": "mmHg",
  "measuredAt": "2024-10-27T07:30:00Z",
  "note": "Đo sau khi thức dậy",
  "createdAt": "2024-10-27T07:32:00Z"
}
```

---

### **3. Subcollection: `users/{userId}/reminders`**

Lưu trữ các lịch nhắc nhở mà người dùng đã tạo.

**Cấu trúc Document**

`users/{userId}/reminders/{reminderId}`

**Schema**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `reminderId` | String | ID tự động của Firestore | Yes |
| `title` | String | Tiêu đề nhắc nhở | Yes |
| `description` | String | Nội dung chi tiết | No |
| `reminderTime` | Timestamp | Thời gian nhắc nhở | Yes |
| `frequency` | String | Tần suất: "once", "daily", "weekly", "monthly" | Yes |
| `isActive` | Boolean | Trạng thái bật/tắt | Yes |
| `medicineId` | String | ID thuốc liên kết (nếu là nhắc uống thuốc) | No |
| `createdAt` | Timestamp | Thời điểm tạo | Yes |
| `updatedAt` | Timestamp | Thời điểm cập nhật | Yes |

**Ví dụ Document**

```json
{
  "reminderId": "reminder001",
  "title": "Uống thuốc huyết áp",
  "description": "Uống 1 viên Amlodipine 5mg sau bữa sáng",
  "reminderTime": "2024-10-27T08:00:00Z",
  "frequency": "daily",
  "isActive": true,
  "medicineId": "med001",
  "createdAt": "2024-10-15T10:00:00Z",
  "updatedAt": "2024-10-15T10:00:00Z"
}
```

---

### **4. Subcollection: `users/{userId}/medicines`**

Lưu trữ danh sách các loại thuốc mà người dùng đang sử dụng.

**Cấu trúc Document**

`users/{userId}/medicines/{medicineId}`

**Schema**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `medicineId` | String | ID tự động của Firestore | Yes |
| `name` | String | Tên thuốc | Yes |
| `dosage` | String | Liều lượng (ví dụ: "1 viên", "500mg") | Yes |
| `instructions` | String | Hướng dẫn sử dụng | No |
| `startDate` | Timestamp | Ngày bắt đầu dùng | No |
| `endDate` | Timestamp | Ngày kết thúc (nếu có) | No |
| `isActive` | Boolean | Đang sử dụng hay không | Yes |
| `createdAt` | Timestamp | Thời điểm tạo | Yes |
| `updatedAt` | Timestamp | Thời điểm cập nhật | Yes |

**Ví dụ Document**

```json
{
  "medicineId": "med001",
  "name": "Amlodipine",
  "dosage": "5mg",
  "instructions": "Uống 1 viên mỗi sáng sau bữa ăn",
  "startDate": "2024-01-10T00:00:00Z",
  "endDate": null,
  "isActive": true,
  "createdAt": "2024-01-10T09:00:00Z",
  "updatedAt": "2024-01-10T09:00:00Z"
}
```

---

### **5. Subcollection: `users/{userId}/medicalRecords`**

Lưu trữ lịch sử bệnh án, các lần khám bệnh và ghi chú sức khỏe.

**Cấu trúc Document**

`users/{userId}/medicalRecords/{recordId}`

**Schema**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `recordId` | String | ID tự động của Firestore | Yes |
| `title` | String | Tiêu đề ghi chú | Yes |
| `recordDate` | Timestamp | Ngày của sự kiện y tế | Yes |
| `content` | String | Nội dung chi tiết | Yes |
| `hospital` | String | Tên bệnh viện/phòng khám | No |
| `doctor` | String | Tên bác sĩ | No |
| `diagnosis` | String | Chẩn đoán | No |
| `attachments` | Array | Danh sách URL file đính kèm (ảnh, PDF) | No |
| `createdAt` | Timestamp | Thời điểm tạo | Yes |
| `updatedAt` | Timestamp | Thời điểm cập nhật | Yes |

**Ví dụ Document**

```json
{
  "recordId": "record001",
  "title": "Khám định kỳ tim mạch",
  "recordDate": "2024-08-15T00:00:00Z",
  "content": "Khám định kỳ 3 tháng. Huyết áp ổn định. Bác sĩ khuyên tiếp tục dùng thuốc và tập thể dục đều đặn.",
  "hospital": "Bệnh viện Chợ Rẫy",
  "doctor": "BS. Nguyễn Thị B",
  "diagnosis": "Tăng huyết áp độ I, đang kiểm soát tốt",
  "attachments": [
    "https://storage.googleapis.com/healthylife-hub/records/user001/record001_ecg.pdf"
  ],
  "createdAt": "2024-08-15T15:00:00Z",
  "updatedAt": "2024-08-15T15:00:00Z"
}
```

---

### **6. Collection: `reports`**

Lưu trữ thông tin về các báo cáo sức khỏe đã được xuất.

**Cấu trúc Document**

`reports/{reportId}`

**Schema**

| Field | Type | Description | Required |
| :--- | :--- | :--- | :--- |
| `reportId` | String | ID tự động của Firestore | Yes |
| `userId` | String | ID người dùng sở hữu báo cáo | Yes |
| `reportType` | String | Loại báo cáo: "pdf", "excel" | Yes |
| `startDate` | Timestamp | Ngày bắt đầu của khoảng thời gian báo cáo | Yes |
| `endDate` | Timestamp | Ngày kết thúc của khoảng thời gian báo cáo | Yes |
| `fileURL` | String | URL file báo cáo trên Firebase Storage | Yes |
| `createdAt` | Timestamp | Thời điểm tạo báo cáo | Yes |

**Ví dụ Document**

```json
{
  "reportId": "report001",
  "userId": "user001",
  "reportType": "pdf",
  "startDate": "2024-09-27T00:00:00Z",
  "endDate": "2024-10-27T00:00:00Z",
  "fileURL": "https://storage.googleapis.com/healthylife-hub/reports/user001/report_2024-10-27.pdf",
  "createdAt": "2024-10-27T10:00:00Z"
}
```

---

### **7. Các nguyên tắc thiết kế đã áp dụng**

*   **Denormalization (Phi chuẩn hóa):** Lưu `userId` trong collection `reports` để dễ dàng truy vấn. Lưu `medicineId` trong `reminders` để liên kết nhắc nhở với thuốc.
*   **Subcollections cho dữ liệu phân cấp:** Sử dụng subcollections (`healthMetrics`, `reminders`, `medicines`, `medicalRecords`) để tổ chức dữ liệu theo người dùng.
*   **Timestamps cho audit trail:** Mỗi document đều có `createdAt` và `updatedAt` để theo dõi lịch sử thay đổi.
*   **Flexible schema:** Sử dụng nested objects (như `profile`, `value`) để nhóm các trường liên quan.
*   **Boolean flags:** Sử dụng `isActive` để đánh dấu trạng thái mà không cần xóa dữ liệu.

---

### **8. Firestore Security Rules (Đề xuất)**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Chỉ cho phép người dùng đọc/ghi dữ liệu của chính họ
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      // Subcollections kế thừa quyền từ parent document
      match /{subcollection=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }

    // Reports: chỉ người tạo mới được đọc
    match /reports/{reportId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }
  }
}
```

---

### **9. Firestore Indexes (Đề xuất)**

*   **Index cho `healthMetrics`**
    *   **Collection:** `users/{userId}/healthMetrics`
    *   **Fields:** `type` (Ascending), `measuredAt` (Descending)
    *   **Mục đích:** Truy vấn các chỉ số theo loại và sắp xếp theo thời gian.

*   **Index cho `reminders`**
    *   **Collection:** `users/{userId}/reminders`
    *   **Fields:** `isActive` (Ascending), `reminderTime` (Ascending)
    *   **Mục đích:** Lấy các nhắc nhở đang hoạt động sắp xếp theo thời gian.

*   **Index cho `medicalRecords`**
    *   **Collection:** `users/{userId}/medicalRecords`
    *   **Fields:** `recordDate` (Descending)
    *   **Mục đích:** Sắp xếp hồ sơ bệnh án theo thời gian mới nhất.

---

### **10. Kết luận**

Cấu trúc dữ liệu NoSQL này được thiết kế tối ưu cho Firebase Firestore, đảm bảo:

*   **Hiệu suất cao:** Sử dụng subcollections và denormalization để giảm số lần truy vấn.
*   **Bảo mật tốt:** Mỗi người dùng chỉ truy cập được dữ liệu của mình.
*   **Dễ mở rộng:** Cấu trúc linh hoạt, dễ dàng thêm trường mới.
*   **Phù hợp cho bài tập lớn:** Đầy đủ các tính năng cần thiết.
*   **Có dữ liệu mẫu thực tế:** Mỗi collection đều có ví dụ cụ thể để tham khảo.

Chắc chắn rồi, đây là nội dung của tệp thứ hai được chuyển đổi sang định dạng văn bản thông thường.

### **12 USE CASE CHO ỨNG DỤNG HEALTHYLIFE HUB**
**Với Xử lý Bất đồng bộ và Đa luồng trong Android Java**

---

### **UC-HLH-01: Đăng nhập (Login)**

**Thông tin cơ bản**
*   **Mã Use case:** UC-HLH-01
*   **Tên Use case:** Đăng nhập
*   **Tác nhân:** Người dùng, Quản trị viên
*   **Độ phức tạp:** Trung bình
*   **Xử lý bất đồng bộ:** Có (Firebase Authentication)

**Mô tả**
Chức năng đăng nhập cho phép người dùng xác thực và truy cập vào ứng dụng. Quá trình xác thực được thực hiện bất đồng bộ với Firebase Authentication để không chặn UI thread.

**Xử lý kỹ thuật (Android Java)**
Bất đồng bộ với `CompletableFuture` và `ExecutorService`:

```java
public class LoginViewModel extends ViewModel {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final MutableLiveData<AuthResult> authResult = new MutableLiveData<>();

    public void loginWithEmail(String email, String password) {
        CompletableFuture.supplyAsync(() -> {
            // Xác thực với Firebase trên background thread
            Task<AuthResult> task = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password);
            // Đợi kết quả (blocking trên background thread)
            Tasks.await(task);
            return task.getResult();
        }, executorService)
        .thenAcceptAsync(result -> {
            // Cập nhật LiveData trên main thread
            authResult.postValue(result);
        }, ContextCompat.getMainExecutor(context))
        .exceptionally(throwable -> {
            // Xử lý lỗi
            authResult.postValue(null);
            return null;
        });
    }
}
```

**Luồng sự kiện chính**

| # | Thực hiện bởi | Hành động | Xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| 1 | Người dùng | Nhập email và mật khẩu | UI Thread |
| 2 | Người dùng | Nhấn nút "Đăng nhập" | UI Thread |
| 3 | Hệ thống | Kiểm tra validation dữ liệu đầu vào | UI Thread |
| 4 | Hệ thống | Gửi yêu cầu xác thực đến Firebase | Background Thread (ExecutorService) |
| 5 | Hệ thống | Nhận kết quả xác thực từ Firebase | Background Thread |
| 6 | Hệ thống | Lưu session và thông tin người dùng vào SharedPreferences | Background Thread |
| 7 | Hệ thống | Cập nhật UI và chuyển hướng đến Dashboard | Main Thread (Handler) |

---

### **UC-HLH-02: Đăng ký (Register)**

**Thông tin cơ bản**
*   **Mã Use case:** UC-HLH-02
*   **Tên Use case:** Đăng ký tài khoản
*   **Tác nhân:** Khách hàng
*   **Độ phức tạp:** Trung bình
*   **Xử lý bất đồng bộ:** Có (Firebase Authentication + Firestore)

**Xử lý kỹ thuật (Android Java)**
Chuỗi xử lý bất đồng bộ với RxJava:

```java
public class RegisterViewModel extends ViewModel {
    public void registerUser(String email, String password, UserProfile profile) {
        Observable.fromCallable(() -> {
            // Bước 1: Tạo tài khoản Firebase Auth
            Task<AuthResult> authTask = firebaseAuth
                    .createUserWithEmailAndPassword(email, password);
            Tasks.await(authTask);
            return authTask.getResult().getUser();
        })
        .flatMap(firebaseUser -> {
            // Bước 2: Tạo document trong Firestore
            return Observable.fromCallable(() -> {
                Task<Void> firestoreTask = firestore
                        .collection("users")
                        .document(firebaseUser.getUid())
                        .set(profile);
                Tasks.await(firestoreTask);
                return firebaseUser;
            });
        })
        .flatMap(firebaseUser -> {
            // Bước 3: Gửi email xác thực
            return Observable.fromCallable(() -> {
                Task<Void> emailTask = firebaseUser.sendEmailVerification();
                Tasks.await(emailTask);
                return true;
            });
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            success -> {
                // Thành công - cập nhật UI
                registrationResult.setValue(new Result.Success());
            },
            error -> {
                // Lỗi - hiển thị thông báo
                registrationResult.setValue(new Result.Error(error));
            }
        );
    }
}
```

**Luồng sự kiện chính**

| # | Thực hiện bởi | Hành động | Xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| 1 | Người dùng | Nhập thông tin đăng ký (email, mật khẩu, họ tên) | UI Thread |
| 2 | Hệ thống | Validate dữ liệu đầu vào | UI Thread |
| 3 | Hệ thống | Tạo tài khoản Firebase Authentication | Background Thread (RxJava - IO Scheduler) |
| 4 | Hệ thống | Tạo document user trong Firestore | Background Thread (Chain operation) |
| 5 | Hệ thống | Gửi email xác thực | Background Thread (Chain operation) |
| 6 | Hệ thống | Cập nhật UI thông báo thành công | Main Thread (AndroidSchedulers.mainThread()) |

---

### **UC-HLH-03: Quản lý hồ sơ sức khỏe cá nhân**

**Thông tin cơ bản**
*   **Mã Use case:** UC-HLH-03
*   **Tên Use case:** Quản lý hồ sơ sức khỏe cá nhân
*   **Tác nhân:** Người dùng
*   **Độ phức tạp:** Trung bình
*   **Xử lý bất đồng bộ:** Có (Firestore read/write)

**Xử lý kỹ thuật (Android Java)**
Đọc và ghi dữ liệu bất đồng bộ với LiveData:

```java
public class ProfileRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LiveData<UserProfile> getUserProfile(String userId) {
        MutableLiveData<UserProfile> profileLiveData = new MutableLiveData<>();
        // Lắng nghe realtime updates từ Firestore
        firestore.collection("users").document(userId)
            .addSnapshotListener(executor, (snapshot, error) -> {
                if (error != null) {
                    profileLiveData.postValue(null);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    profileLiveData.postValue(profile);
                }
            });
        return profileLiveData;
    }

    public CompletableFuture<Boolean> updateProfile(String userId, UserProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Task<Void> task = firestore.collection("users")
                    .document(userId)
                    .update("profile", profile);
                Tasks.await(task);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, executor);
    }
}
```

**Luồng sự kiện chính**

| # | Thực hiện bởi | Hành động | Xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| 1 | Người dùng | Mở màn hình hồ sơ sức khỏe | UI Thread |
| 2 | Hệ thống | Tải dữ liệu hồ sơ từ Firestore | Background Thread (Firestore listener) |
| 3 | Hệ thống | Hiển thị dữ liệu lên UI | Main Thread (LiveData observer) |
| 4 | Người dùng | Chỉnh sửa thông tin (chiều cao, cân nặng, nhóm máu) | UI Thread |
| 5 | Người dùng | Nhấn "Lưu" | UI Thread |
| 6 | Hệ thống | Validate dữ liệu | UI Thread |
| 7 | Hệ thống | Cập nhật Firestore | Background Thread (CompletableFuture) |
| 8 | Hệ thống | Nhận realtime update và cập nhật UI | Main Thread (Snapshot listener callback) |

---

### **UC-HLH-04: Quản lý và phân tích chỉ số sức khỏe**

**Thông tin cơ bản**
*   **Mã Use case:** UC-HLH-04
*   **Tên Use case:** Quản lý và phân tích chỉ số sức khỏe
*   **Tác nhân:** Người dùng
*   **Độ phức tạp:** Cao
*   **Xử lý bất đồng bộ:** Có (Đa luồng để tính toán thống kê)

**Mô tả**
Chức năng này cho phép người dùng ghi lại các chỉ số sức khỏe và hệ thống sẽ tự động phân tích, tính toán xu hướng, và đưa ra cảnh báo nếu có bất thường. Quá trình phân tích được thực hiện trên background thread để không làm chậm UI.

**Xử lý kỹ thuật (Android Java)**
Xử lý đa luồng với `ThreadPoolExecutor`:

```java
public class HealthMetricsAnalyzer {
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
    );

    public CompletableFuture<AnalysisResult> analyzeMetrics(String userId, String metricType, int days) {
        return CompletableFuture.supplyAsync(() -> {
            // Thread 1: Lấy dữ liệu từ Firestore
            return fetchMetricsFromFirestore(userId, metricType, days);
        }, threadPool)
        .thenApplyAsync(metrics -> {
            // Thread 2: Tính toán thống kê
            Statistics stats = calculateStatistics(metrics);
            return new Pair<>(metrics, stats);
        }, threadPool)
        .thenApplyAsync(pair -> {
            // Thread 3: Phát hiện bất thường
            List<Anomaly> anomalies = detectAnomalies(pair.first, pair.second);
            return new Pair<>(pair.second, anomalies);
        }, threadPool)
        .thenApplyAsync(pair -> {
            // Thread 4: Tạo khuyến nghị
            List<Recommendation> recommendations = generateRecommendations(pair.first, pair.second);
            return new AnalysisResult(pair.first, pair.second, recommendations);
        }, threadPool);
    }
}
```

**Luồng sự kiện chính**

| # | Thực hiện bởi | Hành động | Xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| 1 | Người dùng | Nhập chỉ số sức khỏe mới (huyết áp: 140/90) | UI Thread |
| 2 | Hệ thống | Lưu vào Firestore | Background Thread |
| 3 | Hệ thống | Tự động kích hoạt phân tích | Background Thread Pool |
| 4 | Hệ thống | **Thread 1:** Lấy 30 ngày dữ liệu gần nhất | Worker Thread 1 |
| 5 | Hệ thống | **Thread 2:** Tính toán thống kê (mean, std, trend) | Worker Thread 2 |
| 6 | Hệ thống | **Thread 3:** Phát hiện bất thường | Worker Thread 3 |
| 7 | Hệ thống | **Thread 4:** Tạo khuyến nghị dựa trên phân tích | Worker Thread 4 |
| 8 | Hệ thống | Hiển thị kết quả phân tích và cảnh báo (nếu có) | Main Thread |
| 9 | Hệ thống | Nếu phát hiện bất thường nghiêm trọng, gửi notification | Background Thread (FCM) |

---

### **UC-HLH-05: Hệ thống nhắc nhở thông minh với AI**

**Thông tin cơ bản**
*   **Mã Use case:** UC-HLH-05
*   **Tên Use case:** Hệ thống nhắc nhở thông minh
*   **Tác nhân:** Người dùng, Hệ thống (tự động)
*   **Độ phức tạp:** Cao
*   **Xử lý bất đồng bộ:** Có (Background service, WorkManager)

**Mô tả**
Hệ thống không chỉ nhắc nhở theo lịch cố định mà còn học hành vi người dùng và tự động điều chỉnh thời gian nhắc nhở để tối ưu hiệu quả.

**Xử lý kỹ thuật (Android Java)**
`WorkManager` cho background task định kỳ:

```java
public class SmartReminderWorker extends Worker {
    @Override
    public Result doWork() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            // Parallel tasks
            CompletableFuture<List<Reminder>> fetchTask = CompletableFuture.supplyAsync(() -> fetchActiveReminders(), executor);
            CompletableFuture<UserBehavior> behaviorTask = CompletableFuture.supplyAsync(() -> analyzeUserBehavior(), executor);

            // Đợi cả 2 tasks hoàn thành
            CompletableFuture.allOf(fetchTask, behaviorTask).join();

            List<Reminder> reminders = fetchTask.get();
            UserBehavior behavior = behaviorTask.get();

            // Xử lý từng reminder
            for (Reminder reminder : reminders) {
                processReminderWithAI(reminder, behavior);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        } finally {
            executor.shutdown();
        }
    }
}
```

**Luồng sự kiện chính**

| # | Thực hiện bởi | Hành động | Xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| 1 | Hệ thống | WorkManager kích hoạt mỗi 15 phút | Background Service |
| 2 | Hệ thống | **Parallel Task 1:** Lấy danh sách reminder active | Worker Thread 1 |
| 3 | Hệ thống | **Parallel Task 2:** Phân tích hành vi người dùng | Worker Thread 2 |
| 4 | Hệ thống | Đợi cả 2 tasks hoàn thành (join) | Background |
| 5 | Hệ thống | Với mỗi reminder, tính toán thời gian tối ưu | Worker Thread 3 |
| 6 | Hệ thống | So sánh với thời gian hiện tại | Background |
| 7 | Hệ thống | Nếu cần, đề xuất thay đổi thời gian | Background |
| 8 | Hệ thống | Gửi notification đến người dùng | FCM (Background) |
| 9 | Người dùng | Nhận notification và tương tác | UI Thread |

---

### **UC-HLH-12: Đồng bộ dữ liệu offline-first**

**Thông tin cơ bản**
*   **Mã Use case:** UC-HLH-12
*   **Tên Use case:** Đồng bộ dữ liệu offline-first
*   **Tác nhân:** Hệ thống (tự động)
*   **Độ phức tạp:** Rất cao
*   **Xử lý bất đồng bộ:** Có (Background sync, Conflict resolution)

**Mô tả**
Ứng dụng hoạt động offline, lưu dữ liệu local và tự động đồng bộ khi có kết nối. Xử lý conflict khi dữ liệu local và remote khác nhau.

**Xử lý kỹ thuật (Android Java)**
`WorkManager` cho background sync:

```java
public class DataSyncWorker extends Worker {
    @Override
    public Result doWork() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            // Parallel sync của nhiều collections
            CompletableFuture<Void> syncMetrics = CompletableFuture.runAsync(() -> syncHealthMetrics(), executor);
            CompletableFuture<Void> syncReminders = CompletableFuture.runAsync(() -> syncReminders(), executor);
            // ... các collection khác
            
            CompletableFuture.allOf(syncMetrics, syncReminders).get(5, TimeUnit.MINUTES);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        } finally {
            executor.shutdown();
        }
    }

    private void syncHealthMetrics() {
        // 1. Lấy dữ liệu local chưa sync
        // 2. Upload lên Firestore
        // 3. Đánh dấu đã sync
        // 4. Pull dữ liệu mới từ Firestore
        // 5. Xử lý conflict (ví dụ: last-write-wins)
    }
}
```

**Luồng sự kiện chính**

| # | Thực hiện bởi | Hành động | Xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| 1 | Hệ thống | Phát hiện kết nối mạng khả dụng | ConnectivityManager |
| 2 | Hệ thống | Kích hoạt DataSyncWorker | WorkManager |
| 3 | Hệ thống | **Parallel Sync 1:** Health Metrics | Worker Thread 1 |
| 4 | Hệ thống | **Parallel Sync 2:** Reminders | Worker Thread 2 |
| 5 | Hệ thống | ... (các sync khác) | ... |
| 6 | Hệ thống | Mỗi sync: Upload local changes | Background |
| 7 | Hệ thống | Mỗi sync: Download remote changes | Background |
| 8 | Hệ thống | Phát hiện và giải quyết conflicts | Background |
| 9 | Hệ thống | Cập nhật UI nếu có thay đổi | Main Thread (LiveData) |

---

### **Tổng kết về Xử lý Bất đồng bộ và Đa luồng**

**Công nghệ sử dụng**
1.  **ExecutorService & ThreadPoolExecutor:** Quản lý thread pool cho các tác vụ nặng.
2.  **CompletableFuture:** Xử lý async operations với pipeline.
3.  **RxJava:** Reactive programming cho event streams.
4.  **Kotlin Coroutines:** Modern async programming.
5.  **WorkManager:** Background tasks với constraints.
6.  **LiveData & Flow:** Reactive UI updates.
7.  **Firebase Firestore Listeners:** Real-time data sync.

**Lợi ích**
*   ✅ **Hiệu suất cao:** Không block UI thread.
*   ✅ **Responsive UI:** Ứng dụng luôn mượt mà.
*   ✅ **Scalability:** Xử lý được lượng dữ liệu lớn.
*   ✅ **Offline-first:** Hoạt động tốt khi không có mạng.
*   ✅ **Real-time:** Cập nhật dữ liệu ngay lập tức.
*   ✅ **Battery efficient:** Tối ưu pin với WorkManager.