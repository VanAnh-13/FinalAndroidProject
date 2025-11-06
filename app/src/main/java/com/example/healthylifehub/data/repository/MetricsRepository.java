package com.example.healthylifehub.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.MetricHistory;
import com.example.healthylifehub.data.model.MetricItem;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for Metrics data from Firebase Firestore.
 * Collection structure: metrics/{metricId}
 * Data format matches the provided JSON structure with value object containing specific metric data
 */
public class MetricsRepository extends FirebaseRepository {
    
    private static final String COLLECTION_HEALTH_METRICS = "healthMetrics";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault());
    
    /**
     * Load all metrics for current user
     * @return LiveData list of metric items
     */
    public LiveData<List<MetricItem>> loadMetrics() {
        MutableLiveData<List<MetricItem>> metricsLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            metricsLiveData.setValue(new ArrayList<>());
            return metricsLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    metricsLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<MetricItem> metrics = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            String type = doc.getString("type");
                            String unit = doc.getString("unit");
                            String note = doc.getString("note");
                            com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                            Map<String, Object> valueMap = (Map<String, Object>) doc.get("value");
                            
                            if (type != null && valueMap != null && measuredAt != null) {
                                String displayValue = formatMetricValue(type, valueMap, unit);
                                String displayTitle = getMetricTitle(type);
                                String displayTime = dateFormat.format(measuredAt.toDate());
                                
                                metrics.add(new MetricItem(displayValue, note, displayTitle, displayTime, type));
                            }
                        } catch (Exception e) {
                            // Skip invalid records
                        }
                    }
                    metricsLiveData.setValue(metrics);
                }
            });
        
        return metricsLiveData;
    }
    
    /**
     * Load metric history for a specific metric type
     * @param metricType Type of metric (e.g., "blood_pressure", "heart_rate")
     * @return LiveData list of metric history
     */
    public LiveData<List<MetricHistory>> loadMetricHistory(String metricType) {
        MutableLiveData<List<MetricHistory>> historyLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            historyLiveData.setValue(new ArrayList<>());
            return historyLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_HEALTH_METRICS)
            .whereEqualTo("type", metricType)
            .orderBy("measuredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    historyLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<MetricHistory> history = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            String unit = doc.getString("unit");
                            com.google.firebase.Timestamp measuredAt = doc.getTimestamp("measuredAt");
                            Map<String, Object> valueMap = (Map<String, Object>) doc.get("value");
                            
                            if (valueMap != null && measuredAt != null) {
                                String displayValue = formatMetricValue(metricType, valueMap, unit);
                                String displayTime = formatHistoryTime(measuredAt.toDate());
                                
                                history.add(new MetricHistory(displayValue, displayTime, unit));
                            }
                        } catch (Exception e) {
                            // Skip invalid records
                        }
                    }
                    historyLiveData.setValue(history);
                }
            });
        
        return historyLiveData;
    }
    
    /**
     * Format metric value based on type and value map
     */
    private String formatMetricValue(String type, Map<String, Object> valueMap, String unit) {
        switch (type) {
            case "blood_pressure":
                Object systolic = valueMap.get("systolic");
                Object diastolic = valueMap.get("diastolic");
                if (systolic != null && diastolic != null) {
                    return systolic + "/" + diastolic + " " + (unit != null ? unit : "mmHg");
                }
                break;
            case "heart_rate":
                Object bpm = valueMap.get("bpm");
                if (bpm != null) {
                    return bpm + " " + (unit != null ? unit : "bpm");
                }
                break;
            case "blood_sugar":
                Object level = valueMap.get("level");
                if (level != null) {
                    return level + " " + (unit != null ? unit : "mg/dL");
                }
                break;
            case "weight":
                Object weight = valueMap.get("weight");
                if (weight != null) {
                    return weight + " " + (unit != null ? unit : "kg");
                }
                break;
            case "temperature":
                Object celsius = valueMap.get("celsius");
                if (celsius != null) {
                    return celsius + " " + (unit != null ? unit : "°C");
                }
                break;
        }
        return "N/A";
    }
    
    /**
     * Get display title for metric type
     */
    private String getMetricTitle(String type) {
        switch (type) {
            case "blood_pressure": return "Huyết áp";
            case "heart_rate": return "Nhịp tim";
            case "blood_sugar": return "Đường huyết";
            case "weight": return "Cân nặng";
            case "temperature": return "Nhiệt độ";
            default: return type;
        }
    }
    
    /**
     * Format history time from Date
     */
    private String formatHistoryTime(Date date) {
        SimpleDateFormat historyFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
        return historyFormat.format(date);
    }
}

