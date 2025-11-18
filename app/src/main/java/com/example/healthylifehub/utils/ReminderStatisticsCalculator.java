package com.example.healthylifehub.utils;

import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.ReminderHistory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for calculating reminder statistics including completion rates and streaks
 * Requirements: 7.5 - Reminder statistics (completion rate, streak tracking)
 */
public class ReminderStatisticsCalculator {
    
    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
    
    /**
     * Statistics container class
     */
    public static class ReminderStatistics {
        private final float completionRate;
        private final int currentStreak;
        private final int bestStreak;
        private final int totalCompleted;
        private final int totalActions;
        
        public ReminderStatistics(float completionRate, int currentStreak, int bestStreak, 
                                int totalCompleted, int totalActions) {
            this.completionRate = completionRate;
            this.currentStreak = currentStreak;
            this.bestStreak = bestStreak;
            this.totalCompleted = totalCompleted;
            this.totalActions = totalActions;
        }
        
        public float getCompletionRate() { return completionRate; }
        public int getCurrentStreak() { return currentStreak; }
        public int getBestStreak() { return bestStreak; }
        public int getTotalCompleted() { return totalCompleted; }
        public int getTotalActions() { return totalActions; }
    }
    
    /**
     * Calculate comprehensive statistics for a reminder
     * 
     * @param reminder The reminder object
     * @param historyList List of reminder history entries
     * @return ReminderStatistics object containing all calculated statistics
     */
    public static ReminderStatistics calculateStatistics(Reminder reminder, List<ReminderHistory> historyList) {
        if (reminder == null || historyList == null || historyList.isEmpty()) {
            return new ReminderStatistics(0f, 0, 0, 0, 0);
        }
        
        // Sort history by timestamp (oldest first)
        List<ReminderHistory> sortedHistory = new ArrayList<>(historyList);
        Collections.sort(sortedHistory, (h1, h2) -> Long.compare(h1.getTimestamp(), h2.getTimestamp()));
        
        // Calculate basic statistics
        int totalCompleted = 0;
        int totalActions = sortedHistory.size();
        
        for (ReminderHistory history : sortedHistory) {
            if (history.isCompleted()) {
                totalCompleted++;
            }
        }
        
        // Calculate completion rate
        float completionRate = totalActions > 0 ? (totalCompleted * 100f) / totalActions : 0f;
        
        // Calculate streaks
        int currentStreak = calculateCurrentStreak(sortedHistory);
        int bestStreak = calculateBestStreak(sortedHistory);
        
        return new ReminderStatistics(completionRate, currentStreak, bestStreak, totalCompleted, totalActions);
    }
    
    /**
     * Calculate current completion streak
     * A streak is consecutive completed actions from the most recent entry backwards
     * 
     * @param sortedHistory History entries sorted by timestamp (oldest first)
     * @return Current streak count
     */
    private static int calculateCurrentStreak(List<ReminderHistory> sortedHistory) {
        if (sortedHistory.isEmpty()) {
            return 0;
        }
        
        int streak = 0;
        
        // Start from the most recent entry and work backwards
        for (int i = sortedHistory.size() - 1; i >= 0; i--) {
            ReminderHistory history = sortedHistory.get(i);
            
            if (history.isCompleted()) {
                streak++;
            } else {
                // Streak is broken by a skipped action
                break;
            }
        }
        
        return streak;
    }
    
    /**
     * Calculate the best (longest) completion streak in history
     * 
     * @param sortedHistory History entries sorted by timestamp (oldest first)
     * @return Best streak count
     */
    private static int calculateBestStreak(List<ReminderHistory> sortedHistory) {
        if (sortedHistory.isEmpty()) {
            return 0;
        }
        
        int bestStreak = 0;
        int currentStreak = 0;
        
        for (ReminderHistory history : sortedHistory) {
            if (history.isCompleted()) {
                currentStreak++;
                bestStreak = Math.max(bestStreak, currentStreak);
            } else {
                currentStreak = 0; // Reset streak on skip
            }
        }
        
        return bestStreak;
    }
    
    /**
     * Calculate daily completion streak (consecutive days with at least one completion)
     * This is different from action streak - it counts consecutive days, not individual actions
     * 
     * @param sortedHistory History entries sorted by timestamp (oldest first)
     * @return Current daily streak count
     */
    public static int calculateDailyStreak(List<ReminderHistory> sortedHistory) {
        if (sortedHistory.isEmpty()) {
            return 0;
        }
        
        // Group completions by day
        List<Long> completionDays = new ArrayList<>();
        
        for (ReminderHistory history : sortedHistory) {
            if (history.isCompleted()) {
                long dayTimestamp = getDayTimestamp(history.getTimestamp());
                if (!completionDays.contains(dayTimestamp)) {
                    completionDays.add(dayTimestamp);
                }
            }
        }
        
        if (completionDays.isEmpty()) {
            return 0;
        }
        
        // Sort days
        Collections.sort(completionDays);
        
        // Calculate consecutive days from the end
        int streak = 1;
        long today = getDayTimestamp(System.currentTimeMillis());
        
        // Check if the most recent completion was today or yesterday
        long lastCompletionDay = completionDays.get(completionDays.size() - 1);
        if (lastCompletionDay < today - MILLIS_PER_DAY) {
            return 0; // Streak is broken if last completion was more than 1 day ago
        }
        
        // Count consecutive days backwards
        for (int i = completionDays.size() - 2; i >= 0; i--) {
            long currentDay = completionDays.get(i);
            long nextDay = completionDays.get(i + 1);
            
            if (nextDay - currentDay == MILLIS_PER_DAY) {
                streak++;
            } else {
                break; // Streak is broken
            }
        }
        
        return streak;
    }
    
    /**
     * Get the timestamp for the start of the day (midnight)
     * 
     * @param timestamp Original timestamp
     * @return Timestamp for start of day
     */
    private static long getDayTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    
    /**
     * Calculate completion rate for a specific time period
     * 
     * @param historyList All history entries
     * @param startTime Start of time period
     * @param endTime End of time period
     * @return Completion rate percentage for the period
     */
    public static float calculateCompletionRateForPeriod(List<ReminderHistory> historyList, 
                                                       long startTime, long endTime) {
        if (historyList == null || historyList.isEmpty()) {
            return 0f;
        }
        
        int totalActions = 0;
        int completedActions = 0;
        
        for (ReminderHistory history : historyList) {
            long timestamp = history.getTimestamp();
            if (timestamp >= startTime && timestamp <= endTime) {
                totalActions++;
                if (history.isCompleted()) {
                    completedActions++;
                }
            }
        }
        
        return totalActions > 0 ? (completedActions * 100f) / totalActions : 0f;
    }
    
    /**
     * Get formatted completion rate string
     * 
     * @param completionRate Completion rate percentage
     * @return Formatted string (e.g., "85%")
     */
    public static String formatCompletionRate(float completionRate) {
        return String.format("%.0f%%", completionRate);
    }
    
    /**
     * Get formatted streak string
     * 
     * @param streak Streak count
     * @return Formatted string (e.g., "7" for 7 days)
     */
    public static String formatStreak(int streak) {
        return String.valueOf(streak);
    }
    
    /**
     * Check if the reminder has good adherence (completion rate >= 80%)
     * 
     * @param completionRate Completion rate percentage
     * @return true if adherence is good, false otherwise
     */
    public static boolean hasGoodAdherence(float completionRate) {
        return completionRate >= 80f;
    }
    
    /**
     * Get adherence level description
     * 
     * @param completionRate Completion rate percentage
     * @return Adherence level string
     */
    public static String getAdherenceLevel(float completionRate) {
        if (completionRate >= 90f) {
            return "Xuất sắc";
        } else if (completionRate >= 80f) {
            return "Tốt";
        } else if (completionRate >= 60f) {
            return "Trung bình";
        } else {
            return "Cần cải thiện";
        }
    }
}