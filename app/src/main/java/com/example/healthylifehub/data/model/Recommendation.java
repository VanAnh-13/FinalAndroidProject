package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recommendation - Health recommendation based on analysis
 * Generated from statistics and anomaly patterns
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    private String type; // "LIFESTYLE", "MEDICAL", "MONITORING"
    private String title;
    private String description;
    private String priority; // "LOW", "MEDIUM", "HIGH"
    
    /**
     * Create a lifestyle recommendation
     */
    public static Recommendation lifestyle(String title, String description, String priority) {
        return new Recommendation("LIFESTYLE", title, description, priority);
    }
    
    /**
     * Create a medical recommendation
     */
    public static Recommendation medical(String title, String description, String priority) {
        return new Recommendation("MEDICAL", title, description, priority);
    }
    
    /**
     * Create a monitoring recommendation
     */
    public static Recommendation monitoring(String title, String description, String priority) {
        return new Recommendation("MONITORING", title, description, priority);
    }
}
