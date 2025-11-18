package com.example.healthylifehub.utils.chart;

import android.util.Log;

import com.example.healthylifehub.data.cache.ChartDataCacheManager;
import com.example.healthylifehub.data.model.MetricHistory;
import com.github.mikephil.charting.data.Entry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ChartDataProcessor {
    private static final String TAG = "ChartDataProcessor";

    private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat MONTH_KEY_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private static final SimpleDateFormat MONTH_LABEL_FORMAT = new SimpleDateFormat("MMM", Locale.getDefault());
    private static final SimpleDateFormat DAY_LABEL_FORMAT = new SimpleDateFormat("EEE", Locale.getDefault());

    private ChartDataProcessor() { }

    /**
     * Data structure for blood pressure chart data with dual series
     */
    public static class BloodPressureData {
        public List<Entry> systolicEntries;
        public List<Entry> diastolicEntries;
        public List<String> labels;
        
        public BloodPressureData() {
            this.systolicEntries = new ArrayList<>();
            this.diastolicEntries = new ArrayList<>();
            this.labels = new ArrayList<>();
        }
        
        public boolean isEmpty() {
            return (systolicEntries == null || systolicEntries.isEmpty()) &&
                   (diastolicEntries == null || diastolicEntries.isEmpty());
        }
    }

    /**
     * Statistics structure for blood pressure data
     */
    public static class BloodPressureStats {
        public double avgSystolic;
        public double avgDiastolic;
        public double maxSystolic;
        public double maxDiastolic;
        public double minSystolic;
        public double minDiastolic;
        public int totalReadings;
        public String trend;
        
        public BloodPressureStats() {
            this.avgSystolic = 0.0;
            this.avgDiastolic = 0.0;
            this.maxSystolic = 0.0;
            this.maxDiastolic = 0.0;
            this.minSystolic = 0.0;
            this.minDiastolic = 0.0;
            this.totalReadings = 0;
            this.trend = "ổn định";
        }
    }

    /**
     * Process blood pressure data to create dual series for systolic and diastolic values
     */
    public static BloodPressureData processBloodPressureData(List<MetricHistory> metricHistory, String period) {
        if (metricHistory == null || metricHistory.isEmpty()) {
            Log.w(TAG, "Empty blood pressure data provided");
            return new BloodPressureData();
        }

        BloodPressureData result = new BloodPressureData();
        
        switch (period) {
            case "day":
                result = processBloodPressureDayData(metricHistory);
                break;
            case "week":
                result = processBloodPressureWeekData(metricHistory);
                break;
            case "month":
                result = processBloodPressureMonthData(metricHistory);
                break;
            case "year":
                result = processBloodPressureYearData(metricHistory);
                break;
            default:
                result = processBloodPressureDayData(metricHistory);
        }

        Log.d(TAG, "Processed blood pressure data: " + result.systolicEntries.size() + " systolic, " + 
              result.diastolicEntries.size() + " diastolic entries");
        return result;
    }

    /**
     * Calculate comprehensive statistics for blood pressure data
     */
    public static BloodPressureStats calculateBloodPressureStatistics(List<MetricHistory> metricHistory, String period) {
        BloodPressureStats stats = new BloodPressureStats();
        
        if (metricHistory == null || metricHistory.isEmpty()) {
            Log.w(TAG, "No blood pressure data for statistics calculation");
            return stats;
        }

        List<Double> systolicValues = new ArrayList<>();
        List<Double> diastolicValues = new ArrayList<>();
        
        // Use all data for statistics calculation - period filtering is handled in chart processing
        List<MetricHistory> filteredData = metricHistory;
        
        for (MetricHistory history : filteredData) {
            double systolic = history.getSystolic();
            double diastolic = history.getDiastolic();
            
            if (systolic > 0 && diastolic > 0) {
                systolicValues.add(systolic);
                diastolicValues.add(diastolic);
            }
        }

        if (systolicValues.isEmpty()) {
            Log.w(TAG, "No valid blood pressure values found for statistics");
            return stats;
        }

        // Calculate statistics
        stats.totalReadings = systolicValues.size();
        
        // Initialize min/max with first values
        stats.minSystolic = systolicValues.get(0);
        stats.maxSystolic = systolicValues.get(0);
        stats.minDiastolic = diastolicValues.get(0);
        stats.maxDiastolic = diastolicValues.get(0);
        
        // Systolic statistics
        double systolicSum = 0;
        for (double value : systolicValues) {
            systolicSum += value;
            if (value > stats.maxSystolic) stats.maxSystolic = value;
            if (value < stats.minSystolic) stats.minSystolic = value;
        }
        stats.avgSystolic = systolicSum / systolicValues.size();
        
        // Diastolic statistics
        double diastolicSum = 0;
        for (double value : diastolicValues) {
            diastolicSum += value;
            if (value > stats.maxDiastolic) stats.maxDiastolic = value;
            if (value < stats.minDiastolic) stats.minDiastolic = value;
        }
        stats.avgDiastolic = diastolicSum / diastolicValues.size();
        
        // Calculate trend
        stats.trend = calculateBloodPressureTrend(systolicValues, diastolicValues);
        
        Log.d(TAG, "Blood pressure stats calculated: avg=" + stats.avgSystolic + "/" + stats.avgDiastolic + 
              ", readings=" + stats.totalReadings);
        return stats;
    }

    public static ChartDataCacheManager.ChartDataCache processMetricData(String metricType, List<MetricHistory> metricHistory, String period) {
        ChartDataCacheManager cacheManager = ChartDataCacheManager.getInstance();
        String cacheKey = cacheManager.generateCacheKey(metricType, period);
        ChartDataCacheManager.ChartDataCache cached = cacheManager.getChartData(cacheKey);
        if (cached != null) return cached;

        ChartDataCacheManager.ChartDataCache result;
        switch (period) {
            case "day":
                result = processDayData(metricType, metricHistory);
                break;
            case "week":
                result = processWeekData(metricType, metricHistory);
                break;
            case "month":
                result = processMonthData(metricType, metricHistory);
                break;
            case "year":
                result = processYearData(metricType, metricHistory);
                break;
            default:
                result = processDayData(metricType, metricHistory);
        }

        cacheManager.putChartData(cacheKey, result);
        return result;
    }

    private static ChartDataCacheManager.ChartDataCache processDayData(String metricType, List<MetricHistory> metricHistory) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfDay = today.getTimeInMillis();

        List<MetricHistory> todayList = new ArrayList<>();
        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d != null && d.getTime() >= startOfDay) {
                todayList.add(h);
            }
        }
        todayList.sort((a, b) -> {
            Date da = parseDateSafe(a.getFormattedDate());
            Date db = parseDateSafe(b.getFormattedDate());
            if (da == null || db == null) return 0;
            return da.compareTo(db); // cũ -> mới
        });

        for (int i = 0; i < todayList.size(); i++) {
            MetricHistory h = todayList.get(i);
            float v = (float) getMetricValue(h, metricType);
            entries.add(new Entry(i, v));
            Date d = parseDateSafe(h.getFormattedDate());
            labels.add(d != null ? TIME_FORMAT.format(d) : ("T" + (i + 1)));
        }

        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    private static ChartDataCacheManager.ChartDataCache processWeekData(String metricType, List<MetricHistory> metricHistory) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        List<String> last7 = new ArrayList<>();
        Map<String, List<Double>> daily = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String key = DAY_FORMAT.format(c.getTime());
            last7.add(key);
            daily.put(key, new ArrayList<>());
        }

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null) continue;
            String key = DAY_FORMAT.format(d);
            if (daily.containsKey(key)) {
                double v = getMetricValue(h, metricType);
                if (v > 0) daily.get(key).add(v);
            }
        }

        double last = 0;
        boolean has = false;
        for (int i = 0; i < last7.size(); i++) {
            String key = last7.get(i);
            List<Double> vals = daily.get(key);
            float avg = 0f;
            if (vals != null && !vals.isEmpty()) {
                double sum = 0;
                for (double v : vals) sum += v;
                avg = (float) (sum / vals.size());
                last = avg;
                has = true;
            } else if (has) {
                avg = (float) last; // forward fill
            }
            entries.add(new Entry(i, avg));

            try {
                Date dayDate = DAY_FORMAT.parse(key);
                if (dayDate != null) {
                    Calendar labelCal = Calendar.getInstance();
                    labelCal.setTime(dayDate);
                    Calendar today = Calendar.getInstance();
                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);
                    if (isSameDay(labelCal, today)) labels.add("Hôm nay");
                    else if (isSameDay(labelCal, yesterday)) labels.add("Hôm qua");
                    else labels.add(DAY_LABEL_FORMAT.format(dayDate));
                } else labels.add(key);
            } catch (ParseException e) {
                labels.add(key);
            }
        }

        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    private static ChartDataCacheManager.ChartDataCache processMonthData(String metricType, List<MetricHistory> metricHistory) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        long startMs = cal.getTimeInMillis();

        Map<Integer, List<Double>> buckets = new HashMap<>();
        for (int i = 0; i < 10; i++) buckets.put(i, new ArrayList<>());

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null || d.getTime() < startMs) continue;
            long days = (d.getTime() - startMs) / (24L * 60 * 60 * 1000);
            int period = Math.min(9, (int) (days / 3));
            double v = getMetricValue(h, metricType);
            if (v > 0) buckets.get(period).add(v);
        }

        double last = 0;
        boolean has = false;
        for (int i = 0; i < 10; i++) {
            List<Double> vals = buckets.get(i);
            float avg = 0f;
            if (vals != null && !vals.isEmpty()) {
                double sum = 0; for (double v : vals) sum += v;
                avg = (float) (sum / vals.size());
                last = avg; has = true;
            } else if (has) {
                avg = (float) last;
            }
            entries.add(new Entry(i, avg));

            Calendar labelCal = Calendar.getInstance();
            labelCal.setTimeInMillis(startMs);
            labelCal.add(Calendar.DAY_OF_YEAR, i * 3);
            labels.add(DAY_FORMAT.format(labelCal.getTime()));
        }

        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    private static ChartDataCacheManager.ChartDataCache processYearData(String metricType, List<MetricHistory> metricHistory) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        List<String> last12 = new ArrayList<>();
        Map<String, List<Double>> monthly = new TreeMap<>();
        for (int i = 11; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.MONTH, -i);
            String key = MONTH_KEY_FORMAT.format(c.getTime());
            last12.add(key);
            monthly.put(key, new ArrayList<>());
        }

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null) continue;
            String key = MONTH_KEY_FORMAT.format(d);
            if (monthly.containsKey(key)) {
                double v = getMetricValue(h, metricType);
                if (v > 0) monthly.get(key).add(v);
            }
        }

        double last = 0; boolean has = false;
        for (int i = 0; i < last12.size(); i++) {
            String key = last12.get(i);
            List<Double> vals = monthly.get(key);
            float avg = 0f;
            if (vals != null && !vals.isEmpty()) {
                double sum = 0; for (double v : vals) sum += v;
                avg = (float) (sum / vals.size());
                last = avg; has = true;
            } else if (has) {
                avg = (float) last;
            }
            entries.add(new Entry(i, avg));

            try {
                Date md = MONTH_KEY_FORMAT.parse(key);
                labels.add(md != null ? MONTH_LABEL_FORMAT.format(md) : key);
            } catch (ParseException e) {
                labels.add(key);
            }
        }

        return new ChartDataCacheManager.ChartDataCache(entries, labels);
    }

    public static List<Entry> processDiastolicValues(List<MetricHistory> metricHistory, String period) {
        switch (period) {
            case "day":
                return processDayDiastolicData(metricHistory);
            case "week":
                return processWeekDiastolicData(metricHistory);
            case "month":
                return processMonthDiastolicData(metricHistory);
            case "year":
                return processYearDiastolicData(metricHistory);
            default:
                return processDayDiastolicData(metricHistory);
        }
    }

    private static List<Entry> processDayDiastolicData(List<MetricHistory> metricHistory) {
        List<Entry> entries = new ArrayList<>();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfDay = today.getTimeInMillis();

        List<Date> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d != null && d.getTime() >= startOfDay) {
                times.add(d);
                values.add(h.getDiastolic());
            }
        }
        // sort by time asc
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) idx.add(i);
        idx.sort((a, b) -> times.get(a).compareTo(times.get(b)));
        for (int i = 0; i < idx.size(); i++) entries.add(new Entry(i, values.get(idx.get(i)).floatValue()));
        return entries;
    }

    private static List<Entry> processWeekDiastolicData(List<MetricHistory> metricHistory) {
        Map<String, List<Double>> daily = new TreeMap<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            cal.add(Calendar.DATE, -1);
            daily.put(DAY_FORMAT.format(cal.getTime()), new ArrayList<>());
        }
        cal.add(Calendar.DATE, 7);

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null) continue;
            String key = DAY_FORMAT.format(d);
            if (daily.containsKey(key)) daily.get(key).add(h.getDiastolic());
        }

        List<Entry> entries = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, List<Double>> e : daily.entrySet()) {
            float avg = 0f;
            if (!e.getValue().isEmpty()) {
                double sum = 0; for (double v : e.getValue()) sum += v;
                avg = (float) (sum / e.getValue().size());
            }
            entries.add(new Entry(i++, avg));
        }
        return entries;
    }

    private static List<Entry> processMonthDiastolicData(List<MetricHistory> metricHistory) {
        Map<Integer, List<Double>> buckets = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        Date start = cal.getTime();
        for (int i = 0; i < 10; i++) buckets.put(i, new ArrayList<>());
        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d != null && d.getTime() >= start.getTime()) {
                long diffMs = d.getTime() - start.getTime();
                int dayDiff = (int) (diffMs / (1000 * 60 * 60 * 24));
                int p = Math.min(9, dayDiff / 3);
                buckets.get(p).add(h.getDiastolic());
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (int p = 0; p < 10; p++) {
            float avg = 0f; List<Double> vals = buckets.get(p);
            if (vals != null && !vals.isEmpty()) { double s = 0; for (double v : vals) s += v; avg = (float) (s / vals.size()); }
            entries.add(new Entry(p, avg));
        }
        return entries;
    }

    private static List<Entry> processYearDiastolicData(List<MetricHistory> metricHistory) {
        Map<Integer, List<Double>> monthly = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        Date start = cal.getTime();
        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d != null && d.getTime() >= start.getTime()) {
                cal.setTime(d);
                int m = cal.get(Calendar.MONTH);
                monthly.computeIfAbsent(m, k -> new ArrayList<>()).add(h.getDiastolic());
            }
        }
        List<Entry> entries = new ArrayList<>();
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        for (int off = 0; off < 12; off++) {
            int month = (currentMonth - (11 - off) + 12) % 12;
            float avg = 0f; List<Double> vals = monthly.get(month);
            if (vals != null && !vals.isEmpty()) { double s = 0; for (double v : vals) s += v; avg = (float) (s / vals.size()); }
            entries.add(new Entry(off, avg));
        }
        return entries;
    }

    private static double getMetricValue(MetricHistory history, String metricType) {
        if ("blood_pressure".equals(metricType)) return history.getSystolic();
        return history.getValueAsDouble();
    }

    private static boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private static Date parseDateSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return INPUT_DATE_FORMAT.parse(s);
        } catch (ParseException e) {
            try {
                return SIMPLE_DATE_FORMAT.parse(s);
            } catch (ParseException ex) {
                return null;
            }
        }
    }

    public static double parseSystolic(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                Log.w(TAG, "Empty systolic value provided");
                return 0.0;
            }
            
            value = value.trim();
            
            // Handle blood pressure format "systolic/diastolic"
            if (value.contains("/")) {
                String[] parts = value.split("/");
                if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                    double systolic = Double.parseDouble(parts[0].trim());
                    // Validate reasonable blood pressure range
                    if (systolic < 50 || systolic > 300) {
                        Log.w(TAG, "Systolic value out of reasonable range: " + systolic);
                        return 0.0;
                    }
                    return systolic;
                } else {
                    Log.w(TAG, "Invalid blood pressure format, missing systolic: " + value);
                    return 0.0;
                }
            }
            
            // Handle single systolic value
            double systolic = Double.parseDouble(value);
            if (systolic < 50 || systolic > 300) {
                Log.w(TAG, "Systolic value out of reasonable range: " + systolic);
                return 0.0;
            }
            return systolic;
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing systolic value: " + value, e);
            return 0.0;
        }
    }

    public static double parseDiastolic(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                Log.w(TAG, "Empty diastolic value provided");
                return 0.0;
            }
            
            value = value.trim();
            
            // Handle blood pressure format "systolic/diastolic"
            if (value.contains("/")) {
                String[] parts = value.split("/");
                if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                    double diastolic = Double.parseDouble(parts[1].trim());
                    // Validate reasonable blood pressure range
                    if (diastolic < 30 || diastolic > 200) {
                        Log.w(TAG, "Diastolic value out of reasonable range: " + diastolic);
                        return 0.0;
                    }
                    return diastolic;
                } else {
                    Log.w(TAG, "Invalid blood pressure format, missing diastolic: " + value);
                    return 0.0;
                }
            }
            
            // No diastolic value in single number format
            Log.w(TAG, "No diastolic value found in: " + value);
            return 0.0;
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing diastolic value: " + value, e);
            return 0.0;
        }
    }

    // Helper methods for blood pressure data processing
    private static BloodPressureData processBloodPressureDayData(List<MetricHistory> metricHistory) {
        BloodPressureData result = new BloodPressureData();
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfDay = today.getTimeInMillis();

        List<MetricHistory> todayList = new ArrayList<>();
        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d != null && d.getTime() >= startOfDay) {
                double systolic = h.getSystolic();
                double diastolic = h.getDiastolic();
                if (systolic > 0 && diastolic > 0) {
                    todayList.add(h);
                }
            }
        }
        
        todayList.sort((a, b) -> {
            Date da = parseDateSafe(a.getFormattedDate());
            Date db = parseDateSafe(b.getFormattedDate());
            if (da == null || db == null) return 0;
            return da.compareTo(db);
        });

        for (int i = 0; i < todayList.size(); i++) {
            MetricHistory h = todayList.get(i);
            result.systolicEntries.add(new Entry(i, (float) h.getSystolic()));
            result.diastolicEntries.add(new Entry(i, (float) h.getDiastolic()));
            
            Date d = parseDateSafe(h.getFormattedDate());
            result.labels.add(d != null ? TIME_FORMAT.format(d) : ("T" + (i + 1)));
        }

        return result;
    }

    private static BloodPressureData processBloodPressureWeekData(List<MetricHistory> metricHistory) {
        BloodPressureData result = new BloodPressureData();
        
        Calendar cal = Calendar.getInstance();
        List<String> last7 = new ArrayList<>();
        Map<String, List<Double>> dailySystolic = new TreeMap<>();
        Map<String, List<Double>> dailyDiastolic = new TreeMap<>();
        
        for (int i = 6; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String key = DAY_FORMAT.format(c.getTime());
            last7.add(key);
            dailySystolic.put(key, new ArrayList<>());
            dailyDiastolic.put(key, new ArrayList<>());
        }

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null) continue;
            String key = DAY_FORMAT.format(d);
            if (dailySystolic.containsKey(key)) {
                double systolic = h.getSystolic();
                double diastolic = h.getDiastolic();
                if (systolic > 0 && diastolic > 0) {
                    dailySystolic.get(key).add(systolic);
                    dailyDiastolic.get(key).add(diastolic);
                }
            }
        }

        // Forward fill logic - keep track of last valid values
        float lastSys = 0f, lastDia = 0f;
        boolean hasData = false;
        
        for (int i = 0; i < last7.size(); i++) {
            String key = last7.get(i);
            List<Double> sysVals = dailySystolic.get(key);
            List<Double> diaVals = dailyDiastolic.get(key);
            
            float avgSys = 0f, avgDia = 0f;
            if (sysVals != null && !sysVals.isEmpty()) {
                double sum = 0;
                for (double v : sysVals) sum += v;
                avgSys = (float) (sum / sysVals.size());
                lastSys = avgSys; // Update last valid systolic
                hasData = true;
            } else if (hasData) {
                avgSys = lastSys; // Forward fill with last valid value
            }
            
            if (diaVals != null && !diaVals.isEmpty()) {
                double sum = 0;
                for (double v : diaVals) sum += v;
                avgDia = (float) (sum / diaVals.size());
                lastDia = avgDia; // Update last valid diastolic
            } else if (hasData) {
                avgDia = lastDia; // Forward fill with last valid value
            }
            
            result.systolicEntries.add(new Entry(i, avgSys));
            result.diastolicEntries.add(new Entry(i, avgDia));

            try {
                Date dayDate = DAY_FORMAT.parse(key);
                if (dayDate != null) {
                    Calendar labelCal = Calendar.getInstance();
                    labelCal.setTime(dayDate);
                    Calendar today = Calendar.getInstance();
                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);
                    if (isSameDay(labelCal, today)) result.labels.add("Hôm nay");
                    else if (isSameDay(labelCal, yesterday)) result.labels.add("Hôm qua");
                    else result.labels.add(DAY_LABEL_FORMAT.format(dayDate));
                } else result.labels.add(key);
            } catch (ParseException e) {
                result.labels.add(key);
            }
        }

        return result;
    }

    private static BloodPressureData processBloodPressureMonthData(List<MetricHistory> metricHistory) {
        BloodPressureData result = new BloodPressureData();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        long startMs = cal.getTimeInMillis();

        Map<Integer, List<Double>> systolicBuckets = new HashMap<>();
        Map<Integer, List<Double>> diastolicBuckets = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            systolicBuckets.put(i, new ArrayList<>());
            diastolicBuckets.put(i, new ArrayList<>());
        }

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null || d.getTime() < startMs) continue;
            long days = (d.getTime() - startMs) / (24L * 60 * 60 * 1000);
            int period = Math.min(9, (int) (days / 3));
            
            double systolic = h.getSystolic();
            double diastolic = h.getDiastolic();
            if (systolic > 0 && diastolic > 0) {
                systolicBuckets.get(period).add(systolic);
                diastolicBuckets.get(period).add(diastolic);
            }
        }

        for (int i = 0; i < 10; i++) {
            List<Double> sysVals = systolicBuckets.get(i);
            List<Double> diaVals = diastolicBuckets.get(i);
            
            float avgSys = 0f, avgDia = 0f;
            if (sysVals != null && !sysVals.isEmpty()) {
                double sum = 0;
                for (double v : sysVals) sum += v;
                avgSys = (float) (sum / sysVals.size());
            }
            if (diaVals != null && !diaVals.isEmpty()) {
                double sum = 0;
                for (double v : diaVals) sum += v;
                avgDia = (float) (sum / diaVals.size());
            }
            
            result.systolicEntries.add(new Entry(i, avgSys));
            result.diastolicEntries.add(new Entry(i, avgDia));

            Calendar labelCal = Calendar.getInstance();
            labelCal.setTimeInMillis(startMs);
            labelCal.add(Calendar.DAY_OF_YEAR, i * 3);
            result.labels.add(DAY_FORMAT.format(labelCal.getTime()));
        }

        return result;
    }

    private static BloodPressureData processBloodPressureYearData(List<MetricHistory> metricHistory) {
        BloodPressureData result = new BloodPressureData();
        
        Calendar cal = Calendar.getInstance();
        List<String> last12 = new ArrayList<>();
        Map<String, List<Double>> monthlySystolic = new TreeMap<>();
        Map<String, List<Double>> monthlyDiastolic = new TreeMap<>();
        
        for (int i = 11; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.MONTH, -i);
            String key = MONTH_KEY_FORMAT.format(c.getTime());
            last12.add(key);
            monthlySystolic.put(key, new ArrayList<>());
            monthlyDiastolic.put(key, new ArrayList<>());
        }

        for (MetricHistory h : metricHistory) {
            Date d = parseDateSafe(h.getFormattedDate());
            if (d == null) continue;
            String key = MONTH_KEY_FORMAT.format(d);
            if (monthlySystolic.containsKey(key)) {
                double systolic = h.getSystolic();
                double diastolic = h.getDiastolic();
                if (systolic > 0 && diastolic > 0) {
                    monthlySystolic.get(key).add(systolic);
                    monthlyDiastolic.get(key).add(diastolic);
                }
            }
        }

        for (int i = 0; i < last12.size(); i++) {
            String key = last12.get(i);
            List<Double> sysVals = monthlySystolic.get(key);
            List<Double> diaVals = monthlyDiastolic.get(key);
            
            float avgSys = 0f, avgDia = 0f;
            if (sysVals != null && !sysVals.isEmpty()) {
                double sum = 0;
                for (double v : sysVals) sum += v;
                avgSys = (float) (sum / sysVals.size());
            }
            if (diaVals != null && !diaVals.isEmpty()) {
                double sum = 0;
                for (double v : diaVals) sum += v;
                avgDia = (float) (sum / diaVals.size());
            }
            
            result.systolicEntries.add(new Entry(i, avgSys));
            result.diastolicEntries.add(new Entry(i, avgDia));

            try {
                Date md = MONTH_KEY_FORMAT.parse(key);
                result.labels.add(md != null ? MONTH_LABEL_FORMAT.format(md) : key);
            } catch (ParseException e) {
                result.labels.add(key);
            }
        }

        return result;
    }

    private static List<MetricHistory> filterDataByPeriod(List<MetricHistory> metricHistory, String period) {
        List<MetricHistory> filtered = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        long cutoffTime = 0;
        
        switch (period) {
            case "day":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cutoffTime = cal.getTimeInMillis();
                break;
            case "week":
                cal.add(Calendar.DAY_OF_YEAR, -7);
                cutoffTime = cal.getTimeInMillis();
                break;
            case "month":
                cal.add(Calendar.DAY_OF_YEAR, -30);
                cutoffTime = cal.getTimeInMillis();
                break;
            case "year":
                cal.add(Calendar.YEAR, -1);
                cutoffTime = cal.getTimeInMillis();
                break;
            default:
                return metricHistory; // Return all data for unknown periods
        }
        
        for (MetricHistory history : metricHistory) {
            Date d = parseDateSafe(history.getFormattedDate());
            if (d != null && d.getTime() >= cutoffTime) {
                filtered.add(history);
            }
        }
        
        return filtered;
    }

    private static String calculateBloodPressureTrend(List<Double> systolicValues, List<Double> diastolicValues) {
        if (systolicValues.size() < 2) return "ổn định";
        
        // Calculate trend based on first half vs second half averages
        int midPoint = systolicValues.size() / 2;
        
        double firstHalfSys = 0, secondHalfSys = 0;
        double firstHalfDia = 0, secondHalfDia = 0;
        
        for (int i = 0; i < midPoint; i++) {
            firstHalfSys += systolicValues.get(i);
            firstHalfDia += diastolicValues.get(i);
        }
        for (int i = midPoint; i < systolicValues.size(); i++) {
            secondHalfSys += systolicValues.get(i);
            secondHalfDia += diastolicValues.get(i);
        }
        
        firstHalfSys /= midPoint;
        firstHalfDia /= midPoint;
        secondHalfSys /= (systolicValues.size() - midPoint);
        secondHalfDia /= (diastolicValues.size() - midPoint);
        
        double sysTrend = secondHalfSys - firstHalfSys;
        double diaTrend = secondHalfDia - firstHalfDia;
        
        // Consider significant if change is > 5 mmHg
        if (Math.abs(sysTrend) > 5 || Math.abs(diaTrend) > 5) {
            if (sysTrend > 0 || diaTrend > 0) return "tăng";
            else return "giảm";
        }
        
        return "ổn định";
    }
}
