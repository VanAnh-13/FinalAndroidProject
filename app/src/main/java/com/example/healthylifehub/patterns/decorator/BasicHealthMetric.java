package com.example.healthylifehub.patterns.decorator;

/**
 * Decorator Pattern - Concrete Component
 * Basic health metric without any decorations
 */
public class BasicHealthMetric implements HealthMetricComponent {
    private final String type;
    private final double value;
    private final String unit;

    public BasicHealthMetric(String type, double value, String unit) {
        this.type = type;
        this.value = value;
        this.unit = unit;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public String getDisplayValue() {
        return String.format("%.1f %s", value, unit);
    }
}
