package com.example.healthylifehub.ui.profile.privacy;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseViewModel;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel for Privacy & Security settings
 * Handles password changes, account deletion, privacy settings
 */
public class PrivacySecurityViewModel extends BaseViewModel {
    
    private static final String TAG = "PrivacySecurityVM";
    
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    
    private final MutableLiveData<PrivacySettings> privacySettings = new MutableLiveData<>();
    private final MutableLiveData<String> actionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    public PrivacySecurityViewModel(@NonNull Application application) {
        super(application);
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Load privacy settings from Firestore
     */
    public void loadPrivacySettings() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in");
            return;
        }
        
        showLoading();
        
        firestore.collection("users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(document -> {
                hideLoading();
                
                if (document.exists()) {
                    // Parse privacy settings or create default
                    PrivacySettings settings = parsePrivacySettings(document.getData());
                    privacySettings.setValue(settings);
                } else {
                    // Create default settings
                    PrivacySettings defaultSettings = PrivacySettings.getDefaultSettings();
                    savePrivacySettings(defaultSettings);
                    privacySettings.setValue(defaultSettings);
                }
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Log.e(TAG, "Error loading privacy settings", e);
                actionResult.setValue("Không thể tải cài đặt quyền riêng tư");
            });
    }
    
    /**
     * Update data collection setting
     */
    public void updateDataCollectionEnabled(boolean enabled) {
        updatePrivacySetting("dataCollectionEnabled", enabled);
    }
    
    /**
     * Update personalized ads setting
     */
    public void updatePersonalizedAdsEnabled(boolean enabled) {
        updatePrivacySetting("personalizedAdsEnabled", enabled);
    }
    
    /**
     * Update analytics setting
     */
    public void updateAnalyticsEnabled(boolean enabled) {
        updatePrivacySetting("analyticsEnabled", enabled);
    }
    
    /**
     * Update location services setting
     */
    public void updateLocationServicesEnabled(boolean enabled) {
        updatePrivacySetting("locationServicesEnabled", enabled);
    }
    
    /**
     * Change user password
     */
    public void changePassword(String currentPassword, String newPassword) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            actionResult.setValue("Không thể đổi mật khẩu: Người dùng không hợp lệ");
            return;
        }
        
        showLoading();
        
        // Re-authenticate first
        AuthCredential credential = EmailAuthProvider.getCredential(
            currentUser.getEmail(), currentPassword);
        
        currentUser.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                // Re-authentication success, now change password
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid2 -> {
                        hideLoading();
                        actionResult.setValue("✅ Đổi mật khẩu thành công");
                        Log.d(TAG, "Password changed successfully");
                        
                        // Log security event
                        logSecurityEvent("password_changed");
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        Log.e(TAG, "Failed to change password", e);
                        actionResult.setValue("❌ Đổi mật khẩu thất bại: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Log.e(TAG, "Re-authentication failed", e);
                actionResult.setValue("❌ Mật khẩu hiện tại không đúng");
            });
    }
    
    /**
     * Request data export
     */
    public void requestDataExport() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            actionResult.setValue("Không thể xuất dữ liệu: Người dùng không hợp lệ");
            return;
        }
        
        showLoading();
        
        Map<String, Object> exportRequest = new HashMap<>();
        exportRequest.put("userId", currentUser.getUid());
        exportRequest.put("email", currentUser.getEmail());
        exportRequest.put("requestedAt", com.google.firebase.Timestamp.now());
        exportRequest.put("status", "pending");
        exportRequest.put("type", "full_export");
        
        firestore.collection("dataExportRequests")
            .add(exportRequest)
            .addOnSuccessListener(documentReference -> {
                hideLoading();
                actionResult.setValue("✅ Yêu cầu xuất dữ liệu đã được gửi. " +
                    "Bạn sẽ nhận được file qua email trong 24-48h.");
                Log.d(TAG, "Data export requested: " + documentReference.getId());
                
                // Log security event
                logSecurityEvent("data_export_requested");
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Log.e(TAG, "Failed to request data export", e);
                actionResult.setValue("❌ Không thể gửi yêu cầu xuất dữ liệu");
            });
    }
    
    /**
     * Delete user account permanently
     */
    public void deleteAccount(String password) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            actionResult.setValue("Không thể xóa tài khoản: Người dùng không hợp lệ");
            return;
        }
        
        showLoading();
        
        // Re-authenticate first
        AuthCredential credential = EmailAuthProvider.getCredential(
            currentUser.getEmail(), password);
        
        currentUser.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                String userId = currentUser.getUid();
                
                // First delete user data from Firestore
                deleteUserDataFromFirestore(userId)
                    .addOnSuccessListener(aVoid2 -> {
                        // Then delete Firebase Auth account
                        currentUser.delete()
                            .addOnSuccessListener(aVoid3 -> {
                                hideLoading();
                                actionResult.setValue("✅ Tài khoản đã được xóa thành công");
                                Log.d(TAG, "Account deleted successfully");
                                
                                // Navigate to login screen (will be handled in Activity)
                            })
                            .addOnFailureListener(e -> {
                                hideLoading();
                                Log.e(TAG, "Failed to delete Firebase Auth account", e);
                                actionResult.setValue("❌ Không thể xóa tài khoản: " + e.getMessage());
                            });
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        Log.e(TAG, "Failed to delete user data", e);
                        actionResult.setValue("❌ Không thể xóa dữ liệu người dùng");
                    });
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Log.e(TAG, "Re-authentication failed for account deletion", e);
                actionResult.setValue("❌ Mật khẩu không đúng");
            });
    }
    
    /**
     * Delete all user data from Firestore
     */
    private com.google.android.gms.tasks.Task<Void> deleteUserDataFromFirestore(String userId) {
        // Create batch delete operation
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        
        // Delete main user document
        batch.delete(firestore.collection("users").document(userId));
        
        // Note: In production, you'd need to delete subcollections too
        // This is simplified for demo purposes
        
        return batch.commit();
    }
    
    /**
     * Update a specific privacy setting
     */
    private void updatePrivacySetting(String settingKey, Object value) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            actionResult.setValue("Không thể cập nhật cài đặt: Người dùng không hợp lệ");
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("privacySettings." + settingKey, value);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        
        firestore.collection("users")
            .document(currentUser.getUid())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Privacy setting updated: " + settingKey + " = " + value);
                
                // Update local state
                PrivacySettings current = privacySettings.getValue();
                if (current != null) {
                    updateLocalPrivacySettings(current, settingKey, value);
                    privacySettings.setValue(current);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update privacy setting: " + settingKey, e);
                actionResult.setValue("Không thể cập nhật cài đặt quyền riêng tư");
            });
    }
    
    /**
     * Save complete privacy settings to Firestore
     */
    private void savePrivacySettings(PrivacySettings settings) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("privacySettings", settings.toMap());
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        
        firestore.collection("users")
            .document(currentUser.getUid())
            .update(updates)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Privacy settings saved"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to save privacy settings", e));
    }
    
    /**
     * Parse privacy settings from Firestore document
     */
    private PrivacySettings parsePrivacySettings(Map<String, Object> userData) {
        if (userData == null) {
            return PrivacySettings.getDefaultSettings();
        }
        
        Object privacyObj = userData.get("privacySettings");
        if (privacyObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> privacyMap = (Map<String, Object>) privacyObj;
            return PrivacySettings.fromMap(privacyMap);
        }
        
        return PrivacySettings.getDefaultSettings();
    }
    
    /**
     * Update local privacy settings object
     */
    private void updateLocalPrivacySettings(PrivacySettings settings, String key, Object value) {
        switch (key) {
            case "dataCollectionEnabled":
                settings.setDataCollectionEnabled((Boolean) value);
                break;
            case "personalizedAdsEnabled":
                settings.setPersonalizedAdsEnabled((Boolean) value);
                break;
            case "analyticsEnabled":
                settings.setAnalyticsEnabled((Boolean) value);
                break;
            case "locationServicesEnabled":
                settings.setLocationServicesEnabled((Boolean) value);
                break;
        }
    }
    
    /**
     * Log security events
     */
    private void logSecurityEvent(String eventType) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("userId", currentUser.getUid());
        securityEvent.put("eventType", eventType);
        securityEvent.put("timestamp", com.google.firebase.Timestamp.now());
        securityEvent.put("ipAddress", "unknown"); // Would need to get real IP
        securityEvent.put("userAgent", "HealthyLife Hub Android App");
        
        firestore.collection("securityEvents")
            .add(securityEvent)
            .addOnSuccessListener(doc -> Log.d(TAG, "Security event logged: " + eventType))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to log security event", e));
    }
    
    // Getters for LiveData
    public LiveData<PrivacySettings> getPrivacySettings() {
        return privacySettings;
    }
    
    public LiveData<String> getActionResult() {
        return actionResult;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // Helper methods for loading state
    @Override
    public void showLoading() {
        isLoading.setValue(true);
    }
    
    @Override
    public void hideLoading() {
        isLoading.setValue(false);
    }
}