package com.example.healthylifehub.ui.profile.privacy;

import java.util.HashMap;
import java.util.Map;

/**
 * Privacy Settings model for user preferences
 */
public class PrivacySettings {
    
    private boolean dataCollectionEnabled;
    private boolean personalizedAdsEnabled;
    private boolean analyticsEnabled;
    private boolean locationServicesEnabled;
    private boolean crashReportingEnabled;
    private boolean usageStatisticsEnabled;
    
    public PrivacySettings() {
        // Default constructor for Firebase
    }
    
    public PrivacySettings(
        boolean dataCollectionEnabled,
        boolean personalizedAdsEnabled,
        boolean analyticsEnabled,
        boolean locationServicesEnabled,
        boolean crashReportingEnabled,
        boolean usageStatisticsEnabled
    ) {
        this.dataCollectionEnabled = dataCollectionEnabled;
        this.personalizedAdsEnabled = personalizedAdsEnabled;
        this.analyticsEnabled = analyticsEnabled;
        this.locationServicesEnabled = locationServicesEnabled;
        this.crashReportingEnabled = crashReportingEnabled;
        this.usageStatisticsEnabled = usageStatisticsEnabled;
    }
    
    /**
     * Get default privacy settings for new users
     */
    public static PrivacySettings getDefaultSettings() {
        return new PrivacySettings(
            true,  // dataCollectionEnabled - needed for core features
            false, // personalizedAdsEnabled - opt-in
            true,  // analyticsEnabled - helps improve app
            false, // locationServicesEnabled - opt-in
            true,  // crashReportingEnabled - helps fix bugs
            true   // usageStatisticsEnabled - helps improve UX
        );
    }
    
    /**
     * Create from Firestore map
     */
    public static PrivacySettings fromMap(Map<String, Object> map) {
        PrivacySettings settings = new PrivacySettings();
        
        settings.dataCollectionEnabled = (Boolean) map.getOrDefault("dataCollectionEnabled", true);
        settings.personalizedAdsEnabled = (Boolean) map.getOrDefault("personalizedAdsEnabled", false);
        settings.analyticsEnabled = (Boolean) map.getOrDefault("analyticsEnabled", true);
        settings.locationServicesEnabled = (Boolean) map.getOrDefault("locationServicesEnabled", false);
        settings.crashReportingEnabled = (Boolean) map.getOrDefault("crashReportingEnabled", true);
        settings.usageStatisticsEnabled = (Boolean) map.getOrDefault("usageStatisticsEnabled", true);
        
        return settings;
    }
    
    /**
     * Convert to Firestore map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("dataCollectionEnabled", dataCollectionEnabled);
        map.put("personalizedAdsEnabled", personalizedAdsEnabled);
        map.put("analyticsEnabled", analyticsEnabled);
        map.put("locationServicesEnabled", locationServicesEnabled);
        map.put("crashReportingEnabled", crashReportingEnabled);
        map.put("usageStatisticsEnabled", usageStatisticsEnabled);
        
        return map;
    }
    
    // Getters and Setters
    
    public boolean isDataCollectionEnabled() {
        return dataCollectionEnabled;
    }
    
    public void setDataCollectionEnabled(boolean dataCollectionEnabled) {
        this.dataCollectionEnabled = dataCollectionEnabled;
    }
    
    public boolean isPersonalizedAdsEnabled() {
        return personalizedAdsEnabled;
    }
    
    public void setPersonalizedAdsEnabled(boolean personalizedAdsEnabled) {
        this.personalizedAdsEnabled = personalizedAdsEnabled;
    }
    
    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }
    
    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }
    
    public boolean isLocationServicesEnabled() {
        return locationServicesEnabled;
    }
    
    public void setLocationServicesEnabled(boolean locationServicesEnabled) {
        this.locationServicesEnabled = locationServicesEnabled;
    }
    
    public boolean isCrashReportingEnabled() {
        return crashReportingEnabled;
    }
    
    public void setCrashReportingEnabled(boolean crashReportingEnabled) {
        this.crashReportingEnabled = crashReportingEnabled;
    }
    
    public boolean isUsageStatisticsEnabled() {
        return usageStatisticsEnabled;
    }
    
    public void setUsageStatisticsEnabled(boolean usageStatisticsEnabled) {
        this.usageStatisticsEnabled = usageStatisticsEnabled;
    }
    
    @Override
    public String toString() {
        return "PrivacySettings{" +
                "dataCollectionEnabled=" + dataCollectionEnabled +
                ", personalizedAdsEnabled=" + personalizedAdsEnabled +
                ", analyticsEnabled=" + analyticsEnabled +
                ", locationServicesEnabled=" + locationServicesEnabled +
                ", crashReportingEnabled=" + crashReportingEnabled +
                ", usageStatisticsEnabled=" + usageStatisticsEnabled +
                '}';
    }
}