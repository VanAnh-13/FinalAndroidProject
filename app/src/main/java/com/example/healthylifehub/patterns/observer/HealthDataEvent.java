package com.example.healthylifehub.patterns.observer;

/**
 * Observer Pattern - Event class
 * Contains information about health data changes
 */
public class HealthDataEvent {
    
    public enum EventType {
        METRIC_ADDED,
        METRIC_UPDATED,
        METRIC_DELETED,
        RECORD_ADDED,
        RECORD_UPDATED,
        RECORD_DELETED,
        REMINDER_TRIGGERED,
        SYNC_COMPLETED,
        ANOMALY_DETECTED
    }

    private final EventType type;
    private final String entityId;
    private final String entityType;
    private final Object data;
    private final long timestamp;

    public HealthDataEvent(EventType type, String entityId, String entityType, Object data) {
        this.type = type;
        this.entityId = entityId;
        this.entityType = entityType;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public EventType getType() { return type; }
    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public Object getData() { return data; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "HealthDataEvent{" +
                "type=" + type +
                ", entityId='" + entityId + '\'' +
                ", entityType='" + entityType + '\'' +
                '}';
    }
}
