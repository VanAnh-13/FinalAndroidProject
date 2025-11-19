package com.example.healthylifehub.data.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AnalysisResult - Contains the complete analysis of health metrics
 * Used by HealthMetricsAnalyzer to return analysis results
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private Statistics statistics;
    private List<Anomaly> anomalies;
    private List<Recommendation> recommendations;
    private long analyzedAt;
    
    /**
     * Check if analysis has any anomalies
     */
    public boolean hasAnomalies() {
        return anomalies != null && !anomalies.isEmpty();
    }
    
    /**
     * Check if analysis has high or critical anomalies
     */
    public boolean hasHighSeverityAnomalies() {
        if (anomalies == null) return false;
        
        for (Anomaly anomaly : anomalies) {
            String severity = anomaly.getSeverity();
            if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get count of anomalies
     */
    public int getAnomalyCount() {
        return anomalies != null ? anomalies.size() : 0;
    }

    /**
     * Check if analysis has recommendations
     */
    public boolean hasRecommendations() {
        return recommendations != null && !recommendations.isEmpty();
    }
}
