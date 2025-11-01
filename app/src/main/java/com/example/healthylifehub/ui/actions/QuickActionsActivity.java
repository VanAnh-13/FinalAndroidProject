package com.example.healthylifehub.ui.actions;

import android.widget.Toast;
import com.bumptech.glide.Glide;
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
        // Load icons from internet using Glide
        loadActionIcons();
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().cardAddMetric.setOnClickListener(v -> {
            Toast.makeText(this, "Add Metric - Coming soon", Toast.LENGTH_SHORT).show();
        });

        getBinding().cardAnalyzeTrends.setOnClickListener(v -> {
            Toast.makeText(this, "Analyze Trends - Coming soon", Toast.LENGTH_SHORT).show();
        });

        getBinding().cardSetReminder.setOnClickListener(v -> {
            Toast.makeText(this, "Set Reminder - Coming soon", Toast.LENGTH_SHORT).show();
        });

        getBinding().cardGenerateReport.setOnClickListener(v -> {
            Toast.makeText(this, "Generate Report - Coming soon", Toast.LENGTH_SHORT).show();
        });

        getBinding().cardStartActivity.setOnClickListener(v -> {
            Toast.makeText(this, "Start Activity - Coming soon", Toast.LENGTH_SHORT).show();
        });

        getBinding().cardScanFood.setOnClickListener(v -> {
            Toast.makeText(this, "Scan Food - Coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadActionIcons() {
        // Load action icons from internet using Glide

        // Add Metric Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/plus-math.png")
                .into(getBinding().ivAddMetricActionIcon);

        // Analyze Trends Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/analytics.png")
                .into(getBinding().ivAnalyzeTrendsIcon);

        // Set Reminder Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/appointment-reminders.png")
                .into(getBinding().ivSetReminderIcon);

        // Generate Report Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/document.png")
                .into(getBinding().ivGenerateReportIcon);

        // Start Activity Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/running.png")
                .into(getBinding().ivStartActivityIcon);

        // Scan Food Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/barcode.png")
                .into(getBinding().ivScanFoodIcon);
    }
}
