package com.example.healthylifehub.utils.report;

import android.content.Context;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fetcher for report data from local database
 * Avoids Firestore deserialization issues
 */
public class ReportDataFetcher {
    
    private final AppDatabase database;
    
    public ReportDataFetcher(Context context) {
        this.database = AppDatabase.getInstance(context);
    }
    
    /**
     * Fetch health metrics from local database
     */
    public CompletableFuture<List<HealthMetric>> fetchHealthMetrics(String userId, Date startDate, Date endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Fetch all metrics from Room database
                List<HealthMetric> metrics = database.healthMetricDao().getAllMetrics();
                
                // Filter by userId and date range
                List<HealthMetric> filtered = new ArrayList<>();
                for (HealthMetric metric : metrics) {
                    if (metric.getUserId() != null && metric.getUserId().equals(userId)) {
                        if (metric.getMeasuredAt() != null) {
                            long time = metric.getMeasuredAt().getTime();
                            if (time >= startDate.getTime() && time <= endDate.getTime()) {
                                filtered.add(metric);
                            }
                        }
                    }
                }
                
                return filtered;
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Fetch reminders from local database
     */
    public CompletableFuture<List<Reminder>> fetchReminders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Fetch from Room database
                List<Reminder> reminders = database.reminderDao().getAllRemindersSync(userId);
                
                // Filter by userId if needed
                List<Reminder> filtered = new ArrayList<>();
                for (Reminder reminder : reminders) {
                    if (reminder.getUserId() != null && reminder.getUserId().equals(userId)) {
                        filtered.add(reminder);
                    }
                }
                
                return filtered.isEmpty() ? reminders : filtered;
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Fetch all health metrics (no date filter)
     */
    public CompletableFuture<List<HealthMetric>> fetchAllHealthMetrics(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<HealthMetric> metrics = database.healthMetricDao().getAllMetrics();
                
                // Filter by userId if needed
                List<HealthMetric> filtered = new ArrayList<>();
                for (HealthMetric metric : metrics) {
                    if (metric.getUserId() != null && metric.getUserId().equals(userId)) {
                        filtered.add(metric);
                    }
                }
                
                return filtered.isEmpty() ? metrics : filtered;
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
}
