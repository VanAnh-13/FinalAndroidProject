package com.example.healthylifehub.ui.profile.history;

import android.widget.Toast;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMedicalHistoryBinding;
import com.google.android.material.appbar.MaterialToolbar;

public class MedicalHistoryActivity extends BaseActivity<ActivityMedicalHistoryBinding> {

    public MedicalHistoryActivity() {
        super(ActivityMedicalHistoryBinding::inflate);
    }

    @Override
    public void initData() {
    }

    @Override
    public void bindData() {
        MaterialToolbar toolbar = getBinding().toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public void setOnClick() {
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());

        getBinding().btnSaveHistory.setOnClickListener(v -> {
            String conditions = getBinding().etChronicConditions.getText().toString();
            String allergies = getBinding().etAllergies.getText().toString();
            String medications = getBinding().etCurrentMedications.getText().toString();
            String surgeries = getBinding().etPastSurgeries.getText().toString();
            String familyHistory = getBinding().etFamilyHistory.getText().toString();

            Toast.makeText(this, "Medical history saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
