package com.example.healthylifehub.patterns.decorator;

/**
 * Decorator Pattern - Concrete Decorator
 * Adds trend indicator (up/down/stable) to metrics
 */
public class TrendDecorator extends HealthMetricDecorator {
    
    public enum Trend {
        UP("↑", "Tăng"),
        DOWN("↓", "Giảm"),
        STABLE("→", "Ổn định");

        private final String symbol;
        private final String description;

        Trend(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        public String getSymbol() { return symbol; }
        public String getDescription() { return description; }
    }

    private final Trend trend;
    private final double changePercent;

    public TrendDecorator(HealthMetricComponent component, Trend trend, double changePercent) {
        super(component);
        this.trend = trend;
        this.changePercent = changePercent;
    }

    @Override
    public String getDisplayValue() {
        String baseValue = wrappedComponent.getDisplayValue();
        return String.format("%s %s (%.1f%%)", baseValue, trend.getSymbol(), Math.abs(changePercent));
    }

    public Trend getTrend() {
        return trend;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public String getTrendDescription() {
        return String.format("%s %.1f%% so với lần đo trước", trend.getDescription(), Math.abs(changePercent));
    }
}
