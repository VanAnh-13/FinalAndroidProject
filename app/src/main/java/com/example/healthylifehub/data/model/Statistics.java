package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Statistics {
    private double mean;
    private double stdDev;
    private double min;
    private double max;
    private double median;
    private String trend; // INCREASING, DECREASING, STABLE
}
