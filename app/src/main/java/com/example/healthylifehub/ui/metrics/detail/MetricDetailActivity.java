package com.example.healthylifehub.ui.metrics.detail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Calendar;
import java.util.List;

public class MetricDetailActivity extends BaseActivity<ActivityMetricDetailBinding> {

    private static final String TAG = "MetricDetailActivity";
    
    public static final String EXTRA_METRIC_TYPE = "metric_type";
    public static final String METRIC_HEART_RATE = "heart_rate";
    public static final String METRIC_BLOOD_PRESSURE = "blood_pressure";
    public static final String METRIC_BLOOD_SUGAR = "blood_sugar";
    public static final String METRIC_BMI = "weight";

    private String metricType;
    private MetricHistoryAdapter historyAdapter;
    private String currentPeriod = "week";
    private String currentUnit = "bpm";
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

        metricsRepository = new MetricsRepository(this);

        // Setup RecyclerView
        historyAdapter = new MetricHistoryAdapter(history -> {
            Toast.makeText(this, "Clicked: " + history.getDisplayValue(), Toast.LENGTH_SHORT).show();
        });
        
        // Set action listener for menu items
        historyAdapter.setActionListener(new MetricHistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onViewDetail(MetricHistory history) {
                Toast.makeText(MetricDetailActivity.this, "Chi tiết: " + history.getDisplayValue(), Toast.LENGTH_SHORT).show();
                // TODO: Show detail dialog or activity
            }

            @Override
            public void onDelete(MetricHistory history) {
                // TODO: Delete from Firestore and Room
                Toast.makeText(MetricDetailActivity.this, "Xóa: " + history.getDisplayValue(), Toast.LENGTH_SHORT).show();
            }
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
        String label;
        
        switch (metricType) {
            case METRIC_BLOOD_PRESSURE:
                title = getString(R.string.blood_pressure);
                label = "Huyết áp (mmHg)";
                currentUnit = "mmHg";
                break;
            case METRIC_BLOOD_SUGAR:
                title = getString(R.string.blood_sugar);
                label = "Đường huyết (mg/dL)";
                currentUnit = "mg/dL";
                break;
            case METRIC_BMI:
                title = getString(R.string.weight);
                label = "Cân nặng (kg)";
                currentUnit = "kg";
                break;
            case METRIC_HEART_RATE:
            default:
                title = getString(R.string.heart_rate);
                label = "Nhịp tim (bpm)";
                currentUnit = "bpm";
                break;
        }
        
        getBinding().tvMetricTitle.setText(title);
        getBinding().tvMetricLabel.setText(label);
    }

    private void setupChart() {
        // Thiết lập biểu đồ trống ban đầu
        getBinding().lineChart.setNoDataText("Không có dữ liệu");
        getBinding().lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        getBinding().lineChart.invalidate();
        // Biểu đồ sẽ được cập nhật sau khi tải dữ liệu lịch sử
        // Xem loadHistoryData()
    }

    private void loadHistoryData() {
        loadHistoryDataForPeriod(currentPeriod);
        Log.d(TAG, "Loading history data for metric type: " + metricType);
    }
    
    
    private void loadHistoryDataForPeriod(String period) {
        Log.d(TAG, "Loading data for period: " + period);
        
        // Calculate date range based on period
        Calendar calendar = Calendar.getInstance();
        long endDate = calendar.getTimeInMillis();
        
        switch (period) {
            case "day":
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case "week":
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "month":
                calendar.add(Calendar.MONTH, -1);
                break;
            case "year":
                calendar.add(Calendar.YEAR, -1);
                break;
        }
        
        long startDate = calendar.getTimeInMillis();
        
        // Load history from Firebase with date range
        LiveData<List<MetricHistory>> historyLiveData = 
            metricsRepository.loadMetricHistoryByDateRange(metricType, startDate, endDate);
        
        // Remove previous observers to avoid duplicate updates
        historyLiveData.removeObservers(this);
        
        // Add new observer - will be called whenever data changes (real-time)
        historyLiveData.observe(this, historyList -> {
            if (historyList != null && !historyList.isEmpty()) {
                // Update adapter with all history data for the list view
                historyAdapter.setHistoryList(historyList);
                
                // Update statistics based on all loaded data
                calculateAndDisplayStatistics(historyList);
                
                // Update chart with processed data according to period
                updateChartWithData(historyList, period);
                
                Log.d(TAG, "✅ Chart updated with " + historyList.size() + " items for period: " + period);
            } else {
                Log.d(TAG, "⚠️ No history data available");
            }
        });
    }
    
    private void updateChartWithData(List<MetricHistory> historyList, String period) {
        // Kiểm tra dữ liệu trước khi cập nhật biểu đồ
        if (historyList == null || historyList.isEmpty()) {
            Log.d(TAG, "❗ Không có dữ liệu để hiển thị trên biểu đồ");
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText("Không có dữ liệu cho " + period);
            getBinding().lineChart.invalidate();
            return;
        }

        // Sắp xếp dữ liệu từ mới đến cũ trước khi xử lý
        List<MetricHistory> sortedHistory = new ArrayList<>(historyList);
        
        if (METRIC_BLOOD_PRESSURE.equals(metricType)) {
            updateBloodPressureChart(sortedHistory, period);
        } else {
            updateSingleLineChart(sortedHistory, period);
        }
    }
    
    private void updateSingleLineChart(List<MetricHistory> historyList, String period) {
        try {
            // Sử dụng ChartDataProcessor để lấy dữ liệu đã xử lý dựa trên kỳ
            com.example.healthylifehub.data.cache.ChartDataCacheManager.ChartDataCache chartData = 
                    com.example.healthylifehub.utils.chart.ChartDataProcessor.processMetricData(
                            metricType, historyList, period);
            
            List<Entry> entries = chartData.getEntries();
            List<String> labels = chartData.getLabels();
            
            // Kiểm tra lại dữ liệu sau khi xử lý
            if (entries.isEmpty()) {
                Log.d(TAG, "❗ Không có dữ liệu sau khi xử lý");
                getBinding().lineChart.clear();
                getBinding().lineChart.setNoDataText("Không có dữ liệu cho " + period);
                getBinding().lineChart.invalidate();
                return;
            }
            
            // Lấy cấu hình biểu đồ
            int colorRes = getChartColorForMetricType();
            String yAxisUnit = getYAxisUnit();
            float[] yAxisRange = getYAxisRange();
            
            // Cấu hình biểu đồ với dữ liệu thực và đơn vị
            ChartConfigurator.configureLineChart(
                getBinding().lineChart,
                this,
                entries,
                labels,
                colorRes,
                R.color.border_color,
                yAxisUnit,
                yAxisRange[0],
                yAxisRange[1]
            );
            
            // Hoạt động làm mới biểu đồ
            getBinding().lineChart.animateX(500);
            
            Log.d(TAG, "✅ Đã cập nhật biểu đồ đơn với " + entries.size() + " mục cho kỳ: " + period);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cập nhật biểu đồ đơn: " + e.getMessage());
            e.printStackTrace();
            
            // Hiển thị thông báo lỗi trên biểu đồ
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText("Lỗi khi tải dữ liệu");
            getBinding().lineChart.invalidate();
        }
    }
    
    private void updateBloodPressureChart(List<MetricHistory> historyList, String period) {
        try {
            // Xử lý dữ liệu tâm thu với ChartDataProcessor
            com.example.healthylifehub.data.cache.ChartDataCacheManager.ChartDataCache chartData = 
                    com.example.healthylifehub.utils.chart.ChartDataProcessor.processMetricData(
                            metricType, historyList, period);
            
            // Xử lý dữ liệu tâm trương riêng biệt
            List<Entry> diastolicEntries = com.example.healthylifehub.utils.chart.ChartDataProcessor
                    .processDiastolicValues(historyList, period);
            
            // Kiểm tra dữ liệu sau khi xử lý
            if (chartData.getEntries().isEmpty() && diastolicEntries.isEmpty()) {
                Log.d(TAG, "❗ Không có dữ liệu huyết áp sau khi xử lý");
                getBinding().lineChart.clear();
                getBinding().lineChart.setNoDataText("Không có dữ liệu huyết áp cho " + period);
                getBinding().lineChart.invalidate();
                return;
            }
            
            // Cấu hình biểu đồ hai đường (đỏ cho tâm thu, xanh cho tâm trương)
            ChartConfigurator.configureDualLineChart(
                getBinding().lineChart,
                this,
                chartData.getEntries(),  // Systolic entries
                diastolicEntries,        // Diastolic entries
                chartData.getLabels(),   // X-axis labels
                R.color.error_red,       // Systolic - Red
                R.color.primary_blue,    // Diastolic - Blue
                R.color.border_color
            );
            
            // Hoạt động làm mới biểu đồ
            getBinding().lineChart.animateX(500);
            
            Log.d(TAG, "✅ Đã cập nhật biểu đồ huyết áp với " + chartData.getEntries().size() + 
                    " mục cho kỳ: " + period);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cập nhật biểu đồ huyết áp: " + e.getMessage());
            e.printStackTrace();
            
            // Hiển thị thông báo lỗi trên biểu đồ
            getBinding().lineChart.clear();
            getBinding().lineChart.setNoDataText("Lỗi khi tải dữ liệu huyết áp");
            getBinding().lineChart.invalidate();
        }
    }
    
    private String getYAxisUnit() {
        switch (metricType) {
            case METRIC_BLOOD_PRESSURE:
                return "mmHg";
            case METRIC_BLOOD_SUGAR:
                return "mg/dL";
            case METRIC_HEART_RATE:
                return "bpm";
            case METRIC_BMI: // This is actually weight
                return "kg";
            default:
                return "";
        }
    }
    
    private float[] getYAxisRange() {
        switch (metricType) {
            case METRIC_BLOOD_PRESSURE:
                return new float[]{40f, 200f};
            case METRIC_BLOOD_SUGAR:
                return new float[]{40f, 450f};
            case METRIC_HEART_RATE:
                return new float[]{40f, 220f};
            case METRIC_BMI: // This is actually weight
                return new float[]{30f, 150f};
            default:
                return new float[]{0f, 0f};
        }
    }
    
    private int getChartColorForMetricType() {
        switch (metricType) {
            case METRIC_BLOOD_PRESSURE:
                return R.color.error_red; // Red
            case METRIC_BLOOD_SUGAR:
                return R.color.warning_orange; // Orange
            case METRIC_HEART_RATE:
                return R.color.primary_red; // Red/Pink
            case METRIC_BMI: // This is actually weight
                return R.color.success_green; // Green
            default:
                return R.color.primary_blue;
        }
    }

    private void calculateAndDisplayStatistics(List<MetricHistory> historyList) {
        // Kiểm tra dữ liệu trước khi tính toán
        if (historyList == null || historyList.isEmpty()) {
            // Đặt giá trị mặc định nếu không có dữ liệu
            getBinding().tvCurrentValue.setText("--");
            getBinding().tvCurrentStat.setText("--");
            getBinding().tvAverageValue.setText("--");
            getBinding().tvHighestValue.setText("--");
            getBinding().tvLowestValue.setText("--");
            getBinding().tvChangePercentage.setText("0%");
            return;
        }
        
        try {
            // Lấy giá trị gần đây nhất
            MetricHistory latest = historyList.get(0);
            getBinding().tvCurrentValue.setText(latest.getValue());
            
            if (METRIC_BLOOD_PRESSURE.equals(metricType)) {
                calculateAndDisplayBloodPressureStatistics(historyList, latest);
            } else {
                calculateAndDisplayStandardStatistics(historyList, latest);
            }
            
            // Tính toán và hiển thị phần trăm thay đổi
            calculateAndDisplayPercentageChange(historyList);
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tính toán thống kê: " + e.getMessage());
            
            // Xử lý lỗi an toàn
            getBinding().tvCurrentValue.setText("--");
            getBinding().tvCurrentStat.setText("--");
            getBinding().tvAverageValue.setText("--");
            getBinding().tvHighestValue.setText("--");
            getBinding().tvLowestValue.setText("--");
            getBinding().tvChangePercentage.setText("0%");
        }
    }

    private void calculateAndDisplayBloodPressureStatistics(List<MetricHistory> historyList, MetricHistory latest) {
        double sumSys = 0;
        double sumDia = 0;
        double maxSys = Double.MIN_VALUE;
        double maxDia = Double.MIN_VALUE;
        double minSys = Double.MAX_VALUE;
        double minDia = Double.MAX_VALUE;
        
        for (MetricHistory history : historyList) {
            double sys = history.getSystolic();
            double dia = history.getDiastolic();
            
            sumSys += sys;
            sumDia += dia;
            
            maxSys = Math.max(maxSys, sys);
            maxDia = Math.max(maxDia, dia);
            
            minSys = Math.min(minSys, sys);
            minDia = Math.min(minDia, dia);
        }
        
        double avgSys = sumSys / historyList.size();
        double avgDia = sumDia / historyList.size();
        
        // Hiển thị thống kê cho huyết áp (Sys/Dia)
        getBinding().tvCurrentStat.setText(latest.getValue());
        getBinding().tvAverageValue.setText(String.format("%.0f/%.0f", avgSys, avgDia));
        getBinding().tvHighestValue.setText(String.format("%.0f/%.0f", maxSys, maxDia));
        getBinding().tvLowestValue.setText(String.format("%.0f/%.0f", minSys, minDia));
        
        Log.d(TAG, "✅ Đã tính toán thống kê huyết áp: avg=" + avgSys + "/" + avgDia);
    }

    private void calculateAndDisplayStandardStatistics(List<MetricHistory> historyList, MetricHistory latest) {
        // TÍNH TOÁN THỐNG KÊ:
        // - Sum: Tổng các giá trị
        // - Highest: Giá trị cao nhất trong kỳ
        // - Lowest: Giá trị thấp nhất trong kỳ
        // - Average: Sum / Count
        double sum = 0;
        double highest = Double.MIN_VALUE;
        double lowest = Double.MAX_VALUE;
        
        for (MetricHistory history : historyList) {
            double value = history.getValueAsDouble();
            sum += value;
            highest = Math.max(highest, value);
            lowest = Math.min(lowest, value);
        }
        
        double average = sum / historyList.size();
        
        // Hiển thị thống kê đã tính toán (định dạng với 1 chữ số thập phân)
        getBinding().tvCurrentStat.setText(String.format("%.1f", latest.getValueAsDouble()));
        getBinding().tvAverageValue.setText(String.format("%.1f", average));
        getBinding().tvHighestValue.setText(String.format("%.1f", highest));
        getBinding().tvLowestValue.setText(String.format("%.1f", lowest));
        
        Log.d(TAG, "✅ Đã tính toán thống kê chuẩn: avg=" + average + ", min=" + lowest + ", max=" + highest);
    }
    
    private void calculateAndDisplayPercentageChange(List<MetricHistory> historyList) {
        if (historyList.size() < 2) {
            getBinding().tvChangePercentage.setText("+0%");
            return;
        }
        
        // Get current period average
        double currentAverage = 0;
        int currentCount = 0;
        
        // Get previous period average (if available)
        double previousAverage = 0;
        int previousCount = 0;
        
        // Split data into current and previous half
        int midpoint = historyList.size() / 2;
        
        boolean isBloodPressure = METRIC_BLOOD_PRESSURE.equals(metricType);

        // Current period (first half - more recent)
        for (int i = 0; i < midpoint; i++) {
            double value = isBloodPressure ? historyList.get(i).getSystolic() : historyList.get(i).getValueAsDouble();
            currentAverage += value;
            currentCount++;
        }
        if (currentCount > 0) {
            currentAverage /= currentCount;
        }
        
        // Previous period (second half - older)
        for (int i = midpoint; i < historyList.size(); i++) {
            double value = isBloodPressure ? historyList.get(i).getSystolic() : historyList.get(i).getValueAsDouble();
            previousAverage += value;
            previousCount++;
        }
        if (previousCount > 0) {
            previousAverage /= previousCount;
        }
        
        // Calculate percentage change
        double percentageChange = 0;
        if (previousAverage != 0) {
            percentageChange = ((currentAverage - previousAverage) / previousAverage) * 100;
        }
        
        // Display with color
        String changeText = String.format("%+.0f%%", percentageChange);
        getBinding().tvChangePercentage.setText(changeText);
        
        // Set color based on metric type
        int colorRes = getPercentageChangeColor(percentageChange);
        getBinding().tvChangePercentage.setTextColor(getResources().getColor(colorRes));
        
        Log.d(TAG, "Percentage change: " + changeText + 
            " (current avg: " + currentAverage + ", previous avg: " + previousAverage + ")");
    }
    
    private int getPercentageChangeColor(double percentageChange) {
        // For most metrics: green = good (lower is better for BP/sugar, higher is better for HR)
        // For simplicity: positive = green, negative = red
        if (percentageChange > 0) {
            return R.color.success_green;
        } else if (percentageChange < 0) {
            return R.color.error_red;
        } else {
            return R.color.text_secondary;
        }
    }

    private void updatePeriodSelection(String period) {
        // Hiển thị giao diện đang tải
        getBinding().lineChart.clear();
        getBinding().lineChart.setNoDataText("Đang tải dữ liệu cho " + period + "...");
        getBinding().lineChart.invalidate();
        
        // Cập nhật kỳ hiện tại
        currentPeriod = period;
        
        // Xóa cache cho metric này để buộc tải lại
        com.example.healthylifehub.data.cache.ChartDataCacheManager.getInstance()
            .clearChartData(metricType + "_" + period);
        
        // Tải lại dữ liệu cho kỳ mới
        loadHistoryDataForPeriod(period);
        
        Log.d(TAG, "Đã chuyển kỳ sang: " + period);
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
