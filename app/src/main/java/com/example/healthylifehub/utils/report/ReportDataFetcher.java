package com.example.healthylifehub.utils.report;

import android.content.Context;
import android.util.Log;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fetcher for report data from Firestore
 * Falls back to local database if Firestore fails
 */
public class ReportDataFetcher {
    
    private static final String TAG = "ReportDataFetcher";
    private static final String COLLECTION_HEALTH_METRICS = "healthMetrics";
    
    private final AppDatabase database;
    private final FirebaseFirestore firestore;
    
    public ReportDataFetcher(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Fetch health metrics from Firestore
     * Falls back to local database if Firestore fails
     */
    public CompletableFuture<List<HealthMetric>> fetchHealthMetrics(String userId, Date startDate, Date endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "📊 Fetching health metrics from Firestore for user: " + userId);
                Log.d(TAG, "   Date range: " + startDate + " to " + endDate);
                
                // Fetch from Firestore
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = 
                    firestore.collection("users")
                        .document(userId)
                        .collection(COLLECTION_HEALTH_METRICS)
                        .get();
                
                com.google.firebase.firestore.QuerySnapshot querySnapshot = Tasks.await(task);
                
                List<HealthMetric> metrics = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    try {
                        HealthMetric metric = parseHealthMetric(doc, userId);
                        if (metric != null && metric.getMeasuredAt() != null) {
                            long time = metric.getMeasuredAt().getTime();
                            if (time >= startDate.getTime() && time <= endDate.getTime()) {
                                metrics.add(metric);
                                Log.d(TAG, "   ✅ Added metric: " + metric.getType() + " - " + metric.getDisplayValue());
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "   ⚠️ Skipping invalid metric: " + e.getMessage());
                    }
                }
                
                Log.d(TAG, "✅ Fetched " + metrics.size() + " metrics from Firestore");
                
                // If no data from Firestore, try local database
                if (metrics.isEmpty()) {
                    Log.d(TAG, "⚠️ No metrics from Firestore, trying local database...");
                    return fetchFromLocalDatabase(userId, startDate, endDate);
                }
                
                return metrics;
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error fetching from Firestore, falling back to local: " + e.getMessage());
                return fetchFromLocalDatabase(userId, startDate, endDate);
            }
        });
    }
    
    /**
     * Parse a Firestore document to HealthMetric
     */
    private HealthMetric parseHealthMetric(QueryDocumentSnapshot doc, String userId) {
        String id = doc.getId();
        String type = doc.getString("type");
        String unit = doc.getString("unit");
        String note = doc.getString("note");
        if (note == null) {
            note = doc.getString("notes"); // Fallback
        }
        
        com.google.firebase.Timestamp timestamp = doc.getTimestamp("measuredAt");
        Date measuredAt = timestamp != null ? timestamp.toDate() : new Date();
        
        // Parse value based on type
        Double numericValue = 0.0;
        Long systolic = null;
        Long diastolic = null;
        
        if ("blood_pressure".equals(type)) {
            // Check for root level systolic/diastolic
            systolic = doc.getLong("systolic");
            diastolic = doc.getLong("diastolic");
            
            // If not at root, check in value map
            if (systolic == null || diastolic == null) {
                Object valueObj = doc.get("value");
                if (valueObj instanceof Map) {
                    Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                    Object sysObj = valueMap.get("systolic");
                    Object diaObj = valueMap.get("diastolic");
                    if (sysObj instanceof Number) systolic = ((Number) sysObj).longValue();
                    if (diaObj instanceof Number) diastolic = ((Number) diaObj).longValue();
                }
            }
        } else {
            // Other types: value is a number
            Object valueObj = doc.get("value");
            if (valueObj instanceof Number) {
                numericValue = ((Number) valueObj).doubleValue();
            } else if (valueObj instanceof Map) {
                // Some types might have nested value
                Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                Object val = valueMap.get("value");
                if (val instanceof Number) {
                    numericValue = ((Number) val).doubleValue();
                }
            }
        }
        
        // Create HealthMetric
        HealthMetric metric = new HealthMetric();
        metric.setId(id);
        metric.setUserId(userId);
        metric.setType(type);
        metric.setUnit(unit != null ? unit : getDefaultUnit(type));
        metric.setNotes(note);
        metric.setMeasuredAt(measuredAt);
        
        if ("blood_pressure".equals(type) && systolic != null && diastolic != null) {
            metric.setSystolic(systolic.intValue());
            metric.setDiastolic(diastolic.intValue());
            metric.setValue(0.0); // Not used for blood pressure
        } else {
            metric.setValue(numericValue);
        }
        
        return metric;
    }
    
    /**
     * Get default unit for metric type
     */
    private String getDefaultUnit(String type) {
        if (type == null) return "";
        switch (type) {
            case "blood_pressure": return "mmHg";
            case "heart_rate": return "bpm";
            case "blood_sugar": return "mg/dL";
            case "weight": return "kg";
            case "temperature": return "°C";
            default: return "";
        }
    }
    
    /**
     * Fallback: Fetch from local Room database
     */
    private List<HealthMetric> fetchFromLocalDatabase(String userId, Date startDate, Date endDate) {
        try {
            List<HealthMetric> metrics = database.healthMetricDao().getAllMetrics();
            
            List<HealthMetric> filtered = new ArrayList<>();
            for (HealthMetric metric : metrics) {
                if (metric.getUserId() != null && metric.getUserId().equals(userId)) {
                    if (metric.getMeasuredAt() != null) {
                        long time = metric.getMeasuredAt().getTime();
                        if (time >= startDate.getTime() && time <= endDate.getTime()) {
                            filtered.add(metric);
                        }
                    }
                }
            }
            
            Log.d(TAG, "📱 Fetched " + filtered.size() + " metrics from local database");
            return filtered;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error fetching from local database: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetch reminders from local database
     */
    public CompletableFuture<List<Reminder>> fetchReminders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Fetch from Room database
                List<Reminder> reminders = database.reminderDao().getAllRemindersSync(userId);
                
                // Filter by userId if needed
                List<Reminder> filtered = new ArrayList<>();
                for (Reminder reminder : reminders) {
                    if (reminder.getUserId() != null && reminder.getUserId().equals(userId)) {
                        filtered.add(reminder);
                    }
                }
                
                return filtered.isEmpty() ? reminders : filtered;
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Fetch all health metrics (no date filter)
     */
    public CompletableFuture<List<HealthMetric>> fetchAllHealthMetrics(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "📊 Fetching ALL health metrics from Firestore for user: " + userId);
                
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = 
                    firestore.collection("users")
                        .document(userId)
                        .collection(COLLECTION_HEALTH_METRICS)
                        .get();
                
                com.google.firebase.firestore.QuerySnapshot querySnapshot = Tasks.await(task);
                
                List<HealthMetric> metrics = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    try {
                        HealthMetric metric = parseHealthMetric(doc, userId);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "   ⚠️ Skipping invalid metric: " + e.getMessage());
                    }
                }
                
                Log.d(TAG, "✅ Fetched " + metrics.size() + " total metrics from Firestore");
                return metrics;
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error fetching from Firestore: " + e.getMessage());
                
                // Fallback to local
                List<HealthMetric> metrics = database.healthMetricDao().getAllMetrics();
                List<HealthMetric> filtered = new ArrayList<>();
                for (HealthMetric metric : metrics) {
                    if (metric.getUserId() != null && metric.getUserId().equals(userId)) {
                        filtered.add(metric);
                    }
                }
                return filtered.isEmpty() ? metrics : filtered;
            }
        });
    }
}
