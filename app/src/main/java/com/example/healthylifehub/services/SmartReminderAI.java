package com.example.healthylifehub.services;

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
     * Enhanced with hourly completion rates and preferred hours (Requirement 5.3)
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
        
        // Enhanced: Track completed vs total per hour (Requirement 5.3)
        Map<Integer, Integer> hourCompletedCounts = new HashMap<>();
        Map<Integer, Integer> hourTotalCounts = new HashMap<>();
        
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
            
            // Enhanced: Track hourly completion rates (Requirement 5.3)
            hourTotalCounts.put(hour, hourTotalCounts.getOrDefault(hour, 0) + 1);
            if (reminder.isActive()) {
                hourCompletedCounts.put(hour, hourCompletedCounts.getOrDefault(hour, 0) + 1);
            }
            
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
        
        // Calculate hourly completion rates (Requirement 5.3)
        Map<Integer, Double> hourlyCompletionRates = calculateHourlyCompletionRates(
            hourCompletedCounts, hourTotalCounts
        );
        
        // Identify preferred hours (top 3 with highest completion) (Requirement 5.3)
        List<Integer> preferredHours = identifyPreferredHours(hourlyCompletionRates);
        
        // Calculate overall completion rate (Requirement 5.3)
        double overallCompletionRate = total > 0 ? (double) completed / total : 0.0;
        
        // Set basic stats
        behavior.setUserId("");
        behavior.setTotalReminders(total);
        behavior.setCompletedReminders(completed);
        behavior.setDismissedReminders(dismissed);
        behavior.setCompletionRate(overallCompletionRate);
        
        // Set time patterns
        behavior.setActiveHours(hourCounts);
        behavior.setMostActiveHour(findMostActiveHour(hourCounts));
        behavior.setLeastActiveHour(findLeastActiveHour(hourCounts));
        
        // Set enhanced time patterns (Requirement 5.3)
        behavior.setHourlyCompletionRates(hourlyCompletionRates);
        behavior.setPreferredHours(preferredHours);
        
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
        behavior.setAnalyzedAt(System.currentTimeMillis());
        behavior.setDataPoints(total);
        
        return behavior;
    }
    
    /**
     * Calculate hourly completion rates (Requirement 5.3)
     * @param completedCounts Map of hour -> completed count
     * @param totalCounts Map of hour -> total count
     * @return Map of hour -> completion rate (0.0-1.0)
     */
    private static Map<Integer, Double> calculateHourlyCompletionRates(
            Map<Integer, Integer> completedCounts,
            Map<Integer, Integer> totalCounts) {
        
        Map<Integer, Double> rates = new HashMap<>();
        
        for (Map.Entry<Integer, Integer> entry : totalCounts.entrySet()) {
            int hour = entry.getKey();
            int total = entry.getValue();
            int completed = completedCounts.getOrDefault(hour, 0);
            
            double rate = total > 0 ? (double) completed / total : 0.0;
            rates.put(hour, rate);
        }
        
        return rates;
    }
    
    /**
     * Identify preferred hours (top 3 with highest completion) (Requirement 5.3)
     * @param hourlyCompletionRates Map of hour -> completion rate
     * @return List of top 3 hours with highest completion rates
     */
    private static List<Integer> identifyPreferredHours(Map<Integer, Double> hourlyCompletionRates) {
        List<Integer> preferredHours = new ArrayList<>();
        
        if (hourlyCompletionRates.isEmpty()) {
            return preferredHours;
        }
        
        // Sort hours by completion rate (descending)
        List<Map.Entry<Integer, Double>> sortedEntries = new ArrayList<>(hourlyCompletionRates.entrySet());
        sortedEntries.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // Get top 3 hours
        int count = Math.min(3, sortedEntries.size());
        for (int i = 0; i < count; i++) {
            preferredHours.add(sortedEntries.get(i).getKey());
        }
        
        return preferredHours;
    }
    
    /**
     * Generate smart suggestions based on user behavior
     * Enhanced with TIME_ADJUSTMENT and FREQUENCY_CHANGE suggestions (Requirements 5.3, 5.4)
     * @param reminders Current reminders
     * @param behavior Analyzed user behavior
     * @return List of smart suggestions
     */
    public static List<SmartSuggestion> generateSmartSuggestions(List<Reminder> reminders, UserBehavior behavior) {
        List<SmartSuggestion> suggestions = new ArrayList<>();
        
        if (behavior.getDataPoints() < MIN_DATA_POINTS) {
            // Not enough data for meaningful suggestions
            return suggestions;
        }
        
        // Enhanced Suggestion 1: TIME_ADJUSTMENT based on preferred hours (Requirements 5.3, 5.4)
        List<SmartSuggestion> timeAdjustments = suggestTimeAdjustments(reminders, behavior);
        suggestions.addAll(timeAdjustments);
        
        // Enhanced Suggestion 2: FREQUENCY_CHANGE based on completion patterns (Requirements 5.3, 5.4)
        List<SmartSuggestion> frequencyChanges = suggestFrequencyChanges(reminders, behavior);
        suggestions.addAll(frequencyChanges);
        
        // Suggestion 3: Create water reminder if none exists
        SmartSuggestion waterSuggestion = suggestWaterReminder(reminders);
        if (waterSuggestion != null) {
            suggestions.add(waterSuggestion);
        }
        
        // Suggestion 4: Merge similar reminders
        SmartSuggestion mergeSuggestion = suggestMergeReminders(reminders);
        if (mergeSuggestion != null) {
            suggestions.add(mergeSuggestion);
        }
        
        return suggestions;
    }
    
    /**
     * Suggest TIME_ADJUSTMENT for reminders not aligned with preferred hours (Requirements 5.3, 5.4)
     * Generate suggestion if difference > 30 minutes from preferred hours
     */
    private static List<SmartSuggestion> suggestTimeAdjustments(List<Reminder> reminders, UserBehavior behavior) {
        List<SmartSuggestion> suggestions = new ArrayList<>();
        
        List<Integer> preferredHours = behavior.getPreferredHours();
        if (preferredHours == null || preferredHours.isEmpty()) {
            return suggestions;
        }
        
        // Get the best preferred hour (first in list, highest completion rate)
        int bestPreferredHour = preferredHours.get(0);
        
        for (Reminder reminder : reminders) {
            if (!reminder.isActive()) continue;
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(reminder.getReminderTime());
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);
            int currentMinute = cal.get(Calendar.MINUTE);
            
            // Calculate time difference in minutes
            int currentTimeInMinutes = currentHour * 60 + currentMinute;
            int preferredTimeInMinutes = bestPreferredHour * 60;
            int differenceInMinutes = Math.abs(currentTimeInMinutes - preferredTimeInMinutes);
            
            // Generate TIME_ADJUSTMENT suggestion if difference > 30 minutes (Requirement 5.4)
            if (differenceInMinutes > 30) {
                SmartSuggestion suggestion = new SmartSuggestion();
                suggestion.setSuggestionId("time_adj_" + reminder.getReminderId() + "_" + System.currentTimeMillis());
                suggestion.setType("TIME_ADJUSTMENT");
                suggestion.setTitle("Điều chỉnh thời gian nhắc nhở");
                suggestion.setDescription(String.format(
                    "Chuyển \"%s\" từ %02d:%02d sang %02d:00 để tăng khả năng hoàn thành",
                    reminder.getTitle(), currentHour, currentMinute, bestPreferredHour
                ));
                suggestion.setReason(String.format(
                    "Bạn có tỷ lệ hoàn thành cao nhất vào %02d:00 (%.0f%%)",
                    bestPreferredHour,
                    behavior.getHourlyCompletionRates().getOrDefault(bestPreferredHour, 0.0) * 100
                ));
                suggestion.setReminderId(reminder.getReminderId());
                
                // Set current and suggested values (Requirement 5.4)
                suggestion.setCurrentValue(String.format("%02d:%02d", currentHour, currentMinute));
                suggestion.setSuggestedValue(String.format("%02d:00", bestPreferredHour));
                
                // Calculate suggested time
                Calendar suggestedCal = Calendar.getInstance();
                suggestedCal.setTimeInMillis(reminder.getReminderTime());
                suggestedCal.set(Calendar.HOUR_OF_DAY, bestPreferredHour);
                suggestedCal.set(Calendar.MINUTE, 0);
                suggestion.setSuggestedTime(suggestedCal.getTimeInMillis());
                
                suggestion.setConfidenceScore(HIGH_CONFIDENCE);
                suggestion.setPriority(1);
                suggestion.setStatus("pending");
                suggestion.setCreatedAt(System.currentTimeMillis());
                
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Suggest FREQUENCY_CHANGE based on completion patterns (Requirements 5.3, 5.4)
     */
    private static List<SmartSuggestion> suggestFrequencyChanges(List<Reminder> reminders, UserBehavior behavior) {
        List<SmartSuggestion> suggestions = new ArrayList<>();
        
        double overallCompletionRate = behavior.getCompletionRate();
        
        for (Reminder reminder : reminders) {
            if (!reminder.isActive()) continue;
            
            String currentFrequency = reminder.getFrequency();
            if (currentFrequency == null) continue;
            
            // Suggest reducing frequency if completion rate is low (< 50%)
            if (overallCompletionRate < 0.5 && "daily".equals(currentFrequency)) {
                SmartSuggestion suggestion = new SmartSuggestion();
                suggestion.setSuggestionId("freq_change_" + reminder.getReminderId() + "_" + System.currentTimeMillis());
                suggestion.setType("FREQUENCY_CHANGE");
                suggestion.setTitle("Giảm tần suất nhắc nhở");
                suggestion.setDescription(String.format(
                    "Chuyển \"%s\" từ hàng ngày sang hàng tuần",
                    reminder.getTitle()
                ));
                suggestion.setReason(String.format(
                    "Tỷ lệ hoàn thành thấp (%.0f%%) - giảm tần suất có thể giúp bạn duy trì tốt hơn",
                    overallCompletionRate * 100
                ));
                suggestion.setReminderId(reminder.getReminderId());
                
                // Set current and suggested values (Requirement 5.4)
                suggestion.setCurrentValue("daily");
                suggestion.setSuggestedValue("weekly");
                suggestion.setSuggestedFrequency("weekly");
                
                suggestion.setConfidenceScore(MEDIUM_CONFIDENCE);
                suggestion.setPriority(2);
                suggestion.setStatus("pending");
                suggestion.setCreatedAt(System.currentTimeMillis());
                
                suggestions.add(suggestion);
            }
            // Suggest increasing frequency if completion rate is high (> 80%)
            else if (overallCompletionRate > 0.8 && "weekly".equals(currentFrequency)) {
                SmartSuggestion suggestion = new SmartSuggestion();
                suggestion.setSuggestionId("freq_change_" + reminder.getReminderId() + "_" + System.currentTimeMillis());
                suggestion.setType("FREQUENCY_CHANGE");
                suggestion.setTitle("Tăng tần suất nhắc nhở");
                suggestion.setDescription(String.format(
                    "Chuyển \"%s\" từ hàng tuần sang hàng ngày",
                    reminder.getTitle()
                ));
                suggestion.setReason(String.format(
                    "Tỷ lệ hoàn thành cao (%.0f%%) - bạn có thể duy trì tần suất cao hơn",
                    overallCompletionRate * 100
                ));
                suggestion.setReminderId(reminder.getReminderId());
                
                // Set current and suggested values (Requirement 5.4)
                suggestion.setCurrentValue("weekly");
                suggestion.setSuggestedValue("daily");
                suggestion.setSuggestedFrequency("daily");
                
                suggestion.setConfidenceScore(MEDIUM_CONFIDENCE);
                suggestion.setPriority(2);
                suggestion.setStatus("pending");
                suggestion.setCreatedAt(System.currentTimeMillis());
                
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }
    

    
    /**
     * Suggest creating water reminder
     */
    private static SmartSuggestion suggestWaterReminder(List<Reminder> reminders) {
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
        suggestion.setTitle("Tạo lời nhắc uống nước");
        suggestion.setDescription("Uống đủ nước mỗi ngày giúp cải thiện sức khỏe");
        suggestion.setReason("Bạn chưa có lời nhắc uống nước");
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
    private static SmartSuggestion suggestMergeReminders(List<Reminder> reminders) {
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
                    suggestion.setTitle("Gộp lời nhắc");
                    suggestion.setDescription("Gộp \"" + r1.getTitle() + "\" và \"" + r2.getTitle() + "\"");
                    suggestion.setReason("Hai lời nhắc này gần nhau về thời gian");
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
