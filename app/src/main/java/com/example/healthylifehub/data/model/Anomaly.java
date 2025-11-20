package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Anomaly - Represents an anomalous health metric value
 * Detected when value exceeds expected range (e.g., >2 standard deviations)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {
    private String metricId;
    private double value;
    private double deviation; // How many standard deviations from mean
    private String severity; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String message;
    private long detectedAt;
    
    /**
     * Constructor with HealthMetric and message
     */
    public Anomaly(HealthMetric metric, String message) {
        this.metricId = metric.getId();
        this.value = metric.getValue();
        this.message = message;
        this.detectedAt = System.currentTimeMillis();
    }
    
    /**
     * Determine severity based on deviation
     */
    public static String determineSeverity(double deviation) {
        double absDeviation = Math.abs(deviation);
        
        if (absDeviation >= 3.0) {
            return "CRITICAL";
        } else if (absDeviation >= 2.5) {
            return "HIGH";
        } else if (absDeviation >= 2.0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
