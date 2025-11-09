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
}
