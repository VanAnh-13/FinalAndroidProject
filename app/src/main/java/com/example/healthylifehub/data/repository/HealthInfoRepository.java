package com.example.healthylifehub.data.repository;

import android.util.LruCache;

import com.example.healthylifehub.base.BaseRepository;
import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.HealthArticleDao;
import com.example.healthylifehub.data.model.HealthArticle;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * HealthInfoRepository - Multi-level caching repository for health articles
 * 
 * Implements 3-level caching strategy:
 * - L1: LruCache (memory) - instant access, limited size
 * - L2: Room database (disk) - fast access, larger capacity
 * - L3: Firestore (network) - fallback, always up-to-date
 * 
 * This architecture ensures:
 * - Fast access to frequently used articles (L1 cache hit)
 * - Offline access to previously viewed articles (L2 cache hit)
 * - Always fresh content when online (L3 network fetch)
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
public class HealthInfoRepository extends BaseRepository {
    
    private final LruCache<String, HealthArticle> memoryCache;
    private final HealthArticleDao diskCache;
    private final FirebaseFirestore firestore;
    
    // Cache monitoring metrics (Requirements: 10.1, 10.5)
    private int cacheHits = 0;
    private int cacheMisses = 0;
    private int diskCacheHits = 0;
    private int networkFetches = 0;
    private long lastStatsLogTime = System.currentTimeMillis();
    private static final long STATS_LOG_INTERVAL = 60000; // Log stats every 60 seconds
    
    /**
     * Initialize repository with multi-level caching
     * 
     * LruCache size is set to 1/8 of available heap memory
     * This provides a good balance between memory usage and cache effectiveness
     */
    public HealthInfoRepository(AppDatabase database) {
        // Initialize L1 cache (memory)
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // in KB
        int cacheSize = maxMemory / 8; // Use 1/8 of available memory
        
        this.memoryCache = new LruCache<String, HealthArticle>(cacheSize) {
            @Override
            protected int sizeOf(String key, HealthArticle article) {
                // Estimate size: key + title + content length in KB
                int estimatedSize = key.length() + 
                                  article.getTitle().length() + 
                                  article.getContent().length();
                return estimatedSize / 1024; // Convert to KB
            }
        };
        
        // Initialize L2 cache (disk)
        this.diskCache = database.healthArticleDao();
        
        // Initialize L3 (network)
        this.firestore = FirebaseFirestore.getInstance();
        
        // Log initial cache configuration
        logCacheConfiguration();
    }
    
    /**
     * Get the memory cache instance
     * Useful for testing and monitoring
     */
    public LruCache<String, HealthArticle> getMemoryCache() {
        return memoryCache;
    }
    
    /**
     * Get the disk cache DAO
     * Useful for testing and direct access
     */
    public HealthArticleDao getDiskCache() {
        return diskCache;
    }
    
    /**
     * Clear all caches
     * Useful for logout or cache invalidation
     */
    public void clearAllCaches() {
        memoryCache.evictAll();
        executeAsync(() -> {
            diskCache.deleteAll();
            return null;
        });
    }
    
    /**
     * Get cache statistics
     * Requirements: 10.1, 10.5 - Log cache hit rate, cache size and max size
     * 
     * @return String with cache hit rates and sizes
     */
    public String getCacheStats() {
        int memorySize = memoryCache.size();
        int memoryMaxSize = memoryCache.maxSize();
        
        // Calculate hit rates
        int totalRequests = cacheHits + cacheMisses;
        double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
        double diskHitRate = cacheMisses > 0 ? (double) diskCacheHits / cacheMisses * 100 : 0;
        
        return String.format(
            "Memory Cache: %d/%d KB (%.1f%% full) | " +
            "Hit Rate: %.1f%% (L1: %d hits, L2: %d hits, L3: %d fetches) | " +
            "Total Requests: %d",
            memorySize, memoryMaxSize, 
            (double) memorySize / memoryMaxSize * 100,
            hitRate,
            cacheHits, diskCacheHits, networkFetches,
            totalRequests
        );
    }
    
    /**
     * Log cache configuration on initialization
     */
    private void logCacheConfiguration() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = memoryCache.maxSize();
        
        android.util.Log.i("HealthInfoRepository", 
            String.format("Cache initialized - Max Memory: %d KB, Cache Size: %d KB (%.1f%%)",
                maxMemory, cacheSize, (double) cacheSize / maxMemory * 100));
    }
    
    /**
     * Log cache statistics periodically
     * Requirements: 10.1, 10.5
     */
    private void logCacheStatsIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatsLogTime >= STATS_LOG_INTERVAL) {
            String stats = getCacheStats();
            android.util.Log.i("HealthInfoRepository", "Cache Stats: " + stats);
            
            // Check if cache size needs adjustment
            checkAndAdjustCacheSize();
            
            lastStatsLogTime = currentTime;
        }
    }
    
    /**
     * Check cache performance and adjust size if needed
     * Requirements: 10.1, 10.5 - Adjust cache size if needed
     */
    private void checkAndAdjustCacheSize() {
        int totalRequests = cacheHits + cacheMisses;
        
        // Only adjust if we have enough data
        if (totalRequests < 50) {
            return;
        }
        
        double hitRate = (double) cacheHits / totalRequests * 100;
        int currentSize = memoryCache.size();
        int maxSize = memoryCache.maxSize();
        double fillRate = (double) currentSize / maxSize * 100;
        
        // If hit rate is low (<50%) and cache is nearly full (>80%), suggest increasing cache
        if (hitRate < 50 && fillRate > 80) {
            android.util.Log.w("HealthInfoRepository", 
                String.format("Low cache hit rate (%.1f%%) with high fill rate (%.1f%%). " +
                    "Consider increasing cache size for better performance.", 
                    hitRate, fillRate));
        }
        
        // If hit rate is high (>90%) and cache is not full (<50%), cache size might be too large
        if (hitRate > 90 && fillRate < 50) {
            android.util.Log.i("HealthInfoRepository", 
                String.format("High cache hit rate (%.1f%%) with low fill rate (%.1f%%). " +
                    "Cache size is optimal or could be reduced.", 
                    hitRate, fillRate));
        }
    }
    
    /**
     * Reset cache statistics
     * Useful for testing or after cache clear
     */
    public void resetCacheStats() {
        cacheHits = 0;
        cacheMisses = 0;
        diskCacheHits = 0;
        networkFetches = 0;
        lastStatsLogTime = System.currentTimeMillis();
        android.util.Log.d("HealthInfoRepository", "Cache statistics reset");
    }
    
    /**
     * Get detailed cache metrics
     * @return CacheMetrics object with detailed statistics
     */
    public CacheMetrics getCacheMetrics() {
        int totalRequests = cacheHits + cacheMisses;
        double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
        double diskHitRate = cacheMisses > 0 ? (double) diskCacheHits / cacheMisses * 100 : 0;
        
        return new CacheMetrics(
            memoryCache.size(),
            memoryCache.maxSize(),
            cacheHits,
            cacheMisses,
            diskCacheHits,
            networkFetches,
            hitRate,
            diskHitRate
        );
    }
    
    /**
     * Inner class to hold cache metrics
     */
    public static class CacheMetrics {
        public final int memorySize;
        public final int memoryMaxSize;
        public final int cacheHits;
        public final int cacheMisses;
        public final int diskCacheHits;
        public final int networkFetches;
        public final double hitRate;
        public final double diskHitRate;
        
        public CacheMetrics(int memorySize, int memoryMaxSize, int cacheHits, 
                          int cacheMisses, int diskCacheHits, int networkFetches,
                          double hitRate, double diskHitRate) {
            this.memorySize = memorySize;
            this.memoryMaxSize = memoryMaxSize;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.diskCacheHits = diskCacheHits;
            this.networkFetches = networkFetches;
            this.hitRate = hitRate;
            this.diskHitRate = diskHitRate;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheMetrics{memorySize=%d/%d KB, hitRate=%.1f%%, " +
                "cacheHits=%d, diskHits=%d, networkFetches=%d}",
                memorySize, memoryMaxSize, hitRate, 
                cacheHits, diskCacheHits, networkFetches
            );
        }
    }
    
    /**
     * Get article with 3-level caching strategy
     * 
     * Cache lookup order:
     * 1. L1 (Memory): Check LruCache - returns immediately if found (< 1ms)
     * 2. L2 (Disk): Check Room database - fast access (< 100ms)
     * 3. L3 (Network): Fetch from Firestore - slower but always fresh (< 3s)
     * 
     * After network fetch, both L1 and L2 caches are updated for future access.
     * 
     * Tracks cache hit/miss statistics for monitoring (Requirements: 10.1, 10.5)
     * 
     * @param articleId Unique identifier for the article
     * @return CompletableFuture containing the article, or null if not found
     * 
     * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
     */
    public CompletableFuture<HealthArticle> getArticle(String articleId) {
        // L1: Check memory cache (instant access)
        HealthArticle cachedArticle = memoryCache.get(articleId);
        if (cachedArticle != null) {
            cacheHits++;
            android.util.Log.d("HealthInfoRepository", "L1 cache hit for article: " + articleId);
            logCacheStatsIfNeeded();
            return CompletableFuture.completedFuture(cachedArticle);
        }
        
        // L1 miss
        cacheMisses++;
        
        // L2 & L3: Check disk cache and network on background thread
        return CompletableFuture.supplyAsync(() -> {
            // L2: Check disk cache
            HealthArticle diskCached = diskCache.getArticleById(articleId);
            if (diskCached != null && !diskCached.isStale()) {
                diskCacheHits++;
                android.util.Log.d("HealthInfoRepository", "L2 cache hit for article: " + articleId);
                // Update L1 cache
                memoryCache.put(articleId, diskCached);
                logCacheStatsIfNeeded();
                return diskCached;
            }
            
            // L3: Fetch from network
            networkFetches++;
            android.util.Log.d("HealthInfoRepository", "L3 network fetch for article: " + articleId);
            try {
                HealthArticle article = fetchFromFirestore(articleId);
                
                if (article != null) {
                    // Update both caches
                    article.setCachedAt(new Date());
                    memoryCache.put(articleId, article);
                    diskCache.insert(article);
                    android.util.Log.d("HealthInfoRepository", "Article cached: " + articleId);
                }
                
                logCacheStatsIfNeeded();
                return article;
            } catch (Exception e) {
                android.util.Log.e("HealthInfoRepository", "Error fetching article from Firestore", e);
                // Return stale cache if available, better than nothing
                return diskCached;
            }
        }, getIoExecutor());
    }
    
    /**
     * Fetch article from Firestore
     * This is a blocking call that should be executed on a background thread
     * 
     * @param articleId Article ID to fetch
     * @return HealthArticle or null if not found
     * @throws Exception if network error occurs
     */
    private HealthArticle fetchFromFirestore(String articleId) throws Exception {
        DocumentSnapshot document = Tasks.await(firestore.collection("health_articles")
            .document(articleId)
            .get()); // Blocking call - must be on background thread
        
        if (document.exists()) {
            HealthArticle article = new HealthArticle();
            article.setId(document.getId());
            article.setTitle(document.getString("title"));
            article.setContent(document.getString("content"));
            article.setCategory(document.getString("category"));
            article.setAuthor(document.getString("author"));
            article.setImageUrl(document.getString("imageUrl"));
            
            // Handle publishedAt timestamp
            com.google.firebase.Timestamp publishedTimestamp = document.getTimestamp("publishedAt");
            if (publishedTimestamp != null) {
                article.setPublishedAt(publishedTimestamp.toDate());
            }
            
            // Set defaults for local fields
            article.setCachedAt(new Date());
            article.setReadCount(0);
            article.setFavorite(false);
            
            return article;
        }
        
        return null;
    }
    
    /**
     * Prefetch articles by category
     * Useful for preloading content when user navigates to a category
     * 
     * @param category Category to prefetch
     * @return CompletableFuture that completes when prefetch is done
     */
    public CompletableFuture<Void> prefetchCategory(String category) {
        return CompletableFuture.runAsync(() -> {
            try {
                firestore.collection("health_articles")
                    .whereEqualTo("category", category)
                    .limit(10)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                HealthArticle article = new HealthArticle();
                                article.setId(doc.getId());
                                article.setTitle(doc.getString("title"));
                                article.setContent(doc.getString("content"));
                                article.setCategory(doc.getString("category"));
                                article.setAuthor(doc.getString("author"));
                                article.setImageUrl(doc.getString("imageUrl"));
                                
                                com.google.firebase.Timestamp publishedTimestamp = doc.getTimestamp("publishedAt");
                                if (publishedTimestamp != null) {
                                    article.setPublishedAt(publishedTimestamp.toDate());
                                }
                                
                                article.setCachedAt(new Date());
                                article.setReadCount(0);
                                article.setFavorite(false);
                                
                                // Cache in both levels
                                memoryCache.put(article.getId(), article);
                                diskCache.insert(article);
                            } catch (Exception e) {
                                android.util.Log.e("HealthInfoRepository", "Error prefetching article", e);
                            }
                        }
                        android.util.Log.d("HealthInfoRepository", "Prefetched category: " + category);
                    });
            } catch (Exception e) {
                android.util.Log.e("HealthInfoRepository", "Error prefetching category", e);
            }
        }, getIoExecutor());
    }
    
    /**
     * Invalidate cache for a specific article
     * Forces next access to fetch from network
     * 
     * @param articleId Article to invalidate
     */
    public void invalidateArticle(String articleId) {
        memoryCache.remove(articleId);
        executeAsync(() -> {
            diskCache.deleteArticle(articleId);
            return null;
        });
    }
    
    /**
     * Update article favorite status
     * 
     * @param articleId Article ID
     * @param isFavorite New favorite status
     * @return CompletableFuture that completes when update is done
     */
    public CompletableFuture<Void> updateFavoriteStatus(String articleId, boolean isFavorite) {
        return CompletableFuture.runAsync(() -> {
            diskCache.updateFavoriteStatus(articleId, isFavorite);
            
            // Update memory cache if present
            HealthArticle cached = memoryCache.get(articleId);
            if (cached != null) {
                cached.setFavorite(isFavorite);
                memoryCache.put(articleId, cached);
            }
        }, getIoExecutor());
    }
    
    /**
     * Increment read count for an article
     * 
     * @param articleId Article ID
     */
    public void incrementReadCount(String articleId) {
        executeAsync(() -> {
            diskCache.incrementReadCount(articleId);
            
            // Update memory cache if present
            HealthArticle cached = memoryCache.get(articleId);
            if (cached != null) {
                cached.setReadCount(cached.getReadCount() + 1);
                memoryCache.put(articleId, cached);
            }
            return null;
        });
    }
    
    /**
     * Clear old disk cache entries (older than 7 days)
     * 
     * This implements the cache eviction strategy to prevent disk cache from growing indefinitely.
     * LruCache automatically evicts least recently used items when memory limit is reached,
     * but disk cache needs manual cleanup.
     * 
     * Articles older than 7 days are considered stale and should be refreshed from network.
     * This method should be called periodically (e.g., on app startup or daily).
     * 
     * @return CompletableFuture containing the number of articles deleted
     * 
     * Requirement: 10.4
     */
    public CompletableFuture<Integer> clearOldCacheEntries() {
        return CompletableFuture.supplyAsync(() -> {
            // Calculate timestamp for 7 days ago
            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            
            android.util.Log.d("HealthInfoRepository", "Clearing cache entries older than 7 days");
            
            int deletedCount = diskCache.deleteOldArticles(sevenDaysAgo);
            
            android.util.Log.d("HealthInfoRepository", 
                "Cache cleanup complete. Deleted " + deletedCount + " old articles");
            
            return deletedCount;
        }, getIoExecutor());
    }
    
    /**
     * Clear old cache entries with custom age threshold
     * 
     * @param daysOld Number of days - articles older than this will be deleted
     * @return CompletableFuture containing the number of articles deleted
     */
    public CompletableFuture<Integer> clearOldCacheEntries(int daysOld) {
        return CompletableFuture.supplyAsync(() -> {
            long threshold = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
            
            android.util.Log.d("HealthInfoRepository", 
                "Clearing cache entries older than " + daysOld + " days");
            
            int deletedCount = diskCache.deleteOldArticles(threshold);
            
            android.util.Log.d("HealthInfoRepository", 
                "Cache cleanup complete. Deleted " + deletedCount + " old articles");
            
            return deletedCount;
        }, getIoExecutor());
    }
    
    /**
     * Get disk cache size
     * 
     * @return CompletableFuture containing the number of articles in disk cache
     */
    public CompletableFuture<Integer> getDiskCacheSize() {
        return CompletableFuture.supplyAsync(() -> {
            return diskCache.getArticleCount();
        }, getIoExecutor());
    }
    
    /**
     * Perform cache maintenance
     * 
     * This method should be called periodically (e.g., on app startup) to:
     * 1. Clear old disk cache entries (> 7 days)
     * 2. Log cache statistics
     * 
     * @return CompletableFuture that completes when maintenance is done
     */
    public CompletableFuture<Void> performCacheMaintenance() {
        return CompletableFuture.runAsync(() -> {
            android.util.Log.d("HealthInfoRepository", "Starting cache maintenance");
            
            // Clear old entries
            try {
                int deletedCount = clearOldCacheEntries().get();
                android.util.Log.d("HealthInfoRepository", 
                    "Maintenance: Deleted " + deletedCount + " old articles");
            } catch (Exception e) {
                android.util.Log.e("HealthInfoRepository", "Error during cache maintenance", e);
            }
            
            // Log cache stats
            try {
                int diskSize = getDiskCacheSize().get();
                String memoryStats = getCacheStats();
                android.util.Log.d("HealthInfoRepository", 
                    "Cache stats - Disk: " + diskSize + " articles, " + memoryStats);
            } catch (Exception e) {
                android.util.Log.e("HealthInfoRepository", "Error getting cache stats", e);
            }
            
            android.util.Log.d("HealthInfoRepository", "Cache maintenance complete");
        }, getIoExecutor());
    }
}
