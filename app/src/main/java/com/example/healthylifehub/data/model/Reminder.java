package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reminder model - Matches Firestore schema from Project_Sumary.md
 * Collection: users/{userId}/reminders/{reminderId}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
	
	// For backward compatibility
	public Reminder(String title, String time) {
		this.title = title;
		this.reminderTime = System.currentTimeMillis();
		this.frequency = "once";
		this.isActive = true;
	}
}
