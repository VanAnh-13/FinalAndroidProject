package com.example.healthylifehub.patterns.strategy;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.model.Reminder;

import java.util.ArrayList;
import java.util.List;

/**
 * Data container for export operations
 */
public class ExportData {
    private String title;
    private String userId;
    private long startDate;
    private long endDate;
    private List<HealthMetric> healthMetrics;
    private List<MedicalRecord> medicalRecords;
    private List<Reminder> reminders;

    public ExportData() {
        this.healthMetrics = new ArrayList<>();
        this.medicalRecords = new ArrayList<>();
        this.reminders = new ArrayList<>();
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public List<HealthMetric> getHealthMetrics() { return healthMetrics; }
    public void setHealthMetrics(List<HealthMetric> healthMetrics) { this.healthMetrics = healthMetrics; }

    public List<MedicalRecord> getMedicalRecords() { return medicalRecords; }
    public void setMedicalRecords(List<MedicalRecord> medicalRecords) { this.medicalRecords = medicalRecords; }

    public List<Reminder> getReminders() { return reminders; }
    public void setReminders(List<Reminder> reminders) { this.reminders = reminders; }
}
