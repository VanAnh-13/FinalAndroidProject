package com.example.healthylifehub.utils.chart;

import android.util.Log;

import com.example.healthylifehub.data.cache.ChartDataCacheManager;
import com.example.healthylifehub.data.model.MetricHistory;
import com.github.mikephil.charting.data.Entry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Processes raw metric data into chart data based on period
 * Implements special data processing for different time periods:
 * - Day: All measurements that day
 * - Week: 7 values (daily averages)
 * - Month: 10 values (3-day averages)
 * - Year: 12 values (monthly averages)
 */
public class ChartDataProcessor {
    private static final String TAG = "ChartDataProcessor";

    // Date format for parsing metric dates
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMM", Locale.getDefault());

    private ChartDataProcessor() {
        throw new AssertionError("No instances");
    }

    /**
     * Process metric history data into chart entries and labels based on period
     * @param metricType Type of metric (blood_pressure, heart_rate, etc.)
     * @param metricHistory List of metric history entries
     * @param period Period (day, week, month, year)
     * @return Processed chart data with entries and labels
     */
    public static ChartDataCacheManager.ChartDataCache processMetricData(
            String metricType, List<MetricHistory> metricHistory, String period) {
        
        // Check cache first
        ChartDataCacheManager cacheManager = ChartDataCacheManager.getInstance();
        String cacheKey = cacheManager.generateCacheKey(metricType, period);
        ChartDataCacheManager.ChartDataCache cachedData = cacheManager.getChartData(cacheKey);
        if (cachedData != null) {
            return cachedData;
        }
        
        // Process data based on period
        ChartDataCacheManager.ChartDataCache processedData;
        
        switch (period) {
            case "day":
                processedData = processDayData(metricType, metricHistory);
                break;
            case "week":
                processedData = processWeekData(metricType, metricHistory);
                break;
            case "month":
                processedData = processMonthData(metricType, metricHistory);
                break;
            case "year":
                processedData = processYearData(metricType, metricHistory);
                break;
            default:
                processedData = processDayData(metricType, metricHistory);
        }
        
        // Cache the processed data
        cacheManager.putChartData(cacheKey, processedData);
        
        return processedData;
    }

    /**
     * Process data for daily view (all measurements that day)
     */
    private static ChartDataCacheManager.ChartDataCache processDayData(
            String metricType, List<MetricHistory> metricHistory) {
        
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // For daily view, we use all data points with their exact times
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Date startOfDay = today.getTime();
        
        // Filter for today's data only
        List<MetricHistory> todayData = new ArrayList<>();
        
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null && !measurementDate.before(startOfDay)) {
                    todayData.add(history);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Sort by time
        Collections.reverse(todayData); // Oldest first for x-axis chronology
        
        // Create entries
        for (int i = 0; i < todayData.size(); i++) {
            MetricHistory history = todayData.get(i);
            
            if ("blood_pressure".equals(metricType)) {
                entries.add(new Entry(i, (float) history.getSystolic()));
            } else {
                entries.add(new Entry(i, (float) history.getValueAsDouble()));
            }
            
            // Extract time portion only
            String timeLabel = history.getFormattedDate();
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    timeLabel = timeFormat.format(measurementDate);
                }
            } catch (ParseException e) {
                // Use original format if parsing fails
            }
            
            labels.add(timeLabel);
        }
        
        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    /**
     * Process data for weekly view (7 daily averages)
     * If a day has no data, use the previous day's value (forward fill)
     */
    private static ChartDataCacheManager.ChartDataCache processWeekData(
            String metricType, List<MetricHistory> metricHistory) {
        
        // For weekly view, we want 7 data points (last 7 days)
        Calendar calendar = Calendar.getInstance();
        Map<String, List<Double>> dailyValues = new TreeMap<>();
        
        // Initialize the last 7 days
        for (int i = 0; i < 7; i++) {
            calendar.add(Calendar.DATE, -1);
            String dateKey = DAY_FORMAT.format(calendar.getTime());
            dailyValues.put(dateKey, new ArrayList<>());
        }
        calendar.add(Calendar.DATE, 7); // Reset calendar
        
        // Group measurements by day
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null) {
                    String dateKey = DAY_FORMAT.format(measurementDate);
                    
                    // Only include if in the last 7 days
                    if (dailyValues.containsKey(dateKey)) {
                        if ("blood_pressure".equals(metricType)) {
                            dailyValues.get(dateKey).add(history.getSystolic());
                        } else {
                            dailyValues.get(dateKey).add(history.getValueAsDouble());
                        }
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Calculate averages for each day with forward fill
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int index = 0;
        double lastValue = 0f;
        boolean hasData = false;
        
        for (Map.Entry<String, List<Double>> entry : dailyValues.entrySet()) {
            String dateKey = entry.getKey();
            List<Double> values = entry.getValue();
            
            float average = 0f;
            if (!values.isEmpty()) {
                double sum = 0;
                for (Double value : values) {
                    sum += value;
                }
                average = (float) (sum / values.size());
                lastValue = average;
                hasData = true;
            } else if (hasData) {
                // Use previous day's value if available
                average = (float) lastValue;
            }
            
            entries.add(new Entry(index, average));
            labels.add(dateKey);
            index++;
        }
        
        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    /**
     * Process data for monthly view (10 data points, each 3 days)
     * If a period has no data, use the previous period's value (forward fill)
     */
    private static ChartDataCacheManager.ChartDataCache processMonthData(
            String metricType, List<MetricHistory> metricHistory) {
        
        // For monthly view, we want 10 data points (each representing 3 days)
        Calendar calendar = Calendar.getInstance();
        Map<Integer, List<Double>> periodValues = new HashMap<>();
        
        // Initialize 10 periods (30 days total)
        calendar.add(Calendar.DATE, -30);
        Date thirtyDaysAgo = calendar.getTime();
        
        // Group measurements by 3-day periods
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null && !measurementDate.before(thirtyDaysAgo)) {
                    // Calculate days since 30 days ago
                    long diffMs = measurementDate.getTime() - thirtyDaysAgo.getTime();
                    int dayDiff = (int) (diffMs / (1000 * 60 * 60 * 24));
                    
                    // Determine which 3-day period (0-9) this belongs to
                    int period = Math.min(9, dayDiff / 3);
                    
                    if (!periodValues.containsKey(period)) {
                        periodValues.put(period, new ArrayList<>());
                    }
                    
                    if ("blood_pressure".equals(metricType)) {
                        periodValues.get(period).add(history.getSystolic());
                    } else {
                        periodValues.get(period).add(history.getValueAsDouble());
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Calculate averages for each period with forward fill
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        double lastValue = 0f;
        boolean hasData = false;
        
        for (int i = 0; i < 10; i++) {
            float average = 0f;
            List<Double> values = periodValues.get(i);
            
            if (values != null && !values.isEmpty()) {
                double sum = 0;
                for (Double value : values) {
                    sum += value;
                }
                average = (float) (sum / values.size());
                lastValue = average;
                hasData = true;
            } else if (hasData) {
                // Use previous period's value if available
                average = (float) lastValue;
            }
            
            // Calculate the date for this period (i*3 days from 30 days ago)
            calendar.setTime(thirtyDaysAgo);
            calendar.add(Calendar.DATE, i * 3);
            String dateLabel = DAY_FORMAT.format(calendar.getTime());
            
            entries.add(new Entry(i, average));
            labels.add(dateLabel);
        }
        
        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    /**
     * Process data for yearly view (12 monthly averages)
     * If a month has no data, use the previous month's value (forward fill)
     */
    private static ChartDataCacheManager.ChartDataCache processYearData(
            String metricType, List<MetricHistory> metricHistory) {
        
        // For yearly view, we want 12 data points (monthly averages)
        Calendar calendar = Calendar.getInstance();
        Map<Integer, List<Double>> monthlyValues = new HashMap<>();
        
        // Initialize 12 months
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();
        
        // Group measurements by month
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null && !measurementDate.before(oneYearAgo)) {
                    calendar.setTime(measurementDate);
                    int month = calendar.get(Calendar.MONTH);
                    
                    if (!monthlyValues.containsKey(month)) {
                        monthlyValues.put(month, new ArrayList<>());
                    }
                    
                    if ("blood_pressure".equals(metricType)) {
                        monthlyValues.get(month).add(history.getSystolic());
                    } else {
                        monthlyValues.get(month).add(history.getValueAsDouble());
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Calculate averages for each month with forward fill
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Start with current month and go back 12 months
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        double lastValue = 0f;
        boolean hasData = false;
        
        for (int i = 0; i < 12; i++) {
            int month = (currentMonth - i + 12) % 12; // Ensure positive month value
            
            float average = 0f;
            List<Double> values = monthlyValues.get(month);
            
            if (values != null && !values.isEmpty()) {
                double sum = 0;
                for (Double value : values) {
                    sum += value;
                }
                average = (float) (sum / values.size());
                lastValue = average;
                hasData = true;
            } else if (hasData) {
                // Use previous month's value if available
                average = (float) lastValue;
            }
            
            calendar.set(Calendar.MONTH, month);
            String monthLabel = MONTH_FORMAT.format(calendar.getTime());
            
            entries.add(new Entry(11 - i, average)); // Reverse order for x-axis chronology
            labels.add(monthLabel);
        }
        
        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }
    
    /**
     * Process diastolic values for blood pressure chart
     */
    public static List<Entry> processDiastolicValues(List<MetricHistory> metricHistory, String period) {
        // Similar implementation as above but for diastolic values
        // Only relevant for blood pressure
        
        List<Entry> entries = new ArrayList<>();
        
        switch (period) {
            case "day":
                entries = processDayDiastolicData(metricHistory);
                break;
            case "week":
                entries = processWeekDiastolicData(metricHistory);
                break;
            case "month":
                entries = processMonthDiastolicData(metricHistory);
                break;
            case "year":
                entries = processYearDiastolicData(metricHistory);
                break;
        }
        
        return entries;
    }
    
    /**
     * Process diastolic data for daily view
     */
    private static List<Entry> processDayDiastolicData(List<MetricHistory> metricHistory) {
        List<Entry> entries = new ArrayList<>();
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Date startOfDay = today.getTime();
        
        // Filter and sort today's data
        List<MetricHistory> todayData = new ArrayList<>();
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null && !measurementDate.before(startOfDay)) {
                    todayData.add(history);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Sort by time (oldest first)
        Collections.reverse(todayData);
        
        for (int i = 0; i < todayData.size(); i++) {
            entries.add(new Entry(i, (float) todayData.get(i).getDiastolic()));
        }
        
        return entries;
    }
    
    /**
     * Process diastolic data for weekly view
     */
    private static List<Entry> processWeekDiastolicData(List<MetricHistory> metricHistory) {
        Calendar calendar = Calendar.getInstance();
        Map<String, List<Double>> dailyValues = new TreeMap<>();
        
        // Initialize the last 7 days
        for (int i = 0; i < 7; i++) {
            calendar.add(Calendar.DATE, -1);
            String dateKey = DAY_FORMAT.format(calendar.getTime());
            dailyValues.put(dateKey, new ArrayList<>());
        }
        calendar.add(Calendar.DATE, 7); // Reset calendar
        
        // Group measurements by day
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null) {
                    String dateKey = DAY_FORMAT.format(measurementDate);
                    
                    if (dailyValues.containsKey(dateKey)) {
                        dailyValues.get(dateKey).add(history.getDiastolic());
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Calculate averages
        List<Entry> entries = new ArrayList<>();
        int index = 0;
        
        for (Map.Entry<String, List<Double>> entry : dailyValues.entrySet()) {
            List<Double> values = entry.getValue();
            
            float average = 0f;
            if (!values.isEmpty()) {
                double sum = 0;
                for (Double value : values) {
                    sum += value;
                }
                average = (float) (sum / values.size());
            }
            
            entries.add(new Entry(index, average));
            index++;
        }
        
        return entries;
    }
    
    /**
     * Process diastolic data for monthly view
     */
    private static List<Entry> processMonthDiastolicData(List<MetricHistory> metricHistory) {
        Calendar calendar = Calendar.getInstance();
        Map<Integer, List<Double>> periodValues = new HashMap<>();
        
        calendar.add(Calendar.DATE, -30);
        Date thirtyDaysAgo = calendar.getTime();
        
        // Group by 3-day periods
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null && !measurementDate.before(thirtyDaysAgo)) {
                    long diffMs = measurementDate.getTime() - thirtyDaysAgo.getTime();
                    int dayDiff = (int) (diffMs / (1000 * 60 * 60 * 24));
                    int period = Math.min(9, dayDiff / 3);
                    
                    if (!periodValues.containsKey(period)) {
                        periodValues.put(period, new ArrayList<>());
                    }
                    periodValues.get(period).add(history.getDiastolic());
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Calculate averages
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            float average = 0f;
            List<Double> values = periodValues.get(i);
            
            if (values != null && !values.isEmpty()) {
                double sum = 0;
                for (Double value : values) {
                    sum += value;
                }
                average = (float) (sum / values.size());
            }
            
            entries.add(new Entry(i, average));
        }
        
        return entries;
    }
    
    /**
     * Process diastolic data for yearly view
     */
    private static List<Entry> processYearDiastolicData(List<MetricHistory> metricHistory) {
        Calendar calendar = Calendar.getInstance();
        Map<Integer, List<Double>> monthlyValues = new HashMap<>();
        
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();
        
        // Group by month
        for (MetricHistory history : metricHistory) {
            try {
                Date measurementDate = DATE_FORMAT.parse(history.getFormattedDate());
                if (measurementDate != null && !measurementDate.before(oneYearAgo)) {
                    calendar.setTime(measurementDate);
                    int month = calendar.get(Calendar.MONTH);
                    
                    if (!monthlyValues.containsKey(month)) {
                        monthlyValues.put(month, new ArrayList<>());
                    }
                    monthlyValues.get(month).add(history.getDiastolic());
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + history.getFormattedDate(), e);
            }
        }
        
        // Calculate monthly averages
        List<Entry> entries = new ArrayList<>();
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        
        for (int i = 0; i < 12; i++) {
            int month = (currentMonth - i + 12) % 12;
            
            float average = 0f;
            List<Double> values = monthlyValues.get(month);
            
            if (values != null && !values.isEmpty()) {
                double sum = 0;
                for (Double value : values) {
                    sum += value;
                }
                average = (float) (sum / values.size());
            }
            
            entries.add(new Entry(11 - i, average));
        }
        
        return entries;
    }
}
