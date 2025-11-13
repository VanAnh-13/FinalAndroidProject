package com.example.healthylifehub.ui.settings;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.NotificationSettings;
import com.example.healthylifehub.data.repository.NotificationSettingsRepository;

import java.util.concurrent.CompletableFuture;

/**
 * ViewModel for NotificationSettingsActivity
 * Follows project pattern: BaseViewModel + Repository + LiveData
 */
public class NotificationSettingsViewModel extends BaseViewModel {
    
    private static final String TAG = "NotificationSettingsVM";
    
    private final NotificationSettingsRepository repository;
    private final MediatorLiveData<NotificationSettings> settings = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public NotificationSettingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new NotificationSettingsRepository(application.getApplicationContext());
    }
    
    /**
     * Load notification settings
     */
    public void loadSettings() {
        showLoading();
        
        // Observe settings from repository (offline-first)
        LiveData<NotificationSettings> settingsLiveData = repository.loadNotificationSettings();
        
        // Add source to mediator to forward updates
        settings.addSource(settingsLiveData, newSettings -> {
            settings.setValue(newSettings);
            hideLoading();
            
            if (newSettings != null) {
                Log.d(TAG, "✅ Loaded notification settings");
            } else {
                Log.w(TAG, "⚠️ No notification settings found");
            }
        });
    }
    
    /**
     * Save notification settings
     */
    public void saveSettings(NotificationSettings newSettings) {
        if (newSettings == null) {
            Log.e(TAG, "Cannot save null settings");
            saveResult.setValue(false);
            return;
        }
        
        showLoading();
        Log.d(TAG, "Saving notification settings...");
        
        CompletableFuture<Boolean> saveFuture = repository.saveNotificationSettings(newSettings);
        
        saveFuture.thenAccept(success -> {
            hideLoading();
            saveResult.postValue(success);
            
            if (success) {
                Log.d(TAG, "✅ Settings saved successfully");
                // Update local settings
                settings.postValue(newSettings);
            } else {
                Log.e(TAG, "❌ Failed to save settings");
                errorMessage.postValue("Không thể lưu cài đặt. Vui lòng thử lại.");
            }
        }).exceptionally(throwable -> {
            hideLoading();
            saveResult.postValue(false);
            Log.e(TAG, "❌ Error saving settings", throwable);
            errorMessage.postValue("Lỗi khi lưu cài đặt: " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Reset to default settings
     */
    public void resetToDefaults() {
        com.google.firebase.auth.FirebaseUser currentUser = 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Log.e(TAG, "No user logged in");
            errorMessage.setValue("Bạn cần đăng nhập để thực hiện thao tác này");
            return;
        }
        
        Log.d(TAG, "Resetting to default settings...");
        
        NotificationSettings defaultSettings = NotificationSettings.getDefaultSettings(currentUser.getUid());
        saveSettings(defaultSettings);
    }
    
    /**
     * Update specific setting
     */
    public void updateRemindersEnabled(boolean enabled) {
        NotificationSettings current = settings.getValue();
        if (current != null) {
            current.setRemindersEnabled(enabled);
            saveSettings(current);
        }
    }
    
    public void updateHealthAlertsEnabled(boolean enabled) {
        NotificationSettings current = settings.getValue();
        if (current != null) {
            current.setHealthAlertsEnabled(enabled);
            saveSettings(current);
        }
    }
    
    public void updateSuggestionsEnabled(boolean enabled) {
        NotificationSettings current = settings.getValue();
        if (current != null) {
            current.setSuggestionsEnabled(enabled);
            saveSettings(current);
        }
    }
    
    public void updateQuietHours(boolean enabled, String startTime, String endTime) {
        NotificationSettings current = settings.getValue();
        if (current != null) {
            current.setQuietHoursEnabled(enabled);
            if (startTime != null) {
                current.setQuietStartTime(startTime);
            }
            if (endTime != null) {
                current.setQuietEndTime(endTime);
            }
            saveSettings(current);
        }
    }
    
    /**
     * Check if notifications are allowed at current time
     */
    public boolean areNotificationsAllowedNow() {
        NotificationSettings current = settings.getValue();
        return current == null || current.isNotificationAllowedNow();
    }
    
    /**
     * Get current settings summary for display
     */
    public String getSettingsSummary() {
        NotificationSettings current = settings.getValue();
        if (current == null) {
            return "Đang tải cài đặt...";
        }
        
        int enabledCount = 0;
        if (current.isRemindersEnabled()) enabledCount++;
        if (current.isHealthAlertsEnabled()) enabledCount++;
        if (current.isSuggestionsEnabled()) enabledCount++;
        
        String quietHoursInfo = current.isQuietHoursEnabled() 
            ? String.format("Giờ im lặng: %s - %s", current.getQuietStartTime(), current.getQuietEndTime())
            : "Không có giờ im lặng";
        
        return String.format("%d loại thông báo đang bật. %s", enabledCount, quietHoursInfo);
    }
    
    // Getters for LiveData
    public LiveData<NotificationSettings> getSettings() {
        return settings;
    }
    
    public LiveData<Boolean> getSaveResult() {
        return saveResult;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");
    }
}