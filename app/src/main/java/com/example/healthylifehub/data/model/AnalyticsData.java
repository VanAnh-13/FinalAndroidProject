package com.example.healthylifehub.data.model;

import java.util.List;

/**
 * AnalyticsData - Aggregated health metrics statistics
 * Contains calculated statistics for all health metric types
 */
public class AnalyticsData {
    
    /**
     * Statistics for a single metric type
     */
    public static class MetricStatistics {
        public String metricType; // "blood_pressure", "blood_sugar", "weight", "heart_rate"
        public List<DataPoint> dataPoints; // Raw data points for chart
        
        // Calculated statistics
        public double average;
        public double minimum;
        public double maximum;
        public double trend; // Positive = increasing, Negative = decreasing
        public String unit;
        
        // For blood pressure (systolic/diastolic)
        public double avgSystolic;
        public double avgDiastolic;
        public double minSystolic;
        public double maxSystolic;
        
        public MetricStatistics(String metricType) {
            this.metricType = metricType;
        }
    }
    
    /**
     * Single data point for chart
     */
    public static class DataPoint {
        public long timestamp; // milliseconds
        public double value; // For simple metrics
        public double systolic; // For blood pressure
        public double diastolic; // For blood pressure
        public String label; // For display (e.g., "Oct 27")
        
        public DataPoint(long timestamp, double value, String label) {
            this.timestamp = timestamp;
            this.value = value;
            this.label = label;
        }
        
        public DataPoint(long timestamp, double systolic, double diastolic, String label) {
            this.timestamp = timestamp;
            this.systolic = systolic;
            this.diastolic = diastolic;
            this.label = label;
        }
    }
    
    // Statistics for each metric type
    public MetricStatistics bloodPressure;
    public MetricStatistics bloodSugar;
    public MetricStatistics weight;
    public MetricStatistics heartRate;
    
    // Time range info
    public long startDate; // milliseconds
    public long endDate; // milliseconds
    public int dayRange; // 7, 30, 90 days
    
    // Metadata
    public long generatedAt; // When this data was generated
    public boolean isCached; // Whether this data came from cache
    
    public AnalyticsData(int dayRange) {
        this.dayRange = dayRange;
        this.generatedAt = System.currentTimeMillis();
        this.isCached = false;
        
        // Initialize metric statistics
        this.bloodPressure = new MetricStatistics("blood_pressure");
        this.bloodSugar = new MetricStatistics("blood_sugar");
        this.weight = new MetricStatistics("weight");
        this.heartRate = new MetricStatistics("heart_rate");
    }
    
    /**
     * Check if data is stale (older than 5 minutes)
     */
    public boolean isStale() {
        long ageMs = System.currentTimeMillis() - generatedAt;
        return ageMs > 5 * 60 * 1000; // 5 minutes
    }
    
    /**
     * Get age of data in seconds
     */
    public long getAgeSeconds() {
        return (System.currentTimeMillis() - generatedAt) / 1000;
    }
}
