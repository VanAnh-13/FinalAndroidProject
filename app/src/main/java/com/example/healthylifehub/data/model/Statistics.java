package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Statistics - Statistical analysis of health metrics
 * Contains mean, standard deviation, min, max, median, and trend
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Statistics {
    private double mean;
    private double stdDev;
    private double min;
    private double max;
    private double median;
    private String trend; // "INCREASING", "DECREASING", "STABLE"
    
    /**
     * Constructor with essential fields
     */
    public Statistics(double mean, double stdDev, String trend) {
        this.mean = mean;
        this.stdDev = stdDev;
        this.trend = trend;
    }
}
