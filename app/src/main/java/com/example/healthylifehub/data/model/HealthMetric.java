package com.example.healthylifehub.data.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetric {
    private String id;
    private String type; // blood_pressure, blood_sugar, heart_rate, weight, bmi
    private double value;
    private double systolic; // For blood pressure
    private double diastolic; // For blood pressure
    private Date measuredAt;
    private String notes;
    private boolean synced;

    public HealthMetric(String type, double value, Date measuredAt) {
        this.type = type;
        this.value = value;
        this.measuredAt = measuredAt;
        this.synced = false;
    }
}
