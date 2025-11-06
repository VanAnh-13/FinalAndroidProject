package com.example.healthylifehub.data.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Medicine {
    
    public enum MedicineStatus {
        ACTIVE, STOPPED
    }

    private String id;
    private String name;
    private String dosage;
    private String frequency;
    private String instructions;
    private String startDate;
    private String endDate;
    private MedicineStatus status;

    public Medicine(String name, String dosage, String frequency, String instructions, String startDate, MedicineStatus status) {
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.instructions = instructions;
        this.startDate = startDate;
        this.status = status;
    }

    public boolean isActive() {
        return status == MedicineStatus.ACTIVE;
    }
}
