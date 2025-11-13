package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.healthylifehub.data.local.converter.DateConverter;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HealthMetric - Vừa là Model vừa là Room Entity
 * Dùng chung cho cả UI và Database để đơn giản
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(
    tableName = "health_metrics",
    indices = {@Index(value = {"userId", "type", "measuredAt"})}
)
@TypeConverters(DateConverter.class)
public class HealthMetric {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    
    private String userId; // Foreign key
    private String type; // blood_pressure, blood_sugar, heart_rate, weight, bmi
    private double value;
    private double systolic; // For blood pressure
    private double diastolic; // For blood pressure
    private Date measuredAt;
    private String notes;
    private boolean synced;
    
    // Sync metadata
    private Date lastSyncedAt;
    private boolean needsSync;

    @Ignore
    public HealthMetric(String type, double value, Date measuredAt) {
        this.type = type;
        this.value = value;
        this.measuredAt = measuredAt;
        this.synced = false;
        this.needsSync = true;
        this.lastSyncedAt = new Date();
    }
    
    /**
     * Get value as double for analytics
     * @param fieldName "value", "systolic", "diastolic"
     * @param defaultValue Default if field not available
     */
    @Ignore
    public double getValueAsDouble(String fieldName, double defaultValue) {
        switch (fieldName) {
            case "systolic":
                return systolic > 0 ? systolic : defaultValue;
            case "diastolic":
                return diastolic > 0 ? diastolic : defaultValue;
            case "value":
            default:
                return value > 0 ? value : defaultValue;
        }
    }
}
