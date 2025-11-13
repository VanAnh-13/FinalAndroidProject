# 🔧 MetricDetailActivity Fix Plan

## Issues to Fix

### 1. Time Range Filtering
**Current:** Loads all data, doesn't filter by time range
**Fix:** Filter data based on selected period (Day/Week/Month/Year)

```java
// Current (wrong)
loadHistoryData() // Loads all data

// Fixed (correct)
loadHistoryDataForPeriod(String period) {
    Calendar cal = Calendar.getInstance();
    long endDate = cal.getTimeInMillis();
    
    switch(period) {
        case "day":
            cal.add(Calendar.DAY_OF_MONTH, -1);
            break;
        case "week":
            cal.add(Calendar.DAY_OF_MONTH, -7);
            break;
        case "month":
            cal.add(Calendar.MONTH, -1);
            break;
        case "year":
            cal.add(Calendar.YEAR, -1);
            break;
    }
    
    long startDate = cal.getTimeInMillis();
    
    // Fetch data between startDate and endDate
    metricsRepository.loadMetricHistoryByDateRange(metricType, startDate, endDate);
}
```

### 2. Statistics Calculation
**Current:** Calculates from all data
**Fix:** Recalculate when period changes

```java
// Current (wrong)
calculateAndDisplayStatistics(historyList); // Called once

// Fixed (correct)
@Override
public void setOnClick() {
    getBinding().periodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
        if (!isChecked) return;
        
        String period = getPeriodFromId(checkedId);
        loadHistoryDataForPeriod(period); // Reload data for new period
        // Statistics will be recalculated automatically
    });
}
```

### 3. UI Hardcoding
**Current:** Layout has hardcoded `@string/heart_rate_bpm`
**Fix:** Set metric label dynamically in code

```xml
<!-- Current (wrong) -->
<TextView
    android:text="@string/heart_rate_bpm"
    ... />

<!-- Fixed (correct) -->
<TextView
    android:id="@+id/tv_metric_label"
    android:text=""
    ... />
```

```java
// In setMetricTitle()
private void setMetricTitle() {
    String title;
    String label;
    String unit;
    
    switch (metricType) {
        case METRIC_BLOOD_PRESSURE:
            title = getString(R.string.blood_pressure);
            label = "Huyết áp (mmHg)";
            unit = "mmHg";
            break;
        case METRIC_BLOOD_SUGAR:
            title = getString(R.string.blood_sugar);
            label = "Đường huyết (mg/dL)";
            unit = "mg/dL";
            break;
        case METRIC_HEART_RATE:
            title = getString(R.string.heart_rate);
            label = "Nhịp tim (bpm)";
            unit = "bpm";
            break;
        case METRIC_BMI:
            title = getString(R.string.bmi);
            label = "BMI (kg/m²)";
            unit = "kg/m²";
            break;
        default:
            title = getString(R.string.heart_rate);
            label = "Nhịp tim (bpm)";
            unit = "bpm";
    }
    
    getBinding().tvMetricTitle.setText(title);
    getBinding().tvMetricLabel.setText(label);
    this.currentUnit = unit;
}
```

### 4. Unit Duplication
**Current:** Value shows "90/88 mmHg mmHg" (unit repeated)
**Fix:** Don't append unit in display value, only show number

```java
// Current (wrong)
getBinding().tvCurrentValue.setText(latest.getDisplayValue()); // "90/88 mmHg mmHg"

// Fixed (correct)
getBinding().tvCurrentValue.setText(latest.getValue()); // "90/88"
// Unit shown separately in label
```

### 5. Bottom Nav Sync
**Current:** Sidebar changes tab, but bottom nav doesn't update
**Fix:** Update bottom nav when sidebar changes tab

```java
// In MainActivity
public void navigateToTab(String tabName) {
    switch(tabName) {
        case "dashboard":
            getBinding().bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            break;
        case "metrics":
            getBinding().bottomNavigation.setSelectedItemId(R.id.nav_metrics);
            break;
        case "reminders":
            getBinding().bottomNavigation.setSelectedItemId(R.id.nav_reminders);
            break;
        case "records":
            getBinding().bottomNavigation.setSelectedItemId(R.id.nav_records);
            break;
        case "profile":
            getBinding().bottomNavigation.setSelectedItemId(R.id.nav_profile);
            break;
    }
}
```

## Implementation Steps

### Step 1: Update Layout
- [ ] Add `android:id="@+id/tv_metric_label"` to metric label TextView
- [ ] Remove hardcoded `@string/heart_rate_bpm`

### Step 2: Update MetricsRepository
- [ ] Add `loadMetricHistoryByDateRange(type, startDate, endDate)` method

### Step 3: Update MetricDetailActivity
- [ ] Add `setMetricTitle()` to set label dynamically
- [ ] Add `loadHistoryDataForPeriod(period)` method
- [ ] Update period selector listener to reload data
- [ ] Fix value display (remove unit duplication)
- [ ] Add `currentUnit` field

### Step 4: Update MainActivity
- [ ] Add `navigateToTab(tabName)` method
- [ ] Call from drawer navigation

### Step 5: Test
- [ ] Select different periods → data updates
- [ ] Statistics recalculate for each period
- [ ] Metric label shows correct type
- [ ] Unit not duplicated
- [ ] Sidebar changes tab → bottom nav updates

## Files to Modify

1. `activity_metric_detail.xml` - Add metric label ID
2. `MetricsRepository.java` - Add date range filter method
3. `MetricDetailActivity.java` - Implement all fixes
4. `MainActivity.java` - Add tab navigation sync

## Expected Result

When user:
1. Opens Blood Pressure metric
   - Title: "Huyết áp"
   - Label: "Huyết áp (mmHg)"
   - Value: "90/88" (no unit duplication)
   - Unit shown in label, not in value

2. Changes to 30 days
   - Data reloads for last 30 days
   - Statistics recalculate
   - Chart updates with new data points

3. Clicks sidebar to go to Reminders
   - Tab changes
   - Bottom nav also updates to Reminders
