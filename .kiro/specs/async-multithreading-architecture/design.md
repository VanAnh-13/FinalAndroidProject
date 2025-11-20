# Design Document

## Overview

This design document outlines the architectural enhancements for asynchronous processing and multithreading across the HealthyLife Hub Android application. The design builds upon the existing architecture which already includes:

- **Base Classes**: BaseViewModel, BaseRepository, BaseActivity, BaseFragment
- **Data Layer**: Room database (AppDatabase) with DAOs, Firestore repositories
- **Workers**: DataSyncWorker, SmartReminderWorker
- **Utilities**: ReportGenerator, NotificationHelper, SyncManager, NetworkMonitor

The enhancements will standardize async patterns, improve performance, and ensure consistent error handling across all features.

### Design Principles

1. **Non-blocking UI**: All heavy operations execute on background threads
2. **Reactive Updates**: LiveData and Firestore listeners for real-time UI updates
3. **Composable Operations**: CompletableFuture chains for complex async workflows
4. **Offline-First**: Room database as source of truth with Firestore sync
5. **Resource Efficiency**: Thread pool reuse and proper lifecycle management
6. **Error Resilience**: Comprehensive error handling with retry logic

## Architecture

### Thread Management Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Thread (Main)                      │
│  - View rendering                                            │
│  - LiveData observation                                      │
│  - User input handling                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                           │
│  - LiveData/MutableLiveData                                  │
│  - Initiates async operations                                │
│  - Observes repository results                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                           │
│  - CompletableFuture for complex operations                  │
│  - ExecutorService for parallel tasks                        │
│  - Firestore listeners for real-time data                    │
│  - Room DAOs for local data                                  │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│   IO Thread Pool         │  │  Compute Thread Pool     │
│  - Network requests      │  │  - Data processing       │
│  - Database operations   │  │  - Statistics calc       │
│  - File I/O              │  │  - Chart rendering       │
│  - Firebase calls        │  │  - ML operations         │
└──────────────────────────┘  └──────────────────────────┘
                │                           │
                └─────────────┬─────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Background Services                        │
│  - WorkManager (DataSyncWorker, SmartReminderWorker)        │
│  - Periodic background tasks                                 │
│  - Constraint-based execution                                │
└─────────────────────────────────────────────────────────────┘
```

### Concurrency Patterns


#### Pattern 1: CompletableFuture Pipeline (Complex Multi-Stage Operations)

**Use Cases**: Report generation, data sync, registration flow

```java
public CompletableFuture<Result> complexOperation() {
    return CompletableFuture
        .supplyAsync(() -> fetchData(), ioExecutor)
        .thenApplyAsync(data -> processData(data), computeExecutor)
        .thenApplyAsync(processed -> saveData(processed), ioExecutor)
        .thenApplyAsync(saved -> updateUI(saved), mainExecutor)
        .exceptionally(error -> handleError(error));
}
```

#### Pattern 2: Parallel Execution (Independent Operations)

**Use Cases**: Dashboard loading, multi-collection sync

```java
CompletableFuture<Metrics> metricsFuture = CompletableFuture.supplyAsync(() -> fetchMetrics(), executor);
CompletableFuture<Reminders> remindersFuture = CompletableFuture.supplyAsync(() -> fetchReminders(), executor);
CompletableFuture<Records> recordsFuture = CompletableFuture.supplyAsync(() -> fetchRecords(), executor);

CompletableFuture.allOf(metricsFuture, remindersFuture, recordsFuture).join();
```

#### Pattern 3: LiveData with Firestore Listeners (Real-time Updates)

**Use Cases**: Metrics list, reminders, profile data

```java
public LiveData<List<Item>> observeItems() {
    MutableLiveData<List<Item>> liveData = new MutableLiveData<>();
    
    firestore.collection("items")
        .addSnapshotListener(executor, (snapshot, error) -> {
            if (error != null) {
                liveData.postValue(Collections.emptyList());
                return;
            }
            List<Item> items = snapshot.toObjects(Item.class);
            liveData.postValue(items);
        });
    
    return liveData;
}
```

#### Pattern 4: RxJava Reactive Streams (Event Processing)

**Use Cases**: Search with debounce, form validation

```java
searchQuerySubject
    .debounce(300, TimeUnit.MILLISECONDS)
    .distinctUntilChanged()
    .switchMap(query -> Observable.fromCallable(() -> search(query))
        .subscribeOn(Schedulers.io()))
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(results -> updateUI(results));
```

#### Pattern 5: WorkManager (Background Tasks)

**Use Cases**: Periodic sync, reminder processing

```java
PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
    DataSyncWorker.class, 15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
    .setConstraints(new Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build())
    .build();

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "data_sync", ExistingPeriodicWorkPolicy.KEEP, workRequest);
```

## Components and Interfaces

### 1. Enhanced BaseRepository

**Purpose**: Provide common async utilities for all repositories

**Key Methods**:
- `getIoExecutor()`: Returns shared IO thread pool
- `getComputeExecutor()`: Returns shared compute thread pool
- `getMainExecutor()`: Returns main thread executor
- `executeAsync(Callable<T>)`: Execute task on IO thread
- `executeParallel(List<Callable<T>>)`: Execute tasks in parallel


**Implementation**:

```java
public abstract class BaseRepository {
    private static final ExecutorService IO_EXECUTOR = 
        Executors.newFixedThreadPool(4);
    private static final ExecutorService COMPUTE_EXECUTOR = 
        Executors.newFixedThreadPool(2);
    private static final Executor MAIN_EXECUTOR = 
        ContextCompat.getMainExecutor(ApplicationContextProvider.getContext());
    
    protected ExecutorService getIoExecutor() {
        return IO_EXECUTOR;
    }
    
    protected ExecutorService getComputeExecutor() {
        return COMPUTE_EXECUTOR;
    }
    
    protected Executor getMainExecutor() {
        return MAIN_EXECUTOR;
    }
    
    protected <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, IO_EXECUTOR);
    }
}
```

### 2. AuthRepository Enhancement

**Current State**: Uses LiveData with observeForever (potential memory leaks)

**Enhancement**: Add CompletableFuture-based methods

**New Methods**:
- `loginWithEmailAsync(String email, String password)`: Returns CompletableFuture<User>
- `registerWithEmailAsync(String email, String password, UserProfile profile)`: Returns CompletableFuture<User>
- `loginWithGoogleAsync(Intent data)`: Returns CompletableFuture<User>

**Implementation Pattern**:

```java
public CompletableFuture<User> loginWithEmailAsync(String email, String password) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            Task<AuthResult> task = firebaseAuth.signInWithEmailAndPassword(email, password);
            Tasks.await(task);
            
            FirebaseUser firebaseUser = task.getResult().getUser();
            return convertToUser(firebaseUser);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }, getIoExecutor());
}
```

### 3. MetricsRepository Enhancement

**Current State**: Uses Firestore listeners with LiveData

**Enhancement**: Add analysis trigger and caching

**New Methods**:
- `saveMetricWithAnalysis(HealthMetric metric)`: Save and trigger analysis
- `analyzeMetricsAsync(String metricType, int days)`: Returns CompletableFuture<AnalysisResult>
- `getCachedMetrics(String metricType)`: Returns cached data immediately

**Integration with HealthMetricsAnalyzer**:

```java
public CompletableFuture<Void> saveMetricWithAnalysis(HealthMetric metric) {
    return CompletableFuture
        .supplyAsync(() -> {
            // Save to Room
            healthMetricDao.insert(metric);
            return metric;
        }, getIoExecutor())
        .thenComposeAsync(savedMetric -> {
            // Save to Firestore
            return uploadToFirestore(savedMetric);
        }, getIoExecutor())
        .thenComposeAsync(savedMetric -> {
            // Trigger analysis
            return healthMetricsAnalyzer.analyzeMetrics(
                metric.getUserId(), 
                metric.getType(), 
                30
            );
        }, getComputeExecutor())
        .thenAcceptAsync(analysis -> {
            // Handle analysis results (notifications, etc.)
            if (analysis.hasAnomalies()) {
                notificationHelper.sendAnomalyAlert(analysis);
            }
        }, getMainExecutor());
}
```


### 4. HealthMetricsAnalyzer (New Component)

**Purpose**: Automated analysis of health metrics with anomaly detection

**Thread Strategy**: Uses ThreadPoolExecutor with 2-4 threads for parallel analysis stages

**Key Methods**:
- `analyzeMetrics(String userId, String metricType, int days)`: Returns CompletableFuture<AnalysisResult>
- `calculateStatistics(List<HealthMetric> metrics)`: Calculate mean, stdDev, trend
- `detectAnomalies(List<HealthMetric> metrics, Statistics stats)`: Find outliers
- `generateRecommendations(Statistics stats, List<Anomaly> anomalies)`: Create suggestions

**Implementation**:

```java
public class HealthMetricsAnalyzer {
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
    );
    
    public CompletableFuture<AnalysisResult> analyzeMetrics(
            String userId, String metricType, int days) {
        
        return CompletableFuture
            // Stage 1: Fetch data
            .supplyAsync(() -> fetchMetrics(userId, metricType, days), threadPool)
            
            // Stage 2: Calculate statistics
            .thenApplyAsync(metrics -> {
                Statistics stats = calculateStatistics(metrics);
                return new Pair<>(metrics, stats);
            }, threadPool)
            
            // Stage 3: Detect anomalies
            .thenApplyAsync(pair -> {
                List<Anomaly> anomalies = detectAnomalies(pair.first, pair.second);
                return new Triple<>(pair.first, pair.second, anomalies);
            }, threadPool)
            
            // Stage 4: Generate recommendations
            .thenApplyAsync(triple -> {
                List<Recommendation> recommendations = 
                    generateRecommendations(triple.second, triple.third);
                return new AnalysisResult(triple.second, triple.third, recommendations);
            }, threadPool);
    }
    
    private Statistics calculateStatistics(List<HealthMetric> metrics) {
        double mean = metrics.stream()
            .mapToDouble(HealthMetric::getValue)
            .average()
            .orElse(0.0);
        
        double variance = metrics.stream()
            .mapToDouble(m -> Math.pow(m.getValue() - mean, 2))
            .sum() / metrics.size();
        
        double stdDev = Math.sqrt(variance);
        
        return new Statistics(mean, stdDev, calculateTrend(metrics));
    }
    
    private List<Anomaly> detectAnomalies(List<HealthMetric> metrics, Statistics stats) {
        return metrics.stream()
            .filter(m -> Math.abs(m.getValue() - stats.getMean()) > 2 * stats.getStdDev())
            .map(m -> new Anomaly(m, "Value exceeds 2 standard deviations"))
            .collect(Collectors.toList());
    }
}
```

### 5. DashboardViewModel Enhancement

**Current State**: Uses Handler/Thread for delayed loading

**Enhancement**: Use CompletableFuture for parallel loading

**Implementation**:

```java
public class DashboardViewModel extends BaseViewModel {
    
    private void loadDashboardData() {
        String userId = getCurrentUserId();
        
        CompletableFuture<List<Reminder>> remindersFuture = 
            CompletableFuture.supplyAsync(() -> 
                remindersRepository.loadRemindersSync(userId), executor);
        
        CompletableFuture<Map<String, String>> metricsFuture = 
            CompletableFuture.supplyAsync(() -> 
                metricsRepository.loadLatestMetricsSync(userId), executor);
        
        CompletableFuture<Integer> notificationsFuture = 
            CompletableFuture.supplyAsync(() -> 
                notificationsRepository.getUnreadCountSync(userId), executor);
        
        CompletableFuture.allOf(remindersFuture, metricsFuture, notificationsFuture)
            .thenAcceptAsync(v -> {
                try {
                    reminders.postValue(remindersFuture.get());
                    latestMetrics.postValue(metricsFuture.get());
                    notificationCount.postValue(notificationsFuture.get());
                } catch (Exception e) {
                    // Handle error
                }
            }, ContextCompat.getMainExecutor(getApplication()));
    }
}
```


### 6. MedicalRecordsViewModel with Search (New Component)

**Purpose**: Full-text search with debouncing using RxJava

**Dependencies**: RxJava3, Room FTS

**Implementation**:

```java
public class MedicalRecordsViewModel extends BaseViewModel {
    private final Subject<String> searchQuerySubject = PublishSubject.create();
    private final MutableLiveData<List<MedicalRecord>> searchResults = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    public MedicalRecordsViewModel(Application application) {
        super(application);
        setupSearch();
    }
    
    private void setupSearch() {
        disposables.add(
            searchQuerySubject
                .debounce(300, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .switchMap(query -> 
                    Observable.fromCallable(() -> performSearch(query))
                        .subscribeOn(Schedulers.io())
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    results -> searchResults.setValue(results),
                    error -> searchResults.setValue(Collections.emptyList())
                )
        );
    }
    
    public void search(String query) {
        searchQuerySubject.onNext(query);
    }
    
    private List<MedicalRecord> performSearch(String query) {
        return medicalRecordDao.searchFullText(query);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
```

### 7. MedicineOCRProcessor (New Component)

**Purpose**: OCR processing with ML Kit

**Thread Strategy**: Single-thread executor for image preprocessing, ML Kit handles its own threading

**Implementation**:

```java
public class MedicineOCRProcessor {
    private final TextRecognizer recognizer = 
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public CompletableFuture<List<Medicine>> processPrescriptionImage(Bitmap image) {
        return CompletableFuture
            // Stage 1: Preprocess image
            .supplyAsync(() -> preprocessImage(image), executor)
            
            // Stage 2: Run OCR
            .thenComposeAsync(processedImage -> {
                CompletableFuture<Text> ocrFuture = new CompletableFuture<>();
                
                InputImage inputImage = InputImage.fromBitmap(processedImage, 0);
                recognizer.process(inputImage)
                    .addOnSuccessListener(ocrFuture::complete)
                    .addOnFailureListener(ocrFuture::completeExceptionally);
                
                return ocrFuture;
            })
            
            // Stage 3: Parse medicine info
            .thenApplyAsync(text -> parseMedicineInfo(text), executor);
    }
    
    private Bitmap preprocessImage(Bitmap image) {
        // Enhance contrast, convert to grayscale, etc.
        return image;
    }
    
    private List<Medicine> parseMedicineInfo(Text text) {
        List<Medicine> medicines = new ArrayList<>();
        Pattern pattern = Pattern.compile("([A-Za-z]+)\\s+(\\d+\\s*mg|\\d+\\s*IU)");
        
        for (Text.TextBlock block : text.getTextBlocks()) {
            Matcher matcher = pattern.matcher(block.getText());
            while (matcher.find()) {
                medicines.add(new Medicine(matcher.group(1), matcher.group(2)));
            }
        }
        
        return medicines;
    }
}
```


### 8. HealthInfoRepository with Caching (New Component)

**Purpose**: Multi-level caching (memory + disk) for health articles

**Caching Strategy**:
- L1: LruCache (memory) - instant access
- L2: Room database (disk) - fast access
- L3: Firestore (network) - fallback

**Implementation**:

```java
public class HealthInfoRepository extends BaseRepository {
    private final LruCache<String, HealthArticle> memoryCache;
    private final HealthArticleDao diskCache;
    
    public HealthInfoRepository() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<>(cacheSize);
    }
    
    public CompletableFuture<HealthArticle> getArticle(String articleId) {
        // Check L1 cache
        HealthArticle cached = memoryCache.get(articleId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Check L2 cache and fetch from network if needed
        return CompletableFuture.supplyAsync(() -> {
            // Check disk cache
            HealthArticle diskCached = diskCache.getArticleById(articleId);
            if (diskCached != null) {
                memoryCache.put(articleId, diskCached);
                return diskCached;
            }
            
            // Fetch from network
            HealthArticle article = fetchFromFirestore(articleId);
            
            // Update caches
            memoryCache.put(articleId, article);
            diskCache.insert(article);
            
            return article;
        }, getIoExecutor());
    }
}
```

### 9. DataSyncWorker Enhancement

**Current State**: Basic parallel sync with CompletableFuture

**Enhancements**:
- Add medicines and medical records sync
- Improve conflict resolution
- Add sync progress tracking

**Implementation**:

```java
public class DataSyncWorker extends Worker {
    private static final int THREAD_POOL_SIZE = 4;
    
    @NonNull
    @Override
    public Result doWork() {
        String userId = getCurrentUserId();
        if (userId == null) return Result.success();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        try {
            // Parallel sync of 4 collections
            CompletableFuture<Void> metricsSync = 
                CompletableFuture.runAsync(() -> syncHealthMetrics(userId), executor);
            CompletableFuture<Void> remindersSync = 
                CompletableFuture.runAsync(() -> syncReminders(userId), executor);
            CompletableFuture<Void> medicinesSync = 
                CompletableFuture.runAsync(() -> syncMedicines(userId), executor);
            CompletableFuture<Void> recordsSync = 
                CompletableFuture.runAsync(() -> syncMedicalRecords(userId), executor);
            
            // Wait for all with timeout
            CompletableFuture.allOf(metricsSync, remindersSync, medicinesSync, recordsSync)
                .get(5, TimeUnit.MINUTES);
            
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        } finally {
            executor.shutdown();
        }
    }
    
    private void syncHealthMetrics(String userId) {
        // 1. Get unsynced entities
        List<SyncStatus> needsSync = syncStatusDao.getEntitiesNeedingSyncByType(userId, "health_metric");
        
        // 2. Upload to Firestore
        for (SyncStatus status : needsSync) {
            HealthMetric metric = healthMetricDao.getMetricById(status.getEntityId());
            if (metric != null) {
                uploadHealthMetric(userId, metric);
                syncStatusDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
            }
        }
        
        // 3. Download recent changes
        long lastSyncTime = syncStatusDao.getLastSyncTime(userId, "health_metric");
        List<HealthMetric> remoteMetrics = downloadHealthMetrics(userId, lastSyncTime);
        
        // 4. Resolve conflicts and merge
        for (HealthMetric remote : remoteMetrics) {
            HealthMetric local = healthMetricDao.getMetricById(remote.getId());
            if (local != null && local.getUpdatedAt() > remote.getUpdatedAt()) {
                // Local is newer - upload
                uploadHealthMetric(userId, local);
            } else {
                // Remote is newer - save locally
                healthMetricDao.insertOrUpdate(remote);
            }
        }
    }
}
```


### 10. ReportGenerator Enhancement

**Current State**: Has CompletableFuture pipeline but uses placeholder PDF

**Enhancement**: Integrate iTextPDF library for actual PDF generation

**Implementation**:

```java
public class ReportGenerator {
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService computeExecutor = Executors.newFixedThreadPool(2);
    
    public CompletableFuture<String> generatePDFReport(
            String userId, Date startDate, Date endDate, ProgressCallback callback) {
        
        return CompletableFuture
            // Stage 1: Fetch data (10%)
            .supplyAsync(() -> {
                callback.onProgress(10, "Đang tải dữ liệu...");
                return fetchReportData(userId, startDate, endDate);
            }, ioExecutor)
            
            // Stage 2: Calculate statistics (30-50%)
            .thenApplyAsync(reportData -> {
                callback.onProgress(30, "Đang tính toán thống kê...");
                
                CompletableFuture<Statistics> statsFuture = 
                    CompletableFuture.supplyAsync(() -> 
                        calculateStatistics(reportData.getMetrics()), computeExecutor);
                
                Statistics stats = statsFuture.join();
                reportData.setStatistics(stats);
                
                callback.onProgress(50, "Thống kê hoàn tất");
                return reportData;
            }, computeExecutor)
            
            // Stage 3: Generate charts (60-75%)
            .thenApplyAsync(reportData -> {
                callback.onProgress(60, "Đang tạo biểu đồ...");
                
                List<CompletableFuture<Bitmap>> chartFutures = Arrays.asList(
                    CompletableFuture.supplyAsync(() -> 
                        generateBloodPressureChart(reportData.getMetrics()), computeExecutor),
                    CompletableFuture.supplyAsync(() -> 
                        generateBloodSugarChart(reportData.getMetrics()), computeExecutor),
                    CompletableFuture.supplyAsync(() -> 
                        generateHeartRateChart(reportData.getMetrics()), computeExecutor)
                );
                
                CompletableFuture.allOf(chartFutures.toArray(new CompletableFuture[0])).join();
                
                callback.onProgress(75, "Biểu đồ hoàn tất");
                return reportData;
            }, computeExecutor)
            
            // Stage 4: Create PDF with iTextPDF (80-90%)
            .thenApplyAsync(reportData -> {
                callback.onProgress(80, "Đang tạo file PDF...");
                File pdfFile = createPDFWithiText(reportData);
                callback.onProgress(90, "PDF hoàn tất");
                return pdfFile;
            }, ioExecutor)
            
            // Stage 5: Upload to Storage (95%)
            .thenComposeAsync(pdfFile -> {
                callback.onProgress(95, "Đang tải lên...");
                return uploadToStorage(userId, pdfFile);
            }, ioExecutor)
            
            // Stage 6: Save metadata (98-100%)
            .thenApplyAsync(downloadUrl -> {
                callback.onProgress(98, "Đang lưu metadata...");
                saveReportMetadata(userId, downloadUrl);
                callback.onProgress(100, "Hoàn tất!");
                return downloadUrl;
            }, ioExecutor);
    }
    
    private File createPDFWithiText(ReportData reportData) {
        File pdfFile = new File(context.getCacheDir(), 
            "health_report_" + System.currentTimeMillis() + ".pdf");
        
        try {
            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Add title
            document.add(new Paragraph("Báo Cáo Sức Khỏe")
                .setFontSize(20)
                .setBold());
            
            // Add statistics table
            Table statsTable = new Table(2);
            statsTable.addCell("Chỉ số");
            statsTable.addCell("Giá trị");
            statsTable.addCell("Trung bình");
            statsTable.addCell(String.format("%.2f", reportData.getStatistics().getMean()));
            // ... add more rows
            
            document.add(statsTable);
            
            // Add charts as images
            for (Bitmap chartBitmap : reportData.getChartBitmaps()) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                document.add(new Image(imageData).setWidth(400));
            }
            
            document.close();
            return pdfFile;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PDF", e);
        }
    }
}
```

## Data Models

### AnalysisResult

```java
public class AnalysisResult {
    private Statistics statistics;
    private List<Anomaly> anomalies;
    private List<Recommendation> recommendations;
    private long analyzedAt;
    
    // Getters and setters
}
```

### Statistics

```java
public class Statistics {
    private double mean;
    private double stdDev;
    private double min;
    private double max;
    private double median;
    private String trend; // "INCREASING", "DECREASING", "STABLE"
    
    // Getters and setters
}
```

### Anomaly

```java
public class Anomaly {
    private String metricId;
    private double value;
    private double deviation;
    private String severity; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String message;
    private long detectedAt;
    
    // Getters and setters
}
```

### Recommendation

```java
public class Recommendation {
    private String type; // "LIFESTYLE", "MEDICAL", "MONITORING"
    private String title;
    private String description;
    private String priority; // "LOW", "MEDIUM", "HIGH"
    
    // Getters and setters
}
```


### SmartSuggestion (Existing Model - Enhanced)

```java
@Entity(tableName = "smart_suggestions")
public class SmartSuggestion {
    @PrimaryKey
    @NonNull
    private String suggestionId;
    private String userId;
    private String reminderId;
    private String type; // "TIME_ADJUSTMENT", "FREQUENCY_CHANGE", "CONTENT_UPDATE"
    private String title;
    private String description;
    private String currentValue;
    private String suggestedValue;
    private String reason;
    private String status; // "pending", "applied", "dismissed"
    private long createdAt;
    private long appliedAt;
    private long dismissedAt;
    
    // Getters and setters
}
```

### UserBehavior (Existing Model - Enhanced)

```java
public class UserBehavior {
    private double completionRate;
    private Map<Integer, Double> hourlyCompletionRates; // Hour -> completion rate
    private List<Integer> preferredHours;
    private int totalReminders;
    private int completedReminders;
    private int missedReminders;
    private long analyzedAt;
    
    // Getters and setters
}
```

## Error Handling

### Error Handling Strategy

1. **Repository Level**: Catch exceptions, wrap in Result/DataState
2. **ViewModel Level**: Observe errors, update UI state
3. **Worker Level**: Return Result.retry() for transient errors
4. **UI Level**: Display user-friendly error messages

### Result Wrapper Pattern

```java
public class Result<T> {
    private final T data;
    private final Throwable error;
    private final Status status;
    
    public enum Status {
        SUCCESS, ERROR, LOADING
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, Status.SUCCESS);
    }
    
    public static <T> Result<T> error(Throwable error) {
        return new Result<>(null, error, Status.ERROR);
    }
    
    public static <T> Result<T> loading() {
        return new Result<>(null, null, Status.LOADING);
    }
    
    // Getters
}
```

### CompletableFuture Error Handling

```java
CompletableFuture<Data> future = fetchDataAsync()
    .exceptionally(throwable -> {
        Log.e(TAG, "Error fetching data", throwable);
        
        // Return default value or rethrow
        if (throwable instanceof NetworkException) {
            return getCachedData();
        } else {
            throw new CompletionException(throwable);
        }
    })
    .whenComplete((data, throwable) -> {
        if (throwable != null) {
            // Log error, send analytics
            Analytics.logError("fetch_data_failed", throwable);
        }
    });
```

### Retry Logic

```java
public <T> CompletableFuture<T> executeWithRetry(
        Supplier<T> task, int maxRetries, long delayMs) {
    
    return CompletableFuture.supplyAsync(task, executor)
        .exceptionally(throwable -> {
            if (maxRetries > 0) {
                try {
                    Thread.sleep(delayMs);
                    return executeWithRetry(task, maxRetries - 1, delayMs * 2).join();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
            throw new CompletionException(throwable);
        });
}
```

## Testing Strategy

### Unit Testing

**Test Async Operations**:
- Use `CompletableFuture.get()` with timeout in tests
- Mock ExecutorService with immediate execution
- Test error handling paths

```java
@Test
public void testAnalyzeMetrics_success() throws Exception {
    // Arrange
    List<HealthMetric> metrics = createTestMetrics();
    when(healthMetricDao.getMetricsByType(anyString(), anyInt()))
        .thenReturn(metrics);
    
    // Act
    CompletableFuture<AnalysisResult> future = 
        analyzer.analyzeMetrics("user123", "blood_pressure", 30);
    AnalysisResult result = future.get(5, TimeUnit.SECONDS);
    
    // Assert
    assertNotNull(result);
    assertEquals(120.0, result.getStatistics().getMean(), 0.1);
}
```

### Integration Testing

**Test WorkManager Workers**:
- Use TestWorkerBuilder
- Verify database state after sync
- Test constraint handling

```java
@Test
public void testDataSyncWorker_syncsSuccessfully() {
    // Arrange
    Context context = ApplicationProvider.getApplicationContext();
    WorkerParameters params = TestWorkerBuilder.from(context, DataSyncWorker.class).build();
    DataSyncWorker worker = new DataSyncWorker(context, params);
    
    // Act
    Worker.Result result = worker.doWork();
    
    // Assert
    assertEquals(Worker.Result.success(), result);
    // Verify database state
}
```

### Performance Testing

**Measure Async Performance**:
- Track execution time for each stage
- Monitor thread pool utilization
- Measure memory usage

```java
@Test
public void testReportGeneration_completesWithin15Seconds() throws Exception {
    long startTime = System.currentTimeMillis();
    
    CompletableFuture<String> future = reportGenerator.generatePDFReport(
        "user123", startDate, endDate, (progress, message) -> {}
    );
    
    String downloadUrl = future.get(20, TimeUnit.SECONDS);
    long duration = System.currentTimeMillis() - startTime;
    
    assertNotNull(downloadUrl);
    assertTrue("Report generation took too long: " + duration + "ms", 
        duration < 15000);
}
```


## Performance Considerations

### Thread Pool Sizing

**IO Thread Pool**: 4 threads
- Handles network requests, database operations, file I/O
- Sized for typical I/O-bound workloads

**Compute Thread Pool**: 2 threads
- Handles CPU-intensive tasks (statistics, chart rendering)
- Sized based on available CPU cores

**WorkManager**: System-managed
- Respects system constraints (battery, network)
- Automatic retry and backoff

### Memory Management

**LruCache Sizing**: 1/8 of available heap
- Automatically evicts least recently used items
- Prevents OutOfMemoryError

**Bitmap Recycling**: Recycle chart bitmaps after PDF creation
```java
for (Bitmap bitmap : chartBitmaps) {
    if (bitmap != null && !bitmap.isRecycled()) {
        bitmap.recycle();
    }
}
```

**ExecutorService Cleanup**: Shutdown in onCleared()
```java
@Override
protected void onCleared() {
    super.onCleared();
    if (executor != null && !executor.isShutdown()) {
        executor.shutdown();
    }
}
```

### Network Optimization

**Firestore Listeners**: Use ExecutorService for listener callbacks
```java
firestore.collection("items")
    .addSnapshotListener(executor, (snapshot, error) -> {
        // Process on background thread
    });
```

**Batch Operations**: Group multiple writes
```java
WriteBatch batch = firestore.batch();
for (HealthMetric metric : metrics) {
    batch.set(docRef, metric);
}
batch.commit();
```

## Security Considerations

### Data Validation

**Input Validation**: Validate before async operations
```java
public CompletableFuture<User> loginWithEmailAsync(String email, String password) {
    if (!isValidEmail(email)) {
        return CompletableFuture.failedFuture(
            new IllegalArgumentException("Invalid email"));
    }
    // Continue with async operation
}
```

### Secure Data Transmission

**HTTPS Only**: Firebase enforces HTTPS
**Token Refresh**: Handle expired tokens in async operations
```java
.exceptionally(throwable -> {
    if (throwable instanceof FirebaseAuthException) {
        // Refresh token and retry
        return refreshTokenAndRetry();
    }
    throw new CompletionException(throwable);
})
```

## Migration Strategy

### Phase 1: Foundation (Week 1-2)
1. Enhance BaseRepository with shared executors
2. Add Result wrapper classes
3. Update AuthRepository with CompletableFuture methods

### Phase 2: Core Features (Week 3-4)
4. Implement HealthMetricsAnalyzer
5. Enhance DashboardViewModel with parallel loading
6. Add MedicalRecordsViewModel with RxJava search

### Phase 3: Advanced Features (Week 5-6)
7. Implement MedicineOCRProcessor
8. Add HealthInfoRepository with caching
9. Enhance ReportGenerator with iTextPDF

### Phase 4: Background Services (Week 7-8)
10. Enhance DataSyncWorker with 4-collection sync
11. Improve SmartReminderWorker analysis
12. Add comprehensive error handling and retry logic

### Phase 5: Testing & Optimization (Week 9-10)
13. Write unit tests for all async operations
14. Perform integration testing
15. Optimize thread pool sizes and memory usage
16. Load testing and performance tuning

## Monitoring and Debugging

### Logging Strategy

```java
private static final String TAG = "HealthMetricsAnalyzer";

Log.d(TAG, "Starting analysis for user: " + userId);
Log.d(TAG, "Fetched " + metrics.size() + " metrics");
Log.d(TAG, "Analysis completed in " + duration + "ms");
```

### Performance Metrics

Track key metrics:
- Async operation duration
- Thread pool queue size
- Memory usage
- Network request count
- Cache hit rate

### Crash Reporting

Integrate Firebase Crashlytics for async errors:
```java
.exceptionally(throwable -> {
    FirebaseCrashlytics.getInstance().recordException(throwable);
    return null;
})
```

## Conclusion

This design provides a comprehensive async/multithreading architecture that:
- ✅ Ensures responsive UI with non-blocking operations
- ✅ Leverages modern concurrency patterns (CompletableFuture, RxJava, WorkManager)
- ✅ Implements offline-first with Room + Firestore sync
- ✅ Provides real-time updates with Firestore listeners
- ✅ Includes robust error handling and retry logic
- ✅ Optimizes performance with thread pools and caching
- ✅ Supports complex workflows (report generation, OCR, analysis)

The architecture is scalable, maintainable, and follows Android best practices.
