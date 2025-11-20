package com.example.healthylifehub.ui.profile.settings;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.healthylifehub.HealthyLifeHubApplication;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivitySettingsBinding;
import com.example.healthylifehub.ui.settings.NotificationSettingsActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends BaseActivity<ActivitySettingsBinding> {

    public SettingsActivity() {
        super(ActivitySettingsBinding::inflate);
    }

    @Override
    public void initData() {
        // Initialize any data or view models here if needed
    }

    @Override
    public void bindData() {
        // Setup toolbar
        MaterialToolbar toolbar = getBinding().toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Update current theme subtitle
        updateThemeSubtitle();
        
        // Update current language subtitle
        updateLanguageSubtitle();
    }

    @Override
    public void setOnClick() {
        // Toolbar back button
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());

        // Account section
        getBinding().accountItem.setOnClickListener(v -> {
            // TODO: Navigate to Account screen
            showToast(getString(R.string.account));
        });

        // Notifications
        getBinding().notificationsItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationSettingsActivity.class);
            startActivity(intent);
        });

        // Data Sync
        getBinding().dataSyncItem.setOnClickListener(v -> {
            // TODO: Navigate to Data Sync settings
            showToast(getString(R.string.data_sync));
        });

        // Units
        getBinding().unitsItem.setOnClickListener(v -> {
            // TODO: Show units selection dialog
            showToast(getString(R.string.units));
        });

        // Language
        getBinding().languageItem.setOnClickListener(v -> showLanguageDialog());

        // Theme
        getBinding().themeItem.setOnClickListener(v -> showThemeDialog());

        // About
        getBinding().aboutItem.setOnClickListener(v -> {
            // TODO: Navigate to About screen
            showToast(getString(R.string.about));
        });

        // Logout
        getBinding().logoutItem.setOnClickListener(v -> {
            // TODO: Show logout confirmation dialog
            showToast(getString(R.string.logout));
        });

        // Delete Account
        getBinding().deleteAccountItem.setOnClickListener(v -> {
            // TODO: Show delete account confirmation dialog
            showToast(getString(R.string.delete_account));
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show theme selection dialog
     */
    private void showThemeDialog() {
        String[] themes = {
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        };
        
        int currentTheme = HealthyLifeHubApplication.getThemeMode(this);
        int selectedIndex = 0;
        
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            selectedIndex = 0;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            selectedIndex = 1;
        } else {
            selectedIndex = 2;
        }
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.theme)
            .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                int mode;
                switch (which) {
                    case 0:
                        mode = AppCompatDelegate.MODE_NIGHT_NO;
                        break;
                    case 1:
                        mode = AppCompatDelegate.MODE_NIGHT_YES;
                        break;
                    case 2:
                    default:
                        mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        break;
                }
                
                HealthyLifeHubApplication.setThemeMode(this, mode);
                updateThemeSubtitle();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show language selection dialog
     */
    private void showLanguageDialog() {
        String[] languages = {
            "English",
            "Tiếng Việt",
            getString(R.string.theme_system)
        };
        
        String currentLang = HealthyLifeHubApplication.getLanguage(this);
        int selectedIndex = 0;
        
        if ("en".equals(currentLang)) {
            selectedIndex = 0;
        } else if ("vi".equals(currentLang)) {
            selectedIndex = 1;
        } else {
            selectedIndex = 2;
        }
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                String langCode;
                switch (which) {
                    case 0:
                        langCode = "en";
                        break;
                    case 1:
                        langCode = "vi";
                        break;
                    case 2:
                    default:
                        langCode = "system";
                        break;
                }
                
                // Save language preference
                HealthyLifeHubApplication.setLanguage(this, langCode);
                
                // Restart app to apply language change
                Intent intent = new Intent(this, com.example.healthylifehub.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Update theme subtitle based on current setting
     */
    private void updateThemeSubtitle() {
        int currentTheme = HealthyLifeHubApplication.getThemeMode(this);
        String subtitle;
        
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            subtitle = getString(R.string.theme_light);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            subtitle = getString(R.string.theme_dark);
        } else {
            subtitle = getString(R.string.theme_system);
        }
        
        getBinding().themeSubtitle.setText(subtitle);
    }
    
    /**
     * Update language subtitle based on current setting
     */
    private void updateLanguageSubtitle() {
        String currentLang = HealthyLifeHubApplication.getLanguage(this);
        String subtitle;
        
        if ("en".equals(currentLang)) {
            subtitle = "English";
        } else if ("vi".equals(currentLang)) {
            subtitle = "Tiếng Việt";
        } else {
            subtitle = getString(R.string.theme_system);
        }
        
        getBinding().languageSubtitle.setText(subtitle);
    }
}
