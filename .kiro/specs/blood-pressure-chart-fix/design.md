# Design Document

## Overview

Thiết kế giải pháp sửa lỗi biểu đồ huyết áp không hiển thị và các thông số thống kê không được tính toán đúng. Vấn đề chính là hệ thống hiện tại không xử lý đúng cấu trúc dữ liệu huyết áp có hai giá trị (tâm thu/tâm trương) và thiếu logic hiển thị biểu đồ dual-line cho huyết áp.

## Architecture

### Current Architecture Issues
1. **ChartDataProcessor.getMetricValue()**: Chỉ trả về giá trị tâm thu cho huyết áp, bỏ qua tâm trương
2. **MetricAnalysisActivity**: Không có logic đặc biệt để xử lý biểu đồ huyết áp dual-line
3. **Statistics Calculation**: Không tính toán riêng biệt cho tâm thu và tâm trương
4. **Chart Configuration**: Method configureDualLineChart() đã có nhưng không được sử dụng

### Proposed Solution Architecture
```
MetricAnalysisActivity (ENHANCED)
├── detectBloodPressureMetric()
├── setupBloodPressureChart()
├── calculateBloodPressureStats()
└── loadBloodPressureData()

ChartDataProcessor (ENHANCED)
├── processBloodPressureData() (NEW)
├── getMetricValue() (FIXED)
└── calculateBloodPressureStatistics() (NEW)

ChartConfigurator (EXISTING)
└── configureDualLineChart() (ALREADY EXISTS)
```

## Components and Interfaces

### 1. MetricAnalysisActivity Enhancement
**Purpose**: Thêm logic xử lý đặc biệt cho huyết áp

**New Methods**:
```java
private boolean isBloodPressureMetric() {
    return "blood_pressure".equals(metricType) || "huyết_áp".equals(metricType);
}

private void setupBloodPressureChart(List<MetricHistory> data) {
    // Sử dụng ChartConfigurator.configureDualLineChart()
}

private void calculateAndDisplayBloodPressureStats(List<MetricHistory> data) {
    // Tính toán và hiển thị thống kê riêng cho tâm thu/tâm trương
}
```

### 2. ChartDataProcessor Enhancement
**New Methods**:
```java
public static class BloodPressureData {
    public List<Entry> systolicEntries;
    public List<Entry> diastolicEntries;
    public List<String> labels;
}

public static BloodPressureData processBloodPressureData(
    List<MetricHistory> metricHistory, String period) {
    // Xử lý riêng cho huyết áp, tạo 2 series dữ liệu
}

public static class BloodPressureStats {
    public double avgSystolic, avgDiastolic;
    public double maxSystolic, maxDiastolic;
    public double minSystolic, minDiastolic;
    public int totalReadings;
}

public static BloodPressureStats calculateBloodPressureStatistics(
    List<MetricHistory> metricHistory, String period) {
    // Tính toán thống kê riêng cho tâm thu và tâm trương
}
```

**Enhanced Methods**:
```java
private static double getMetricValue(MetricHistory history, String metricType) {
    if ("blood_pressure".equals(metricType)) {
        return history.getSystolic(); // Giữ nguyên cho backward compatibility
    }
    return history.getValueAsDouble();
}
```

### 3. Data Flow Design

#### For Blood Pressure Charts:
1. **MetricAnalysisActivity.setupChart()** detects blood pressure metric
2. Calls **ChartDataProcessor.processBloodPressureData()** to get dual data
3. Uses **ChartConfigurator.configureDualLineChart()** to display dual-line chart
4. Calls **ChartDataProcessor.calculateBloodPressureStatistics()** for stats

#### For Other Metrics:
1. Uses existing **ChartDataProcessor.processMetricData()** flow
2. Uses **ChartConfigurator.configureLineChart()** for single-line chart

## Data Models

### BloodPressureData Structure
```java
public static class BloodPressureData {
    public List<Entry> systolicEntries;    // Dữ liệu tâm thu
    public List<Entry> diastolicEntries;   // Dữ liệu tâm trương  
    public List<String> labels;            // Nhãn trục X
    
    public boolean isEmpty() {
        return (systolicEntries == null || systolicEntries.isEmpty()) &&
               (diastolicEntries == null || diastolicEntries.isEmpty());
    }
}
```

### BloodPressureStats Structure
```java
public static class BloodPressureStats {
    public double avgSystolic;      // Trung bình tâm thu
    public double avgDiastolic;     // Trung bình tâm trương
    public double maxSystolic;      // Cao nhất tâm thu
    public double maxDiastolic;     // Cao nhất tâm trương
    public double minSystolic;      // Thấp nhất tâm thu
    public double minDiastolic;     // Thấp nhất tâm trương
    public int totalReadings;       // Tổng số lần đo
    public String trend;            // Xu hướng: "tăng", "giảm", "ổn định"
}
```

## Error Handling

### Data Validation
1. **Empty Data**: Hiển thị "Chưa có dữ liệu huyết áp"
2. **Invalid Format**: Skip records không đúng format "systolic/diastolic"
3. **Parse Errors**: Log error và bỏ qua record lỗi
4. **Zero Values**: Bỏ qua các giá trị <= 0

### Chart Display
1. **No Valid Data**: Hiển thị empty state với message phù hợp
2. **Single Value**: Hiển thị điểm đơn lẻ
3. **Network Error**: Hiển thị cached data nếu có

## Testing Strategy

### Unit Tests
1. **ChartDataProcessor.processBloodPressureData()**
   - Test với dữ liệu hợp lệ
   - Test với dữ liệu rỗng
   - Test với format không đúng
   - Test với các period khác nhau

2. **ChartDataProcessor.calculateBloodPressureStatistics()**
   - Test tính toán trung bình, min, max
   - Test với dữ liệu edge cases
   - Test trend calculation

3. **MetricHistory.getSystolic() và getDiastolic()**
   - Test parsing "120/80"
   - Test parsing invalid formats
   - Test edge cases

### Integration Tests
1. **MetricAnalysisActivity** với blood pressure data
2. **Chart rendering** với dual-line configuration
3. **Statistics display** với calculated values

### UI Tests
1. Chart hiển thị đúng với dữ liệu huyết áp
2. Legend hiển thị "Tâm thu" và "Tâm trương"
3. Statistics hiển thị đúng format
4. Empty state hiển thị đúng message