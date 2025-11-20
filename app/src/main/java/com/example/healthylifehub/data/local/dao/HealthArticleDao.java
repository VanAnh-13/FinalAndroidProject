package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.healthylifehub.data.model.HealthArticle;

import java.util.List;

/**
 * DAO for Health Articles operations
 * Provides disk cache layer for health information articles
 */
@Dao
public interface HealthArticleDao {
    
    /**
     * Insert or replace article
     * Used when caching articles from network
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HealthArticle article);
    
    /**
     * Insert multiple articles
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HealthArticle> articles);
    
    /**
     * Update article
     */
    @Update
    void update(HealthArticle article);
    
    /**
     * Get article by ID (synchronous for cache lookup)
     * Returns null if not found
     */
    @Query("SELECT * FROM health_articles WHERE id = :articleId LIMIT 1")
    HealthArticle getArticleById(String articleId);
    
    /**
     * Get all articles (LiveData for UI)
     */
    @Query("SELECT * FROM health_articles ORDER BY cachedAt DESC")
    LiveData<List<HealthArticle>> getAllArticles();
    
    /**
     * Get articles by category
     */
    @Query("SELECT * FROM health_articles WHERE category = :category ORDER BY publishedAt DESC")
    LiveData<List<HealthArticle>> getArticlesByCategory(String category);
    
    /**
     * Get articles by category (synchronous)
     */
    @Query("SELECT * FROM health_articles WHERE category = :category ORDER BY publishedAt DESC")
    List<HealthArticle> getArticlesByCategorySync(String category);
    
    /**
     * Delete old articles (older than specified timestamp)
     * Used for cache eviction strategy
     * 
     * @param olderThan Timestamp in milliseconds - articles cached before this will be deleted
     * @return Number of articles deleted
     */
    @Query("DELETE FROM health_articles WHERE cachedAt < :olderThan")
    int deleteOldArticles(long olderThan);
    
    /**
     * Delete article by ID
     */
    @Query("DELETE FROM health_articles WHERE id = :articleId")
    void deleteArticle(String articleId);
    
    /**
     * Delete all articles
     */
    @Query("DELETE FROM health_articles")
    void deleteAll();
    
    /**
     * Get count of cached articles
     */
    @Query("SELECT COUNT(*) FROM health_articles")
    int getArticleCount();
    
    /**
     * Get favorite articles
     */
    @Query("SELECT * FROM health_articles WHERE isFavorite = 1 ORDER BY cachedAt DESC")
    LiveData<List<HealthArticle>> getFavoriteArticles();
    
    /**
     * Update favorite status
     */
    @Query("UPDATE health_articles SET isFavorite = :isFavorite WHERE id = :articleId")
    void updateFavoriteStatus(String articleId, boolean isFavorite);
    
    /**
     * Increment read count
     */
    @Query("UPDATE health_articles SET readCount = readCount + 1 WHERE id = :articleId")
    void incrementReadCount(String articleId);
}
