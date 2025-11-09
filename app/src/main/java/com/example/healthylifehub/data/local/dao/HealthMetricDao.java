package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.healthylifehub.data.model.HealthMetric;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * DAO for Health Metrics operations
 * Dùng trực tiếp HealthMetric model (không cần Entity riêng)
 * Supports both LiveData (for UI) and RxJava3 (for complex operations)
 */
@Dao
public interface HealthMetricDao {
    
    /**
     * Insert or replace metric
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMetric(HealthMetric metric);
    
    /**
     * Insert multiple metrics
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMetrics(List<HealthMetric> metrics);
    
    /**
     * Update metric
     */
    @Update
    void updateMetric(HealthMetric metric);
    
    /**
     * Get all metrics for a user (LiveData)
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId ORDER BY measuredAt DESC")
    LiveData<List<HealthMetric>> getMetricsForUser(String userId);
    
    /**
     * Get all metrics for a user (synchronous)
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId ORDER BY measuredAt DESC")
    List<HealthMetric> getMetricsForUserSync(String userId);
    
    /**
     * Get metrics by type for a user
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND type = :type ORDER BY measuredAt DESC")
    LiveData<List<HealthMetric>> getMetricsByType(String userId, String type);
    
    /**
     * Get metric by ID
     */
    @Query("SELECT * FROM health_metrics WHERE id = :metricId LIMIT 1")
    HealthMetric getMetricById(String metricId);
    
    /**
     * Delete metric
     */
    @Query("DELETE FROM health_metrics WHERE id = :metricId")
    void deleteMetric(String metricId);
    
    /**
     * Delete all metrics for a user
     */
    @Query("DELETE FROM health_metrics WHERE userId = :userId")
    void deleteAllMetricsForUser(String userId);
    
    /**
     * Get metrics that need sync
     */
    @Query("SELECT * FROM health_metrics WHERE needsSync = 1")
    List<HealthMetric> getMetricsNeedingSync();
    
    /**
     * Mark metric as synced
     */
    @Query("UPDATE health_metrics SET needsSync = 0, lastSyncedAt = :syncTime WHERE id = :metricId")
    void markAsSynced(String metricId, long syncTime);
    
    /**
     * Get latest metric by type
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND type = :type ORDER BY measuredAt DESC LIMIT 1")
    HealthMetric getLatestMetricByType(String userId, String type);
    
    /**
     * Get metrics count for a user
     */
    @Query("SELECT COUNT(*) FROM health_metrics WHERE userId = :userId")
    int getMetricsCount(String userId);
    
    // ==================== RxJava3 Methods ====================
    
    /**
     * Insert metric (RxJava - returns Completable)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertMetricRx(HealthMetric metric);
    
    /**
     * Insert multiple metrics (RxJava)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertMetricsRx(List<HealthMetric> metrics);
    
    /**
     * Get all metrics for a user (RxJava - Flowable for backpressure support)
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId ORDER BY measuredAt DESC")
    Flowable<List<HealthMetric>> getMetricsForUserRx(String userId);
    
    /**
     * Get metrics by type (RxJava - Flowable)
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND type = :type ORDER BY measuredAt DESC")
    Flowable<List<HealthMetric>> getMetricsByTypeRx(String userId, String type);
    
    /**
     * Get metric by ID (RxJava - Maybe because it might not exist)
     */
    @Query("SELECT * FROM health_metrics WHERE id = :metricId LIMIT 1")
    Maybe<HealthMetric> getMetricByIdRx(String metricId);
    
    /**
     * Get latest metric by type (RxJava - Maybe)
     */
    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND type = :type ORDER BY measuredAt DESC LIMIT 1")
    Maybe<HealthMetric> getLatestMetricByTypeRx(String userId, String type);
    
    /**
     * Delete metric (RxJava - Completable)
     */
    @Query("DELETE FROM health_metrics WHERE id = :metricId")
    Completable deleteMetricRx(String metricId);
    
    /**
     * Get metrics count (RxJava - Single)
     */
    @Query("SELECT COUNT(*) FROM health_metrics WHERE userId = :userId")
    Single<Integer> getMetricsCountRx(String userId);
    
    /**
     * Get metrics that need sync (RxJava - Single)
     */
    @Query("SELECT * FROM health_metrics WHERE needsSync = 1")
    Single<List<HealthMetric>> getMetricsNeedingSyncRx();
}
