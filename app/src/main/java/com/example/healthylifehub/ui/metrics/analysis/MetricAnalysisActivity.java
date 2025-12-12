package com.example.healthylifehub.ui.metrics.analysis;

import android.content.Intent;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMetricAnalysisBinding;
import com.example.healthylifehub.ui.profile.reports.ExportReportsActivity;
import com.example.healthylifehub.data.repository.MetricsRepository;
import com.github.mikephil.charting.data.Entry;
import com.example.healthylifehub.utils.chart.ChartConfigurator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.healthylifehub.ui.metrics.analysis.adapter.AlertsAdapter;
import com.example.healthylifehub.ui.metrics.analysis.adapter.RecommendationsAdapter;
import com.example.healthylifehub.data.model.AlertItem;
import com.example.healthylifehub.data.model.RecommendationItem;
import com.example.healthylifehub.utils.chart.ChartDataProcessor;
import com.example.healthylifehub.data.model.MetricHistory;

public class MetricAnalysisActivity extends BaseActivity<ActivityMetricAnalysisBinding> {
    public static final String EXTRA_METRIC_TYPE = "metric_type";
    private String metricType;
    private String currentPeriod = "month";
    private AlertsAdapter alertsAdapter;
    private RecommendationsAdapter recommendationsAdapter;
    private MetricsRepository metricsRepository;
    private List<MetricHistory> currentMetricData = new ArrayList<>();

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

        metricsRepository = new MetricsRepository(this);

        alertsAdapter = new AlertsAdapter(alert -> {
            Toast.makeText(this, getString(R.string.alert_clicked), Toast.LENGTH_SHORT).show();
        });

        recommendationsAdapter = new RecommendationsAdapter(recommendation -> {
            Toast.makeText(this, getString(R.string.recommendation_clicked), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void bindData() {
        updateTitle();
        setupRecyclerViews();
        loadMetricData();
        loadAlerts();
        loadRecommendations();
        // Default selection via toggle group
        getBinding().periodGroup.check(R.id.btn_month);
    }

    private void updateTitle() {
        String title;
        switch (metricType) {
            case "blood_pressure":
                title = getString(R.string.blood_pressure_analysis);
                break;
            case "blood_sugar":
                title = getString(R.string.blood_sugar_analysis);
                break;
            case "weight":
            case "bmi":
                title = getString(R.string.weight_analysis);
                break;
            case "heart_rate":
            default:
                title = getString(R.string.heart_rate_analysis);
                break;
        }
        getBinding().tvTitle.setText(title);
    }

    private void loadMetricData() {
        // Calculate date range based on current period
        long endDate = System.currentTimeMillis();
        long startDate;
        
        switch (currentPeriod) {
            case "week":
                startDate = endDate - (7L * 24 * 60 * 60 * 1000); // 7 days
                break;
            case "year":
                startDate = endDate - (365L * 24 * 60 * 60 * 1000); // 365 days
                break;
            case "month":
            default:
                startDate = endDate - (30L * 24 * 60 * 60 * 1000); // 30 days
                break;
        }
        
        // Use one-time fetch instead of snapshot listener to avoid multiple observers
        metricsRepository.fetchMetricHistoryByDateRange(metricType, startDate, endDate, data -> {
            runOnUiThread(() -> {
                if (data != null) {
                    currentMetricData = data;
                } else {
                    currentMetricData = new ArrayList<>();
                }
                setupChart();
                loadStatistics();
            });
        });
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().ivFilter.setOnClickListener(v -> showFilterDialog());

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
        if (isBloodPressureMetric()) {
            setupBloodPressureChart(currentMetricData);
            return;
        }
        
        if (currentMetricData == null || currentMetricData.isEmpty()) {
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText(getString(R.string.no_data_available));
            getBinding().lineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < currentMetricData.size(); i++) {
            MetricHistory item = currentMetricData.get(i);
            try {
                float value = Float.parseFloat(item.getValue().replaceAll("[^0-9.]", ""));
                entries.add(new Entry(i, value));
                // Format date shorter for chart display
                String shortDate = formatShortDate(item.getDate());
                labels.add(shortDate);
            } catch (NumberFormatException e) {
                // Skip invalid values
            }
        }

        if (entries.isEmpty()) {
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText(getString(R.string.no_data_available));
            getBinding().lineChart.invalidate();
            return;
        }

        ChartConfigurator.configureLineChart(
            getBinding().lineChart,
            this,
            entries,
            labels,
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
        if (isBloodPressureMetric()) {
            calculateAndDisplayBloodPressureStats(currentMetricData);
        } else {
            calculateAndDisplayStats(currentMetricData);
        }
    }

    private void calculateAndDisplayStats(List<MetricHistory> data) {
        if (data == null || data.isEmpty()) {
            getBinding().tvTrendValue.setText(getString(R.string.not_available));
            getBinding().tvTrendChange.setText(getString(R.string.dash));
            getBinding().tvStatAverage.setText(getString(R.string.not_available));
            getBinding().tvStatHighest.setText(getString(R.string.not_available));
            getBinding().tvStatLowest.setText(getString(R.string.not_available));
            return;
        }

        List<Float> values = new ArrayList<>();
        String unit = "";
        
        for (MetricHistory item : data) {
            try {
                String valueStr = item.getValue();
                float value = Float.parseFloat(valueStr.replaceAll("[^0-9.]", ""));
                values.add(value);
                if (unit.isEmpty() && item.getUnit() != null) {
                    unit = item.getUnit();
                }
            } catch (NumberFormatException e) {
                // Skip invalid values
            }
        }

        if (values.isEmpty()) {
            getBinding().tvTrendValue.setText(getString(R.string.not_available));
            getBinding().tvTrendChange.setText(getString(R.string.dash));
            getBinding().tvStatAverage.setText(getString(R.string.not_available));
            getBinding().tvStatHighest.setText(getString(R.string.not_available));
            getBinding().tvStatLowest.setText(getString(R.string.not_available));
            return;
        }

        float sum = 0, min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        for (float v : values) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float avg = sum / values.size();

        String finalUnit = unit.isEmpty() ? "" : " " + unit;
        getBinding().tvTrendValue.setText(String.format("%.1f%s", avg, finalUnit));
        // Hide trend change since we don't have historical comparison yet
        getBinding().tvTrendChange.setVisibility(android.view.View.GONE);
        getBinding().tvStatAverage.setText(String.format("%.1f%s", avg, finalUnit));
        getBinding().tvStatHighest.setText(String.format("%.1f%s", max, finalUnit));
        getBinding().tvStatLowest.setText(String.format("%.1f%s", min, finalUnit));
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

    /**
     * Check if current metric is blood pressure
     */
    private boolean isBloodPressureMetric() {
        return "blood_pressure".equals(metricType) || "huyết_áp".equals(metricType);
    }

    /**
     * Setup blood pressure chart with dual lines for systolic and diastolic
     */
    private void setupBloodPressureChart(List<MetricHistory> data) {
        if (data == null || data.isEmpty()) {
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText(getString(R.string.no_blood_pressure_data));
            getBinding().lineChart.invalidate();
            return;
        }

        ChartDataProcessor.BloodPressureData bpData = 
            ChartDataProcessor.processBloodPressureData(data, currentPeriod);
        
        if (bpData.isEmpty()) {
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText(getString(R.string.no_valid_blood_pressure_data));
            getBinding().lineChart.invalidate();
            return;
        }

        ChartConfigurator.configureDualLineChart(
            getBinding().lineChart,
            this,
            bpData.systolicEntries,
            bpData.diastolicEntries,
            bpData.labels,
            R.color.error_red,     // Systolic color
            R.color.primary_blue,  // Diastolic color
            R.color.divider
        );
    }

    /**
     * Calculate and display blood pressure statistics
     */
    private void calculateAndDisplayBloodPressureStats(List<MetricHistory> data) {
        if (data == null || data.isEmpty()) {
            getBinding().tvTrendValue.setText(getString(R.string.not_available));
            getBinding().tvTrendChange.setText(getString(R.string.dash));
            getBinding().tvStatAverage.setText(getString(R.string.not_available));
            getBinding().tvStatHighest.setText(getString(R.string.not_available));
            getBinding().tvStatLowest.setText(getString(R.string.not_available));
            return;
        }

        ChartDataProcessor.BloodPressureStats stats = 
            ChartDataProcessor.calculateBloodPressureStatistics(data, currentPeriod);
        
        if (stats.totalReadings == 0) {
            getBinding().tvTrendValue.setText(getString(R.string.not_available));
            getBinding().tvTrendChange.setText(getString(R.string.dash));
            getBinding().tvStatAverage.setText(getString(R.string.not_available));
            getBinding().tvStatHighest.setText(getString(R.string.not_available));
            getBinding().tvStatLowest.setText(getString(R.string.not_available));
            return;
        }

        // Display statistics in systolic/diastolic format
        getBinding().tvTrendValue.setText(String.format("%.0f/%.0f mmHg", 
            stats.avgSystolic, stats.avgDiastolic));
        getBinding().tvTrendChange.setText(stats.trend);
        getBinding().tvStatAverage.setText(String.format("%.0f/%.0f mmHg", 
            stats.avgSystolic, stats.avgDiastolic));
        getBinding().tvStatHighest.setText(String.format("%.0f/%.0f mmHg", 
            stats.maxSystolic, stats.maxDiastolic));
        getBinding().tvStatLowest.setText(String.format("%.0f/%.0f mmHg", 
            stats.minSystolic, stats.minDiastolic));
    }

    /**
     * Format date string to shorter format for chart labels
     * Input: "dd/MM/yyyy HH:mm" -> Output: "dd/MM"
     */
    private String formatShortDate(String fullDate) {
        if (fullDate == null || fullDate.isEmpty()) return "";
        try {
            // Extract just dd/MM from the full date
            String[] parts = fullDate.split(" ");
            if (parts.length > 0) {
                String datePart = parts[0]; // "dd/MM/yyyy"
                String[] dateParts = datePart.split("/");
                if (dateParts.length >= 2) {
                    return dateParts[0] + "/" + dateParts[1]; // "dd/MM"
                }
            }
            return fullDate;
        } catch (Exception e) {
            return fullDate;
        }
    }

    private void showFilterDialog() {
        String[] metricTypes = {
            getString(R.string.heart_rate),
            getString(R.string.blood_pressure),
            getString(R.string.blood_sugar),
            getString(R.string.weight)
        };
        String[] metricValues = {"heart_rate", "blood_pressure", "blood_sugar", "weight"};
        
        int currentIndex = 0;
        for (int i = 0; i < metricValues.length; i++) {
            if (metricValues[i].equals(metricType)) {
                currentIndex = i;
                break;
            }
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_metric_type))
            .setSingleChoiceItems(metricTypes, currentIndex, (dialog, which) -> {
                metricType = metricValues[which];
                updateTitle();
                loadMetricData();
                dialog.dismiss();
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void updatePeriodSelection(String period) {
        currentPeriod = period;

        // Update period label text
        switch (period) {
            case "week":
                getBinding().tvPeriodLabel.setText(getString(R.string.this_week));
                break;
            case "year":
                getBinding().tvPeriodLabel.setText(getString(R.string.this_year));
                break;
            case "month":
            default:
                getBinding().tvPeriodLabel.setText(getString(R.string.this_month));
                break;
        }

        getBinding().btnWeek.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            period.equals("week") ? R.color.primary_blue : android.R.color.transparent));
        getBinding().btnWeek.setTextColor(period.equals("week") ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.text_secondary));

        getBinding().btnMonth.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            period.equals("month") ? R.color.primary_blue : android.R.color.transparent));
        getBinding().btnMonth.setTextColor(period.equals("month") ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.text_secondary));

        getBinding().btnYear.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            period.equals("year") ? R.color.primary_blue : android.R.color.transparent));
        getBinding().btnYear.setTextColor(period.equals("year") ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.text_secondary));
        
        // Reload data with new period
        loadMetricData();
    }
}