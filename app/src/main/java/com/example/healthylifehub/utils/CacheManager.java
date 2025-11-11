package com.example.healthylifehub.utils;

import android.util.LruCache;
import java.util.HashMap;
import java.util.Map;

/**
 * CacheManager for Analytics data with LRU + TTL (Time-To-Live)
 * - LRU: Least Recently Used eviction when max size exceeded
 * - TTL: Automatic expiration after specified time
 * - Thread-safe for concurrent access
 */
public class CacheManager {
    private static final int MAX_CACHE_SIZE = 20; // Max 20 cache entries
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000; // 5 minutes default
    
    private static CacheManager instance;
    private final LruCache<String, CacheEntry> cache;
    private final Map<String, Long> ttlMap = new HashMap<>();
    
    /**
     * CacheEntry wraps data with timestamp
     */
    public static class CacheEntry {
        public final Object data;
        public final long timestamp;
        
        public CacheEntry(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private CacheManager() {
        this.cache = new LruCache<String, CacheEntry>(MAX_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, CacheEntry value) {
                return 1; // Each entry counts as 1 unit
            }
        };
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Put data in cache with default TTL
     */
    public synchronized void put(String key, Object data) {
        put(key, data, DEFAULT_TTL_MS);
    }
    
    /**
     * Put data in cache with custom TTL
     * @param key Cache key
     * @param data Data to cache
     * @param ttlMs Time-to-live in milliseconds
     */
    public synchronized void put(String key, Object data, long ttlMs) {
        cache.put(key, new CacheEntry(data));
        ttlMap.put(key, System.currentTimeMillis() + ttlMs);
    }
    
    /**
     * Get data from cache if valid (not expired)
     * @param key Cache key
     * @return Cached data or null if not found or expired
     */
    public synchronized Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        // Check if expired
        Long expiryTime = ttlMap.get(key);
        if (expiryTime != null && System.currentTimeMillis() > expiryTime) {
            // Expired, remove from cache
            cache.remove(key);
            ttlMap.remove(key);
            return null;
        }
        
        return entry.data;
    }
    
    /**
     * Check if cache entry exists and is valid
     */
    public synchronized boolean isValid(String key) {
        return get(key) != null;
    }
    
    /**
     * Remove specific cache entry
     */
    public synchronized void remove(String key) {
        cache.remove(key);
        ttlMap.remove(key);
    }
    
    /**
     * Clear all cache
     */
    public synchronized void clear() {
        cache.evictAll();
        ttlMap.clear();
    }
    
    /**
     * Get cache size
     */
    public synchronized int size() {
        return cache.size();
    }
}
