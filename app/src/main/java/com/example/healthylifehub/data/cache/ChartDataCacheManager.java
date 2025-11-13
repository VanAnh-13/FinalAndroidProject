package com.example.healthylifehub.data.cache;

import android.util.Log;
import android.util.LruCache;

import com.github.mikephil.charting.data.Entry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager for chart data to optimize chart rendering performance
 * Stores processed chart data to avoid recalculating for historical data
 */
public class ChartDataCacheManager {
    private static final String TAG = "ChartDataCacheManager";
    private static ChartDataCacheManager instance;
    
    // Cache for processed chart data
    private final LruCache<String, ChartDataCache> chartDataCache;
    
    // Mapping of cache keys to timestamps for invalidation
    private final Map<String, Long> cacheTimes;
    
    // Cache size - 20 chart datasets should be enough for most use cases
    private static final int CACHE_SIZE = 20;
    
    // Cache expiry - 10 minutes (historical data doesn't change)
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000;
    
    private ChartDataCacheManager() {
        this.chartDataCache = new LruCache<>(CACHE_SIZE);
        this.cacheTimes = new ConcurrentHashMap<>();
    }
    
    public static synchronized ChartDataCacheManager getInstance() {
        if (instance == null) {
            instance = new ChartDataCacheManager();
        }
        return instance;
    }
    
    /**
     * Cache entry for chart data
     */
    public static class ChartDataCache {
        private final List<Entry> entries;
        private final List<String> labels;
        
        public ChartDataCache(List<Entry> entries, List<String> labels) {
            this.entries = entries;
            this.labels = labels;
        }
        
        public List<Entry> getEntries() {
            return entries;
        }
        
        public List<String> getLabels() {
            return labels;
        }
    }
    
    /**
     * Get cached chart data if available
     * @param key Cache key (format: "metricType_period")
     * @return Cached chart data or null if not available
     */
    public ChartDataCache getChartData(String key) {
        // Check if cache expired
        Long cacheTime = cacheTimes.get(key);
        if (cacheTime == null || System.currentTimeMillis() - cacheTime > CACHE_EXPIRY_MS) {
            // Cache expired or not found
            return null;
        }
        
        ChartDataCache cache = chartDataCache.get(key);
        if (cache != null) {
            Log.d(TAG, "Cache hit for: " + key);
        }
        return cache;
    }
    
    /**
     * Store chart data in cache
     * @param key Cache key (format: "metricType_period")
     * @param chartData Chart data to cache
     */
    public void putChartData(String key, ChartDataCache chartData) {
        chartDataCache.put(key, chartData);
        cacheTimes.put(key, System.currentTimeMillis());
        Log.d(TAG, "Cached chart data for: " + key);
    }
    
    /**
     * Generate cache key for chart data
     * @param metricType Metric type (blood_pressure, heart_rate, etc.)
     * @param period Period (day, week, month, year)
     * @return Cache key
     */
    public String generateCacheKey(String metricType, String period) {
        return metricType + "_" + period;
    }
    
    /**
     * Clear chart data for a specific key
     * @param key Cache key to clear
     */
    public void clearChartData(String key) {
        chartDataCache.remove(key);
        cacheTimes.remove(key);
        Log.d(TAG, "Cleared cache for: " + key);
    }
    
    /**
     * Invalidate cache for a specific metric type
     * @param metricType Metric type to invalidate
     */
    public void invalidateMetricCache(String metricType) {
        for (String key : cacheTimes.keySet()) {
            if (key.startsWith(metricType + "_")) {
                chartDataCache.remove(key);
                cacheTimes.remove(key);
                Log.d(TAG, "Invalidated cache for: " + key);
            }
        }
    }
    
    /**
     * Invalidate all cache
     */
    public void invalidateAllCache() {
        chartDataCache.evictAll();
        cacheTimes.clear();
        Log.d(TAG, "Invalidated all chart data cache");
    }
}
