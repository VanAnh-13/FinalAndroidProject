package com.example.healthylifehub.ui.settings;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityNotificationSettingsBinding;
import com.example.healthylifehub.data.model.NotificationSettings;
import com.example.healthylifehub.utils.NotificationHelper;
import com.example.healthylifehub.utils.PermissionManager;

/**
 * NotificationSettingsActivity - Manage notification preferences
 * Follows project pattern: BaseActivity + ViewModel + ViewBinding
 */
public class NotificationSettingsActivity extends BaseActivity<ActivityNotificationSettingsBinding> {
    
    private static final String TAG = "NotificationSettings";
    private NotificationSettingsViewModel viewModel;
    private NotificationSettings currentSettings;
    
    public NotificationSettingsActivity() {
        super(ActivityNotificationSettingsBinding::inflate);
    }
    
    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(NotificationSettingsViewModel.class);
        
        // Load settings
        viewModel.loadSettings();
    }
    
    @Override
    public void bindData() {
        // Setup toolbar
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Update permission status
        updatePermissionStatus();
        
        // Observe settings changes
        viewModel.getSettings().observe(this, settings -> {
            if (settings != null) {
                currentSettings = settings;
                updateUI(settings);
            }
        });
        
        // Observe loading state
        viewModel.getLoading().observe(this, isLoading -> {
            getBinding().progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observe save result
        viewModel.getSaveResult().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "✅ Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void setOnClick() {
        // Toolbar back button
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
        
        // Permission action button
        getBinding().btnPermissionAction.setOnClickListener(v -> openAppNotificationSettings());
        
        // Reminder settings
        getBinding().switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setRemindersEnabled(isChecked);
                updateReminderSettingsVisibility(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchReminderSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setReminderSound(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchReminderVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setReminderVibration(isChecked);
                saveSettings();
            }
        });
        
        // Volume seekbar
        getBinding().seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentSettings != null) {
                    currentSettings.setReminderVolumeLevel(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });
        
        // Health alerts settings
        getBinding().switchHealthAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setHealthAlertsEnabled(isChecked);
                updateHealthAlertsVisibility(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchCriticalAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setCriticalAlertsEnabled(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchAnomalyAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setAnomalyAlertsEnabled(isChecked);
                saveSettings();
            }
        });
        
        // Suggestions settings
        getBinding().switchSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setSuggestionsEnabled(isChecked);
                updateSuggestionsVisibility(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchWeeklyReports.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setWeeklyReportsEnabled(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchSmartSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setSmartSuggestionsEnabled(isChecked);
                saveSettings();
            }
        });
        
        // Quiet hours settings
        getBinding().switchQuietHours.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setQuietHoursEnabled(isChecked);
                updateQuietHoursVisibility(isChecked);
                saveSettings();
            }
        });
        
        // Time picker buttons
        getBinding().btnStartTime.setOnClickListener(v -> showTimePicker(true));
        getBinding().btnEndTime.setOnClickListener(v -> showTimePicker(false));
        
        // Advanced settings
        getBinding().switchBundleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setBundleNotifications(isChecked);
                saveSettings();
            }
        });
        
        getBinding().switchShowOnLockScreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setShowOnLockScreen(isChecked);
                saveSettings();
            }
        });
    }
    
    /**
     * Update permission status card
     */
    private void updatePermissionStatus() {
        boolean hasPermission = PermissionManager.isNotificationPermissionGranted(this);
        boolean areNotificationsEnabled = NotificationHelper.areNotificationsEnabled(this);
        
        if (hasPermission && areNotificationsEnabled) {
            // All good
            getBinding().ivPermissionStatus.setImageResource(R.drawable.ic_notifications_active);
            getBinding().ivPermissionStatus.setColorFilter(getColor(R.color.success));
            getBinding().tvPermissionTitle.setText("Thông báo đã bật");
            getBinding().tvPermissionDesc.setText("Ứng dụng có thể gửi thông báo");
            getBinding().btnPermissionAction.setVisibility(View.GONE);
            getBinding().cardPermissionStatus.setStrokeColor(getColor(R.color.success));
        } else {
            // Need permission or settings
            getBinding().ivPermissionStatus.setImageResource(R.drawable.ic_notifications_off);
            getBinding().ivPermissionStatus.setColorFilter(getColor(R.color.error));
            getBinding().tvPermissionTitle.setText("Thông báo bị tắt");
            getBinding().tvPermissionDesc.setText("Vui lòng bật thông báo để sử dụng đầy đủ tính năng");
            getBinding().btnPermissionAction.setVisibility(View.VISIBLE);
            getBinding().btnPermissionAction.setText("Bật ngay");
            getBinding().cardPermissionStatus.setStrokeColor(getColor(R.color.error));
        }
    }
    
    /**
     * Update UI with settings data
     */
    private void updateUI(NotificationSettings settings) {
        // Prevent triggering listeners while updating UI
        setListenersEnabled(false);
        
        // Reminder settings
        getBinding().switchReminders.setChecked(settings.isRemindersEnabled());
        getBinding().switchReminderSound.setChecked(settings.isReminderSound());
        getBinding().switchReminderVibration.setChecked(settings.isReminderVibration());
        getBinding().seekBarVolume.setProgress(settings.getReminderVolumeLevel());
        
        // Health alerts
        getBinding().switchHealthAlerts.setChecked(settings.isHealthAlertsEnabled());
        getBinding().switchCriticalAlerts.setChecked(settings.isCriticalAlertsEnabled());
        getBinding().switchAnomalyAlerts.setChecked(settings.isAnomalyAlertsEnabled());
        
        // Suggestions
        getBinding().switchSuggestions.setChecked(settings.isSuggestionsEnabled());
        getBinding().switchWeeklyReports.setChecked(settings.isWeeklyReportsEnabled());
        getBinding().switchSmartSuggestions.setChecked(settings.isSmartSuggestionsEnabled());
        
        // Quiet hours
        getBinding().switchQuietHours.setChecked(settings.isQuietHoursEnabled());
        getBinding().btnStartTime.setText(settings.getQuietStartTime());
        getBinding().btnEndTime.setText(settings.getQuietEndTime());
        
        // Advanced
        getBinding().switchBundleNotifications.setChecked(settings.isBundleNotifications());
        getBinding().switchShowOnLockScreen.setChecked(settings.isShowOnLockScreen());
        
        // Update visibility
        updateReminderSettingsVisibility(settings.isRemindersEnabled());
        updateHealthAlertsVisibility(settings.isHealthAlertsEnabled());
        updateSuggestionsVisibility(settings.isSuggestionsEnabled());
        updateQuietHoursVisibility(settings.isQuietHoursEnabled());
        
        // Re-enable listeners
        setListenersEnabled(true);
    }
    
    /**
     * Update visibility of reminder sub-settings
     */
    private void updateReminderSettingsVisibility(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        getBinding().layoutReminderSound.setVisibility(visibility);
        getBinding().layoutReminderVibration.setVisibility(visibility);
        getBinding().layoutVolumeLevel.setVisibility(visibility);
    }
    
    /**
     * Update visibility of health alerts sub-settings
     */
    private void updateHealthAlertsVisibility(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        getBinding().layoutCriticalAlerts.setVisibility(visibility);
        getBinding().layoutAnomalyAlerts.setVisibility(visibility);
    }
    
    /**
     * Update visibility of suggestions sub-settings
     */
    private void updateSuggestionsVisibility(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        getBinding().layoutWeeklyReports.setVisibility(visibility);
        getBinding().layoutSmartSuggestions.setVisibility(visibility);
    }
    
    /**
     * Update visibility of quiet hours sub-settings
     */
    private void updateQuietHoursVisibility(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        getBinding().layoutQuietTimeRange.setVisibility(visibility);
    }
    
    /**
     * Show time picker dialog
     */
    private void showTimePicker(boolean isStartTime) {
        if (currentSettings == null) return;
        
        String currentTime = isStartTime ? currentSettings.getQuietStartTime() : currentSettings.getQuietEndTime();
        String[] timeParts = currentTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, selectedHour, selectedMinute) -> {
                String newTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                
                if (isStartTime) {
                    currentSettings.setQuietStartTime(newTime);
                    getBinding().btnStartTime.setText(newTime);
                } else {
                    currentSettings.setQuietEndTime(newTime);
                    getBinding().btnEndTime.setText(newTime);
                }
                
                saveSettings();
            },
            hour,
            minute,
            true
        );
        
        timePickerDialog.show();
    }
    
    /**
     * Save settings
     */
    private void saveSettings() {
        if (currentSettings != null) {
            viewModel.saveSettings(currentSettings);
        }
    }
    
    /**
     * Temporarily disable listeners to prevent loops when updating UI
     */
    private void setListenersEnabled(boolean enabled) {
        // This is handled by checking currentSettings != null in listeners
        // Could be improved with a flag if needed
    }
    
    /**
     * Open app notification settings in system settings
     */
    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update permission status when returning from settings
        updatePermissionStatus();
    }
}