package com.example.healthylifehub.ui.analytics.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.AnalysisResult;
import com.example.healthylifehub.data.model.AnalyticsData;
import com.example.healthylifehub.data.repository.AnalyticsRepository;
import com.example.healthylifehub.utils.analysis.HealthMetricsAnalyzer;

/**
 * AnalyticsViewModel - Manages analytics data and UI state
 * 
 * Responsibilities:
 * - Fetch analytics data from repository
 * - Manage time range selection
 * - Handle loading/error states
 * - Cache management
 * - Display health metrics analysis results (Requirements: 4.5)
 * - Trigger anomaly notifications (Requirements: 4.4)
 */
public class AnalyticsViewModel extends BaseViewModel {
    private static final String TAG = "AnalyticsViewModel";
    
    private final AnalyticsRepository analyticsRepository = new AnalyticsRepository();
    private final HealthMetricsAnalyzer healthMetricsAnalyzer;
    
    public AnalyticsViewModel(Application application) {
        super(application);
        this.healthMetricsAnalyzer = new HealthMetricsAnalyzer(application);
    }
    
    // Current time range (7, 30, 90 days)
    private final MutableLiveData<Integer> selectedDayRange = new MutableLiveData<>(7);
    
    // Loading state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // Error message
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Analysis result LiveData (Requirements: 4.5)
    private final MutableLiveData<AnalysisResult> analysisResult = new MutableLiveData<>();
    
    // Analysis loading state (Requirements: 4.5)
    private final MutableLiveData<Boolean> analysisLoading = new MutableLiveData<>(false);
    
    // Anomaly notification trigger (Requirements: 4.4)
    // This LiveData is used to trigger in-app notification/dialog display
    private final MutableLiveData<AnalysisResult> anomalyNotificationTrigger = new MutableLiveData<>();
    
    // Analytics data - uses switchMap to automatically update when dayRange changes
    private final LiveData<AnalyticsData> analyticsData = Transformations.switchMap(
        selectedDayRange,
        dayRange -> {
            Log.d(TAG, "📊 Loading analytics for " + dayRange + " days");
            isLoading.setValue(true);
            errorMessage.setValue(null);
            return analyticsRepository.getAnalyticsData(dayRange);
        }
    );
    
    /**
     * Get analytics data for current time range
     */
    public LiveData<AnalyticsData> getAnalyticsData() {
        return analyticsData;
    }
    
    /**
     * Get loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * Get error message
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get selected day range
     */
    public LiveData<Integer> getSelectedDayRange() {
        return selectedDayRange;
    }
    
    /**
     * Get analysis result LiveData
     * Requirements: 4.5
     */
    public LiveData<AnalysisResult> getAnalysisResult() {
        return analysisResult;
    }
    
    /**
     * Get analysis loading state
     * Requirements: 4.5
     */
    public LiveData<Boolean> getAnalysisLoading() {
        return analysisLoading;
    }
    
    /**
     * Get anomaly notification trigger LiveData
     * This is observed by the UI to display in-app notifications/dialogs
     * Requirements: 4.4
     */
    public LiveData<AnalysisResult> getAnomalyNotificationTrigger() {
        return anomalyNotificationTrigger;
    }
    
    /**
     * Load analytics data for specified time range
     */
    public void loadAnalyticsData(int dayRange) {
        Log.d(TAG, "🔄 loadAnalyticsData called with dayRange: " + dayRange);
        selectedDayRange.setValue(dayRange);
        isLoading.setValue(false);
    }
    
    /**
     * Change time range (7, 30, or 90 days)
     */
    public void setTimeRange(int dayRange) {
        if (dayRange != selectedDayRange.getValue()) {
            loadAnalyticsData(dayRange);
        }
    }
    
    /**
     * Refresh analytics data (clear cache and reload)
     */
    public void refreshAnalyticsData() {
        analyticsRepository.clearCache();
        int currentRange = selectedDayRange.getValue() != null ? selectedDayRange.getValue() : 7;
        loadAnalyticsData(currentRange);
    }
    
    /**
     * Clear cache
     */
    public void clearCache() {
        analyticsRepository.clearCache();
    }
    
    /**
     * Analyze health metrics for a specific metric type
     * 
     * Calls healthMetricsAnalyzer.analyzeMetrics() and updates LiveData with results on main thread
     * Shows loading indicator during analysis
     * 
     * Requirements: 4.2, 4.5
     * - 4.2: Trigger HealthMetricsAnalyzer and generate recommendations
     * - 4.5: Update AnalyticsViewModel LiveData with analysis results on main thread
     * 
     * @param metricType Type of metric to analyze (e.g., "blood_pressure", "blood_sugar", "heart_rate")
     * @param days Number of days to analyze (e.g., 7, 30, 90)
     */
    public void analyzeMetrics(String metricType, int days) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "⚠️ User not logged in, cannot analyze metrics");
            errorMessage.setValue("Vui lòng đăng nhập để phân tích dữ liệu");
            return;
        }
        
        Log.d(TAG, "🔍 Starting metrics analysis: type=" + metricType + ", days=" + days);
        
        // Show loading indicator
        analysisLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Call healthMetricsAnalyzer.analyzeMetrics()
        healthMetricsAnalyzer.analyzeMetrics(userId, metricType, days)
            .thenAcceptAsync(result -> {
                // Update LiveData with results on main thread
                analysisResult.postValue(result);
                analysisLoading.postValue(false);
                
                Log.d(TAG, "✅ Analysis complete: " + result.getAnomalyCount() + " anomalies found");
                
                // Display anomaly notification if needed (Requirements: 4.4)
                displayAnomalyNotificationIfNeeded(result);
            }, getApplication().getMainExecutor())
            .exceptionally(error -> {
                // Handle error
                Log.e(TAG, "❌ Analysis failed", error);
                analysisLoading.postValue(false);
                errorMessage.postValue("Không thể phân tích dữ liệu. Vui lòng thử lại.");
                return null;
            });
    }
    
    /**
     * Display anomaly notification if analysis has anomalies
     * 
     * This method checks if the analysis result contains anomalies and triggers
     * an in-app notification or dialog to alert the user. The UI layer observes
     * the anomalyNotificationTrigger LiveData to display the notification.
     * 
     * Requirements: 4.4
     * - Check if analysis has anomalies
     * - Display in-app notification or dialog
     * 
     * @param result The analysis result containing anomalies
     */
    public void displayAnomalyNotificationIfNeeded(AnalysisResult result) {
        if (result == null) {
            Log.d(TAG, "⚠️ Analysis result is null, no notification to display");
            return;
        }
        
        // Check if analysis has anomalies
        if (result.hasAnomalies()) {
            Log.d(TAG, "🚨 Anomalies detected: " + result.getAnomalyCount() + " anomalies");
            
            // Trigger in-app notification/dialog by posting to LiveData
            // The UI layer (Fragment/Activity) will observe this and display appropriate UI
            anomalyNotificationTrigger.postValue(result);
            
            // Also send system notification for high severity anomalies
            if (result.hasHighSeverityAnomalies()) {
                Log.d(TAG, "⚠️ High severity anomalies detected, sending system notification");
                healthMetricsAnalyzer.sendAnomalyAlertIfNeeded(result);
            }
        } else {
            Log.d(TAG, "✅ No anomalies detected in analysis");
        }
    }
    
    /**
     * Get current user ID from Firebase Auth
     */
    private String getCurrentUserId() {
        com.google.firebase.auth.FirebaseUser user = 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Shutdown HealthMetricsAnalyzer thread pool
        if (healthMetricsAnalyzer != null) {
            healthMetricsAnalyzer.shutdown();
        }
    }
}
