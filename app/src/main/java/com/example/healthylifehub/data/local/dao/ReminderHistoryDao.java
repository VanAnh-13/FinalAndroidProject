package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthylifehub.data.model.ReminderHistory;

import java.util.List;

/**
 * DAO for ReminderHistory entity
 * Provides CRUD operations for reminder interaction history
 */
@Dao
public interface ReminderHistoryDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReminderHistory reminderHistory);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ReminderHistory> reminderHistories);
    
    // ==================== UPDATE ====================
    
    @Update
    void update(ReminderHistory reminderHistory);
    
    // ==================== DELETE ====================
    
    @Delete
    void delete(ReminderHistory reminderHistory);
    
    @Query("DELETE FROM reminder_history WHERE id = :id")
    void deleteById(String id);
    
    @Query("DELETE FROM reminder_history WHERE reminderId = :reminderId")
    void deleteByReminderId(String reminderId);
    
    @Query("DELETE FROM reminder_history")
    void deleteAll();
    
    // ==================== QUERY ====================
    
    /**
     * Get all history for a specific reminder (LiveData - auto-updates UI)
     */
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    LiveData<List<ReminderHistory>> getHistoryByReminderId(String reminderId);
    
    /**
     * Get all history for a specific reminder (non-LiveData for sync operations)
     */
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId ORDER BY timestamp DESC")
    List<ReminderHistory> getHistoryByReminderIdSync(String reminderId);
    
    /**
     * Get single history entry by ID
     */
    @Query("SELECT * FROM reminder_history WHERE id = :id LIMIT 1")
    ReminderHistory getHistoryById(String id);
    
    /**
     * Get completed count for a specific reminder
     */
    @Query("SELECT COUNT(*) FROM reminder_history WHERE reminderId = :reminderId AND actionType = 'completed'")
    int getCompletedCountByReminderId(String reminderId);
    
    /**
     * Get skipped count for a specific reminder
     */
    @Query("SELECT COUNT(*) FROM reminder_history WHERE reminderId = :reminderId AND actionType = 'skipped'")
    int getSkippedCountByReminderId(String reminderId);
    
    /**
     * Get total interaction count for a specific reminder
     */
    @Query("SELECT COUNT(*) FROM reminder_history WHERE reminderId = :reminderId")
    int getTotalInteractionCount(String reminderId);
    
    /**
     * Get recent history entries (last 30 days)
     */
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId AND timestamp > :thirtyDaysAgo ORDER BY timestamp DESC")
    List<ReminderHistory> getRecentHistory(String reminderId, long thirtyDaysAgo);
    
    /**
     * Get history by action type
     */
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId AND actionType = :actionType ORDER BY timestamp DESC")
    List<ReminderHistory> getHistoryByActionType(String reminderId, String actionType);
    
    /**
     * Get history within date range
     */
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<ReminderHistory> getHistoryInDateRange(String reminderId, long startTime, long endTime);
    
    /**
     * Get latest history entry for a reminder
     */
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId ORDER BY timestamp DESC LIMIT 1")
    ReminderHistory getLatestHistoryEntry(String reminderId);
    
    /**
     * Check if reminder has any history
     */
    @Query("SELECT EXISTS(SELECT 1 FROM reminder_history WHERE reminderId = :reminderId)")
    boolean hasHistory(String reminderId);
    
    /**
     * Get completion rate for a reminder (percentage of completed vs total interactions)
     */
    @Query("SELECT CASE WHEN COUNT(*) = 0 THEN 0.0 ELSE " +
           "(CAST(SUM(CASE WHEN actionType = 'completed' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) * 100 END " +
           "FROM reminder_history WHERE reminderId = :reminderId")
    float getCompletionRate(String reminderId);
}