package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reminder model - Matches Firestore schema from Project_Sumary.md
 * Collection: users/{userId}/reminders/{reminderId}
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(tableName = "reminders")
public class Reminder {
	@PrimaryKey(autoGenerate = false)
	@NonNull
	private String reminderId;          // Firestore auto-generated ID
	private String userId;              // Owner user ID
	private String title;               // Reminder title
	private String description;         // Detailed content
	private long reminderTime;          // Timestamp (milliseconds)
	private String frequency;           // "once", "daily", "weekly", "monthly"
	private boolean isActive;           // Active/inactive status
	private String medicineId;          // Linked medicine ID (optional)
	private long createdAt;             // Creation timestamp
	private long updatedAt;             // Last update timestamp
	
	// New fields for smart reminder system
	private Long deadline;              // Deadline timestamp (nullable for unlimited reminders)
	private int totalExpected;          // Total expected reminder count based on frequency and deadline
	private int completedCount;         // Number of completed reminders
	
	// For backward compatibility
	@Ignore
	public Reminder(String title, String time) {
		this.title = title;
		this.reminderTime = System.currentTimeMillis();
		this.frequency = "once";
		this.isActive = true;
		this.totalExpected = 0;
		this.completedCount = 0;
	}

    @Ignore
    public Reminder(String reminderId, String userId, String title, String description, long reminderTime, String frequency, boolean isActive, String medicineId, long createdAt, long updatedAt, Long deadline, int totalExpected, int completedCount) {
        this.reminderId = reminderId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.reminderTime = reminderTime;
        this.frequency = frequency;
        this.isActive = isActive;
        this.medicineId = medicineId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deadline = deadline;
        this.totalExpected = totalExpected;
        this.completedCount = completedCount;
    }
	
	/**
	 * Calculate progress percentage for this reminder
	 * @return Progress percentage (0-100), or 0 if no deadline set
	 */
	public float getProgressPercentage() {
		if (totalExpected == 0) return 0f;
		return Math.min(100f, (completedCount * 100f) / totalExpected);
	}
	
	/**
	 * Check if reminder has a deadline
	 */
	public boolean hasDeadline() {
		return deadline != null;
	}
	
	/**
	 * Check if reminder is expired (past deadline)
	 */
	public boolean isExpired() {
		return deadline != null && System.currentTimeMillis() > deadline;
	}
	
	/**
	 * Check if reminder is completed (100% progress)
	 */
	public boolean isCompleted() {
		return totalExpected > 0 && completedCount >= totalExpected;
	}
}
