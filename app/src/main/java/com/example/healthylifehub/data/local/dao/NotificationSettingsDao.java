package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthylifehub.data.model.NotificationSettings;

/**
 * DAO for NotificationSettings
 * Follows project pattern: Room database operations
 */
@Dao
public interface NotificationSettingsDao {
    
    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    LiveData<NotificationSettings> getSettingsForUser(String userId);
    
    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    NotificationSettings getSettingsForUserSync(String userId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSettings(NotificationSettings settings);
    
    @Update
    void updateSettings(NotificationSettings settings);
    
    @Query("DELETE FROM notification_settings WHERE userId = :userId")
    void deleteSettingsForUser(String userId);
    
    @Query("DELETE FROM notification_settings")
    void deleteAllSettings();
    
    @Query("SELECT * FROM notification_settings WHERE needsSync = 1")
    java.util.List<NotificationSettings> getUnsyncedSettings();
    
    @Query("UPDATE notification_settings SET needsSync = 0, lastSyncedAt = :timestamp WHERE userId = :userId")
    void markAsSynced(String userId, long timestamp);
}