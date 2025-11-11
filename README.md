# HealthyLife Hub

> Ứng dụng quản lý sức khỏe toàn diện, giúp người dùng theo dõi chỉ số sinh học, lịch uống thuốc, hồ sơ bệnh án và tạo báo cáo sức khỏe. Hoạt động bền bỉ cả khi ngoại tuyến với kiến trúc offline-first và đồng bộ realtime.

## Tóm tắt nhanh
- **Kiến trúc:** MVVM + Repository đa tầng, tách biệt UI, dữ liệu, đồng bộ
- **Chiến lược dữ liệu:** Offline-first (Room) + Firestore đồng bộ hai chiều
- **Bảo mật:** Firebase Auth (email/Google), phân quyền Firestore theo người dùng
- **Trải nghiệm:** LiveData realtime, nhắc nhở AlarmManager + WorkManager, báo cáo PDF/Excel

## Công nghệ chủ đạo
**Nền tảng Android:** Java, View Binding, Material Components, Navigation đa fragment  
**Xử lý bất đồng bộ:** RxJava3, CompletableFuture, ExecutorService, WorkManager  
**Hạ tầng dữ liệu:** Firebase Firestore, Firebase Storage, Room Database, CacheManager, SyncManager  
**Tiện ích mở rộng:** AlarmManager, BroadcastReceiver, Glide, Snackbar, SharedPreferences bảo mật phiên

## Bức tranh tính năng
### Quản lý người dùng & hồ sơ
- Đăng nhập/đăng ký qua email hoặc Google, xác thực đa bước
- Hồ sơ sức khỏe đồng bộ Firestore, cập nhật realtime
- Cấu hình avatar, thông tin cá nhân và cơ chế nhớ đăng nhập an toàn

### Theo dõi chỉ số sức khỏe
- Lưu và hiển thị huyết áp, đường huyết, cân nặng, nhịp tim, nhiệt độ
- Phân tích xu hướng, phát hiện bất thường với HealthMetricsAnalyzer
- Xuất báo cáo PDF/Excel theo giai đoạn mong muốn

### Hệ thống nhắc nhở thông minh
- Tạo nhắc nhở uống thuốc, tái khám, đo chỉ số theo lịch linh hoạt
- AlarmManager + WorkManager đảm bảo thông báo chuẩn xác kể cả khi offline
- Ghi nhận hoàn thành/bỏ qua và gợi ý tinh chỉnh lịch trình dựa trên hành vi

### Hồ sơ bệnh án & thuốc
- Ghi lại bệnh viện, bác sĩ, chẩn đoán, tài liệu đính kèm
- Theo dõi đơn thuốc, liều lượng, trạng thái sử dụng
- Sử dụng MediatorLiveData để sắp xếp hồ sơ mới nhất

### Đồng bộ & bộ nhớ đệm
- CacheManager gom chuẩn các thao tác Room
- SyncManager đồng bộ hai chiều với Firestore, retry/backoff thông minh
- Kiến trúc module hóa: repository, cache, sync dễ dàng mở rộng

## Tiến độ triển khai
<div align="left">

**Nền tảng người dùng**  
<progress value="100" max="100"></progress> 100%

**Chỉ số sức khỏe**  
<progress value="100" max="100"></progress> 100%

**Nhắc nhở thông minh**  
<progress value="80" max="100"></progress> 80% — cần hoàn thiện kiểm thử quyền POST_NOTIFICATIONS

**Hồ sơ bệnh án & thuốc**  
<progress value="100" max="100"></progress> 100%

**Báo cáo sức khỏe**  
<progress value="60" max="100"></progress> 60% — UI/UX đang được hoàn thiện

</div>

## Hướng dẫn cài đặt chi tiết

### Yêu cầu hệ thống
- Android Studio Hedgehog (2023.1.1) hoặc mới hơn
- JDK 17 trở lên
- Android SDK 34 (API level 34)
- Gradle 8.0+
- Git (để clone dự án)

### Cài đặt trên máy tính cá nhân

#### Bước 1: Clone dự án
```bash
git clone https://github.com/your-username/FinalAndroidProject.git
cd FinalAndroidProject
```

#### Bước 2: Cấu hình Firebase
1. Truy cập [Firebase Console](https://console.firebase.google.com)
2. Tạo dự án mới hoặc chọn dự án hiện có
3. Đăng ký ứng dụng Android với package name: `com.example.healthylifehub`
4. Tải tệp `google-services.json` từ Firebase Console
5. Sao chép `google-services.json` vào thư mục `app/`

#### Bước 3: Cấu hình Google Sign-In
1. Tạo OAuth 2.0 Client ID cho Android trong [Google Cloud Console](https://console.cloud.google.com)
2. Lấy SHA-1 fingerprint của keystore:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
3. Thêm SHA-1 vào Google Cloud Console
4. Cập nhật `BuildConfig.GOOGLE_WEB_CLIENT_ID` trong `build.gradle.kts`

#### Bước 4: Mở dự án trong Android Studio
1. Mở Android Studio
2. Chọn **File > Open** và chọn thư mục dự án
3. Android Studio sẽ tự động đồng bộ Gradle
4. Đợi indexing hoàn thành

#### Bước 5: Chạy ứng dụng
1. Kết nối thiết bị Android hoặc khởi động Android Emulator
2. Chọn **Run > Run 'app'** hoặc nhấn `Shift + F10`
3. Chọn thiết bị mục tiêu
4. Ứng dụng sẽ build và cài đặt tự động

### Cài đặt trên máy tính khác

#### Cách 1: Sử dụng Git Clone
```bash
# Clone repository
git clone https://github.com/your-username/FinalAndroidProject.git
cd FinalAndroidProject

# Đồng bộ Gradle
./gradlew --version

# Build debug APK
./gradlew assembleDebug

# APK sẽ nằm tại: app/build/outputs/apk/debug/app-debug.apk
```

#### Cách 2: Sử dụng Android Studio
1. Mở Android Studio
2. Chọn **File > New > Project from Version Control**
3. Nhập URL repository: `https://github.com/your-username/FinalAndroidProject.git`
4. Chọn thư mục lưu trữ cục bộ
5. Chờ clone và indexing hoàn thành
6. Thực hiện các bước cấu hình Firebase như trên

#### Cách 3: Sử dụng Command Line (Headless)
```bash
# Clone và cấu hình
git clone https://github.com/your-username/FinalAndroidProject.git
cd FinalAndroidProject

# Copy google-services.json vào app/
cp /path/to/google-services.json app/

# Build release APK (cần keystore)
./gradlew assembleRelease -Pandroid.injected.signing.store.file=/path/to/keystore.jks \
  -Pandroid.injected.signing.store.password=password \
  -Pandroid.injected.signing.key.alias=alias \
  -Pandroid.injected.signing.key.password=password

# APK sẽ nằm tại: app/build/outputs/apk/release/app-release.apk
```

### Xử lý sự cố phổ biến

**Lỗi: "google-services.json not found"**
- Đảm bảo file `google-services.json` nằm trong thư mục `app/`
- Kiểm tra tên file chính xác (không có khoảng trắng)

**Lỗi: "Gradle sync failed"**
- Xóa thư mục `.gradle` và `build/`
- Chạy `./gradlew clean build`
- Cập nhật Android Studio lên phiên bản mới nhất

**Lỗi: "Firebase initialization failed"**
- Kiểm tra kết nối internet
- Xác nhận `google-services.json` hợp lệ
- Kiểm tra Firestore Rules cho phép đọc/ghi

**Lỗi: "POST_NOTIFICATIONS permission denied"**
- Cấp quyền thông báo trong Settings > Apps > HealthyLife Hub > Permissions
- Chỉ cần trên Android 13+

## Cấu trúc dữ liệu Firestore (tóm tắt)
- `users/{userId}` chứa thông tin tài khoản, hồ sơ sức khỏe (`profile`)
- Subcollections: `healthMetrics`, `reminders`, `medicines`, `medicalRecords`
- `reports/{reportId}` lưu thông tin báo cáo PDF/Excel
- Mỗi tài liệu có `createdAt`, `updatedAt` (Timestamp) để truy vết lịch sử

## Lưu ý sử dụng
- **Android 13+:** Cấp quyền POST_NOTIFICATIONS trước khi tạo nhắc nhở
- **Thử nghiệm nhắc nhở:** Đặt thời gian trong tương lai, tắt tiết kiệm pin
- **Kiểm tra offline:** Bật máy bay, thao tác, bật mạng để quan sát SyncManager
- **Firestore Rules:** Giới hạn truy cập theo `request.auth.uid` để bảo mật
- **Mở rộng Room:** Tăng `version` và thêm migration khi thay đổi schema

## Cấu trúc thư mục dự án
```
FinalAndroidProject/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/healthylifehub/
│   │   │   │   ├── ui/              (Activities, Fragments, ViewModels)
│   │   │   │   ├── data/            (Models, Repositories, Database)
│   │   │   │   ├── base/            (BaseActivity, BaseViewModel, BaseFragment)
│   │   │   │   └── utils/           (Helpers, Constants, Security)
│   │   │   └── res/                 (Layouts, Drawables, Values)
│   │   ├── test/                    (Unit tests)
│   │   └── androidTest/             (Instrumented tests)
│   ├── build.gradle.kts
│   └── google-services.json
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

## Đóng góp & Hỗ trợ
Dự án đang phát triển các tính năng báo cáo trực quan và khuyến nghị cá nhân hóa dựa trên AI. Mọi ý tưởng, bug report, pull request luôn được hoan nghênh.

## Giấy phép
Dự án này được phát hành dưới giấy phép MIT. Xem file LICENSE để biết chi tiết.
