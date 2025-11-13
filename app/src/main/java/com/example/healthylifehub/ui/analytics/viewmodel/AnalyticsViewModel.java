package com.example.healthylifehub.ui.analytics.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.AnalyticsData;
import com.example.healthylifehub.data.repository.AnalyticsRepository;

/**
 * AnalyticsViewModel - Manages analytics data and UI state
 * 
 * Responsibilities:
 * - Fetch analytics data from repository
 * - Manage time range selection
 * - Handle loading/error states
 * - Cache management
 */
public class AnalyticsViewModel extends BaseViewModel {
    private static final String TAG = "AnalyticsViewModel";
    
    private final AnalyticsRepository analyticsRepository = new AnalyticsRepository();
    
    public AnalyticsViewModel(Application application) {
        super(application);
    }
    
    // Current time range (7, 30, 90 days)
    private final MutableLiveData<Integer> selectedDayRange = new MutableLiveData<>(7);
    
    // Loading state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // Error message
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
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
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources if needed
    }
}
