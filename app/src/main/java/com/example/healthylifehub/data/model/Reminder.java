package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {
	private String title;
	private String description;
	private String time;
	private boolean completed;

	public Reminder(String title, String time) {
		this.title = title;
		this.time = time;
	}
}
