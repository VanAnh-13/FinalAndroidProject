package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthylifehub.data.model.Reminder;

import java.util.List;

/**
 * DAO for Reminder entity
 * Provides offline-first data access
 */
@Dao
public interface ReminderDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Reminder reminder);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Reminder> reminders);
    
    // ==================== UPDATE ====================
    
    @Update
    void update(Reminder reminder);
    
    @Query("UPDATE reminders SET isActive = :isActive WHERE reminderId = :reminderId")
    void updateStatus(String reminderId, boolean isActive);
    
    // ==================== DELETE ====================
    
    @Delete
    void delete(Reminder reminder);
    
    @Query("DELETE FROM reminders WHERE reminderId = :reminderId")
    void deleteById(String reminderId);
    
    @Query("DELETE FROM reminders WHERE userId = :userId")
    void deleteByUserId(String userId);
    
    @Query("DELETE FROM reminders")
    void deleteAll();
    
    // ==================== QUERY ====================
    
    /**
     * Get all reminders for a user (LiveData - auto-updates UI)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY reminderTime ASC")
    LiveData<List<Reminder>> getAllReminders(String userId);
    
    /**
     * Get active reminders only
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isActive = 1 ORDER BY reminderTime ASC")
    LiveData<List<Reminder>> getActiveReminders(String userId);
    
    /**
     * Get single reminder by ID
     */
    @Query("SELECT * FROM reminders WHERE reminderId = :reminderId LIMIT 1")
    Reminder getReminderById(String reminderId);
    
    /**
     * Get reminders by frequency
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND frequency = :frequency")
    List<Reminder> getRemindersByFrequency(String userId, String frequency);
    
    /**
     * Get reminders linked to medicine
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND medicineId = :medicineId")
    List<Reminder> getRemindersByMedicine(String userId, String medicineId);
    
    /**
     * Get count of active reminders
     */
    @Query("SELECT COUNT(*) FROM reminders WHERE userId = :userId AND isActive = 1")
    int getActiveReminderCount(String userId);
    
    /**
     * Get all reminders (non-LiveData, for sync operations)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId")
    List<Reminder> getAllRemindersSync(String userId);
    
    /**
     * Get reminders that need sync (for offline-first)
     * Note: This requires a syncStatus field in Reminder model
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND updatedAt > :lastSyncTime")
    List<Reminder> getUnsyncedReminders(String userId, long lastSyncTime);
    
    // ==================== PROGRESS TRACKING QUERIES ====================
    
    /**
     * Get all reminders with progress information (for smart reminder system)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY " +
           "CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, " +
           "deadline ASC, reminderTime ASC")
    LiveData<List<Reminder>> getAllRemindersWithProgress(String userId);
    
    /**
     * Update completed count for a reminder
     */
    @Query("UPDATE reminders SET completedCount = :completedCount, updatedAt = :updatedAt WHERE reminderId = :reminderId")
    void updateCompletedCount(String reminderId, int completedCount, long updatedAt);
    
    /**
     * Update total expected count for a reminder
     */
    @Query("UPDATE reminders SET totalExpected = :totalExpected, updatedAt = :updatedAt WHERE reminderId = :reminderId")
    void updateTotalExpected(String reminderId, int totalExpected, long updatedAt);
    
    /**
     * Update deadline for a reminder
     */
    @Query("UPDATE reminders SET deadline = :deadline, updatedAt = :updatedAt WHERE reminderId = :reminderId")
    void updateDeadline(String reminderId, Long deadline, long updatedAt);
    
    /**
     * Get reminders with deadlines (for scheduling notifications)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND deadline IS NOT NULL AND isActive = 1")
    List<Reminder> getRemindersWithDeadlines(String userId);
    
    /**
     * Get expired reminders (past deadline)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND deadline IS NOT NULL AND deadline < :currentTime")
    List<Reminder> getExpiredReminders(String userId, long currentTime);
    
    /**
     * Get reminders expiring soon (within specified time)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND deadline IS NOT NULL AND " +
           "deadline BETWEEN :currentTime AND :expiryTime AND isActive = 1")
    List<Reminder> getRemindersExpiringSoon(String userId, long currentTime, long expiryTime);
    
    /**
     * Get completed reminders (100% progress)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND totalExpected > 0 AND completedCount >= totalExpected")
    List<Reminder> getCompletedReminders(String userId);
    
    /**
     * Get reminders with low completion rate (less than specified percentage)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND totalExpected > 0 AND " +
           "(CAST(completedCount AS REAL) / totalExpected) * 100 < :minCompletionRate")
    List<Reminder> getLowCompletionReminders(String userId, float minCompletionRate);
    
    /**
     * Get reminder statistics for a user
     */
    @Query("SELECT COUNT(*) as total, " +
           "SUM(CASE WHEN isActive = 1 THEN 1 ELSE 0 END) as active, " +
           "SUM(CASE WHEN totalExpected > 0 AND completedCount >= totalExpected THEN 1 ELSE 0 END) as completed, " +
           "SUM(CASE WHEN deadline IS NOT NULL AND deadline < :currentTime THEN 1 ELSE 0 END) as expired " +
           "FROM reminders WHERE userId = :userId")
    ReminderStats getReminderStats(String userId, long currentTime);
    
    /**
     * Batch update progress for multiple reminders
     */
    @Query("UPDATE reminders SET completedCount = :completedCount, updatedAt = :updatedAt WHERE reminderId IN (:reminderIds)")
    void batchUpdateCompletedCount(List<String> reminderIds, int completedCount, long updatedAt);
    
    /**
     * Get reminders that need progress recalculation
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND totalExpected > 0 AND updatedAt < :lastCalculationTime")
    List<Reminder> getRemindersNeedingProgressUpdate(String userId, long lastCalculationTime);
    
    /**
     * Get all active reminders (for notification scheduling)
     */
    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY reminderTime ASC")
    List<Reminder> getAllActiveReminders();
    
    /**
     * Inner class for reminder statistics
     */
    class ReminderStats {
        public int total;
        public int active;
        public int completed;
        public int expired;
    }
}
