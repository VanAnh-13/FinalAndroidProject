package com.example.healthylifehub.data.cache;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache Interceptor for Query Optimization
 * 
 * Features:
 * - Caches query results with TTL (Time-To-Live)
 * - Prevents duplicate queries within TTL window
 * - Reduces database load
 * - Improves response time
 * 
 * Usage:
 * - Wrap repository methods with caching logic
 * - Automatically invalidate cache on mutations
 * - Manual cache invalidation available
 * 
 * Example:
 * CacheInterceptor<List<HealthMetric>> cache = new CacheInterceptor<>(5, TimeUnit.MINUTES);
 * List<HealthMetric> result = cache.getOrFetch("metrics_user123", () -> fetchFromDb());
 */
public class CacheInterceptor<T> {
    
    private static final String TAG = "CacheInterceptor";
    
    private final long ttlMillis;
    private final Map<String, CacheEntry<T>> cache = new HashMap<>();
    
    /**
     * Constructor with TTL
     */
    public CacheInterceptor(long duration, TimeUnit unit) {
        this.ttlMillis = unit.toMillis(duration);
    }
    
    /**
     * Get value from cache or fetch from supplier
     */
    public T getOrFetch(String key, CacheSupplier<T> supplier) throws Exception {
        CacheEntry<T> entry = cache.get(key);
        
        // Check if cache hit and not expired
        if (entry != null && !entry.isExpired()) {
            Log.d(TAG, "✅ Cache hit: " + key);
            return entry.value;
        }
        
        // Cache miss or expired - fetch from supplier
        Log.d(TAG, "🔄 Cache miss/expired: " + key + " - fetching...");
        T value = supplier.get();
        
        // Store in cache
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
        Log.d(TAG, "💾 Cached: " + key);
        
        return value;
    }
    
    /**
     * Invalidate specific cache entry
     */
    public void invalidate(String key) {
        cache.remove(key);
        Log.d(TAG, "🗑️ Invalidated cache: " + key);
    }
    
    /**
     * Invalidate all cache entries matching pattern
     */
    public void invalidatePattern(String pattern) {
        cache.keySet().removeIf(key -> key.matches(pattern));
        Log.d(TAG, "🗑️ Invalidated cache pattern: " + pattern);
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        cache.clear();
        Log.d(TAG, "🗑️ Cleared all cache");
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Get cache statistics
     */
    public String getStats() {
        return "CacheStats{" +
                "size=" + cache.size() +
                ", ttlMillis=" + ttlMillis +
                '}';
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Cache entry with expiration time
     */
    private static class CacheEntry<T> {
        final T value;
        final long expiresAt;
        
        CacheEntry(T value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
    
    /**
     * Supplier interface for fetching values
     */
    @FunctionalInterface
    public interface CacheSupplier<T> {
        T get() throws Exception;
    }
}
