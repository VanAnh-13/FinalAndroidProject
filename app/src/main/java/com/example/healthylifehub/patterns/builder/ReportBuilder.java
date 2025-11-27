package com.example.healthylifehub.patterns.builder;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.model.Reminder;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder Pattern - Report Builder
 * Fluent API for constructing complex Report objects
 * 
 * Usage:
 * Report report = new ReportBuilder()
 *     .setTitle("Báo cáo sức khỏe")
 *     .setUserId(userId)
 *     .setDateRange(startDate, endDate)
 *     .includeHealthMetrics(true)
 *     .includeMedicalRecords(true)
 *     .includeReminders(false)
 *     .addHealthMetrics(metricsList)
 *     .build();
 */
public class ReportBuilder {
    private String title;
    private String userId;
    private long startDate;
    private long endDate;
    private boolean includeHealthMetrics = true;
    private boolean includeMedicalRecords = true;
    private boolean includeReminders = false;
    private boolean includeCharts = false;
    private boolean includeSummary = true;
    private List<HealthMetric> healthMetrics = new ArrayList<>();
    private List<MedicalRecord> medicalRecords = new ArrayList<>();
    private List<Reminder> reminders = new ArrayList<>();
    private String format = "PDF"; // PDF, EXCEL, CSV

    public ReportBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public ReportBuilder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public ReportBuilder setDateRange(long startDate, long endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    public ReportBuilder setStartDate(long startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReportBuilder setEndDate(long endDate) {
        this.endDate = endDate;
        return this;
    }

    public ReportBuilder includeHealthMetrics(boolean include) {
        this.includeHealthMetrics = include;
        return this;
    }

    public ReportBuilder includeMedicalRecords(boolean include) {
        this.includeMedicalRecords = include;
        return this;
    }

    public ReportBuilder includeReminders(boolean include) {
        this.includeReminders = include;
        return this;
    }

    public ReportBuilder includeCharts(boolean include) {
        this.includeCharts = include;
        return this;
    }

    public ReportBuilder includeSummary(boolean include) {
        this.includeSummary = include;
        return this;
    }

    public ReportBuilder addHealthMetrics(List<HealthMetric> metrics) {
        if (metrics != null) {
            this.healthMetrics.addAll(metrics);
        }
        return this;
    }

    public ReportBuilder addMedicalRecords(List<MedicalRecord> records) {
        if (records != null) {
            this.medicalRecords.addAll(records);
        }
        return this;
    }

    public ReportBuilder addReminders(List<Reminder> reminders) {
        if (reminders != null) {
            this.reminders.addAll(reminders);
        }
        return this;
    }

    public ReportBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public Report build() {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("UserId is required");
        }
        if (title == null || title.isEmpty()) {
            title = "Báo cáo sức khỏe";
        }
        if (endDate == 0) {
            endDate = System.currentTimeMillis();
        }
        if (startDate == 0) {
            // Default to 30 days ago
            startDate = endDate - (30L * 24 * 60 * 60 * 1000);
        }

        return new Report(this);
    }

    // Getters for Report class
    String getTitle() { return title; }
    String getUserId() { return userId; }
    long getStartDate() { return startDate; }
    long getEndDate() { return endDate; }
    boolean isIncludeHealthMetrics() { return includeHealthMetrics; }
    boolean isIncludeMedicalRecords() { return includeMedicalRecords; }
    boolean isIncludeReminders() { return includeReminders; }
    boolean isIncludeCharts() { return includeCharts; }
    boolean isIncludeSummary() { return includeSummary; }
    List<HealthMetric> getHealthMetrics() { return healthMetrics; }
    List<MedicalRecord> getMedicalRecords() { return medicalRecords; }
    List<Reminder> getReminders() { return reminders; }
    String getFormat() { return format; }
}
