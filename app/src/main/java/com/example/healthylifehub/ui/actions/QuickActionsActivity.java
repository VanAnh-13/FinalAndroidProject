package com.example.healthylifehub.ui.actions;

import android.content.Intent;
import android.widget.Toast;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityQuickActionsBinding;

public class QuickActionsActivity extends BaseActivity<ActivityQuickActionsBinding> {

    public QuickActionsActivity() {
        super(ActivityQuickActionsBinding::inflate);
    }

    @Override
    public void initData() {
        // No initialization needed
    }

    @Override
    public void bindData() {
        // Icons are now using Material Symbols text
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().cardAddMetric.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.healthylifehub.ui.metrics.add_edit.AddEditMetricActivity.class);
            startActivity(intent);
        });

        getBinding().cardAnalyzeTrends.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.healthylifehub.ui.metrics.analysis.MetricAnalysisActivity.class);
            intent.putExtra(com.example.healthylifehub.ui.metrics.analysis.MetricAnalysisActivity.EXTRA_METRIC_TYPE, "heart_rate");
            startActivity(intent);
        });

        getBinding().cardSetReminder.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity.class);
            startActivity(intent);
        });

        getBinding().cardGenerateReport.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.healthylifehub.ui.profile.reports.ExportReportsActivity.class);
            startActivity(intent);
        });

        getBinding().cardStartActivity.setOnClickListener(v -> {
            // Navigate to Enhanced Analytics for activity tracking
            Intent intent = new Intent(this, com.example.healthylifehub.ui.analytics.enhanced.EnhancedAnalyticsActivity.class);
            startActivity(intent);
        });

        getBinding().cardScanFood.setOnClickListener(v -> {
            // Navigate to OCR scanning (can be used for food labels)
            Intent intent = new Intent(this, com.example.healthylifehub.ui.medicines.ocr.MedicineOCRActivity.class);
            startActivity(intent);
        });
    }
}
