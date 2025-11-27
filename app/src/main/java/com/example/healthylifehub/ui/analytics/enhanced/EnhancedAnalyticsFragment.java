package com.example.healthylifehub.ui.analytics.enhanced;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentEnhancedAnalyticsBinding;
import com.example.healthylifehub.utils.chart.ChartDataProcessor;
import com.example.healthylifehub.data.cache.ChartDataCacheManager;
import com.example.healthylifehub.data.model.HealthMetric;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Analytics Fragment with Period-based Charts
 * Supports Day, Week, Month, Year views with proper data aggregation
 */
public class EnhancedAnalyticsFragment extends BaseFragment<FragmentEnhancedAnalyticsBinding> {
    
    private static final String TAG = "EnhancedAnalytics";
    private EnhancedAnalyticsViewModel viewModel;
    
    // Current period selection
    private String currentPeriod = "week"; // default
    private String currentMetricType = "blood_pressure"; // default
    
    public EnhancedAnalyticsFragment() {
        super(FragmentEnhancedAnalyticsBinding::inflate);
    }
    
    @Override
    protected BaseViewModel getViewModel() {
        viewModel = new ViewModelProvider(this).get(EnhancedAnalyticsViewModel.class);
        return viewModel;
    }
    
    @Override
    public void initData() {
        // Load initial data
        viewModel.loadMetricData(currentMetricType, currentPeriod);
    }
    
    @Override
    public void bindData() {
        // Setup chart styling
        setupChartStyles();
        
        // Setup period indicators
        updatePeriodButtons();
        updateMetricButtons();
    }
    
    @Override
    public void observeData() {
        // Observe metric data changes
        viewModel.getMetricData().observe(this, healthMetrics -> {
            if (healthMetrics != null && !healthMetrics.isEmpty()) {
                updateCharts(healthMetrics);
            } else {
                showEmptyState();
            }
        });
        
        // Observe loading state
        viewModel.getLoading().observe(this, isLoading -> {
            getBinding().progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
    
    @Override
    public void setOnClick() {
        // Period selection buttons
        getBinding().btnDay.setOnClickListener(v -> {
            currentPeriod = "day";
            updatePeriodButtons();
            reloadData();
        });
        
        getBinding().btnWeek.setOnClickListener(v -> {
            currentPeriod = "week";
            updatePeriodButtons();
            reloadData();
        });
        
        getBinding().btnMonth.setOnClickListener(v -> {
            currentPeriod = "month";
            updatePeriodButtons();
            reloadData();
        });
        
        getBinding().btnYear.setOnClickListener(v -> {
            currentPeriod = "year";
            updatePeriodButtons();
            reloadData();
        });
        
        // Metric type selection
        getBinding().btnBloodPressure.setOnClickListener(v -> {
            currentMetricType = "blood_pressure";
            updateMetricButtons();
            reloadData();
        });
        
        getBinding().btnHeartRate.setOnClickListener(v -> {
            currentMetricType = "heart_rate";
            updateMetricButtons();
            reloadData();
        });
        
        getBinding().btnWeight.setOnClickListener(v -> {
            currentMetricType = "weight";
            updateMetricButtons();
            reloadData();
        });
        
        getBinding().btnBloodSugar.setOnClickListener(v -> {
            currentMetricType = "blood_sugar";
            updateMetricButtons();
            reloadData();
        });
        
        // Refresh button
        getBinding().btnRefresh.setOnClickListener(v -> {
            viewModel.clearCache();
            reloadData();
        });
    }
    
    /**
     * Setup professional chart styling
     */
    private void setupChartStyles() {
        LineChart chart = getBinding().chartMain;
        
        // General chart settings
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface)); // Theme-aware background
        
        // X-Axis styling
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.chart_grid)); // Theme-aware grid
        xAxis.setGridLineWidth(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.chart_text)); // Theme-aware text
        xAxis.setTextSize(12f);
        xAxis.setAvoidFirstLastClipping(true);
        
        // Y-Axis styling
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.chart_grid)); // Theme-aware grid
        leftAxis.setGridLineWidth(1f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.chart_text)); // Theme-aware text
        leftAxis.setTextSize(12f);
        leftAxis.setSpaceTop(10f);
        leftAxis.setSpaceBottom(10f);
        
        // Disable right axis
        chart.getAxisRight().setEnabled(false);
        
        // Legend styling
        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(14f);
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.chart_text)); // Theme-aware text
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setYOffset(10f);
    }
    
    /**
     * Update charts with new data
     */
    private void updateCharts(List<HealthMetric> healthMetrics) {
        LineChart chart = getBinding().chartMain;
        
        // Process data using ChartDataProcessor - convert HealthMetric to MetricHistory
        List<com.example.healthylifehub.data.model.MetricHistory> metricHistory = convertToMetricHistory(healthMetrics);
        ChartDataCacheManager.ChartDataCache chartData = 
            ChartDataProcessor.processMetricData(currentMetricType, metricHistory, currentPeriod);
        
        if (chartData.getEntries().isEmpty()) {
            showEmptyState();
            return;
        }
        
        List<LineDataSet> dataSets = new ArrayList<>();
        
        if ("blood_pressure".equals(currentMetricType)) {
            // Create dual-line chart for blood pressure
            createBloodPressureChart(healthMetrics, chartData, dataSets);
        } else {
            // Create single-line chart for other metrics
            createSingleMetricChart(chartData, dataSets);
        }
        
        // Setup line data - convert to ILineDataSet
        List<com.github.mikephil.charting.interfaces.datasets.ILineDataSet> iDataSets = new ArrayList<>();
        for (LineDataSet dataSet : dataSets) {
            iDataSets.add(dataSet);
        }
        LineData lineData = new LineData(iDataSets);
        lineData.setValueTextColor(Color.DKGRAY);
        lineData.setValueTextSize(10f);
        
        // Apply data to chart
        chart.setData(lineData);
        
        // Setup X-axis labels
        setupXAxisLabels(chart, chartData.getLabels());
        
        // Setup Y-axis formatting
        setupYAxisFormatting(chart);
        
        // Animate and refresh
        chart.animateX(800);
        chart.invalidate();
        
        // Update statistics
        updateStatistics(healthMetrics);
        
        // Show chart, hide empty state
        getBinding().chartMain.setVisibility(View.VISIBLE);
        getBinding().emptyState.setVisibility(View.GONE);
    }
    
    /**
     * Convert HealthMetric to MetricHistory for ChartDataProcessor compatibility
     */
    private List<com.example.healthylifehub.data.model.MetricHistory> convertToMetricHistory(List<HealthMetric> healthMetrics) {
        List<com.example.healthylifehub.data.model.MetricHistory> metricHistories = new ArrayList<>();
        
        for (HealthMetric metric : healthMetrics) {
            com.example.healthylifehub.data.model.MetricHistory history = 
                new com.example.healthylifehub.data.model.MetricHistory();
            
            // Convert basic fields
            if (metric.getMeasuredAt() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", 
                    java.util.Locale.getDefault());
                history.setDate(sdf.format(metric.getMeasuredAt()));
            }
            
            // Handle different metric types
            if ("blood_pressure".equals(metric.getType())) {
                history.setValue(String.format("%.0f/%.0f", metric.getSystolic(), metric.getDiastolic()));
            } else {
                history.setValue(String.valueOf(metric.getValue()));
            }
            
            metricHistories.add(history);
        }
        
        return metricHistories;
    }
    
    /**
     * Create blood pressure chart (systolic/diastolic lines)
     */
    private void createBloodPressureChart(List<HealthMetric> healthMetrics, 
                                        ChartDataCacheManager.ChartDataCache chartData, 
                                        List<LineDataSet> dataSets) {
        
        // Convert and get diastolic values
        List<com.example.healthylifehub.data.model.MetricHistory> metricHistory = convertToMetricHistory(healthMetrics);
        List<Entry> diastolicEntries = ChartDataProcessor.processDiastolicValues(metricHistory, currentPeriod);
        
        // Systolic line (Red)
        LineDataSet systolicSet = new LineDataSet(chartData.getEntries(), "Tâm thu");
        systolicSet.setColor(getResources().getColor(R.color.error_red, null));
        systolicSet.setLineWidth(3f);
        systolicSet.setCircleRadius(4f);
        systolicSet.setCircleColor(getResources().getColor(R.color.error_red, null));
        systolicSet.setDrawCircleHole(false);
        systolicSet.setDrawValues(false);
        systolicSet.setDrawFilled(false);
        systolicSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        systolicSet.setCubicIntensity(0.1f);
        
        // Diastolic line (Blue)
        LineDataSet diastolicSet = new LineDataSet(diastolicEntries, "Tâm trương");
        diastolicSet.setColor(getResources().getColor(R.color.primary, null));
        diastolicSet.setLineWidth(3f);
        diastolicSet.setCircleRadius(4f);
        diastolicSet.setCircleColor(getResources().getColor(R.color.primary, null));
        diastolicSet.setDrawCircleHole(false);
        diastolicSet.setDrawValues(false);
        diastolicSet.setDrawFilled(false);
        diastolicSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        diastolicSet.setCubicIntensity(0.1f);
        
        dataSets.add(systolicSet);
        dataSets.add(diastolicSet);
    }
    
    /**
     * Create single metric chart
     */
    private void createSingleMetricChart(ChartDataCacheManager.ChartDataCache chartData, List<LineDataSet> dataSets) {
        String label = getMetricLabel(currentMetricType);
        int color = getMetricColor(currentMetricType);
        
        LineDataSet dataSet = new LineDataSet(chartData.getEntries(), label);
        dataSet.setColor(color);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(color);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        
        dataSets.add(dataSet);
    }
    
    /**
     * Setup X-axis labels based on period
     */
    private void setupXAxisLabels(LineChart chart, List<String> labels) {
        XAxis xAxis = chart.getXAxis();
        
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        
        // Adjust label count based on period
        switch (currentPeriod) {
            case "day":
                xAxis.setLabelCount(Math.min(6, labels.size()), true);
                break;
            case "week":
                xAxis.setLabelCount(7, true);
                break;
            case "month":
                xAxis.setLabelCount(5, true);
                break;
            case "year":
                xAxis.setLabelCount(6, true);
                break;
        }
    }
    
    /**
     * Setup Y-axis formatting with units
     */
    private void setupYAxisFormatting(LineChart chart) {
        YAxis leftAxis = chart.getAxisLeft();
        String unit = getMetricUnit(currentMetricType);
        
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.format("%.0f%s", value, unit);
            }
        });
    }
    
    /**
     * Update statistics display
     */
    private void updateStatistics(List<HealthMetric> healthMetrics) {
        if (healthMetrics.isEmpty()) return;
        
        double sum = 0, min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        int count = 0;
        
        for (HealthMetric metric : healthMetrics) {
            double value;
            if ("blood_pressure".equals(currentMetricType)) {
                value = metric.getSystolic(); // Use systolic for BP stats
            } else {
                value = metric.getValue();
            }
            
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
            count++;
        }
        
        double avg = sum / count;
        String unit = getMetricUnit(currentMetricType);
        
        String statsText = String.format("TB: %.1f%s | Min: %.1f%s | Max: %.1f%s | Số lần đo: %d", 
                                        avg, unit, min, unit, max, unit, count);
        
        getBinding().tvStatistics.setText(statsText);
        getBinding().tvStatistics.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show empty state when no data
     */
    private void showEmptyState() {
        getBinding().chartMain.setVisibility(View.GONE);
        getBinding().emptyState.setVisibility(View.VISIBLE);
        getBinding().tvStatistics.setVisibility(View.GONE);
        
        String emptyMessage = String.format("Chưa có dữ liệu %s cho %s", 
                                           getMetricLabel(currentMetricType).toLowerCase(),
                                           getPeriodLabel(currentPeriod));
        getBinding().tvEmptyMessage.setText(emptyMessage);
    }
    
    /**
     * Update period button states
     */
    private void updatePeriodButtons() {
        // Reset all buttons
        getBinding().btnDay.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        getBinding().btnWeek.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        getBinding().btnMonth.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        getBinding().btnYear.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        
        // Highlight selected button
        switch (currentPeriod) {
            case "day":
                getBinding().btnDay.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
            case "week":
                getBinding().btnWeek.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
            case "month":
                getBinding().btnMonth.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
            case "year":
                getBinding().btnYear.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
        }
    }
    
    /**
     * Update metric button states
     */
    private void updateMetricButtons() {
        // Reset all buttons
        getBinding().btnBloodPressure.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        getBinding().btnHeartRate.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        getBinding().btnWeight.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        getBinding().btnBloodSugar.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected, null));
        
        // Highlight selected button
        switch (currentMetricType) {
            case "blood_pressure":
                getBinding().btnBloodPressure.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
            case "heart_rate":
                getBinding().btnHeartRate.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
            case "weight":
                getBinding().btnWeight.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
            case "blood_sugar":
                getBinding().btnBloodSugar.setBackgroundTintList(getResources().getColorStateList(R.color.primary, null));
                break;
        }
    }
    
    /**
     * Reload data with current settings
     */
    private void reloadData() {
        viewModel.loadMetricData(currentMetricType, currentPeriod);
    }
    
    // Helper methods
    private String getMetricLabel(String metricType) {
        switch (metricType) {
            case "blood_pressure": return "Huyết áp";
            case "heart_rate": return "Nhịp tim";
            case "weight": return "Cân nặng";
            case "blood_sugar": return "Đường huyết";
            default: return "Chỉ số";
        }
    }
    
    private String getMetricUnit(String metricType) {
        switch (metricType) {
            case "blood_pressure": return " mmHg";
            case "heart_rate": return " bpm";
            case "weight": return " kg";
            case "blood_sugar": return " mg/dL";
            default: return "";
        }
    }
    
    private int getMetricColor(String metricType) {
        switch (metricType) {
            case "heart_rate": return getResources().getColor(R.color.error_red, null);
            case "weight": return getResources().getColor(R.color.success_green, null);
            case "blood_sugar": return getResources().getColor(R.color.warning_orange, null);
            default: return getResources().getColor(R.color.primary, null);
        }
    }
    
    private String getPeriodLabel(String period) {
        switch (period) {
            case "day": return "ngày";
            case "week": return "tuần";
            case "month": return "tháng";
            case "year": return "năm";
            default: return "khoảng thời gian";
        }
    }
}