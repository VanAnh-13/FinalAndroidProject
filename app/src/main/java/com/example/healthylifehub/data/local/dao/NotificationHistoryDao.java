package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthylifehub.data.model.NotificationHistory;

import java.util.List;

/**
 * DAO for NotificationHistory
 */
@Dao
public interface NotificationHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotificationHistory notification);
    
    @Update
    void update(NotificationHistory notification);
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<NotificationHistory>> getAllNotifications(String userId);
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId AND isRead = 0 ORDER BY timestamp DESC")
    LiveData<List<NotificationHistory>> getUnreadNotifications(String userId);
    
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId AND isRead = 0")
    LiveData<Integer> getUnreadCount(String userId);
    
    @Query("UPDATE notification_history SET isRead = 1 WHERE notificationId = :notificationId")
    void markAsRead(String notificationId);
    
    @Query("UPDATE notification_history SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(String userId);
    
    @Query("DELETE FROM notification_history WHERE userId = :userId AND timestamp < :beforeTimestamp")
    void deleteOldNotifications(String userId, long beforeTimestamp);
    
    @Query("DELETE FROM notification_history WHERE userId = :userId")
    void deleteAllForUser(String userId);
}
