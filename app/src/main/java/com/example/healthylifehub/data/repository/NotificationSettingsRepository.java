package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.NotificationSettingsDao;
import com.example.healthylifehub.data.model.NotificationSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Tasks;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing notification settings with OFFLINE-FIRST architecture
 * Follows project pattern: Room + Firestore sync
 */
public class NotificationSettingsRepository {
    
    private static final String TAG = "NotificationSettingsRepo";
    private static final String COLLECTION_USERS = "users";
    private static final String FIELD_NOTIFICATION_SETTINGS = "notificationSettings";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final NotificationSettingsDao dao;
    private final ExecutorService executorService;
    private final Context context;
    
    public NotificationSettingsRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dao = AppDatabase.getInstance(context).notificationSettingsDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    /**
     * Load notification settings (OFFLINE-FIRST)
     * 1. Return LiveData from Room (instant)
     * 2. Sync from Firestore in background
     */
    public LiveData<NotificationSettings> loadNotificationSettings() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            MutableLiveData<NotificationSettings> liveData = new MutableLiveData<>();
            liveData.setValue(null);
            return liveData;
        }
        
        // Step 1: Return data from Room (instant, works offline)
        LiveData<NotificationSettings> roomData = dao.getSettingsForUser(userId);
        
        // Step 2: Sync from Firestore in background
        syncFromFirestore(userId);
        
        // Step 3: If no local data, create and return default settings
        executorService.execute(() -> {
            NotificationSettings existing = dao.getSettingsForUserSync(userId);
            if (existing == null) {
                Log.d(TAG, "No settings found, creating default settings");
                NotificationSettings defaultSettings = NotificationSettings.getDefaultSettings(userId);
                saveNotificationSettings(defaultSettings);
            }
        });
        
        return roomData;
    }
    
    /**
     * Save notification settings (OFFLINE-FIRST)
     * 1. Save to Room immediately (works offline)
     * 2. Sync to Firestore in background
     */
    public CompletableFuture<Boolean> saveNotificationSettings(NotificationSettings settings) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            future.complete(false);
            return future;
        }
        
        // Set metadata
        settings.setUserId(userId);
        settings.setNeedsSync(true);
        settings.setUpdatedAt(new Date());
        settings.setLastSyncedAt(new Date());
        
        // Step 1: Save to Room (instant, works offline)
        executorService.execute(() -> {
            try {
                dao.insertSettings(settings);
                Log.d(TAG, "✅ Saved to local database");
                
                // Step 2: Sync to Firestore in background
                syncToFirestore(settings)
                    .thenAccept(success -> {
                        if (success) {
                            // Mark as synced in Room
                            executorService.execute(() -> {
                                settings.setNeedsSync(false);
                                dao.insertSettings(settings);
                                Log.d(TAG, "✅ Synced to Firestore");
                            });
                        }
                        future.complete(true); // Return success even if Firestore fails (offline-first)
                    })
                    .exceptionally(throwable -> {
                        Log.w(TAG, "⚠️ Firestore sync failed (will retry later)", throwable);
                        future.complete(true); // Still success because saved locally
                        return null;
                    });
                    
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving to local database", e);
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Sync settings to Firestore
     */
    private CompletableFuture<Boolean> syncToFirestore(NotificationSettings settings) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Convert settings to Firestore-compatible map
        Map<String, Object> settingsData = new HashMap<>();
        settingsData.put("remindersEnabled", settings.isRemindersEnabled());
        settingsData.put("reminderSound", settings.isReminderSound());
        settingsData.put("reminderVibration", settings.isReminderVibration());
        settingsData.put("reminderVolumeLevel", settings.getReminderVolumeLevel());
        
        settingsData.put("healthAlertsEnabled", settings.isHealthAlertsEnabled());
        settingsData.put("criticalAlertsEnabled", settings.isCriticalAlertsEnabled());
        settingsData.put("anomalyAlertsEnabled", settings.isAnomalyAlertsEnabled());
        settingsData.put("healthAlertSound", settings.isHealthAlertSound());
        settingsData.put("healthAlertVibration", settings.isHealthAlertVibration());
        
        settingsData.put("suggestionsEnabled", settings.isSuggestionsEnabled());
        settingsData.put("weeklyReportsEnabled", settings.isWeeklyReportsEnabled());
        settingsData.put("monthlyReportsEnabled", settings.isMonthlyReportsEnabled());
        settingsData.put("smartSuggestionsEnabled", settings.isSmartSuggestionsEnabled());
        
        settingsData.put("quietHoursEnabled", settings.isQuietHoursEnabled());
        settingsData.put("quietStartTime", settings.getQuietStartTime());
        settingsData.put("quietEndTime", settings.getQuietEndTime());
        
        settingsData.put("bundleNotifications", settings.isBundleNotifications());
        settingsData.put("maxNotificationsPerDay", settings.getMaxNotificationsPerDay());
        settingsData.put("showOnLockScreen", settings.isShowOnLockScreen());
        settingsData.put("showInStatusBar", settings.isShowInStatusBar());
        
        settingsData.put("updatedAt", com.google.firebase.Timestamp.now());
        
        // Save to Firestore
        db.collection(COLLECTION_USERS)
            .document(settings.getUserId())
            .update(FIELD_NOTIFICATION_SETTINGS, settingsData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Synced settings to Firestore");
                future.complete(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to sync settings to Firestore", e);
                future.complete(false);
            });
        
        return future;
    }
    
    /**
     * Sync settings from Firestore to Room
     */
    private void syncFromFirestore(String userId) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener(executorService, (snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "⚠️ Firestore sync error (using cached data)", error);
                    return;
                }
                
                if (snapshot != null && snapshot.exists()) {
                    Object settingsObj = snapshot.get(FIELD_NOTIFICATION_SETTINGS);
                    if (settingsObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> settingsMap = (Map<String, Object>) settingsObj;
                        
                        NotificationSettings settings = parseFirestoreSettings(userId, settingsMap);
                        if (settings != null) {
                            // Update Room database
                            dao.insertSettings(settings);
                            Log.d(TAG, "🔄 Synced settings from Firestore");
                        }
                    }
                }
            });
    }
    
    /**
     * Parse Firestore settings map to NotificationSettings object
     */
    private NotificationSettings parseFirestoreSettings(String userId, Map<String, Object> map) {
        try {
            NotificationSettings settings = new NotificationSettings();
            settings.setUserId(userId);
            
            // Reminder settings
            settings.setRemindersEnabled((Boolean) map.getOrDefault("remindersEnabled", true));
            settings.setReminderSound((Boolean) map.getOrDefault("reminderSound", true));
            settings.setReminderVibration((Boolean) map.getOrDefault("reminderVibration", true));
            settings.setReminderVolumeLevel(((Number) map.getOrDefault("reminderVolumeLevel", 80)).intValue());
            
            // Health alert settings
            settings.setHealthAlertsEnabled((Boolean) map.getOrDefault("healthAlertsEnabled", true));
            settings.setCriticalAlertsEnabled((Boolean) map.getOrDefault("criticalAlertsEnabled", true));
            settings.setAnomalyAlertsEnabled((Boolean) map.getOrDefault("anomalyAlertsEnabled", false));
            settings.setHealthAlertSound((Boolean) map.getOrDefault("healthAlertSound", true));
            settings.setHealthAlertVibration((Boolean) map.getOrDefault("healthAlertVibration", true));
            
            // Suggestion settings
            settings.setSuggestionsEnabled((Boolean) map.getOrDefault("suggestionsEnabled", true));
            settings.setWeeklyReportsEnabled((Boolean) map.getOrDefault("weeklyReportsEnabled", true));
            settings.setMonthlyReportsEnabled((Boolean) map.getOrDefault("monthlyReportsEnabled", true));
            settings.setSmartSuggestionsEnabled((Boolean) map.getOrDefault("smartSuggestionsEnabled", false));
            
            // Quiet hours
            settings.setQuietHoursEnabled((Boolean) map.getOrDefault("quietHoursEnabled", true));
            settings.setQuietStartTime((String) map.getOrDefault("quietStartTime", "22:00"));
            settings.setQuietEndTime((String) map.getOrDefault("quietEndTime", "07:00"));
            
            // Advanced settings
            settings.setBundleNotifications((Boolean) map.getOrDefault("bundleNotifications", true));
            settings.setMaxNotificationsPerDay(((Number) map.getOrDefault("maxNotificationsPerDay", 20)).intValue());
            settings.setShowOnLockScreen((Boolean) map.getOrDefault("showOnLockScreen", true));
            settings.setShowInStatusBar((Boolean) map.getOrDefault("showInStatusBar", true));
            
            // Sync metadata
            settings.setNeedsSync(false); // From Firestore, already synced
            settings.setLastSyncedAt(new Date());
            settings.setUpdatedAt(new Date());
            
            return settings;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Firestore settings", e);
            return null;
        }
    }
}