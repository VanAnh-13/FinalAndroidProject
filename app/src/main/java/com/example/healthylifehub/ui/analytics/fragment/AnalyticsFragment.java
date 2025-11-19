package com.example.healthylifehub.ui.analytics.fragment;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.data.model.AnalyticsData;
import com.example.healthylifehub.databinding.FragmentAnalyticsBinding;
import com.example.healthylifehub.ui.analytics.viewmodel.AnalyticsViewModel;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * AnalyticsFragment - Display health metrics analytics with charts
 * 
 * Features:
 * - Real-time chart updates
 * - Multiple time ranges (7, 30, 90 days)
 * - Cached data display
 * - Statistics display (avg, min, max, trend)
 */
public class AnalyticsFragment extends BaseFragment<FragmentAnalyticsBinding> {
    private static final String TAG = "AnalyticsFragment";
    
    private AnalyticsViewModel viewModel;
    private int currentDayRange = 7;
    
    public AnalyticsFragment() {
        super(FragmentAnalyticsBinding::inflate);
    }
    
    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
    }
    
    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }
    
    @Override
    public void bindData() {
        setupTimeRangeButtons();
        setupRefreshButton();
        
        // Load initial data
        viewModel.loadAnalyticsData(currentDayRange);
    }
    
    @Override
    public void observeData() {
        // Observe analytics data
        viewModel.getAnalyticsData().observe(getViewLifecycleOwner(), analyticsData -> {
            if (analyticsData != null) {
                displayAnalytics(analyticsData);
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                getBinding().progressBar.setVisibility(View.VISIBLE);
            } else {
                getBinding().progressBar.setVisibility(View.GONE);
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe analysis results (Requirements: 4.5)
        // Display Statistics in cards (mean, stdDev, trend)
        // Display anomalies list with severity indicators
        // Show recommendations with priority badges
        viewModel.getAnalysisResult().observe(getViewLifecycleOwner(), analysisResult -> {
            if (analysisResult != null) {
                displayAnalysisResults(analysisResult);
            }
        });
        
        // Observe analysis loading state (Requirements: 4.5)
        viewModel.getAnalysisLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Show/hide analysis loading indicator
            if (isLoading) {
                Toast.makeText(requireContext(), "Đang phân tích dữ liệu...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe anomaly notifications (Requirements: 4.4)
        viewModel.getAnomalyNotificationTrigger().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.hasAnomalies()) {
                displayAnomalyAlert(result);
            }
        });
    }
    
    @Override
    public void setOnClick() {
        // Click listeners are setup in bindData()
    }
    
    /**
     * Setup time range buttons (7, 30, 90 days)
     */
    private void setupTimeRangeButtons() {
        getBinding().btn7Days.setOnClickListener(v -> {
            currentDayRange = 7;
            updateButtonStates();
            viewModel.setTimeRange(7);
        });
        
        getBinding().btn30Days.setOnClickListener(v -> {
            currentDayRange = 30;
            updateButtonStates();
            viewModel.setTimeRange(30);
        });
        
        getBinding().btn90Days.setOnClickListener(v -> {
            currentDayRange = 90;
            updateButtonStates();
            viewModel.setTimeRange(90);
        });
        
        // Set initial button state
        updateButtonStates();
    }
    
    /**
     * Update button states based on selected range
     */
    private void updateButtonStates() {
        boolean is7 = currentDayRange == 7;
        boolean is30 = currentDayRange == 30;
        boolean is90 = currentDayRange == 90;
        
        getBinding().btn7Days.setSelected(is7);
        getBinding().btn30Days.setSelected(is30);
        getBinding().btn90Days.setSelected(is90);
    }
    
    /**
     * Setup refresh button
     */
    private void setupRefreshButton() {
        getBinding().btnRefresh.setOnClickListener(v -> {
            viewModel.refreshAnalyticsData();
            Toast.makeText(requireContext(), "Refreshing data...", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Display analytics data and render charts
     */
    private void displayAnalytics(AnalyticsData analyticsData) {
        // Show cache indicator
        if (analyticsData.isCached) {
            getBinding().tvCacheInfo.setVisibility(View.VISIBLE);
            getBinding().tvCacheInfo.setText("📦 Cached (" + analyticsData.getAgeSeconds() + "s old)");
        } else {
            getBinding().tvCacheInfo.setVisibility(View.GONE);
        }
        
        // Display blood pressure chart
        if (analyticsData.bloodPressure != null && !analyticsData.bloodPressure.dataPoints.isEmpty()) {
            displayBloodPressureChart(analyticsData.bloodPressure);
            displayBloodPressureStats(analyticsData.bloodPressure);
        }
        
        // Display blood sugar chart
        if (analyticsData.bloodSugar != null && !analyticsData.bloodSugar.dataPoints.isEmpty()) {
            displayMetricChart(analyticsData.bloodSugar, getBinding().chartBloodSugar);
            displayMetricStats(analyticsData.bloodSugar, getBinding().tvBloodSugarStats);
        }
        
        // Display weight chart
        if (analyticsData.weight != null && !analyticsData.weight.dataPoints.isEmpty()) {
            displayMetricChart(analyticsData.weight, getBinding().chartWeight);
            displayMetricStats(analyticsData.weight, getBinding().tvWeightStats);
        }
        
        // Display heart rate chart
        if (analyticsData.heartRate != null && !analyticsData.heartRate.dataPoints.isEmpty()) {
            displayMetricChart(analyticsData.heartRate, getBinding().chartHeartRate);
            displayMetricStats(analyticsData.heartRate, getBinding().tvHeartRateStats);
        }
    }
    
    /**
     * Display blood pressure chart (systolic/diastolic)
     */
    private void displayBloodPressureChart(AnalyticsData.MetricStatistics stats) {
        LineChart chart = getBinding().chartBloodPressure;
        
        List<Entry> systolicEntries = new ArrayList<>();
        List<Entry> diastolicEntries = new ArrayList<>();
        
        for (int i = 0; i < stats.dataPoints.size(); i++) {
            AnalyticsData.DataPoint point = stats.dataPoints.get(i);
            systolicEntries.add(new Entry(i, (float) point.systolic));
            diastolicEntries.add(new Entry(i, (float) point.diastolic));
        }
        
        // Systolic line (red)
        LineDataSet systolicSet = new LineDataSet(systolicEntries, "Systolic");
        systolicSet.setColor(Color.RED);
        systolicSet.setLineWidth(2);
        systolicSet.setCircleRadius(3);
        systolicSet.setDrawCircles(true);
        
        // Diastolic line (blue)
        LineDataSet diastolicSet = new LineDataSet(diastolicEntries, "Diastolic");
        diastolicSet.setColor(Color.BLUE);
        diastolicSet.setLineWidth(2);
        diastolicSet.setCircleRadius(3);
        diastolicSet.setDrawCircles(true);
        
        LineData lineData = new LineData(systolicSet, diastolicSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }
    
    /**
     * Display generic metric chart
     */
    private void displayMetricChart(AnalyticsData.MetricStatistics stats, LineChart chart) {
        List<Entry> entries = new ArrayList<>();
        
        for (int i = 0; i < stats.dataPoints.size(); i++) {
            AnalyticsData.DataPoint point = stats.dataPoints.get(i);
            entries.add(new Entry(i, (float) point.value));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, stats.metricType);
        dataSet.setColor(Color.GREEN);
        dataSet.setLineWidth(2);
        dataSet.setCircleRadius(3);
        dataSet.setDrawCircles(true);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }
    
    /**
     * Display blood pressure statistics
     */
    private void displayBloodPressureStats(AnalyticsData.MetricStatistics stats) {
        String statsText = String.format(
            "Avg: %.0f/%.0f | Min: %.0f | Max: %.0f | Trend: %+.1f",
            stats.avgSystolic, stats.avgDiastolic, stats.minSystolic, stats.maxSystolic, stats.trend
        );
        getBinding().tvBloodPressureStats.setText(statsText);
    }
    
    /**
     * Display metric statistics
     */
    private void displayMetricStats(AnalyticsData.MetricStatistics stats, TextView textView) {
        String trendIcon = stats.trend > 0 ? "📈" : stats.trend < 0 ? "📉" : "➡️";
        String statsText = String.format(
            "Avg: %.1f | Min: %.1f | Max: %.1f | Trend: %s %+.1f %s",
            stats.average, stats.minimum, stats.maximum, trendIcon, stats.trend, stats.unit
        );
        textView.setText(statsText);
    }
    
    /**
     * Display analysis results from HealthMetricsAnalyzer
     * 
     * Shows:
     * - Statistics in cards (mean, stdDev, trend)
     * - Anomalies list with severity indicators
     * - Recommendations with priority badges
     * 
     * Requirements: 4.5
     */
    private void displayAnalysisResults(com.example.healthylifehub.data.model.AnalysisResult analysisResult) {
        if (analysisResult == null) {
            return;
        }
        
        // Display Statistics
        if (analysisResult.getStatistics() != null) {
            com.example.healthylifehub.data.model.Statistics stats = analysisResult.getStatistics();
            String statsText = String.format(
                "📊 Statistics:\n" +
                "Mean: %.2f | StdDev: %.2f\n" +
                "Min: %.2f | Max: %.2f | Median: %.2f\n" +
                "Trend: %s",
                stats.getMean(), stats.getStdDev(),
                stats.getMin(), stats.getMax(), stats.getMedian(),
                stats.getTrend()
            );
            
            // Display in a toast or update a TextView if available
            Toast.makeText(requireContext(), statsText, Toast.LENGTH_LONG).show();
        }
        
        // Display Anomalies
        if (analysisResult.hasAnomalies()) {
            StringBuilder anomaliesText = new StringBuilder("🚨 Anomalies Detected:\n");
            for (com.example.healthylifehub.data.model.Anomaly anomaly : analysisResult.getAnomalies()) {
                String severityIcon = getSeverityIcon(anomaly.getSeverity());
                anomaliesText.append(String.format(
                    "%s %s (Value: %.2f, Deviation: %.2f)\n",
                    severityIcon, anomaly.getMessage(), anomaly.getValue(), anomaly.getDeviation()
                ));
            }
            Toast.makeText(requireContext(), anomaliesText.toString(), Toast.LENGTH_LONG).show();
        }
        
        // Display Recommendations
        if (analysisResult.hasRecommendations()) {
            StringBuilder recommendationsText = new StringBuilder("💡 Recommendations:\n");
            for (com.example.healthylifehub.data.model.Recommendation recommendation : analysisResult.getRecommendations()) {
                String priorityBadge = getPriorityBadge(recommendation.getPriority());
                recommendationsText.append(String.format(
                    "%s [%s] %s: %s\n",
                    priorityBadge, recommendation.getType(), 
                    recommendation.getTitle(), recommendation.getDescription()
                ));
            }
            Toast.makeText(requireContext(), recommendationsText.toString(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Display anomaly alert dialog
     * 
     * Requirements: 4.4
     */
    private void displayAnomalyAlert(com.example.healthylifehub.data.model.AnalysisResult result) {
        if (result == null || !result.hasAnomalies()) {
            return;
        }
        
        // Build alert message
        StringBuilder message = new StringBuilder();
        message.append("Phát hiện ").append(result.getAnomalyCount()).append(" bất thường trong dữ liệu sức khỏe của bạn:\n\n");
        
        for (com.example.healthylifehub.data.model.Anomaly anomaly : result.getAnomalies()) {
            String severityIcon = getSeverityIcon(anomaly.getSeverity());
            message.append(severityIcon).append(" ").append(anomaly.getMessage()).append("\n");
        }
        
        // Show alert dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Cảnh báo sức khỏe")
            .setMessage(message.toString())
            .setPositiveButton("Xem chi tiết", (dialog, which) -> {
                // Show detailed analysis results
                displayAnalysisResults(result);
            })
            .setNegativeButton("Đóng", null)
            .show();
    }
    
    /**
     * Get severity icon based on severity level
     */
    private String getSeverityIcon(String severity) {
        if (severity == null) return "ℹ️";
        
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return "🔴";
            case "HIGH":
                return "🟠";
            case "MEDIUM":
                return "🟡";
            case "LOW":
                return "🟢";
            default:
                return "ℹ️";
        }
    }
    
    /**
     * Get priority badge based on priority level
     */
    private String getPriorityBadge(String priority) {
        if (priority == null) return "📌";
        
        switch (priority.toUpperCase()) {
            case "HIGH":
                return "🔥";
            case "MEDIUM":
                return "⚡";
            case "LOW":
                return "📌";
            default:
                return "📌";
        }
    }
}
