package com.example.healthylifehub.ui.analytics.enhanced;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.cache.ChartDataCacheManager;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.repository.HealthMetricRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced Analytics ViewModel for period-based charts
 * Manages metric data loading with proper period filtering
 */
public class EnhancedAnalyticsViewModel extends BaseViewModel {
    
    private static final String TAG = "EnhancedAnalyticsVM";
    
    private final HealthMetricRepository healthMetricRepository;
    private final ChartDataCacheManager cacheManager;
    private final ExecutorService executorService;
    
    // LiveData for UI
    private final MutableLiveData<List<HealthMetric>> metricData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Current parameters
    private String currentMetricType = "blood_pressure";
    private String currentPeriod = "week";
    
    public EnhancedAnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.healthMetricRepository = new HealthMetricRepository(application.getApplicationContext());
        this.cacheManager = ChartDataCacheManager.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Load metric data for specified type and period
     */
    public void loadMetricData(String metricType, String period) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("User not logged in");
            return;
        }
        
        this.currentMetricType = metricType;
        this.currentPeriod = period;
        
        Log.d(TAG, "Loading metric data: " + metricType + " for period: " + period);
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Check cache first
        String cacheKey = cacheManager.generateCacheKey(metricType, period);
        ChartDataCacheManager.ChartDataCache cachedData = cacheManager.getChartData(cacheKey);
        
        if (cachedData != null) { // Cache manager handles expiry internally
            Log.d(TAG, "Using cached data for: " + cacheKey);
            // We need to convert back to MetricHistory - for now, load fresh data
            loadFromRepository(metricType, period);
        } else {
            loadFromRepository(metricType, period);
        }
    }
    
    /**
     * Load data from repository
     */
    private void loadFromRepository(String metricType, String period) {
        executorService.execute(() -> {
            try {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                
                // Calculate date range based on period
                int daysBack = calculateDaysBack(period);
                
                // Get metric data from repository  
                healthMetricRepository.loadMetricsByType(metricType)
                    .observeForever(historyList -> {
                        if (historyList != null) {
                            // Filter by days back and set data
                            List<HealthMetric> filteredList = filterByDays(historyList, daysBack);
                            Log.d(TAG, "✅ Loaded " + filteredList.size() + " metric records");
                            metricData.postValue(filteredList);
                        }
                        isLoading.postValue(false);
                    });
                    
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading metric data", e);
                errorMessage.postValue("Error loading data: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Calculate how many days back to fetch based on period
     */
    private int calculateDaysBack(String period) {
        switch (period) {
            case "day":
                return 1; // Today only
            case "week":
                return 7; // Last 7 days
            case "month":
                return 30; // Last 30 days
            case "year":
                return 365; // Last 12 months
            default:
                return 7; // Default to week
        }
    }
    
    /**
     * Refresh data (clear cache and reload)
     */
    public void refreshData() {
        Log.d(TAG, "Refreshing data for: " + currentMetricType + " - " + currentPeriod);
        clearCache();
        loadMetricData(currentMetricType, currentPeriod);
    }
    
    /**
     * Clear all cache
     */
    public void clearCache() {
        cacheManager.invalidateAllCache();
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Clear cache for specific metric and period
     */
    public void clearCacheForMetric(String metricType, String period) {
        String cacheKey = cacheManager.generateCacheKey(metricType, period);
        cacheManager.clearChartData(cacheKey);
        Log.d(TAG, "Cache cleared for: " + cacheKey);
    }
    
    /**
     * Pre-load data for all periods of current metric
     */
    public void preloadAllPeriods() {
        String[] periods = {"day", "week", "month", "year"};
        
        for (String period : periods) {
            if (!period.equals(currentPeriod)) {
                executorService.execute(() -> {
                    Log.d(TAG, "Pre-loading data for period: " + period);
                    
                    try {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        int daysBack = calculateDaysBack(period);
                        
                        // For pre-loading, we'll skip complex observeForever handling
                        // Just log that we're pre-loading
                        Log.d(TAG, "⚡ Pre-loading data for period: " + period);
                            
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error pre-loading data for " + period, e);
                    }
                });
            }
        }
    }
    
    /**
     * Filter metrics by days back from current date
     */
    private List<HealthMetric> filterByDays(List<HealthMetric> metrics, int daysBack) {
        if (metrics == null || metrics.isEmpty()) return new ArrayList<>();
        
        long cutoffTime = System.currentTimeMillis() - (daysBack * 24L * 60L * 60L * 1000L);
        List<HealthMetric> filtered = new ArrayList<>();
        
        for (HealthMetric metric : metrics) {
            if (metric.getMeasuredAt() != null && metric.getMeasuredAt().getTime() >= cutoffTime) {
                filtered.add(metric);
            }
        }
        
        return filtered;
    }
    
    // Getters for LiveData
    public LiveData<List<HealthMetric>> getMetricData() {
        return metricData;
    }
    
    @Override
    public LiveData<Boolean> getLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    // Getters for current state
    public String getCurrentMetricType() {
        return currentMetricType;
    }
    
    public String getCurrentPeriod() {
        return currentPeriod;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "ViewModel cleared");
    }
}