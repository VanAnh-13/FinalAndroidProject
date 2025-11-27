package com.example.healthylifehub.patterns.builder;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.model.Reminder;

import java.util.Collections;
import java.util.List;

/**
 * Builder Pattern - Report (Product)
 * Immutable report object created by ReportBuilder
 */
public class Report {
    private final String title;
    private final String userId;
    private final long startDate;
    private final long endDate;
    private final boolean includeHealthMetrics;
    private final boolean includeMedicalRecords;
    private final boolean includeReminders;
    private final boolean includeCharts;
    private final boolean includeSummary;
    private final List<HealthMetric> healthMetrics;
    private final List<MedicalRecord> medicalRecords;
    private final List<Reminder> reminders;
    private final String format;

    Report(ReportBuilder builder) {
        this.title = builder.getTitle();
        this.userId = builder.getUserId();
        this.startDate = builder.getStartDate();
        this.endDate = builder.getEndDate();
        this.includeHealthMetrics = builder.isIncludeHealthMetrics();
        this.includeMedicalRecords = builder.isIncludeMedicalRecords();
        this.includeReminders = builder.isIncludeReminders();
        this.includeCharts = builder.isIncludeCharts();
        this.includeSummary = builder.isIncludeSummary();
        this.healthMetrics = Collections.unmodifiableList(builder.getHealthMetrics());
        this.medicalRecords = Collections.unmodifiableList(builder.getMedicalRecords());
        this.reminders = Collections.unmodifiableList(builder.getReminders());
        this.format = builder.getFormat();
    }

    // Getters only (immutable)
    public String getTitle() { return title; }
    public String getUserId() { return userId; }
    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
    public boolean isIncludeHealthMetrics() { return includeHealthMetrics; }
    public boolean isIncludeMedicalRecords() { return includeMedicalRecords; }
    public boolean isIncludeReminders() { return includeReminders; }
    public boolean isIncludeCharts() { return includeCharts; }
    public boolean isIncludeSummary() { return includeSummary; }
    public List<HealthMetric> getHealthMetrics() { return healthMetrics; }
    public List<MedicalRecord> getMedicalRecords() { return medicalRecords; }
    public List<Reminder> getReminders() { return reminders; }
    public String getFormat() { return format; }
}
