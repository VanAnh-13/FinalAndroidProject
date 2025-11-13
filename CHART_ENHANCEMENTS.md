# 📈 Chart Enhancements

## 1. Y-Axis Units & Ranges

### Metric Ranges:
```
Heart Rate (bpm):
  - Min: 40 bpm (resting)
  - Max: 200 bpm (max exercise)
  - Y-axis: 0-220 bpm

Blood Pressure (mmHg):
  - Systolic: 60-180 mmHg
  - Diastolic: 40-120 mmHg
  - Y-axis: 0-200 mmHg

Blood Sugar (mg/dL):
  - Min: 40 mg/dL (hypoglycemia)
  - Max: 400 mg/dL (hyperglycemia)
  - Y-axis: 0-450 mg/dL

BMI (kg/m²):
  - Min: 10 kg/m²
  - Max: 60 kg/m² (obesity)
  - Y-axis: 0-70 kg/m²

Weight (kg):
  - Min: 30 kg
  - Max: 500 kg
  - Y-axis: 0-550 kg
```

### Implementation:
```java
// In ChartConfigurator
public static void configureLineChart(
    LineChart chart,
    Context context,
    List<Entry> entries,
    List<String> xAxisLabels,
    int lineColorRes,
    int gridColorRes,
    String yAxisUnit,      // NEW
    float yAxisMin,        // NEW
    float yAxisMax         // NEW
) {
    // ... existing code ...
    
    YAxis leftAxis = chart.getAxisLeft();
    leftAxis.setDrawGridLines(true);
    leftAxis.setGridColor(ContextCompat.getColor(context, gridColorRes));
    leftAxis.setAxisMinimum(yAxisMin);
    leftAxis.setAxisMaximum(yAxisMax);
    leftAxis.setValueFormatter(new YAxisFormatter(yAxisUnit)); // NEW
    
    chart.getAxisRight().setEnabled(false);
}

// New formatter class
public class YAxisFormatter extends ValueFormatter {
    private String unit;
    
    public YAxisFormatter(String unit) {
        this.unit = unit;
    }
    
    @Override
    public String getFormattedValue(float value) {
        return String.format("%.0f %s", value, unit);
    }
}
```

## 2. Blood Pressure Dual-Line Chart

### Data Structure:
```java
// MetricHistory needs to support dual values
public class MetricHistory {
    private String value;           // "90/88" for BP
    private String date;
    private String unit;
    
    // NEW: For blood pressure
    private Double systolic;        // 90
    private Double diastolic;       // 88
    
    public double getSystolic() {
        if (systolic != null) return systolic;
        // Parse from "90/88"
        String[] parts = value.split("/");
        return Double.parseDouble(parts[0]);
    }
    
    public double getDiastolic() {
        if (diastolic != null) return diastolic;
        // Parse from "90/88"
        String[] parts = value.split("/");
        return Double.parseDouble(parts[1]);
    }
}
```

### Dual-Line Implementation:
```java
// In ChartConfigurator
public static void configureDualLineChart(
    LineChart chart,
    Context context,
    List<Entry> systolicEntries,
    List<Entry> diastolicEntries,
    List<String> xAxisLabels
) {
    // Systolic line (Red)
    LineDataSet systolicSet = new LineDataSet(systolicEntries, "Tâm thu (Systolic)");
    systolicSet.setColor(ContextCompat.getColor(context, R.color.error_red));
    systolicSet.setLineWidth(3f);
    systolicSet.setDrawCircles(true);
    systolicSet.setCircleColor(ContextCompat.getColor(context, R.color.error_red));
    systolicSet.setCircleRadius(5f);
    systolicSet.setDrawValues(false);
    systolicSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    systolicSet.setDrawFilled(false);
    
    // Diastolic line (Blue)
    LineDataSet diastolicSet = new LineDataSet(diastolicEntries, "Tâm trương (Diastolic)");
    diastolicSet.setColor(ContextCompat.getColor(context, R.color.primary_blue));
    diastolicSet.setLineWidth(3f);
    diastolicSet.setDrawCircles(true);
    diastolicSet.setCircleColor(ContextCompat.getColor(context, R.color.primary_blue));
    diastolicSet.setCircleRadius(5f);
    diastolicSet.setDrawValues(false);
    diastolicSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    diastolicSet.setDrawFilled(false);
    
    LineData lineData = new LineData(systolicSet, diastolicSet);
    chart.setData(lineData);
    
    // Configure axes
    chart.getDescription().setEnabled(false);
    chart.getLegend().setEnabled(true);
    chart.setTouchEnabled(true);
    chart.setDragEnabled(true);
    chart.setScaleEnabled(false);
    chart.setPinchZoom(false);
    chart.setDrawGridBackground(false);
    
    // X-axis
    XAxis xAxis = chart.getXAxis();
    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    xAxis.setDrawGridLines(false);
    xAxis.setGranularity(1f);
    if (xAxisLabels != null && !xAxisLabels.isEmpty()) {
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
    }
    
    // Y-axis
    YAxis leftAxis = chart.getAxisLeft();
    leftAxis.setDrawGridLines(true);
    leftAxis.setGridColor(ContextCompat.getColor(context, R.color.border_color));
    leftAxis.setAxisMinimum(40f);
    leftAxis.setAxisMaximum(200f);
    leftAxis.setValueFormatter(new YAxisFormatter("mmHg"));
    chart.getAxisRight().setEnabled(false);
    
    chart.invalidate();
}
```

## 3. History Item Menu (Delete/Detail)

### Layout: `item_metric_history.xml`
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="@dimen/padding_normal">
    
    <!-- Content -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">
        
        <TextView
            android:id="@+id/tv_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="@dimen/text_size_large"
            android:textStyle="bold"/>
        
        <TextView
            android:id="@+id/tv_date"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="@dimen/text_size_small"
            android:textColor="@color/text_secondary"/>
    </LinearLayout>
    
    <!-- Menu Button -->
    <ImageButton
        android:id="@+id/btn_menu"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@drawable/ic_more_vert"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/app_name"/>
</LinearLayout>
```

### Adapter Implementation:
```java
public class MetricHistoryAdapter extends RecyclerView.Adapter<MetricHistoryAdapter.ViewHolder> {
    
    private List<MetricHistory> historyList = new ArrayList<>();
    private OnHistoryActionListener listener;
    
    public interface OnHistoryActionListener {
        void onDelete(MetricHistory history);
        void onViewDetail(MetricHistory history);
    }
    
    public MetricHistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_metric_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MetricHistory history = historyList.get(position);
        
        holder.tvValue.setText(history.getDisplayValue());
        holder.tvDate.setText(history.getFormattedDate());
        
        // Menu button
        holder.btnMenu.setOnClickListener(v -> {
            showPopupMenu(v, history);
        });
    }
    
    private void showPopupMenu(View view, MetricHistory history) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenuInflater().inflate(R.menu.menu_history_item, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_view_detail) {
                listener.onViewDetail(history);
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                listener.onDelete(history);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvValue;
        TextView tvDate;
        ImageButton btnMenu;
        
        public ViewHolder(View itemView) {
            super(itemView);
            tvValue = itemView.findViewById(R.id.tv_value);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnMenu = itemView.findViewById(R.id.btn_menu);
        }
    }
}
```

### Menu Resource: `menu_history_item.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/action_view_detail"
        android:title="Chi tiết"/>
    <item
        android:id="@+id/action_delete"
        android:title="Xóa"/>
</menu>
```

## Implementation Steps

1. **Update ChartConfigurator**
   - Add Y-axis unit parameter
   - Add Y-axis min/max
   - Create YAxisFormatter class
   - Add configureDualLineChart() method

2. **Update MetricHistory**
   - Add systolic/diastolic fields
   - Add getSystolic()/getDiastolic() methods

3. **Update MetricDetailActivity**
   - Pass Y-axis unit to chart configurator
   - Handle blood pressure dual-line chart
   - Update chart color based on metric type

4. **Update MetricHistoryAdapter**
   - Add menu button to items
   - Implement PopupMenu
   - Add delete/detail callbacks

5. **Create menu_history_item.xml**
   - Delete option
   - Detail option

6. **Create item_metric_history.xml**
   - Update layout with menu button
