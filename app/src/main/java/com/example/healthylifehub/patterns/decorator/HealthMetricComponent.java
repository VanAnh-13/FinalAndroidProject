package com.example.healthylifehub.patterns.decorator;

/**
 * Decorator Pattern - Component Interface
 * Base interface for health metrics that can be decorated
 */
public interface HealthMetricComponent {
    String getType();
    double getValue();
    String getUnit();
    String getDisplayValue();
}
