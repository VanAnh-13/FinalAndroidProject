package com.example.healthylifehub.data.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.AnalyticsData;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.utils.CacheManager;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AnalyticsRepository - Fetch, aggregate, and cache health metrics data
 * 
 * Strategy:
 * 1. Check cache first (instant display)
 * 2. If cache valid, return cached data
 * 3. If cache expired, fetch from Firestore in background
 * 4. Aggregate data (calculate statistics)
 * 5. Cache result
 * 6. Emit to LiveData
 */
public class AnalyticsRepository extends FirebaseRepository {
    private static final String TAG = "AnalyticsRepository";
    private static final String COLLECTION_HEALTH_METRICS = "healthMetrics";
    
    // Thread pool for parallel data fetching and processing
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS, 
        new java.util.concurrent.LinkedBlockingQueue<>()
    );
    
    private final CacheManager cacheManager = CacheManager.getInstance();
    
    /**
     * Get analytics data for specified time range
     * Returns cached data immediately if valid, fetches new data in background
     */
    public LiveData<AnalyticsData> getAnalyticsData(int dayRange) {
        MutableLiveData<AnalyticsData> analyticsLiveData = new MutableLiveData<>();
        
        String cacheKey = "analytics_" + dayRange + "days";
        
        // Check cache first
        Object cachedData = cacheManager.get(cacheKey);
        if (cachedData instanceof AnalyticsData) {
            AnalyticsData data = (AnalyticsData) cachedData;
            data.isCached = true;
            analyticsLiveData.setValue(data);
            Log.d(TAG, "✅ Returned cached analytics data (" + data.getAgeSeconds() + "s old)");
        }
        
        // Fetch fresh data in background (even if cache exists)
        fetchAnalyticsDataInBackground(dayRange, cacheKey, analyticsLiveData);
        
        return analyticsLiveData;
    }
    
    /**
     * Fetch analytics data in background with parallel queries
     */
    private void fetchAnalyticsDataInBackground(int dayRange, String cacheKey, 
                                               MutableLiveData<AnalyticsData> liveData) {
        CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) {
                Log.e(TAG, "❌ User not logged in");
                return null;
            }
            
            AnalyticsData analyticsData = new AnalyticsData(dayRange);
            
            // Calculate date range
            Calendar cal = Calendar.getInstance();
            analyticsData.endDate = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, -dayRange);
            analyticsData.startDate = cal.getTimeInMillis();
            
            Log.d(TAG, "🔄 Fetching analytics for " + dayRange + " days...");
            
            // Parallel fetch all metric types
            CompletableFuture<Void> bpFuture = fetchAndProcessMetric(
                userId, "blood_pressure", analyticsData.bloodPressure, analyticsData.startDate
            );
            CompletableFuture<Void> bsFuture = fetchAndProcessMetric(
                userId, "blood_sugar", analyticsData.bloodSugar, analyticsData.startDate
            );
            CompletableFuture<Void> weightFuture = fetchAndProcessMetric(
                userId, "weight", analyticsData.weight, analyticsData.startDate
            );
            CompletableFuture<Void> hrFuture = fetchAndProcessMetric(
                userId, "heart_rate", analyticsData.heartRate, analyticsData.startDate
            );
            
            // Wait for all to complete
            try {
                CompletableFuture.allOf(bpFuture, bsFuture, weightFuture, hrFuture)
                    .get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error waiting for metric futures", e);
            }
            
            return analyticsData;
        }, executorService)
        .thenAccept(analyticsData -> {
            if (analyticsData != null) {
                // Cache the result
                cacheManager.put(cacheKey, analyticsData, 5 * 60 * 1000); // 5 min TTL
                analyticsData.isCached = false;
                
                // Emit to LiveData (main thread)
                liveData.postValue(analyticsData);
                Log.d(TAG, "✅ Analytics data fetched and cached");
            }
        })
        .exceptionally(throwable -> {
            Log.e(TAG, "❌ Error fetching analytics", throwable);
            return null;
        });
    }
    
    /**
     * Fetch and process a single metric type
     */
    private CompletableFuture<Void> fetchAndProcessMetric(String userId, String metricType,
                                                          AnalyticsData.MetricStatistics stats,
                                                          long startDate) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Fetch from Firestore
            db.collection("users")
                .document(userId)
                .collection(COLLECTION_HEALTH_METRICS)
                .whereEqualTo("type", metricType)
                .whereGreaterThanOrEqualTo("measuredAt", new Date(startDate))
                .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<HealthMetric> metrics = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            HealthMetric metric = doc.toObject(HealthMetric.class);
                            if (metric != null) {
                                metrics.add(metric);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Failed to parse metric: " + doc.getId(), e);
                            // Skip invalid records
                        }
                    }
                    
                    // Process metrics
                    processMetrics(metrics, stats, metricType);
                    Log.d(TAG, "✅ Processed " + metrics.size() + " " + metricType + " metrics");
                    
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching " + metricType, e);
                    future.complete(null); // Complete even on error
                });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in fetchAndProcessMetric", e);
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * Process metrics and calculate statistics
     */
    private void processMetrics(List<HealthMetric> metrics, 
                               AnalyticsData.MetricStatistics stats,
                               String metricType) {
        if (metrics.isEmpty()) {
            Log.w(TAG, "⚠️ No real data found for " + metricType + ", generating sample data");
            generateSampleData(stats, metricType);
            return;
        }
        
        stats.dataPoints = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.US);
        
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sumSystolic = 0, sumDiastolic = 0;
        double minSystolic = Double.MAX_VALUE, maxSystolic = Double.MIN_VALUE;
        
        // Sort by date ascending for trend calculation
        Collections.sort(metrics, (a, b) -> Long.compare(a.getMeasuredAt().getTime(), 
                                                         b.getMeasuredAt().getTime()));
        
        for (HealthMetric metric : metrics) {
            String label = dateFormat.format(metric.getMeasuredAt());
            
            if ("blood_pressure".equals(metricType)) {
                // Blood pressure has systolic/diastolic
                double systolic = metric.getValueAsDouble("systolic", 0);
                double diastolic = metric.getValueAsDouble("diastolic", 0);
                
                stats.dataPoints.add(new AnalyticsData.DataPoint(
                    metric.getMeasuredAt().getTime(), systolic, diastolic, label
                ));
                
                sumSystolic += systolic;
                sumDiastolic += diastolic;
                minSystolic = Math.min(minSystolic, systolic);
                maxSystolic = Math.max(maxSystolic, systolic);
                
            } else {
                // Other metrics have single value
                double value = metric.getValueAsDouble("value", 0);
                
                stats.dataPoints.add(new AnalyticsData.DataPoint(
                    metric.getMeasuredAt().getTime(), value, label
                ));
                
                sum += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        
        // Calculate averages and trend
        if ("blood_pressure".equals(metricType)) {
            stats.avgSystolic = sumSystolic / metrics.size();
            stats.avgDiastolic = sumDiastolic / metrics.size();
            stats.minSystolic = minSystolic;
            stats.maxSystolic = maxSystolic;
            stats.unit = "mmHg";
            
            // Trend: compare first half vs second half
            int mid = metrics.size() / 2;
            double firstHalfAvg = 0, secondHalfAvg = 0;
            for (int i = 0; i < mid; i++) {
                firstHalfAvg += metrics.get(i).getValueAsDouble("systolic", 0);
            }
            firstHalfAvg /= mid;
            
            for (int i = mid; i < metrics.size(); i++) {
                secondHalfAvg += metrics.get(i).getValueAsDouble("systolic", 0);
            }
            secondHalfAvg /= (metrics.size() - mid);
            
            stats.trend = secondHalfAvg - firstHalfAvg;
            
        } else {
            stats.average = sum / metrics.size();
            stats.minimum = min;
            stats.maximum = max;
            
            // Trend calculation
            double firstHalfSum = 0, secondHalfSum = 0;
            int mid = metrics.size() / 2;
            for (int i = 0; i < mid; i++) {
                firstHalfSum += metrics.get(i).getValueAsDouble("value", 0);
            }
            double firstHalfAvg = firstHalfSum / mid;
            
            for (int i = mid; i < metrics.size(); i++) {
                secondHalfSum += metrics.get(i).getValueAsDouble("value", 0);
            }
            double secondHalfAvg = secondHalfSum / (metrics.size() - mid);
            
            stats.trend = secondHalfAvg - firstHalfAvg;
            
            // Set unit based on metric type
            if ("blood_sugar".equals(metricType)) {
                stats.unit = "mg/dL";
            } else if ("weight".equals(metricType)) {
                stats.unit = "kg";
            } else if ("heart_rate".equals(metricType)) {
                stats.unit = "bpm";
            }
        }
    }
    
    /**
     * Clear analytics cache
     */
    public void clearCache() {
        cacheManager.remove("analytics_7days");
        cacheManager.remove("analytics_30days");
        cacheManager.remove("analytics_90days");
        Log.d(TAG, "✅ Analytics cache cleared");
    }
    /**
     * Generate sample data when no real data exists (for testing/demo)
     */
    private void generateSampleData(AnalyticsData.MetricStatistics stats, String metricType) {
        stats.dataPoints = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        switch (metricType) {
            case "blood_pressure":
                // Generate 7 days of blood pressure data
                for (int i = 6; i >= 0; i--) {
                    cal.add(Calendar.DAY_OF_YEAR, -i);
                    double systolic = 120 + (Math.random() * 20 - 10); // 110-130
                    double diastolic = 80 + (Math.random() * 10 - 5);  // 75-85
                    
                    SimpleDateFormat df = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    stats.dataPoints.add(new AnalyticsData.DataPoint(
                        cal.getTimeInMillis(), systolic, diastolic, df.format(cal.getTime())
                    ));
                    cal = Calendar.getInstance(); // Reset
                }
                stats.avgSystolic = 120;
                stats.avgDiastolic = 80;
                stats.minSystolic = 110;
                stats.maxSystolic = 130;
                stats.unit = "mmHg";
                stats.trend = 2.0; // Slight increase
                break;
                
            case "blood_sugar":
                for (int i = 6; i >= 0; i--) {
                    cal.add(Calendar.DAY_OF_YEAR, -i);
                    double value = 100 + (Math.random() * 40 - 20); // 80-120
                    SimpleDateFormat df = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    stats.dataPoints.add(new AnalyticsData.DataPoint(
                        cal.getTimeInMillis(), value, df.format(cal.getTime())
                    ));
                    cal = Calendar.getInstance();
                }
                stats.average = 100;
                stats.minimum = 80;
                stats.maximum = 120;
                stats.unit = "mg/dL";
                stats.trend = -3.0; // Decreasing
                break;
                
            case "weight":
                for (int i = 6; i >= 0; i--) {
                    cal.add(Calendar.DAY_OF_YEAR, -i);
                    double value = 70 + (Math.random() * 4 - 2); // 68-72
                    SimpleDateFormat df = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    stats.dataPoints.add(new AnalyticsData.DataPoint(
                        cal.getTimeInMillis(), value, df.format(cal.getTime())
                    ));
                    cal = Calendar.getInstance();
                }
                stats.average = 70;
                stats.minimum = 68;
                stats.maximum = 72;
                stats.unit = "kg";
                stats.trend = 0.5; // Slight increase
                break;
                
            case "heart_rate":
                for (int i = 6; i >= 0; i--) {
                    cal.add(Calendar.DAY_OF_YEAR, -i);
                    double value = 72 + (Math.random() * 20 - 10); // 62-82
                    SimpleDateFormat df = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    stats.dataPoints.add(new AnalyticsData.DataPoint(
                        cal.getTimeInMillis(), value, df.format(cal.getTime())
                    ));
                    cal = Calendar.getInstance();
                }
                stats.average = 72;
                stats.minimum = 60;
                stats.maximum = 90;
                stats.unit = "bpm";
                stats.trend = 1.0; // Stable
                break;
        }
        
        Log.d(TAG, "✅ Generated sample data for " + metricType + " with " + 
              stats.dataPoints.size() + " points");
    }
}



