package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Smart Suggestion model
 * AI-generated suggestions for reminder optimization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartSuggestion {
    private String suggestionId;
    private String userId;
    private String type;                    // "optimize_time", "create_reminder", "merge_reminders", "change_frequency"
    private String title;                   // Suggestion title
    private String description;             // Detailed explanation
    private String reason;                  // Why this suggestion is made
    
    // Related data
    private String reminderId;              // Related reminder ID (if applicable)
    private String[] reminderIds;           // Multiple reminders (for merge)
    
    // Suggested changes
    private Long suggestedTime;             // New suggested time (timestamp)
    private String suggestedFrequency;      // New suggested frequency
    private String suggestedTitle;          // New suggested title
    
    // Confidence & priority
    private double confidenceScore;         // 0.0 - 1.0 (AI confidence)
    private int priority;                   // 1 (high) - 3 (low)
    
    // Status
    private String status;                  // "pending", "applied", "dismissed"
    private long createdAt;                 // When suggestion was created
    private long appliedAt;                 // When user applied it (0 if not applied)
    private long dismissedAt;               // When user dismissed it (0 if not dismissed)
}
