package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sync Status model
 * Tracks synchronization state for offline-first architecture
 */
@Data
@NoArgsConstructor
@Entity(tableName = "sync_status")
public class SyncStatus {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String entityId;            // ID of the synced entity
    
    private String entityType;          // "reminder", "health_metric", "user", etc.
    private String userId;              // Owner user ID
    
    private boolean needsSync;          // True if local changes need upload
    private boolean isSyncing;          // True if currently syncing
    private boolean syncFailed;         // True if last sync failed
    
    private long lastSyncTime;          // Timestamp of last successful sync
    private long lastModifiedTime;      // Timestamp of last local modification
    private int syncAttempts;           // Number of sync attempts
    private String syncError;           // Last sync error message
    
    // Conflict resolution
    private String conflictStrategy;    // "last_write_wins", "manual", "merge"
    private boolean hasConflict;        // True if conflict detected
    private String conflictData;        // JSON of conflicting data

    @Ignore
    public SyncStatus(String entityId, String entityType, String userId, boolean needsSync, boolean isSyncing, boolean syncFailed, long lastSyncTime, long lastModifiedTime, int syncAttempts, String syncError, String conflictStrategy, boolean hasConflict, String conflictData) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.userId = userId;
        this.needsSync = needsSync;
        this.isSyncing = isSyncing;
        this.syncFailed = syncFailed;
        this.lastSyncTime = lastSyncTime;
        this.lastModifiedTime = lastModifiedTime;
        this.syncAttempts = syncAttempts;
        this.syncError = syncError;
        this.conflictStrategy = conflictStrategy;
        this.hasConflict = hasConflict;
        this.conflictData = conflictData;
    }
}
