package com.example.healthylifehub.ui.metrics;

public class MetricHistory {
    private String value;
    private String date;
    private String unit;

    public MetricHistory(String value, String date, String unit) {
        this.value = value;
        this.date = date;
        this.unit = unit;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDisplayValue() {
        return value + " " + unit;
    }
}
