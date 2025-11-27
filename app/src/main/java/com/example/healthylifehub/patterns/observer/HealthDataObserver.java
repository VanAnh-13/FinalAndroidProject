package com.example.healthylifehub.patterns.observer;

/**
 * Observer Pattern - Observer Interface
 * Receives notifications when health data changes
 */
public interface HealthDataObserver {
    void onHealthDataChanged(HealthDataEvent event);
}
