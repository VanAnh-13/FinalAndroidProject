package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.healthylifehub.data.local.converter.DateConverter;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HealthArticle - Entity for caching health information articles
 * Used for multi-level caching (memory + disk) to improve performance
 * and enable offline access to health information
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(
    tableName = "health_articles",
    indices = {@Index(value = {"category", "cachedAt"})}
)
@TypeConverters(DateConverter.class)
public class HealthArticle {
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    
    @NonNull
    private String title;
    
    @NonNull
    private String content;
    
    @NonNull
    private String category; // nutrition, exercise, mental_health, disease_prevention, etc.
    
    private Date cachedAt; // When this article was cached locally
    
    private String author;
    
    private String imageUrl;
    
    private Date publishedAt;
    
    private int readCount;
    
    private boolean isFavorite;

    @androidx.room.Ignore
    public HealthArticle(@NonNull String id, @NonNull String title, @NonNull String content, @NonNull String category, Date cachedAt, String author, String imageUrl, Date publishedAt, int readCount, boolean isFavorite) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.cachedAt = cachedAt;
        this.author = author;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.readCount = readCount;
        this.isFavorite = isFavorite;
    }
    
    /**
     * Check if this article is stale (older than 7 days)
     * @return true if article should be refreshed from network
     */
    public boolean isStale() {
        if (cachedAt == null) {
            return true;
        }
        long daysSinceCached = (System.currentTimeMillis() - cachedAt.getTime()) / (1000 * 60 * 60 * 24);
        return daysSinceCached > 7;
    }
}
