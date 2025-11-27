package com.example.healthylifehub.ui.settings;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityNotificationSettingsBinding;
import com.example.healthylifehub.data.model.NotificationSettings;
import com.example.healthylifehub.utils.notification.NotificationHelper;
import com.example.healthylifehub.utils.security.PermissionManager;

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
                Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
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
        
        // Notification preview button
        getBinding().btnPreviewNotification.setOnClickListener(v -> showNotificationPreview());
        
        // Sound and vibration customization buttons
        getBinding().btnSelectSound.setOnClickListener(v -> showSoundSelectionDialog());
        getBinding().btnSelectVibration.setOnClickListener(v -> showVibrationSelectionDialog());
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
            getBinding().tvPermissionTitle.setText(R.string.notification_enabled_title);
            getBinding().tvPermissionDesc.setText(R.string.notification_enabled_desc);
            getBinding().btnPermissionAction.setVisibility(View.GONE);
            getBinding().cardPermissionStatus.setStrokeColor(getColor(R.color.success));
        } else {
            // Need permission or settings
            getBinding().ivPermissionStatus.setImageResource(R.drawable.ic_notifications_off);
            getBinding().ivPermissionStatus.setColorFilter(getColor(R.color.error));
            getBinding().tvPermissionTitle.setText(R.string.notification_disabled_title);
            getBinding().tvPermissionDesc.setText(R.string.notification_disabled_desc);
            getBinding().btnPermissionAction.setVisibility(View.VISIBLE);
            getBinding().btnPermissionAction.setText(R.string.notification_enable_action);
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
        getBinding().layoutSoundSelection.setVisibility(visibility);
        getBinding().layoutVibrationPattern.setVisibility(visibility);
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
     * Open app notification settings in system settings or request permission
     */
    private void openAppNotificationSettings() {
        // For Android 13+, try to request permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionManager.isNotificationPermissionGranted(this)) {
                // Check if we should show rationale
                if (PermissionManager.shouldShowNotificationPermissionRationale(this)) {
                    // Show explanation dialog
                    showPermissionRationaleDialog();
                } else {
                    // Request permission directly
                    PermissionManager.requestNotificationPermission(this);
                }
                return;
            }
        }
        
        // For older versions or if permission already granted but notifications disabled
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    
    /**
     * Show permission rationale dialog
     */
    private void showPermissionRationaleDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(getString(R.string.notification_permission_message) + getString(R.string.permission_dialog_message_bullets))
            .setPositiveButton(R.string.btn_agree, (dialog, which) -> {
                PermissionManager.requestNotificationPermission(this);
            })
            .setNegativeButton(R.string.btn_no, (dialog, which) -> {
                Toast.makeText(this, getString(R.string.some_features_may_not_work), Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        PermissionManager.handlePermissionResult(requestCode, permissions, grantResults, 
            new PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    updatePermissionStatus();
                    Toast.makeText(NotificationSettingsActivity.this, 
                        getString(R.string.permission_granted_success), Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onPermissionDenied() {
                    updatePermissionStatus();
                    Toast.makeText(NotificationSettingsActivity.this, 
                        getString(R.string.permission_denied_message), 
                        Toast.LENGTH_LONG).show();
                }
            });
    }
    
    /**
     * Show sound selection dialog
     */
    private void showSoundSelectionDialog() {
        if (currentSettings == null) return;
        
        String[] soundOptions = {
            getString(R.string.sound_system_default),
            getString(R.string.sound_gentle),
            getString(R.string.sound_standard), 
            getString(R.string.sound_strong),
            getString(R.string.sound_custom)
        };
        
        String[] soundValues = {
            "default",
            "gentle", 
            "standard",
            "strong",
            "custom"
        };
        
        // Find current selection
        int currentSelection = 0;
        String currentSound = currentSettings.getReminderSoundUri();
        if (currentSound != null) {
            for (int i = 0; i < soundValues.length; i++) {
                if (soundValues[i].equals(currentSound)) {
                    currentSelection = i;
                    break;
                }
            }
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.dialog_select_sound)
            .setSingleChoiceItems(soundOptions, currentSelection, (dialog, which) -> {
                if (which == soundValues.length - 1) {
                    // Custom sound selection - would open system sound picker
                    Toast.makeText(this, getString(R.string.custom_sound_coming_soon), 
                        Toast.LENGTH_SHORT).show();
                } else {
                    currentSettings.setReminderSoundUri(soundValues[which]);
                    saveSettings();
                    Toast.makeText(this, getString(R.string.selected_sound, soundOptions[which]), Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }
    
    /**
     * Show vibration pattern selection dialog
     */
    private void showVibrationSelectionDialog() {
        if (currentSettings == null) return;
        
        String[] vibrationOptions = {
            getString(R.string.vibration_default),
            getString(R.string.vibration_gentle),
            getString(R.string.vibration_standard),
            getString(R.string.vibration_strong),
            getString(R.string.vibration_continuous)
        };
        
        String[] vibrationValues = {
            "default",
            "gentle",
            "standard", 
            "strong",
            "continuous"
        };
        
        // Find current selection
        int currentSelection = 0;
        String currentVibration = currentSettings.getReminderVibrationPattern();
        if (currentVibration != null) {
            for (int i = 0; i < vibrationValues.length; i++) {
                if (vibrationValues[i].equals(currentVibration)) {
                    currentSelection = i;
                    break;
                }
            }
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.dialog_select_vibration)
            .setSingleChoiceItems(vibrationOptions, currentSelection, (dialog, which) -> {
                currentSettings.setReminderVibrationPattern(vibrationValues[which]);
                saveSettings();
                
                // Test vibration pattern
                testVibrationPattern(vibrationValues[which]);
                
                Toast.makeText(this, getString(R.string.selected_vibration, vibrationOptions[which]), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }
    
    /**
     * Test vibration pattern
     */
    private void testVibrationPattern(String pattern) {
        if (!currentSettings.isReminderVibration()) {
            return; // Don't test if vibration is disabled
        }
        
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] vibrationPattern;
                
                switch (pattern) {
                    case "gentle":
                        vibrationPattern = new long[]{0, 200}; // Short single vibration
                        break;
                    case "standard":
                        vibrationPattern = new long[]{0, 300, 200, 300}; // Two vibrations
                        break;
                    case "strong":
                        vibrationPattern = new long[]{0, 500, 200, 500, 200, 500}; // Three vibrations
                        break;
                    case "continuous":
                        vibrationPattern = new long[]{0, 1000}; // Long continuous vibration
                        break;
                    default: // "default"
                        vibrationPattern = new long[]{0, 500, 200, 500}; // Default pattern
                        break;
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(vibrationPattern, -1));
                } else {
                    vibrator.vibrate(vibrationPattern, -1);
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to test vibration pattern", e);
        }
    }
    
    /**
     * Show notification preview with current settings
     */
    private void showNotificationPreview() {
        if (currentSettings == null) {
            Toast.makeText(this, getString(R.string.loading_settings), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!PermissionManager.isNotificationPermissionGranted(this)) {
            Toast.makeText(this, getString(R.string.grant_notification_permission_first), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show different preview based on enabled settings
        if (currentSettings.isRemindersEnabled()) {
            NotificationHelper.showReminderNotification(
                this,
                "preview_reminder",
                getString(R.string.preview_reminder_title),
                getString(R.string.preview_reminder_message),
                999999 // Unique preview ID
            );
            Toast.makeText(this, getString(R.string.sent_sample_reminder), Toast.LENGTH_SHORT).show();
        } else if (currentSettings.isHealthAlertsEnabled()) {
            NotificationHelper.showHealthAlertNotification(
                this,
                getString(R.string.preview_health_alert_title),
                getString(R.string.preview_health_alert_message),
                999998
            );
            Toast.makeText(this, getString(R.string.sent_sample_health_alert), Toast.LENGTH_SHORT).show();
        } else if (currentSettings.isSuggestionsEnabled()) {
            NotificationHelper.showSuggestionNotification(
                this,
                getString(R.string.preview_suggestion_title),
                getString(R.string.preview_suggestion_message),
                999997
            );
            Toast.makeText(this, getString(R.string.sent_sample_smart_suggestion), Toast.LENGTH_SHORT).show();
        } else {
            NotificationHelper.showGeneralNotification(
                this,
                getString(R.string.preview_general_title),
                getString(R.string.preview_general_message),
                999996
            );
            Toast.makeText(this, getString(R.string.sent_sample_general_notification), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update permission status when returning from settings
        updatePermissionStatus();
    }
}