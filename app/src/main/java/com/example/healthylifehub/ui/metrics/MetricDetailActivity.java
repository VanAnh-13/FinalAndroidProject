package com.example.healthylifehub.ui.metrics;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMetricDetailBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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

        // Load statistics
        loadStatistics();

        // Set initial period selection
        updatePeriodSelection("week");
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

        // Period selectors
        getBinding().btnDay.setOnClickListener(v -> updatePeriodSelection("day"));
        getBinding().btnWeek.setOnClickListener(v -> updatePeriodSelection("week"));
        getBinding().btnMonth.setOnClickListener(v -> updatePeriodSelection("month"));
        getBinding().btnYear.setOnClickListener(v -> updatePeriodSelection("year"));
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

        LineDataSet dataSet = new LineDataSet(entries, "Heart Rate");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        getBinding().lineChart.setData(lineData);

        // Customize chart appearance
        getBinding().lineChart.getDescription().setEnabled(false);
        getBinding().lineChart.getLegend().setEnabled(false);
        getBinding().lineChart.setTouchEnabled(true);
        getBinding().lineChart.setDragEnabled(true);
        getBinding().lineChart.setScaleEnabled(false);
        getBinding().lineChart.setPinchZoom(false);
        getBinding().lineChart.setDrawGridBackground(false);

        // X-axis configuration
        XAxis xAxis = getBinding().lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        ));

        // Y-axis configuration
        YAxis leftAxis = getBinding().lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.border_color));
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        YAxis rightAxis = getBinding().lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        getBinding().lineChart.invalidate();
    }

    private void loadHistoryData() {
        List<MetricHistory> historyList = new ArrayList<>();

        switch (metricType) {
            case METRIC_HEART_RATE:
                historyList.add(new MetricHistory("88", "Today, 9:41 AM", "bpm"));
                historyList.add(new MetricHistory("92", "Yesterday, 8:15 PM", "bpm"));
                historyList.add(new MetricHistory("75", "Aug 28, 11:30 AM", "bpm"));
                historyList.add(new MetricHistory("81", "Aug 27, 2:00 PM", "bpm"));
                break;
            case METRIC_BLOOD_PRESSURE:
                historyList.add(new MetricHistory("120/80", "Today, 9:41 AM", "mmHg"));
                historyList.add(new MetricHistory("125/82", "Yesterday, 8:15 PM", "mmHg"));
                historyList.add(new MetricHistory("118/78", "Aug 28, 11:30 AM", "mmHg"));
                historyList.add(new MetricHistory("122/80", "Aug 27, 2:00 PM", "mmHg"));
                break;
            case METRIC_BLOOD_SUGAR:
                historyList.add(new MetricHistory("95", "Today, 9:41 AM", "mg/dL"));
                historyList.add(new MetricHistory("102", "Yesterday, 8:15 PM", "mg/dL"));
                historyList.add(new MetricHistory("88", "Aug 28, 11:30 AM", "mg/dL"));
                historyList.add(new MetricHistory("98", "Aug 27, 2:00 PM", "mg/dL"));
                break;
            case METRIC_BMI:
                historyList.add(new MetricHistory("23.5", "Today, 9:41 AM", "kg/m²"));
                historyList.add(new MetricHistory("23.8", "Yesterday, 8:15 PM", "kg/m²"));
                historyList.add(new MetricHistory("23.2", "Aug 28, 11:30 AM", "kg/m²"));
                historyList.add(new MetricHistory("23.6", "Aug 27, 2:00 PM", "kg/m²"));
                break;
        }

        historyAdapter.setHistoryList(historyList);
    }

    private void loadStatistics() {
        switch (metricType) {
            case METRIC_HEART_RATE:
                getBinding().tvCurrentValue.setText("85 bpm");
                getBinding().tvAverageValue.setText("78 bpm");
                getBinding().tvHighestValue.setText("120 bpm");
                getBinding().tvLowestValue.setText("60 bpm");
                break;
            case METRIC_BLOOD_PRESSURE:
                getBinding().tvCurrentValue.setText("120/80 mmHg");
                getBinding().tvAverageValue.setText("118/78 mmHg");
                getBinding().tvHighestValue.setText("130/85 mmHg");
                getBinding().tvLowestValue.setText("110/70 mmHg");
                break;
            case METRIC_BLOOD_SUGAR:
                getBinding().tvCurrentValue.setText("95 mg/dL");
                getBinding().tvAverageValue.setText("92 mg/dL");
                getBinding().tvHighestValue.setText("110 mg/dL");
                getBinding().tvLowestValue.setText("80 mg/dL");
                break;
            case METRIC_BMI:
                getBinding().tvCurrentValue.setText("23.5 kg/m²");
                getBinding().tvAverageValue.setText("23.3 kg/m²");
                getBinding().tvHighestValue.setText("24.2 kg/m²");
                getBinding().tvLowestValue.setText("22.8 kg/m²");
                break;
        }
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
