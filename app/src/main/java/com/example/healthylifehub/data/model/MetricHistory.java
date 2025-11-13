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
	 */
	public double getSystolic() {
		try {
			if (value.contains("/")) {
				String[] parts = value.split("/");
				return Double.parseDouble(parts[0].trim());
			}
			return getValueAsDouble();
		} catch (Exception e) {
			return 0.0;
		}
	}
	
	/**
	 * Get diastolic value (for blood pressure)
	 * Blood pressure format: "systolic/diastolic" e.g. "120/80"
	 */
	public double getDiastolic() {
		try {
			if (value.contains("/")) {
				String[] parts = value.split("/");
				return Double.parseDouble(parts[1].trim());
			}
			return 0.0;
		} catch (Exception e) {
			return 0.0;
		}
	}
}
