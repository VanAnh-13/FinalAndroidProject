package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ReminderHistory entity - Tracks user interactions with reminder notifications
 * Stores completion and skip actions for progress tracking
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(
    tableName = "reminder_history",
    foreignKeys = @ForeignKey(
        entity = Reminder.class,
        parentColumns = "reminderId",
        childColumns = "reminderId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "reminderId"),
        @Index(value = "timestamp")
    }
)
public class ReminderHistory {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;                  // UUID for unique identification
    
    @NonNull
    private String reminderId;          // Foreign key to Reminder
    
    @NonNull
    private String actionType;          // "completed" or "skipped"
    
    private long timestamp;             // When the action was performed
    
    private long scheduledTime;         // When the reminder was originally scheduled
    

    
    /**
     * Check if this history entry represents a completed action
     */
    public boolean isCompleted() {
        return "completed".equals(actionType);
    }
    
    /**
     * Check if this history entry represents a skipped action
     */
    public boolean isSkipped() {
        return "skipped".equals(actionType);
    }
}