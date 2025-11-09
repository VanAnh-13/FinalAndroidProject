package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
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
@AllArgsConstructor
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
}
