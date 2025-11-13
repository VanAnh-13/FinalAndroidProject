package com.example.healthylifehub.ui.profile.settings;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

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
        getBinding().languageItem.setOnClickListener(v -> {
            // TODO: Show language selection dialog
            showToast(getString(R.string.language));
        });

        // Theme
        getBinding().themeItem.setOnClickListener(v -> {
            // TODO: Show theme selection dialog
            showToast(getString(R.string.theme));
        });

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
}
