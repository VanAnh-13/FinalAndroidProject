package com.example.healthylifehub.patterns.decorator;

/**
 * Decorator Pattern - Concrete Decorator
 * Adds alert/warning indicator to abnormal metrics
 */
public class AlertDecorator extends HealthMetricDecorator {
    private final double minNormal;
    private final double maxNormal;

    public AlertDecorator(HealthMetricComponent component, double minNormal, double maxNormal) {
        super(component);
        this.minNormal = minNormal;
        this.maxNormal = maxNormal;
    }

    @Override
    public String getDisplayValue() {
        String baseValue = wrappedComponent.getDisplayValue();
        if (isAbnormal()) {
            return "⚠️ " + baseValue + " (Bất thường)";
        }
        return baseValue;
    }

    public boolean isAbnormal() {
        double value = wrappedComponent.getValue();
        return value < minNormal || value > maxNormal;
    }

    public String getAlertMessage() {
        double value = wrappedComponent.getValue();
        if (value < minNormal) {
            return String.format("%s thấp hơn mức bình thường (%.1f < %.1f)", 
                    wrappedComponent.getType(), value, minNormal);
        } else if (value > maxNormal) {
            return String.format("%s cao hơn mức bình thường (%.1f > %.1f)", 
                    wrappedComponent.getType(), value, maxNormal);
        }
        return null;
    }
}
