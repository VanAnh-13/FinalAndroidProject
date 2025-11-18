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
import com.example.healthylifehub.utils.chart.ChartDataProcessor;
import com.example.healthylifehub.data.model.MetricHistory;

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
            Toast.makeText(this, getString(R.string.alert_clicked), Toast.LENGTH_SHORT).show();
        });

        recommendationsAdapter = new RecommendationsAdapter(recommendation -> {
            Toast.makeText(this, getString(R.string.recommendation_clicked), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, getString(R.string.filter_options), Toast.LENGTH_SHORT).show();
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
        // Check if this is blood pressure metric and handle accordingly
        if (isBloodPressureMetric()) {
            // Load blood pressure data and setup dual-line chart
            List<MetricHistory> sampleData = createSampleBloodPressureData();
            setupBloodPressureChart(sampleData);
            return;
        }
        
        // Default chart setup for other metrics
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
        if (isBloodPressureMetric()) {
            // TODO: Load actual blood pressure data from repository
            // For now, create sample data for testing
            List<MetricHistory> sampleData = createSampleBloodPressureData();
            calculateAndDisplayBloodPressureStats(sampleData);
        } else {
            // Load from Firebase (placeholder for now)
            getBinding().tvTrendValue.setText(getString(R.string.loading_data_ellipsis));
            getBinding().tvTrendChange.setText(getString(R.string.dash));
            getBinding().tvStatAverage.setText(getString(R.string.loading_data_ellipsis));
            getBinding().tvStatHighest.setText(getString(R.string.loading_data_ellipsis));
            getBinding().tvStatLowest.setText(getString(R.string.loading_data_ellipsis));
        }
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
        
        // Reload chart and statistics when period changes
        if (isBloodPressureMetric()) {
            // TODO: Load actual blood pressure data from repository
            // For now, create sample data for testing
            List<MetricHistory> sampleData = createSampleBloodPressureData();
            setupBloodPressureChart(sampleData);
            calculateAndDisplayBloodPressureStats(sampleData);
        } else {
            setupChart(); // Use existing method for other metrics
            loadStatistics();
        }
    }

    /**
     * Create sample blood pressure data for testing
     * TODO: Replace with actual data loading from repository
     */
    private List<MetricHistory> createSampleBloodPressureData() {
        List<MetricHistory> sampleData = new ArrayList<>();
        
        // Sample blood pressure readings in format "systolic/diastolic"
        String[] readings = {"120/80", "125/82", "118/78", "130/85", "122/79", "128/83", "115/75", "135/90", "110/70", "140/95"};
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        
        // Create data for different periods based on currentPeriod
        int dataPoints = 0;
        int dayOffset = 0;
        
        switch (currentPeriod) {
            case "day":
                dataPoints = 5; // 5 readings today
                dayOffset = 0;
                break;
            case "week":
                dataPoints = 7; // 7 days
                dayOffset = 1;
                break;
            case "month":
                dataPoints = 10; // 10 data points over 30 days
                dayOffset = 3;
                break;
            case "year":
                dataPoints = 12; // 12 months
                dayOffset = 30;
                break;
            default:
                dataPoints = 7;
                dayOffset = 1;
        }
        
        for (int i = 0; i < Math.min(dataPoints, readings.length); i++) {
            MetricHistory history = new MetricHistory();
            history.setValue(readings[i]);
            history.setUnit("mmHg");
            
            // Set dates based on period
            cal = java.util.Calendar.getInstance();
            if (currentPeriod.equals("day")) {
                // For day view, create readings throughout today
                cal.add(java.util.Calendar.HOUR_OF_DAY, -(dataPoints - 1 - i) * 2);
            } else {
                // For other periods, spread data over time
                cal.add(java.util.Calendar.DAY_OF_YEAR, -(dataPoints - 1 - i) * dayOffset);
            }
            
            history.setDate(sdf.format(cal.getTime()));
            sampleData.add(history);
            
            // Debug logging
            android.util.Log.d("BloodPressure", "Created sample data: " + history.getValue() + 
                " at " + history.getDate() + " (systolic: " + history.getSystolic() + 
                ", diastolic: " + history.getDiastolic() + ")");
        }
        
        android.util.Log.d("BloodPressure", "Created " + sampleData.size() + " sample records for period: " + currentPeriod);
        return sampleData;
    }

    
}