package com.example.healthylifehub.ui.profile.history;

import android.widget.Toast;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMedicalHistoryBinding;
import com.example.healthylifehub.data.repository.UserRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Activity for editing medical history
 * Single field for free-text medical history (as per Project_Sumary.md)
 */
public class MedicalHistoryActivity extends BaseActivity<ActivityMedicalHistoryBinding> {

    private UserRepository repository;
    private boolean isSaving = false;

    public MedicalHistoryActivity() {
        super(ActivityMedicalHistoryBinding::inflate);
    }

    @Override
    public void initData() {
        repository = new UserRepository(this);
        loadMedicalHistory();
    }

    @Override
    public void bindData() {
        MaterialToolbar toolbar = getBinding().toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Tiền sử bệnh án");
        }
    }

    @Override
    public void setOnClick() {
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
        getBinding().btnSaveHistory.setOnClickListener(v -> saveMedicalHistory());
    }
    
    /**
     * Load existing medical history data
     */
    private void loadMedicalHistory() {
        repository.loadMedicalHistory().observe(this, historyText -> {
            if (historyText != null && !historyText.isEmpty()) {
                getBinding().etMedicalHistory.setText(historyText);
            }
        });
    }
    
    /**
     * Save medical history as single text field
     */
    private void saveMedicalHistory() {
        if (isSaving) return;
        
        String medicalHistory = getBinding().etMedicalHistory.getText().toString().trim();
        
        isSaving = true;
        getBinding().btnSaveHistory.setEnabled(false);
        getBinding().btnSaveHistory.setText("Đang lưu...");
        
        repository.updateMedicalHistory(medicalHistory)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    isSaving = false;
                    getBinding().btnSaveHistory.setEnabled(true);
                    getBinding().btnSaveHistory.setText("Lưu");
                    
                    if (success) {
                        Toast.makeText(this, getString(R.string.toast_medical_history_saved), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showErrorDialog("Lỗi khi lưu tiền sử bệnh án. Vui lòng thử lại.");
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    isSaving = false;
                    getBinding().btnSaveHistory.setEnabled(true);
                    getBinding().btnSaveHistory.setText("Lưu");
                    showErrorDialog("Lỗi: " + throwable.getMessage());
                });
                return null;
            });
    }
    
    /**
     * Show error dialog
     */
    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Lỗi")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
}
