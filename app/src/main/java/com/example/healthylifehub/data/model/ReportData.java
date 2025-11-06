package com.example.healthylifehub.data.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {
    private List<HealthMetric> metrics;
    private Statistics statistics;
    private List<MedicalRecord> medicalRecords;
    private List<Medicine> medicines;
    private UserProfile userProfile;

    public ReportData(List<HealthMetric> metrics, Statistics statistics) {
        this.metrics = metrics;
        this.statistics = statistics;
    }
}
