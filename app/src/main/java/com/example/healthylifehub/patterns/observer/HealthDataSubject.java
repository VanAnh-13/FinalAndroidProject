package com.example.healthylifehub.patterns.observer;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern - Subject (Observable)
 * Manages observers and notifies them of health data changes
 */
public class HealthDataSubject {
    private static final String TAG = "HealthDataSubject";
    private static HealthDataSubject instance;
    private final List<HealthDataObserver> observers;

    private HealthDataSubject() {
        observers = new CopyOnWriteArrayList<>();
    }

    public static synchronized HealthDataSubject getInstance() {
        if (instance == null) {
            instance = new HealthDataSubject();
        }
        return instance;
    }

    public void addObserver(HealthDataObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            Log.d(TAG, "Observer added. Total: " + observers.size());
        }
    }

    public void removeObserver(HealthDataObserver observer) {
        observers.remove(observer);
        Log.d(TAG, "Observer removed. Total: " + observers.size());
    }

    public void notifyObservers(HealthDataEvent event) {
        Log.d(TAG, "Notifying " + observers.size() + " observers: " + event);
        for (HealthDataObserver observer : observers) {
            try {
                observer.onHealthDataChanged(event);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying observer", e);
            }
        }
    }

    // Convenience methods for common events
    public void notifyMetricAdded(String metricId, Object metric) {
        notifyObservers(new HealthDataEvent(
                HealthDataEvent.EventType.METRIC_ADDED, metricId, "HealthMetric", metric));
    }

    public void notifyMetricUpdated(String metricId, Object metric) {
        notifyObservers(new HealthDataEvent(
                HealthDataEvent.EventType.METRIC_UPDATED, metricId, "HealthMetric", metric));
    }

    public void notifyMetricDeleted(String metricId) {
        notifyObservers(new HealthDataEvent(
                HealthDataEvent.EventType.METRIC_DELETED, metricId, "HealthMetric", null));
    }

    public void notifyAnomalyDetected(String metricId, Object anomalyData) {
        notifyObservers(new HealthDataEvent(
                HealthDataEvent.EventType.ANOMALY_DETECTED, metricId, "HealthMetric", anomalyData));
    }

    public void notifySyncCompleted(String userId) {
        notifyObservers(new HealthDataEvent(
                HealthDataEvent.EventType.SYNC_COMPLETED, userId, "Sync", null));
    }

    public int getObserverCount() {
        return observers.size();
    }
}
