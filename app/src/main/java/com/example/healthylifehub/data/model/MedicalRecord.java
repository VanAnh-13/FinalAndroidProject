package com.example.healthylifehub.data.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MedicalRecord {
    
    public enum RecordType {
        CARDIOLOGY, CHECKUP, ALLERGY, VACCINATION, OTHER
    }

    private String id;
    private String date;
    private String title;
    private String description;
    private String attachment;
    private RecordType type;

    public MedicalRecord(String date, String title, String description, String attachment, RecordType type) {
        this.date = date;
        this.title = title;
        this.description = description;
        this.attachment = attachment;
        this.type = type;
    }

    public boolean hasAttachment() {
        return attachment != null && !attachment.isEmpty();
    }
}
