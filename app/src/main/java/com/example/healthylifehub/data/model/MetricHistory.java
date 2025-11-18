package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricHistory {
	private String value;
	private String date;
	private String unit;

	public String getDisplayValue() { return value + " " + unit; }
	
	public String getFormattedDate() { 
		return date != null ? date : ""; 
	}
	
	public double getValueAsDouble() {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	/**
	 * Get systolic value (for blood pressure)
	 * Blood pressure format: "systolic/diastolic" e.g. "120/80"
	 * Uses improved parsing to handle edge cases
	 */
	public double getSystolic() {
		return com.example.healthylifehub.utils.chart.ChartDataProcessor.parseSystolic(value);
	}
	
	/**
	 * Get diastolic value (for blood pressure)
	 * Blood pressure format: "systolic/diastolic" e.g. "120/80"
	 * Uses improved parsing to handle edge cases
	 */
	public double getDiastolic() {
		return com.example.healthylifehub.utils.chart.ChartDataProcessor.parseDiastolic(value);
	}
}
