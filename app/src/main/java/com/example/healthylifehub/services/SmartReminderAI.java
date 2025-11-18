package com.example.healthylifehub.services;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.SmartSuggestion;
import com.example.healthylifehub.data.model.UserBehavior;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Service for Smart Reminder Analysis
 * Analyzes user behavior and generates intelligent suggestions
 */
public class SmartReminderAI {
    
    private static final int MIN_DATA_POINTS = 5;  // Minimum reminders needed for analysis
    private static final double HIGH_CONFIDENCE = 0.8;
    private static final double MEDIUM_CONFIDENCE = 0.6;
    
    /**
     * Analyze user behavior from reminder history
     * @param reminders List of all reminders
     * @return UserBehavior object with analysis results
     */
    public static UserBehavior analyzeUserBehavior(List<Reminder> reminders) {
        UserBehavior behavior = new UserBehavior();
        
        if (reminders == null || reminders.isEmpty()) {
            return behavior;
        }
        
        int total = reminders.size();
        int completed = 0;
        int dismissed = 0;
        int medicineCount = 0;
        
        Map<Integer, Integer> hourCounts = new HashMap<>();
        Map<String, Integer> frequencyCounts = new HashMap<>();
        
        long totalResponseTime = 0;
        long fastest = Long.MAX_VALUE;
        long slowest = 0;
        int responseCount = 0;
        
        // Analyze each reminder
        for (Reminder reminder : reminders) {
            // Count by status
            if (reminder.isActive()) {
                completed++;
            } else {
                dismissed++;
            }
            
            // Count medicine reminders
            if (reminder.getMedicineId() != null && !reminder.getMedicineId().isEmpty()) {
                medicineCount++;
            }
            
            // Analyze time patterns
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(reminder.getReminderTime());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);
            
            // Analyze frequency patterns
            String freq = reminder.getFrequency();
            if (freq != null) {
                frequencyCounts.put(freq, frequencyCounts.getOrDefault(freq, 0) + 1);
            }
            
            // Calculate response time (simplified - using creation to update time)
            if (reminder.getUpdatedAt() > reminder.getCreatedAt()) {
                long responseTime = reminder.getUpdatedAt() - reminder.getCreatedAt();
                totalResponseTime += responseTime;
                responseCount++;
                
                if (responseTime < fastest) fastest = responseTime;
                if (responseTime > slowest) slowest = responseTime;
            }
        }
        
        // Set basic stats
        behavior.setUserId("");
        behavior.setTotalReminders(total);
        behavior.setCompletedReminders(completed);
        behavior.setDismissedReminders(dismissed);
        behavior.setCompletionRate(total > 0 ? (double) completed / total : 0.0);
        
        // Set time patterns
        behavior.setActiveHours(hourCounts);
        behavior.setMostActiveHour(findMostActiveHour(hourCounts));
        behavior.setLeastActiveHour(findLeastActiveHour(hourCounts));
        
        // Set response times
        behavior.setAverageResponseTime(responseCount > 0 ? totalResponseTime / responseCount : 0);
        behavior.setFastestResponseTime(fastest != Long.MAX_VALUE ? fastest : 0);
        behavior.setSlowestResponseTime(slowest);
        
        // Set frequency patterns
        behavior.setFrequencyPreference(frequencyCounts);
        behavior.setPreferredFrequency(findPreferredFrequency(frequencyCounts));
        
        // Set medicine stats
        behavior.setMedicineReminders(medicineCount);
        behavior.setMedicineCompletionRate(medicineCount > 0 ? (double) completed / medicineCount : 0.0);
        
        // Set metadata
        behavior.setLastAnalyzedAt(System.currentTimeMillis());
        behavior.setDataPoints(total);
        
        return behavior;
    }
    
    /**
     * Generate smart suggestions based on user behavior
     * @param reminders Current reminders
     * @param behavior Analyzed user behavior
     * @param context Android context for string resources
     * @return List of smart suggestions
     */
    public static List<SmartSuggestion> generateSmartSuggestions(List<Reminder> reminders, UserBehavior behavior, android.content.Context context) {
        List<SmartSuggestion> suggestions = new ArrayList<>();
        
        if (behavior.getDataPoints() < MIN_DATA_POINTS) {
            // Not enough data for meaningful suggestions
            return suggestions;
        }
        
        // Suggestion 1: Optimize reminder times based on active hours
        if (behavior.getMostActiveHour() > 0) {
            SmartSuggestion timeSuggestion = suggestOptimalTime(reminders, behavior, context);
            if (timeSuggestion != null) {
                suggestions.add(timeSuggestion);
            }
        }
        
        // Suggestion 2: Create water reminder if none exists
        SmartSuggestion waterSuggestion = suggestWaterReminder(reminders, context);
        if (waterSuggestion != null) {
            suggestions.add(waterSuggestion);
        }
        
        // Suggestion 3: Merge similar reminders
        SmartSuggestion mergeSuggestion = suggestMergeReminders(reminders, context);
        if (mergeSuggestion != null) {
            suggestions.add(mergeSuggestion);
        }
        
        // Suggestion 4: Change frequency based on completion rate
        if (behavior.getCompletionRate() < 0.5) {
            SmartSuggestion frequencySuggestion = suggestFrequencyChange(reminders, behavior, context);
            if (frequencySuggestion != null) {
                suggestions.add(frequencySuggestion);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Suggest optimal time for reminders
     */
    private static SmartSuggestion suggestOptimalTime(List<Reminder> reminders, UserBehavior behavior, android.content.Context context) {
        int optimalHour = behavior.getMostActiveHour();
        
        // Find reminders scheduled outside optimal hours
        for (Reminder reminder : reminders) {
            if (!reminder.isActive()) continue;
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(reminder.getReminderTime());
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);
            
            // If reminder is far from optimal hour
            if (Math.abs(currentHour - optimalHour) > 3) {
                SmartSuggestion suggestion = new SmartSuggestion();
                suggestion.setSuggestionId("opt_time_" + reminder.getReminderId());
                suggestion.setType("optimize_time");
                suggestion.setTitle(context.getString(R.string.optimize_reminder_time));
                suggestion.setDescription(context.getString(R.string.change_to_time, reminder.getTitle(), optimalHour));
                suggestion.setReason(context.getString(R.string.optimal_time_reason, optimalHour));
                suggestion.setReminderId(reminder.getReminderId());
                
                // Calculate suggested time
                cal.set(Calendar.HOUR_OF_DAY, optimalHour);
                cal.set(Calendar.MINUTE, 0);
                suggestion.setSuggestedTime(cal.getTimeInMillis());
                
                suggestion.setConfidenceScore(HIGH_CONFIDENCE);
                suggestion.setPriority(1);
                suggestion.setStatus("pending");
                suggestion.setCreatedAt(System.currentTimeMillis());
                
                return suggestion;
            }
        }
        
        return null;
    }
    
    /**
     * Suggest creating water reminder
     */
    private static SmartSuggestion suggestWaterReminder(List<Reminder> reminders, android.content.Context context) {
        // Check if water reminder already exists
        for (Reminder reminder : reminders) {
            if (reminder.getTitle() != null && 
                reminder.getTitle().toLowerCase().contains("nước")) {
                return null; // Already has water reminder
            }
        }
        
        SmartSuggestion suggestion = new SmartSuggestion();
        suggestion.setSuggestionId("create_water_" + System.currentTimeMillis());
        suggestion.setType("create_reminder");
        suggestion.setTitle(context.getString(R.string.create_water_reminder));
        suggestion.setDescription(context.getString(R.string.water_reminder_description));
        suggestion.setReason(context.getString(R.string.no_water_reminder_reason));
        suggestion.setSuggestedTitle("Uống nước");
        suggestion.setSuggestedFrequency("daily");
        suggestion.setConfidenceScore(MEDIUM_CONFIDENCE);
        suggestion.setPriority(2);
        suggestion.setStatus("pending");
        suggestion.setCreatedAt(System.currentTimeMillis());
        
        return suggestion;
    }
    
    /**
     * Suggest merging similar reminders
     */
    private static SmartSuggestion suggestMergeReminders(List<Reminder> reminders, android.content.Context context) {
        // Find reminders with similar times (within 30 minutes)
        for (int i = 0; i < reminders.size(); i++) {
            for (int j = i + 1; j < reminders.size(); j++) {
                Reminder r1 = reminders.get(i);
                Reminder r2 = reminders.get(j);
                
                long timeDiff = Math.abs(r1.getReminderTime() - r2.getReminderTime());
                if (timeDiff < 30 * 60 * 1000) { // 30 minutes
                    SmartSuggestion suggestion = new SmartSuggestion();
                    suggestion.setSuggestionId("merge_" + r1.getReminderId() + "_" + r2.getReminderId());
                    suggestion.setType("merge_reminders");
                    suggestion.setTitle(context.getString(R.string.merge_reminders_suggestion));
                    suggestion.setDescription(context.getString(R.string.merge_two_reminders, r1.getTitle(), r2.getTitle()));
                    suggestion.setReason(context.getString(R.string.merge_reason));
                    suggestion.setReminderIds(new String[]{r1.getReminderId(), r2.getReminderId()});
                    suggestion.setConfidenceScore(MEDIUM_CONFIDENCE);
                    suggestion.setPriority(3);
                    suggestion.setStatus("pending");
                    suggestion.setCreatedAt(System.currentTimeMillis());
                    
                    return suggestion;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Suggest frequency change for low completion rate
     */
    private static SmartSuggestion suggestFrequencyChange(List<Reminder> reminders, UserBehavior behavior, android.content.Context context) {
        // Find daily reminders with low completion
        for (Reminder reminder : reminders) {
            if ("daily".equals(reminder.getFrequency())) {
                SmartSuggestion suggestion = new SmartSuggestion();
                suggestion.setSuggestionId("freq_" + reminder.getReminderId());
                suggestion.setType("change_frequency");
                suggestion.setTitle(context.getString(R.string.change_frequency_suggestion));
                suggestion.setDescription(context.getString(R.string.change_to_weekly, reminder.getTitle()));
                suggestion.setReason(context.getString(R.string.low_completion_reason, (int)(behavior.getCompletionRate() * 100)));
                suggestion.setReminderId(reminder.getReminderId());
                suggestion.setSuggestedFrequency("weekly");
                suggestion.setConfidenceScore(MEDIUM_CONFIDENCE);
                suggestion.setPriority(2);
                suggestion.setStatus("pending");
                suggestion.setCreatedAt(System.currentTimeMillis());
                
                return suggestion;
            }
        }
        
        return null;
    }
    
    // Helper methods
    
    private static int findMostActiveHour(Map<Integer, Integer> hourCounts) {
        int maxHour = 0;
        int maxCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : hourCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxHour = entry.getKey();
            }
        }
        
        return maxHour;
    }
    
    private static int findLeastActiveHour(Map<Integer, Integer> hourCounts) {
        int minHour = 0;
        int minCount = Integer.MAX_VALUE;
        
        for (Map.Entry<Integer, Integer> entry : hourCounts.entrySet()) {
            if (entry.getValue() < minCount) {
                minCount = entry.getValue();
                minHour = entry.getKey();
            }
        }
        
        return minHour;
    }
    
    private static String findPreferredFrequency(Map<String, Integer> frequencyCounts) {
        String preferred = "daily";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : frequencyCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                preferred = entry.getKey();
            }
        }
        
        return preferred;
    }
}
