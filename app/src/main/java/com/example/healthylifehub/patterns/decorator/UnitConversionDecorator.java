package com.example.healthylifehub.patterns.decorator;

/**
 * Decorator Pattern - Concrete Decorator
 * Converts metric units (e.g., kg to lbs, cm to inches)
 */
public class UnitConversionDecorator extends HealthMetricDecorator {
    
    public enum ConversionType {
        KG_TO_LBS(2.20462, "lbs"),
        LBS_TO_KG(0.453592, "kg"),
        CM_TO_INCH(0.393701, "in"),
        INCH_TO_CM(2.54, "cm"),
        CELSIUS_TO_FAHRENHEIT(1.8, "°F"),  // Special: (C * 1.8) + 32
        FAHRENHEIT_TO_CELSIUS(0.5556, "°C"); // Special: (F - 32) * 0.5556

        private final double factor;
        private final String targetUnit;

        ConversionType(double factor, String targetUnit) {
            this.factor = factor;
            this.targetUnit = targetUnit;
        }

        public double getFactor() { return factor; }
        public String getTargetUnit() { return targetUnit; }
    }

    private final ConversionType conversionType;

    public UnitConversionDecorator(HealthMetricComponent component, ConversionType conversionType) {
        super(component);
        this.conversionType = conversionType;
    }

    @Override
    public double getValue() {
        double originalValue = wrappedComponent.getValue();
        
        // Special handling for temperature conversions
        if (conversionType == ConversionType.CELSIUS_TO_FAHRENHEIT) {
            return (originalValue * 1.8) + 32;
        } else if (conversionType == ConversionType.FAHRENHEIT_TO_CELSIUS) {
            return (originalValue - 32) * 0.5556;
        }
        
        return originalValue * conversionType.getFactor();
    }

    @Override
    public String getUnit() {
        return conversionType.getTargetUnit();
    }

    @Override
    public String getDisplayValue() {
        return String.format("%.1f %s", getValue(), getUnit());
    }
}
