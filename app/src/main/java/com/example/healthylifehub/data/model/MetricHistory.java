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
}
