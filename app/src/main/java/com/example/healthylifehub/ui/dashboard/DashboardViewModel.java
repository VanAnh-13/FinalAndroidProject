package com.example.healthylifehub.ui.dashboard;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.repository.NotificationsRepository;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.data.model.MetricHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardViewModel extends BaseViewModel {

    private static final String TAG = "DashboardViewModel";

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<String> greeting = new MutableLiveData<>();
    private final MediatorLiveData<List<Reminder>> reminders = new MediatorLiveData<>();
    private final MediatorLiveData<Integer> notificationCount = new MediatorLiveData<>();
    private final MediatorLiveData<java.util.Map<String, String>> latestMetrics = new MediatorLiveData<>();
    private final MediatorLiveData<List<MetricHistory>> bloodPressureHistory = new MediatorLiveData<>();


    private final RemindersRepository remindersRepository;
    private final NotificationsRepository notificationsRepository;
    private final com.example.healthylifehub.data.repository.MetricsRepository metricsRepository;

    // Shared executor for parallel data loading
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Executor mainExecutor;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        
        remindersRepository = new RemindersRepository();
        notificationsRepository = new NotificationsRepository();
        metricsRepository = new com.example.healthylifehub.data.repository.MetricsRepository(application.getApplicationContext());
        mainExecutor = ContextCompat.getMainExecutor(application.getApplicationContext());

        // Set default values to prevent UI crashes
        userName.setValue("User");
        updateGreeting();
        reminders.setValue(new java.util.ArrayList<>());
        notificationCount.setValue(0);
        latestMetrics.setValue(new java.util.HashMap<>());
        
        // Load user name first, then load dashboard data in parallel
        loadUserName();
        loadDashboardData();
    }
    
    /**
     * Load dashboard data in parallel using CompletableFuture.allOf().
     *
     * Enhancement: Replaces Handler/Thread with CompletableFuture for parallel loading
     * - Creates CompletableFuture for reminders, metrics, notifications
     * - Uses CompletableFuture.allOf() to wait for all
     * - Posts results to MediatorLiveData on main thread
     *
     * Requirements: 6.1, 6.4
     * - 6.1: Fetch latest metrics, reminders, and notification count in parallel using CompletableFuture.allOf()
     * - 6.4: Complete initial dashboard load within 5 seconds with all parallel CompletableFuture operations
     */
    private void loadDashboardData() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "⚠️ User not logged in, skipping dashboard data load");
            return;
        }

        Log.d(TAG, "🚀 Starting parallel dashboard data load for user: " + userId);
        long startTime = System.currentTimeMillis();

        // Create CompletableFuture for each data source
        CompletableFuture<List<Reminder>> remindersFuture =
            CompletableFuture.supplyAsync(() ->
                remindersRepository.loadRemindersSync(userId), executor);

        CompletableFuture<Map<String, String>> metricsFuture =
            CompletableFuture.supplyAsync(() ->
                metricsRepository.loadLatestMetricsSync(userId), executor);

        CompletableFuture<Integer> notificationsFuture =
            CompletableFuture.supplyAsync(() ->
                notificationsRepository.getUnreadCountSync(userId), executor);

        CompletableFuture<List<MetricHistory>> bloodPressureFuture =
            CompletableFuture.supplyAsync(() ->
                metricsRepository.loadMetricHistorySync(userId, "blood_pressure"), executor);

        // Wait for all futures to complete and post results to LiveData on main thread
        CompletableFuture.allOf(remindersFuture, metricsFuture, notificationsFuture, bloodPressureFuture)
            .thenAcceptAsync(v -> {
                try {
                    // Get results from completed futures
                    List<Reminder> remindersResult = remindersFuture.get();
                    Map<String, String> metricsResult = metricsFuture.get();
                    Integer notificationsResult = notificationsFuture.get();
                    List<MetricHistory> bloodPressureResult = bloodPressureFuture.get();

                    // Post results to LiveData on main thread
                    reminders.postValue(remindersResult);
                    latestMetrics.postValue(metricsResult);
                    notificationCount.postValue(notificationsResult);
                    bloodPressureHistory.postValue(bloodPressureResult);

                    long duration = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "✅ Dashboard data loaded in " + duration + "ms");
                    Log.d(TAG, "   - Reminders: " + remindersResult.size());
                    Log.d(TAG, "   - Metrics: " + metricsResult.size());
                    Log.d(TAG, "   - Notifications: " + notificationsResult);
                    Log.d(TAG, "   - Blood Pressure History: " + bloodPressureResult.size());

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error getting results from CompletableFuture", e);
                    // Set empty values on error
                    reminders.postValue(new java.util.ArrayList<>());
                    latestMetrics.postValue(new java.util.HashMap<>());
                    notificationCount.postValue(0);
                    bloodPressureHistory.postValue(new java.util.ArrayList<>());
                }
            }, mainExecutor)
            .exceptionally(throwable -> {
                Log.e(TAG, "❌ Error loading dashboard data in parallel", throwable);
                // Set empty values on error
                reminders.postValue(new java.util.ArrayList<>());
                latestMetrics.postValue(new java.util.HashMap<>());
                notificationCount.postValue(0);
                bloodPressureHistory.postValue(new java.util.ArrayList<>());
                return null;
            });
    }

    private void loadDataAsync() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Thread.sleep(500); // Wait for UI to render
                mainHandler.post(this::loadUserName);
                mainHandler.post(this::loadReminders);
                mainHandler.post(this::loadNotificationCount);
                mainHandler.post(this::loadLatestMetrics);
                mainHandler.post(this::loadBloodPressureHistory);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Get current user ID from Firebase Auth
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void loadUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            
            if (displayName != null && !displayName.isEmpty()) {
                userName.setValue(displayName);
            } else {
                // Load from Firestore if displayName is null
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("displayName");
                                userName.setValue(name != null ? name : "User");
                            } else {
                                userName.setValue("User");
                            }
                        })
                        .addOnFailureListener(e -> userName.setValue("User"));
            }
        } else {
            userName.setValue("User");
        }
    }

    private void updateGreeting() {
        // Get locale-aware context
        android.content.Context localizedContext = com.example.healthylifehub.utils.locale.LocaleHelper.applyLanguage(getApplication());
        
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            greeting.setValue(localizedContext.getString(com.example.healthylifehub.R.string.good_morning));
        } else if (hour < 18) {
            greeting.setValue(localizedContext.getString(com.example.healthylifehub.R.string.good_afternoon));
        } else {
            greeting.setValue(localizedContext.getString(com.example.healthylifehub.R.string.good_evening));
        }
    }

    private void loadReminders() {
        LiveData<List<Reminder>> source = remindersRepository.loadReminders();
        reminders.addSource(source, reminders::setValue);
    }
    
    private void loadNotificationCount() {
        LiveData<Integer> source = notificationsRepository.getUnreadCount();
        notificationCount.addSource(source, notificationCount::setValue);
    }
    
    private void loadLatestMetrics() {
        LiveData<java.util.Map<String, String>> source = metricsRepository.loadLatestMetrics();
        latestMetrics.addSource(source, latestMetrics::setValue);
    }

    private void loadBloodPressureHistory() {
        LiveData<List<MetricHistory>> source = metricsRepository.loadMetricHistory("blood_pressure");
        bloodPressureHistory.addSource(source, bloodPressureHistory::setValue);
    }

    public void snoozeReminder(Reminder reminder) {
    }

    public void completeReminder(Reminder reminder) {
        List<Reminder> currentReminders = reminders.getValue();
        if (currentReminders != null) {
            currentReminders.remove(reminder);
            reminders.setValue(currentReminders);
        }
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<String> getGreeting() {
        return greeting;
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    public LiveData<Integer> getNotificationCount() {
        return notificationCount;
    }
    
    public LiveData<java.util.Map<String, String>> getLatestMetrics() {
        return latestMetrics;
    }

    public LiveData<List<MetricHistory>> getBloodPressureHistory() {
        return bloodPressureHistory;
    }

    /**
     * Refresh blood pressure data - call this when new data is added
     */
    public void refreshBloodPressureData() {
        loadBloodPressureHistory();
    }

    /**
     * Refresh user data - call this when returning from profile edit
     */
    public void refreshUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Force reload from Firebase Auth
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    loadUserName();
                }
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shutdown executor service to prevent memory leaks
        executor.shutdown();
        Log.d(TAG, "🧹 DashboardViewModel cleared, executor shutdown");
    }
}
