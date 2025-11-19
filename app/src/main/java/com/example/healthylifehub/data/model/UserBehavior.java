package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * User Behavior model for AI analysis
 * Tracks user patterns for smart reminder optimization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehavior {
    private String userId;
    
    // Interaction patterns
    private int totalReminders;                    // Total reminders created
    private int completedReminders;                // Reminders marked as done
    private int dismissedReminders;                // Reminders dismissed/ignored
    private double completionRate;                 // completedReminders / totalReminders
    
    // Time patterns (hour of day: 0-23)
    private Map<Integer, Integer> activeHours;     // Hour -> interaction count
    private int mostActiveHour;                    // Peak activity hour
    private int leastActiveHour;                   // Lowest activity hour
    
    // Enhanced time patterns (Requirement 5.3)
    private Map<Integer, Double> hourlyCompletionRates;  // Hour -> completion rate (0.0-1.0)
    private List<Integer> preferredHours;          // Top 3 hours with highest completion
    
    // Response time patterns
    private long averageResponseTime;              // Average time to respond (ms)
    private long fastestResponseTime;              // Fastest response (ms)
    private long slowestResponseTime;              // Slowest response (ms)
    
    // Frequency patterns
    private Map<String, Integer> frequencyPreference; // "daily" -> count, "weekly" -> count
    private String preferredFrequency;             // Most used frequency
    
    // Medicine-related
    private int medicineReminders;                 // Count of medicine reminders
    private double medicineCompletionRate;         // Medicine reminder completion rate
    
    // Last analysis
    private long lastAnalyzedAt;                   // Timestamp of last analysis
    private long analyzedAt;                       // Timestamp when analysis was performed
    private int dataPoints;                        // Number of data points analyzed
}
