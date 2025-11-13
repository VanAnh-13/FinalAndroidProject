# Chart Data Processing Improvements

## Issues Fixed

### 1. Weight Metrics Showing Heart Rate
- **Problem**: Selecting weight metrics was showing heart rate data instead
- **Root Cause**: `METRIC_BMI` constant was incorrectly set to "bmi" but the data was stored as "weight"
- **Solution**:
  - Updated `METRIC_BMI` to refer to "weight" in MetricDetailActivity
  - Fixed DashboardFragment to send correct metric type

### 2. Chart X-Axis Data Processing 
- **Problem**: Chart data wasn't optimized for different time periods
- **Root Cause**: All raw data points were shown regardless of time period
- **Solution**: Implemented period-specific data processing

## Chart Data Processing Logic

### 1. Daily View
- **Data**: All measurements from current day
- **Display**: Each individual data point with exact time
- **X-Axis Labels**: Times (HH:mm)
- **Implementation**: Raw data filtered for today only

### 2. Weekly View (7 Days)
- **Data**: 7 data points, one for each of the past 7 days
- **Value Calculation**: Daily averages of all measurements on each day
- **X-Axis Labels**: Days (DD/MM)
- **Implementation**: Data grouped by day and averaged

### 3. Monthly View (30 Days)
- **Data**: 10 data points, each representing 3 days
- **Value Calculation**: Averages of all measurements in each 3-day period
- **X-Axis Labels**: Start date of each period (DD/MM)
- **Implementation**: Data grouped by 3-day periods and averaged

### 4. Yearly View (12 Months)
- **Data**: 12 data points, one for each month
- **Value Calculation**: Monthly averages of all measurements
- **X-Axis Labels**: Month names (MMM)
- **Implementation**: Data grouped by month and averaged

## Performance Optimizations

### 1. Caching System
- **Component**: `ChartDataCacheManager`
- **Cache Key Format**: "metricType_period" (e.g., "heart_rate_week")
- **Cache Size**: 20 chart datasets (LRU policy)
- **Cache Expiry**: 10 minutes (historical data doesn't change)
- **Benefits**: Instant chart rendering for subsequent views

### 2. Thread-Safety
- All cache operations are thread-safe
- Concurrent HashMap used for cache timing
- Synchronized access to singleton instance

### 3. Memory Efficiency
- Only processed data is cached, not raw data
- Automatic cache invalidation when memory is low
- Small memory footprint (each cache entry ≈ 2KB)

## Metric Types Handled

1. **Blood Pressure** (`blood_pressure`)
   - Dual-line chart with systolic and diastolic values
   - Special processing for "120/80" format
   - Red line for systolic, blue line for diastolic
   
2. **Heart Rate** (`heart_rate`)
   - Single line chart with heart rate values
   - Unit: bpm
   - Y-axis range: 40-220 bpm
   
3. **Blood Sugar** (`blood_sugar`)
   - Single line chart with blood sugar values
   - Unit: mg/dL
   - Y-axis range: 40-450 mg/dL
   
4. **Weight** (`weight`)
   - Single line chart with weight values
   - Unit: kg
   - Y-axis range: 30-150 kg

## Additional Improvements

1. **Y-Axis Formatting**
   - Units displayed on Y-axis (e.g., "80 bpm", "120 mmHg")
   - Custom formatter for each metric type
   
2. **Legend Display**
   - Blood pressure chart shows "Tâm thu" and "Tâm trương" legend
   - Properly configured legend with LINE form
   
3. **Consistent Time Ranges**
   - All charts show the same time periods (Day/Week/Month/Year)
   - Consistent X-axis label formatting

## Components Created

1. **ChartDataCacheManager.java**
   - Singleton cache manager for chart data
   - Provides cache hit/miss statistics
   - Thread-safe operations
   
2. **ChartDataProcessor.java**
   - Static utility for data processing
   - Period-specific data aggregation
   - Format handling for different metric types
