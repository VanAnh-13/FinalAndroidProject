package com.example.healthylifehub.ui.metrics.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMetricDetailBinding;
import com.example.healthylifehub.ui.metrics.detail.adapter.MetricHistoryAdapter;
import com.example.healthylifehub.data.repository.MetricsRepository;
import com.github.mikephil.charting.data.Entry;
import com.example.healthylifehub.data.model.MetricHistory;
import com.example.healthylifehub.utils.chart.ChartConfigurator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetricDetailActivity extends BaseActivity<ActivityMetricDetailBinding> {

    public static final String EXTRA_METRIC_TYPE = "metric_type";
    public static final String METRIC_HEART_RATE = "heart_rate";
    public static final String METRIC_BLOOD_PRESSURE = "blood_pressure";
    public static final String METRIC_BLOOD_SUGAR = "blood_sugar";
    public static final String METRIC_BMI = "bmi";

    private String metricType;
    private MetricHistoryAdapter historyAdapter;
    private String currentPeriod = "week";
    private MetricsRepository metricsRepository;

    public MetricDetailActivity() {
        super(ActivityMetricDetailBinding::inflate);
    }

    @Override
    public void initData() {
        // Get metric type from intent
        Intent intent = getIntent();
        metricType = intent.getStringExtra(EXTRA_METRIC_TYPE);
        if (metricType == null) {
            metricType = METRIC_HEART_RATE;
        }

        metricsRepository = new MetricsRepository();

        // Setup RecyclerView
        historyAdapter = new MetricHistoryAdapter(history -> {
            Toast.makeText(this, "Clicked: " + history.getDisplayValue(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void bindData() {
        // Set title based on metric type
        setMetricTitle();

        // Setup chart
        setupChart();

        // Setup RecyclerView
        getBinding().rvHistory.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvHistory.setAdapter(historyAdapter);

        // Load history data
        loadHistoryData();

        // Statistics are calculated after history data is loaded

        // Set initial period selection via toggle group
        getBinding().periodGroup.check(R.id.btn_week);
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().ivMore.setOnClickListener(v -> {
            Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show();
        });

        getBinding().fabAdd.setOnClickListener(v -> {
            Toast.makeText(this, "Add new metric", Toast.LENGTH_SHORT).show();
        });

        // Period selectors - single selection
        getBinding().periodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_day) {
                updatePeriodSelection("day");
            } else if (checkedId == R.id.btn_week) {
                updatePeriodSelection("week");
            } else if (checkedId == R.id.btn_month) {
                updatePeriodSelection("month");
            } else if (checkedId == R.id.btn_year) {
                updatePeriodSelection("year");
            }
        });
    }

    private void setMetricTitle() {
        String title;
        switch (metricType) {
            case METRIC_BLOOD_PRESSURE:
                title = getString(R.string.blood_pressure);
                break;
            case METRIC_BLOOD_SUGAR:
                title = getString(R.string.blood_sugar);
                break;
            case METRIC_BMI:
                title = getString(R.string.bmi);
                break;
            case METRIC_HEART_RATE:
            default:
                title = getString(R.string.heart_rate);
                break;
        }
        getBinding().tvMetricTitle.setText(title);
    }

    private void setupChart() {
        // Sample data for the chart
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 85));
        entries.add(new Entry(1, 78));
        entries.add(new Entry(2, 90));
        entries.add(new Entry(3, 82));
        entries.add(new Entry(4, 88));
        entries.add(new Entry(5, 92));
        entries.add(new Entry(6, 85));

        ChartConfigurator.configureLineChart(
            getBinding().lineChart,
            this,
            entries,
            Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            R.color.primary_blue,
            R.color.border_color
        );
    }

    private void loadHistoryData() {
        // Load history from Firebase
        LiveData<List<MetricHistory>> historyLiveData = metricsRepository.loadMetricHistory(metricType);
        historyLiveData.observe(this, historyList -> {
            if (historyList != null && !historyList.isEmpty()) {
                historyAdapter.setHistoryList(historyList);
                // Update statistics based on loaded data
                calculateAndDisplayStatistics(historyList);
            }
        });
    }

    private void calculateAndDisplayStatistics(List<MetricHistory> historyList) {
        if (historyList.isEmpty()) return;
        
        // Get most recent value
        MetricHistory latest = historyList.get(0);
        getBinding().tvCurrentValue.setText(latest.getDisplayValue());
        
        // For now, display placeholder statistics
        // In a real app, you would calculate average, highest, lowest from the historyList
        getBinding().tvAverageValue.setText("Calculating...");
        getBinding().tvHighestValue.setText("Calculating...");
        getBinding().tvLowestValue.setText("Calculating...");
    }

    private void updatePeriodSelection(String period) {
        currentPeriod = period;

        // Reset all buttons to default state
        resetButtonStyle(getBinding().btnDay);
        resetButtonStyle(getBinding().btnWeek);
        resetButtonStyle(getBinding().btnMonth);
        resetButtonStyle(getBinding().btnYear);

        // Highlight selected button
        switch (period) {
            case "day":
                setSelectedButtonStyle(getBinding().btnDay);
                break;
            case "week":
                setSelectedButtonStyle(getBinding().btnWeek);
                break;
            case "month":
                setSelectedButtonStyle(getBinding().btnMonth);
                break;
            case "year":
                setSelectedButtonStyle(getBinding().btnYear);
                break;
        }

        // Update chart and data based on period
        Toast.makeText(this, "Period: " + period, Toast.LENGTH_SHORT).show();
    }

    private void resetButtonStyle(com.google.android.material.button.MaterialButton button) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void setSelectedButtonStyle(com.google.android.material.button.MaterialButton button) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_blue));
        button.setTextColor(ContextCompat.getColor(this, R.color.white));
    }
}
