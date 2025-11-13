package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "medical_records")
public class MedicalRecord {
    
    public enum RecordType {
        CARDIOLOGY, CHECKUP, ALLERGY, VACCINATION, OTHER
    }

    @PrimaryKey
    @NonNull
    private String id;
    private String date;
    private String title;
    private String hospital;
    private String doctor;
    private String diagnosis;
    private String description;
    private String attachment;
    private RecordType type;
    @Ignore
    private Long createdAt;
    @Ignore
    private Long updatedAt;

    public MedicalRecord(String date, String title, String hospital, String doctor, String diagnosis, String description, String attachment, RecordType type) {
        this.date = date;
        this.title = title;
        this.hospital = hospital;
        this.doctor = doctor;
        this.diagnosis = diagnosis;
        this.description = description;
        this.attachment = attachment;
        this.type = type;
    }

    public MedicalRecord(String date, String title, String description, String attachment, RecordType type) {
        this(date, title, null, null, null, description, attachment, type);
    }

    public boolean hasAttachment() {
        return attachment != null && !attachment.isEmpty();
    }
}
