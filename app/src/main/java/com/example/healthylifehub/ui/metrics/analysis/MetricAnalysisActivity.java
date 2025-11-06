package com.example.healthylifehub.ui.metrics.analysis;

import android.content.Intent;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMetricAnalysisBinding;
import com.example.healthylifehub.ui.profile.reports.ExportReportsActivity;
import com.github.mikephil.charting.data.Entry;
import com.example.healthylifehub.utils.chart.ChartConfigurator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.healthylifehub.ui.metrics.analysis.adapter.AlertsAdapter;
import com.example.healthylifehub.ui.metrics.analysis.adapter.RecommendationsAdapter;
import com.example.healthylifehub.data.model.AlertItem;
import com.example.healthylifehub.data.model.RecommendationItem;

public class MetricAnalysisActivity extends BaseActivity<ActivityMetricAnalysisBinding> {

    public static final String EXTRA_METRIC_TYPE = "metric_type";
    private String metricType;
    private String currentPeriod = "month";
    private AlertsAdapter alertsAdapter;
    private RecommendationsAdapter recommendationsAdapter;

    public MetricAnalysisActivity() {
        super(ActivityMetricAnalysisBinding::inflate);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        metricType = intent.getStringExtra(EXTRA_METRIC_TYPE);
        if (metricType == null) {
            metricType = "heart_rate";
        }

        alertsAdapter = new AlertsAdapter(alert -> {
            Toast.makeText(this, "Alert clicked", Toast.LENGTH_SHORT).show();
        });

        recommendationsAdapter = new RecommendationsAdapter(recommendation -> {
            Toast.makeText(this, "Recommendation clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void bindData() {
        getBinding().tvTitle.setText(getString(R.string.heart_rate_analysis));
        
        setupChart();
        setupRecyclerViews();
        loadStatistics();
        loadAlerts();
        loadRecommendations();
        // Default selection via toggle group
        getBinding().periodGroup.check(R.id.btn_month);
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().ivFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter options", Toast.LENGTH_SHORT).show();
        });

        getBinding().periodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_week) {
                updatePeriodSelection("week");
            } else if (checkedId == R.id.btn_month) {
                updatePeriodSelection("month");
            } else if (checkedId == R.id.btn_year) {
                updatePeriodSelection("year");
            }
        });

        getBinding().btnExport.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExportReportsActivity.class);
            startActivity(intent);
        });
    }

    private void setupChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 70));
        entries.add(new Entry(1, 75));
        entries.add(new Entry(2, 82));
        entries.add(new Entry(3, 78));
        entries.add(new Entry(4, 85));
        entries.add(new Entry(5, 88));
        entries.add(new Entry(6, 83));
        entries.add(new Entry(7, 90));
        entries.add(new Entry(8, 85));
        entries.add(new Entry(9, 87));

        ChartConfigurator.configureLineChart(
            getBinding().lineChart,
            this,
            entries,
            Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
            R.color.primary_blue,
            R.color.divider
        );
    }

    private void setupRecyclerViews() {
        getBinding().rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvAlerts.setAdapter(alertsAdapter);

        getBinding().rvRecommendations.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvRecommendations.setAdapter(recommendationsAdapter);
    }

    private void loadStatistics() {
        // Load from Firebase (placeholder for now)
        getBinding().tvTrendValue.setText("Loading...");
        getBinding().tvTrendChange.setText("--");
        getBinding().tvStatAverage.setText("Loading...");
        getBinding().tvStatHighest.setText("Loading...");
        getBinding().tvStatLowest.setText("Loading...");
    }

    private void loadAlerts() {
        // Load alerts from Firebase
        // For now, show empty list - will be populated from Firebase
        alertsAdapter.submitList(new ArrayList<>());
    }

    private void loadRecommendations() {
        // Load recommendations from Firebase or AI
        // For now, show empty list - will be populated from Firebase
        recommendationsAdapter.submitList(new ArrayList<>());
    }

    private void updatePeriodSelection(String period) {
        currentPeriod = period;

        getBinding().btnWeek.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            period.equals("week") ? R.color.primary_blue : android.R.color.transparent));
        getBinding().btnWeek.setTextColor(period.equals("week") ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.text_secondary));

        getBinding().btnMonth.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            period.equals("month") ? R.color.primary_blue : android.R.color.transparent));
        getBinding().btnMonth.setTextColor(period.equals("month") ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.text_secondary));

        getBinding().btnYear.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            period.equals("year") ? R.color.primary_blue : android.R.color.transparent));
        getBinding().btnYear.setTextColor(period.equals("year") ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.text_secondary));
        }

    
}