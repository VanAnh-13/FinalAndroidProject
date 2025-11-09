package com.example.healthylifehub.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthylifehub.data.model.SyncStatus;

import java.util.List;

/**
 * DAO for SyncStatus entity
 * Manages synchronization tracking
 */
@Dao
public interface SyncStatusDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SyncStatus syncStatus);
    
    @Update
    void update(SyncStatus syncStatus);
    
    @Query("DELETE FROM sync_status WHERE entityId = :entityId")
    void deleteById(String entityId);
    
    @Query("DELETE FROM sync_status WHERE userId = :userId")
    void deleteByUserId(String userId);
    
    /**
     * Get sync status for specific entity
     */
    @Query("SELECT * FROM sync_status WHERE entityId = :entityId LIMIT 1")
    SyncStatus getSyncStatus(String entityId);
    
    /**
     * Get all entities that need sync
     */
    @Query("SELECT * FROM sync_status WHERE userId = :userId AND needsSync = 1 AND isSyncing = 0")
    List<SyncStatus> getEntitiesNeedingSync(String userId);
    
    /**
     * Get entities by type that need sync
     */
    @Query("SELECT * FROM sync_status WHERE userId = :userId AND entityType = :entityType AND needsSync = 1 AND isSyncing = 0")
    List<SyncStatus> getEntitiesNeedingSyncByType(String userId, String entityType);
    
    /**
     * Get failed sync entities
     */
    @Query("SELECT * FROM sync_status WHERE userId = :userId AND syncFailed = 1")
    List<SyncStatus> getFailedSyncEntities(String userId);
    
    /**
     * Mark entity as needing sync
     */
    @Query("UPDATE sync_status SET needsSync = 1, lastModifiedTime = :timestamp WHERE entityId = :entityId")
    void markNeedsSync(String entityId, long timestamp);
    
    /**
     * Mark entity as syncing
     */
    @Query("UPDATE sync_status SET isSyncing = 1, syncAttempts = syncAttempts + 1 WHERE entityId = :entityId")
    void markSyncing(String entityId);
    
    /**
     * Mark sync success
     */
    @Query("UPDATE sync_status SET needsSync = 0, isSyncing = 0, syncFailed = 0, lastSyncTime = :timestamp, syncError = NULL WHERE entityId = :entityId")
    void markSyncSuccess(String entityId, long timestamp);
    
    /**
     * Mark sync failed
     */
    @Query("UPDATE sync_status SET isSyncing = 0, syncFailed = 1, syncError = :error WHERE entityId = :entityId")
    void markSyncFailed(String entityId, String error);
    
    /**
     * Reset sync attempts
     */
    @Query("UPDATE sync_status SET syncAttempts = 0 WHERE entityId = :entityId")
    void resetSyncAttempts(String entityId);
}
