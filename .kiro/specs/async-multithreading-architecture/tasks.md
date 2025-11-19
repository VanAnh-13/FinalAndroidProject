# Implementation Plan

This implementation plan breaks down the async/multithreading architecture enhancements into discrete, actionable coding tasks. Each task builds incrementally on previous work and references specific requirements from the requirements document.

## Task List

- [x] 1. Enhance BaseRepository with shared thread pools and async utilities



  - Create static ExecutorService instances (IO: 4 threads, Compute: 2 threads)
  - Add getIoExecutor(), getComputeExecutor(), getMainExecutor() methods
  - Implement executeAsync(Callable<T>) helper method returning CompletableFuture
  - Add executeParallel(List<Callable<T>>) for parallel task execution
  - _Requirements: 1.5, 3.4, 4.1, 6.1, 7.1, 12.2_

- [x] 2. Create Result wrapper and error handling utilities


  - Implement Result<T> class with SUCCESS, ERROR, LOADING states
  - Add static factory methods: success(T), error(Throwable), loading()
  - Create RetryHelper utility with executeWithRetry() method supporting exponential backoff
  - _Requirements: 1.4, 2.3, 4.4, 7.5_



- [-] 3. Enhance AuthRepository with CompletableFuture-based authentication


  - [x] 3.1 Implement loginWithEmailAsync(String, String) returning CompletableFuture<User>

    - Use CompletableFuture.supplyAsync() with getIoExecutor()
    - Wrap Firebase signInWithEmailAndPassword with Tasks.await()
    - Convert FirebaseUser to User model on background thread
    - _Requirements: 1.1, 1.3_
  
  - [x] 3.2 Implement registerWithEmailAsync(String, String, UserProfile) with RxJava chain


    - Create Observable chain: createUser → createFirestoreDoc → sendVerification
    - Use Schedulers.io() for background operations
    - Use AndroidSchedulers.mainThread() for result delivery
    - _Requirements: 2.1, 2.2, 2.4_
  

  - [x] 3.3 Implement loginWithGoogleAsync(Intent) returning CompletableFuture<User>


    - Process Google Sign-In result on background thread
    - Handle token exchange asynchronously
    - _Requirements: 1.1, 1.3_
  -

  - [x] 3.4 Write unit tests for AuthRepository async methods




    - Test successful login flow with mocked Firebase
    - Test error handling and timeout scenarios
    - Verify thread execution on correct executors
    - _Requirements: 1.1, 1.4, 2.1_

- [ ] 4. Enhance MetricsRepository with analysis integration

  - [x] 4.1 Implement saveMetricWithAnalysis(HealthMetric) with CompletableFuture pipeline




    - Stage 1: Save to Room on IO thread
    - Stage 2: Upload to Firestore on IO thread
    - Stage 3: Trigger HealthMetricsAnalyzer on compute thread
    - Stage 4: Handle analysis results (notifications) on main thread
    - _Requirements: 4.1, 4.2, 4.5_
  




  - [x] 4.2 Add getCachedMetrics(String metricType) for instant access








    - Check Room database first
    - Return cached data immediately
    - _Requirements: 10.1, 10.5_
  
  - [x] 4.3 Enhance loadMetricHistory() with ExecutorService for Firestore listener






    - Pass executor to addSnapshotListener()
    - Process snapshots on background thread
    - Post results to LiveData on main thread
    - _Requirements: 3.2, 3.3, 6.2_


- [x] 5. Implement HealthMetricsAnalyzer for automated anomaly detection





  - [x] 5.1 Create HealthMetricsAnalyzer class with ThreadPoolExecutor (2-4 threads)


    - Initialize ThreadPoolExecutor with LinkedBlockingQueue
    - Add shutdown() method for cleanup
    - _Requirements: 4.1, 4.2_
  
  - [x] 5.2 Implement analyzeMetrics(String userId, String metricType, int days)


    - Stage 1: Fetch metrics from AppDatabase on worker thread
    - Stage 2: Calculate statistics (mean, stdDev, trend) on worker thread
    - Stage 3: Detect anomalies (>2 stdDev) on worker thread
    - Stage 4: Generate recommendations on worker thread
    - Return CompletableFuture<AnalysisResult>
    - _Requirements: 4.2, 4.3_
  
  - [x] 5.3 Implement calculateStatistics(List<HealthMetric>) method


    - Calculate mean using stream operations
    - Calculate standard deviation and variance
    - Determine trend (INCREASING, DECREASING, STABLE)
    - Return Statistics object
    - _Requirements: 4.3_
  
  - [x] 5.4 Implement detectAnomalies(List<HealthMetric>, Statistics) method



    - Filter metrics exceeding 2 standard deviations
    - Create Anomaly objects with severity levels
    - Return List<Anomaly>
    - _Requirements: 4.4_
  
  - [x] 5.5 Implement generateRecommendations(Statistics, List<Anomaly>) method

    - Analyze anomaly patterns
    - Create Recommendation objects (LIFESTYLE, MEDICAL, MONITORING)
    - Prioritize recommendations
    - _Requirements: 4.2_
  
  - [x] 5.6 Integrate with NotificationHelper for anomaly alerts

    - Check if anomaly severity is HIGH or CRITICAL
    - Call NotificationHelper.sendAnomalyAlert() on background thread
    - _Requirements: 4.4_
  
  - [ ]* 5.7 Write unit tests for HealthMetricsAnalyzer
    - Test statistics calculation accuracy
    - Test anomaly detection with edge cases
    - Test CompletableFuture completion within 3 seconds
    - _Requirements: 4.3_
-

- [x] 6. Enhance DashboardViewModel with parallel data loading







  - [x] 6.1 Replace Handler/Thread with CompletableFuture.allOf() in loadDashboardData()


    - Create CompletableFuture for reminders, metrics, notifications
    - Use CompletableFuture.allOf() to wait for all
    - Post results to MediatorLiveData on main thread
    - _Requirements: 6.1, 6.4_
  
  - [x] 6.2 Add synchronous repository methods for parallel execution


    - Add loadRemindersSync() to RemindersRepository
    - Add loadLatestMetricsSync() to MetricsRepository
    - Add getUnreadCountSync() to NotificationsRepository
    - Execute on background threads via CompletableFuture
    - _Requirements: 6.1_
  
  - [ ]* 6.3 Write integration tests for DashboardViewModel
    - Test parallel loading completes within 5 seconds
    - Verify all LiveData sources update correctly
    - Test error handling when one source fails
    - _Requirements: 6.4_

-

- [x] 7. Implement MedicalRecordsViewModel with RxJava search






  - [x] 7.1 Add RxJava3 dependencies to build.gradle

    - Add io.reactivex.rxjava3:rxjava
    - Add io.reactivex.rxjava3:rxandroid
    - Sync project
    - _Requirements: 8.1_
  
  - [x] 7.2 Create MedicalRecordsViewModel with debounced search


    - Initialize PublishSubject<String> for search queries
    - Setup debounce(300ms), distinctUntilChanged(), switchMap() chain
    - Use Schedulers.io() for search execution
    - Use AndroidSchedulers.mainThread() for result delivery
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 7.3 Implement performSearch(String query) method


    - Query Room FTS table via MedicalRecordDao
    - Return search results within 500ms
    - _Requirements: 8.2, 8.4_

  
  - [x] 7.4 Add CompositeDisposable cleanup in onCleared()


    - Clear disposables to prevent memory leaks
    - _Requirements: 8.1_
  
  - [ ]* 7.5 Create Room FTS table for medical records
    - Define @Fts4 entity for full-text search
    - Create DAO with @Query for FTS
    - Populate FTS table from existing records
    - _Requirements: 8.2, 8.4_
  
  - [ ]* 7.6 Write unit tests for search functionality
    - Test debounce behavior (rapid queries)
    - Test distinctUntilChanged (duplicate queries)
    - Test search cancellation with switchMap
    - _Requirements: 8.1, 8.3_

- [x] 8. Implement MedicineOCRProcessor with ML Kit







  - [x] 8.1 Add ML Kit Text Recognition dependency to build.gradle


    - Add com.google.mlkit:text-recognition
    - Sync project
    - _Requirements: 9.1, 9.2_
  
  - [x] 8.2 Create MedicineOCRProcessor class with single-thread executor


    - Initialize TextRecognizer from ML Kit
    - Create ExecutorService with single thread for preprocessing
    - _Requirements: 9.1_
  
  - [x] 8.3 Implement processPrescriptionImage(Bitmap) with CompletableFuture pipeline


    - Stage 1: Preprocess image (grayscale, contrast) on executor
    - Stage 2: Run ML Kit OCR (returns Task, convert to CompletableFuture)
    - Stage 3: Parse medicine info with regex on executor
    - Return CompletableFuture<List<Medicine>>
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  
  - [x] 8.4 Implement preprocessImage(Bitmap) method


    - Convert to grayscale
    - Enhance contrast
    - Return processed Bitmap
    - _Requirements: 9.1_
  
  - [x] 8.5 Implement parseMedicineInfo(Text) method


    - Use regex pattern to extract medicine names and dosages
    - Pattern: "([A-Za-z]+)\\s+(\\d+\\s*mg|\\d+\\s*IU)"
    - Create Medicine objects from matches
    - _Requirements: 9.3_
  
  - [ ]* 8.6 Write integration tests for OCR processing
    - Test with sample prescription images
    - Verify completion within 5 seconds
    - Test error handling for invalid images
    - _Requirements: 9.4_

-

- [x] 9. Implement HealthInfoRepository with multi-level caching







  - [x] 9.1 Create HealthArticle Room entity and DAO


    - Define @Entity with id, title, content, category, cachedAt fields
    - Create DAO with insert, getArticleById, deleteOldArticles methods
    - _Requirements: 10.2, 10.4_
  
  - [x] 9.2 Create HealthInfoRepository with LruCache


    - Initialize LruCache with size = maxMemory / 8
    - Add HealthArticleDao for disk cache
    - _Requirements: 10.1, 10.2_
  
  - [x] 9.3 Implement getArticle(String articleId) with 3-level cache


    - L1: Check memoryCache.get() - return immediately if found
    - L2: Check diskCache (Room) on IO thread
    - L3: Fetch from Firestore on IO thread
    - Update both caches after network fetch
    - Return CompletableFuture<HealthArticle>
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 9.4 Implement cache eviction strategy


    - LruCache automatically evicts least recently used
    - Add method to clear old disk cache entries (>7 days)
    - _Requirements: 10.4_
  
  - [ ]* 9.5 Write unit tests for caching behavior
    - Test L1 cache hit (instant return)
    - Test L2 cache hit (fast return)
    - Test L3 network fetch (slower)
    - Verify cache updates after fetch
    - _Requirements: 10.1, 10.5_

- [x] 10. Enhance DataSyncWorker with 4-collection sync





  - [x] 10.1 Add syncMedicines(String userId) method


    - Query SyncStatusDao for unsynced medicines
    - Upload to Firestore users/{userId}/medicines
    - Download recent changes
    - Resolve conflicts with last-write-wins
    - _Requirements: 12.2, 12.3, 12.4_
  
  - [x] 10.2 Add syncMedicalRecords(String userId) method


    - Query SyncStatusDao for unsynced medical records
    - Upload to Firestore users/{userId}/medicalRecords
    - Download recent changes
    - Resolve conflicts with last-write-wins
    - _Requirements: 12.2, 12.3, 12.4_
  
  - [x] 10.3 Update doWork() to sync 4 collections in parallel

    - Create CompletableFuture for metrics, reminders, medicines, records
    - Use CompletableFuture.allOf() with 4-thread executor
    - Add 5-minute timeout with .get(5, TimeUnit.MINUTES)
    - _Requirements: 12.2, 12.5_
  
  - [x] 10.4 Enhance resolveConflict() with timestamp comparison

    - Compare local.updatedAt vs remote.updatedAt
    - Keep newer version (last-write-wins)
    - Upload local if local is newer, save remote if remote is newer
    - _Requirements: 12.4_
  
  - [x] 10.5 Add sync progress tracking to SyncStatus table


    - Update status: pending → syncing → success/failed
    - Store lastSyncTime and error message
    - _Requirements: 12.3, 12.7_
  
  - [ ]* 10.6 Write integration tests for DataSyncWorker
    - Test 4-collection parallel sync
    - Test conflict resolution scenarios
    - Verify completion within 30 seconds for 100 records
    - Test retry on failure
    - _Requirements: 12.2, 12.4, 12.5_


- [x] 11. Enhance SmartReminderWorker with improved behavior analysis





  - [x] 11.1 Enhance SmartReminderAI.analyzeUserBehavior() method


    - Calculate hourly completion rates (Map<Integer, Double>)
    - Identify preferred hours (top 3 hours with highest completion)
    - Calculate overall completion rate
    - Return enhanced UserBehavior object
    - _Requirements: 5.3_
  
  - [x] 11.2 Enhance SmartReminderAI.generateSmartSuggestions() method


    - Compare current reminder time with preferred hours
    - Generate TIME_ADJUSTMENT suggestions if difference > 30 minutes
    - Generate FREQUENCY_CHANGE suggestions based on completion patterns
    - Return List<SmartSuggestion>
    - _Requirements: 5.3, 5.4_
  
  - [x] 11.3 Update SmartReminderWorker to save suggestions to Firestore


    - Create document in users/{userId}/suggestions collection
    - Set suggestionId, type, title, description, status="pending"
    - Set createdAt timestamp
    - _Requirements: 5.4_
  
  - [x] 11.4 Add notification for new suggestions


    - Check NotificationSettings before sending
    - Use NotificationHelper.showSuggestionNotification()
    - Include suggestion count in notification
    - _Requirements: 5.5_
  
  - [ ]* 11.5 Write unit tests for SmartReminderAI
    - Test behavior analysis with various completion patterns
    - Test suggestion generation logic
    - Verify 30-minute threshold for time adjustments
    - _Requirements: 5.3, 5.4_

- [x] 12. Enhance ReportGenerator with iTextPDF integration




  - [x] 12.1 Add iTextPDF dependency to build.gradle


    - Add com.itextpdf:itext7-core
    - Sync project
    - _Requirements: 7.4_
  
  - [x] 12.2 Implement createPDFWithiText(ReportData) method


    - Create PdfWriter and PdfDocument
    - Add title paragraph with formatting
    - Create statistics table with data
    - Add chart images from Bitmaps
    - Close document and return File
    - _Requirements: 7.4_
  
  - [x] 12.3 Enhance generatePDFReport() progress callbacks


    - Update callback at each stage: 10%, 30%, 50%, 60%, 75%, 80%, 90%, 95%, 98%, 100%
    - Include descriptive messages for each stage
    - _Requirements: 7.6_
  
  - [x] 12.4 Add chart generation methods for all metric types


    - Implement generateHeartRateChart(List<HealthMetric>)
    - Implement generateWeightChart(List<HealthMetric>)
    - Implement generateTemperatureChart(List<HealthMetric>)
    - Use MPAndroidChart LineChart for all
    - _Requirements: 7.2, 7.3_
  
  - [x] 12.5 Optimize chart rendering to Bitmap


    - Set chart dimensions to 800x600
    - Use ARGB_8888 config for quality
    - Recycle bitmaps after PDF creation
    - _Requirements: 7.3_
  
  - [ ]* 12.6 Write integration tests for report generation
    - Test full pipeline with 30 days of data
    - Verify completion within 15 seconds
    - Test progress callback updates
    - Verify PDF file creation and upload
    - _Requirements: 7.5, 7.6_


- [x] 13. Update LoginViewModel to use CompletableFuture-based AuthRepository





  - [x] 13.1 Replace observeForever pattern with CompletableFuture in login() method


    - Call authRepository.loginWithEmailAsync()
    - Use thenAcceptAsync() to update loginState LiveData on main thread
    - Use exceptionally() for error handling
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [x] 13.2 Update handleGoogleSignInResult() to use loginWithGoogleAsync()


    - Call authRepository.loginWithGoogleAsync()
    - Update loginState with CompletableFuture result
    - _Requirements: 1.1, 1.3_
  
  - [x] 13.3 Remove observeForever observers to prevent memory leaks


    - Remove loginObserver and googleSignInObserver fields
    - Clean up in onCleared() if needed
    - _Requirements: 1.5_

- [x] 14. Create RegisterViewModel with RxJava registration flow





  - [x] 14.1 Create RegisterViewModel extending BaseViewModel


    - Add MutableLiveData<DataState<User>> for registration state
    - Add CompositeDisposable for RxJava cleanup
    - _Requirements: 2.1_
  
  - [x] 14.2 Implement register(String email, String password, UserProfile profile) method


    - Call authRepository.registerWithEmailAsync()
    - Observe Observable chain with loading, success, error states
    - Update registrationState LiveData
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [x] 14.3 Add input validation before registration


    - Validate email format
    - Validate password strength (min 6 chars)
    - Validate required profile fields
    - _Requirements: 2.1_
  
  - [x] 14.4 Add cancel registration functionality


    - Dispose of ongoing Observable chain
    - Reset registration state
    - _Requirements: 2.5_

- [x] 15. Update ProfileViewModel to use enhanced MetricsRepository





  - [x] 15.1 Update saveProfile() to use CompletableFuture


    - Call repository.updateProfile() returning CompletableFuture
    - Update UI state on main thread
    - Handle errors with user-friendly messages
    - _Requirements: 3.4_
  
  - [x] 15.2 Ensure Firestore listener uses ExecutorService


    - Pass executor to addSnapshotListener()
    - Process updates on background thread
    - _Requirements: 3.2, 3.3_

- [x] 16. Create AnalyticsViewModel to display analysis results







  - [x] 16.1 Create AnalyticsViewModel with LiveData for AnalysisResult


    - Add MutableLiveData<AnalysisResult> for analysis data
    - Add MutableLiveData<Boolean> for loading state
    - _Requirements: 4.5_
  
  - [x] 16.2 Implement analyzeMetrics(String metricType, int days) method


    - Call healthMetricsAnalyzer.analyzeMetrics()
    - Update LiveData with results on main thread
    - Show loading indicator during analysis
    - _Requirements: 4.2, 4.5_
  
  - [x] 16.3 Add method to display anomaly notifications

    - Check if analysis has anomalies
    - Display in-app notification or dialog
    - _Requirements: 4.4_


- [x] 17. Create data model classes for analysis features




  - [x] 17.1 Create AnalysisResult class

    - Add fields: Statistics, List<Anomaly>, List<Recommendation>, analyzedAt
    - Add getters, setters, and constructors
    - _Requirements: 4.2, 4.5_
  
  - [x] 17.2 Create Statistics class

    - Add fields: mean, stdDev, min, max, median, trend
    - Add getters, setters, and constructors
    - _Requirements: 4.3_
  
  - [x] 17.3 Create Anomaly class

    - Add fields: metricId, value, deviation, severity, message, detectedAt
    - Add getters, setters, and constructors
    - _Requirements: 4.4_
  
  - [x] 17.4 Create Recommendation class

    - Add fields: type, title, description, priority
    - Add getters, setters, and constructors
    - _Requirements: 4.2_
  
  - [x] 17.5 Enhance SmartSuggestion entity with new fields

    - Add reason field for explanation
    - Add currentValue and suggestedValue fields
    - Update Room schema version
    - _Requirements: 5.4_
  
  - [x] 17.6 Enhance UserBehavior class

    - Add hourlyCompletionRates Map<Integer, Double>
    - Add preferredHours List<Integer>
    - Add analyzedAt timestamp
    - _Requirements: 5.3_

- [x] 18. Update SyncManager to schedule enhanced DataSyncWorker





  - [x] 18.1 Verify periodic sync schedule (15 min interval, 5 min flex)


    - Ensure NetworkType.CONNECTED constraint
    - Ensure RequiresBatteryNotLow constraint
    - _Requirements: 12.1_
  
  - [x] 18.2 Add method to trigger immediate sync after data changes


    - Call triggerImmediateSync() after metric save
    - Call after reminder create/update
    - _Requirements: 12.1_

- [x] 19. Add NetworkMonitor integration for connectivity detection




  - [x] 19.1 Enhance NetworkMonitor to trigger sync on connectivity restore


    - Register ConnectivityManager callback
    - Call SyncManager.triggerImmediateSync() when network available
    - _Requirements: 12.1_
  

  - [x] 19.2 Add LiveData<Boolean> for network status

    - Expose isConnected LiveData
    - Update UI to show offline indicator
    - _Requirements: 12.6_

- [x] 20. Add comprehensive error handling and logging








  - [x] 20.1 Add error logging to all CompletableFuture chains


    - Use .exceptionally() to log errors
    - Include context (user ID, operation type)
    - _Requirements: 1.4, 2.3, 4.4_
  
  - [x] 20.2 Integrate Firebase Crashlytics for async errors


    - Record exceptions in .exceptionally() blocks
    - Add custom keys for debugging (userId, metricType, etc.)
    - _Requirements: 1.4, 2.3_
  
  - [x] 20.3 Add performance logging for async operations


    - Log start and end times
    - Calculate and log duration
    - Log thread pool queue sizes
    - _Requirements: 4.3, 6.4, 7.5, 12.5_


- [x] 21. Update UI components to handle async operations




  - [x] 21.1 Update LoginActivity to observe CompletableFuture-based login

    - Show loading indicator during authentication
    - Handle success navigation to dashboard
    - Display error messages from DataState
    - _Requirements: 1.2, 1.3, 1.4_
  
  - [x] 21.2 Update DashboardFragment to observe parallel loading

    - Show shimmer/skeleton loading for each section
    - Update UI when each CompletableFuture completes
    - Handle partial failures gracefully
    - _Requirements: 6.1, 6.3, 6.4_
  
  - [x] 21.3 Create AnalyticsFragment to display analysis results

    - Show Statistics in cards (mean, stdDev, trend)
    - Display anomalies list with severity indicators
    - Show recommendations with priority badges
    - _Requirements: 4.5_
  
  - [x] 21.4 Create MedicineOCRActivity for prescription scanning

    - Capture image with camera
    - Show processing indicator during OCR
    - Display extracted medicines for confirmation
    - Allow manual editing before saving
    - _Requirements: 9.1, 9.4, 9.5_
  
  - [x] 21.5 Update ReportActivity to show progress during generation

    - Display ProgressBar with percentage
    - Show stage messages (fetching, calculating, rendering, etc.)
    - Enable share button when complete
    - _Requirements: 7.6_

- [x] 22. Optimize thread pool configurations





  - [x] 22.1 Benchmark IO thread pool size (test 2, 4, 8 threads)


    - Measure performance with different sizes
    - Monitor queue length and wait times
    - Select optimal size based on results
    - _Requirements: 6.4, 7.5, 12.5_
  
  - [x] 22.2 Benchmark compute thread pool size (test 1, 2, 4 threads)

    - Measure statistics calculation performance
    - Monitor CPU utilization
    - Select optimal size based on CPU cores
    - _Requirements: 4.3_
  
  - [x] 22.3 Add thread pool monitoring and metrics


    - Log active thread count
    - Log queue size
    - Log completed task count
    - _Requirements: 4.3, 6.4, 7.5_

- [x] 23. Add memory optimization for caching and bitmaps





  - [x] 23.1 Implement bitmap recycling in ReportGenerator


    - Recycle chart bitmaps after PDF creation
    - Check !bitmap.isRecycled() before recycling
    - _Requirements: 7.3_
  
  - [x] 23.2 Add cache size monitoring for LruCache


    - Log cache hit rate
    - Log cache size and max size
    - Adjust cache size if needed
    - _Requirements: 10.1, 10.5_
  
  - [x] 23.3 Implement disk cache cleanup for old articles


    - Delete articles cached > 7 days ago
    - Run cleanup in background thread
    - _Requirements: 10.4_

- [x] 24. Final integration and testing




  - [ ]* 24.1 Write end-to-end integration tests
    - Test complete user flow: login → dashboard → add metric → view analysis
    - Test offline mode: add data offline → sync when online
    - Test report generation: request report → download PDF
    - _Requirements: All_
  
  - [ ]* 24.2 Perform load testing with large datasets
    - Test with 1000+ health metrics
    - Test with 100+ reminders
    - Verify performance meets requirements
    - _Requirements: 4.3, 6.4, 7.5, 12.5_
  
  - [ ]* 24.3 Test memory usage and leak detection
    - Use Android Profiler to monitor memory
    - Check for memory leaks in async operations
    - Verify ExecutorService cleanup
    - _Requirements: All_
  
  - [ ]* 24.4 Test battery impact of background workers
    - Monitor battery usage with WorkManager tasks
    - Verify constraints are respected
    - Optimize if battery drain is excessive
    - _Requirements: 5.1, 12.1_

## Notes

- Tasks marked with `*` are optional testing tasks that can be skipped for faster MVP delivery
- Each task includes requirement references for traceability
- Tasks are ordered to minimize dependencies and enable parallel development where possible
- Estimated total implementation time: 8-10 weeks for full implementation, 4-6 weeks for MVP (skipping optional tasks)
