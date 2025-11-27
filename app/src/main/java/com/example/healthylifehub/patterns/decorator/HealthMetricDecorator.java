package com.example.healthylifehub.patterns.decorator;

/**
 * Decorator Pattern - Base Decorator
 * Wraps HealthMetricComponent to add additional behavior
 */
public abstract class HealthMetricDecorator implements HealthMetricComponent {
    protected HealthMetricComponent wrappedComponent;

    public HealthMetricDecorator(HealthMetricComponent component) {
        this.wrappedComponent = component;
    }

    @Override
    public String getType() {
        return wrappedComponent.getType();
    }

    @Override
    public double getValue() {
        return wrappedComponent.getValue();
    }

    @Override
    public String getUnit() {
        return wrappedComponent.getUnit();
    }

    @Override
    public String getDisplayValue() {
        return wrappedComponent.getDisplayValue();
    }
}
