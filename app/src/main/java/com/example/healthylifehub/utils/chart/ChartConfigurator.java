package com.example.healthylifehub.utils.chart;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.List;

public final class ChartConfigurator {
	private ChartConfigurator() {
		throw new AssertionError("No instances");
	}

	public static void configureLineChart(LineChart chart,
			Context context,
			List<Entry> entries,
			List<String> xAxisLabels,
			int lineColorRes,
			int gridColorRes) {
		configureLineChart(chart, context, entries, xAxisLabels, lineColorRes, gridColorRes, "", 0, 0);
	}

	public static void configureLineChart(LineChart chart,
			Context context,
			List<Entry> entries,
			List<String> xAxisLabels,
			int lineColorRes,
			int gridColorRes,
			String yAxisUnit,
			float yAxisMin,
			float yAxisMax) {
		
		// ✅ FIX: Check if data is empty
		if (entries == null || entries.isEmpty()) {
			chart.clear();
			chart.setNoDataText("Không có dữ liệu để hiển thị");
			chart.invalidate();
			return;
		}
		
		LineDataSet dataSet = new LineDataSet(entries, "");
		dataSet.setColor(ContextCompat.getColor(context, lineColorRes));
		dataSet.setLineWidth(3f);
		dataSet.setDrawCircles(true);
		dataSet.setCircleColor(ContextCompat.getColor(context, lineColorRes));
		dataSet.setCircleRadius(5f);
		dataSet.setDrawCircleHole(false);
		dataSet.setDrawValues(false);
		dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
		dataSet.setDrawFilled(true);
		dataSet.setFillColor(ContextCompat.getColor(context, lineColorRes));
		dataSet.setFillAlpha(50);

		chart.setData(new LineData(dataSet));

		chart.getDescription().setEnabled(false);
		chart.getLegend().setEnabled(false);
		chart.setTouchEnabled(true);
		chart.setDragEnabled(true);
		chart.setScaleEnabled(false);
		chart.setPinchZoom(false);
		chart.setDrawGridBackground(false);

		XAxis xAxis = chart.getXAxis();
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setDrawGridLines(false);
		xAxis.setGranularity(1f);
		xAxis.setGranularityEnabled(true);
		xAxis.setAvoidFirstLastClipping(true); // Tránh cắt nhãn đầu và cuối
		
		// ✅ FIX: Set proper X-axis range and label count
		xAxis.setAxisMinimum(-0.5f); // Mở rộng phần trái để tránh cắt điểm đầu
		xAxis.setAxisMaximum(Math.max(0, entries.size() - 0.5f)); // Mở rộng phần phải
		xAxis.setLabelCount(Math.min(entries.size(), 10), false);
		
		// ✅ FIX: Rotate labels if too many to avoid overlap
		if (entries.size() > 5) {
			xAxis.setLabelRotationAngle(-45f);
		}
		
		// ✅ FIX: Đảm bảo hiển thị đúng số lượng nhãn cần thiết
		if (entries.size() <= 10) {
			xAxis.setLabelCount(entries.size(), false);
		} else {
			// Đối với dữ liệu nhiều, giới hạn số lượng nhãn
			int step = Math.max(1, entries.size() / 10);
			xAxis.setLabelCount(entries.size() / step, false);
		}
		
		if (xAxisLabels != null && !xAxisLabels.isEmpty()) {
			xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
			// ✅ FIX: Đảm bảo rằng chúng ta sẽ hiển thị đầy đủ nhãn theo thứ tự đúng
			xAxis.setLabelCount(Math.min(xAxisLabels.size(), 10), false);
		}

		YAxis leftAxis = chart.getAxisLeft();
		leftAxis.setDrawGridLines(true);
		leftAxis.setGridColor(ContextCompat.getColor(context, gridColorRes));
		
		// Set Y-axis range if provided
		if (yAxisMax > 0) {
			leftAxis.setAxisMinimum(yAxisMin);
			leftAxis.setAxisMaximum(yAxisMax);
		} else {
			// ✅ FIX: Auto-calculate Y-axis range with padding
			float minValue = Float.MAX_VALUE;
			float maxValue = Float.MIN_VALUE;
			for (Entry entry : entries) {
				if (entry.getY() < minValue) minValue = entry.getY();
				if (entry.getY() > maxValue) maxValue = entry.getY();
			}
			
			// Add 10% padding
			float padding = (maxValue - minValue) * 0.1f;
			leftAxis.setAxisMinimum(Math.max(0, minValue - padding));
			leftAxis.setAxisMaximum(maxValue + padding);
		}
		
		// Set Y-axis unit formatter if provided
		if (yAxisUnit != null && !yAxisUnit.isEmpty()) {
			leftAxis.setValueFormatter(new YAxisUnitFormatter(yAxisUnit));
		}
		
		chart.getAxisRight().setEnabled(false);

		chart.invalidate();
	}

	/**
	 * Configure dual-line chart for blood pressure (systolic + diastolic)
	 */
	public static void configureDualLineChart(LineChart chart,
			Context context,
			List<Entry> systolicEntries,
			List<Entry> diastolicEntries,
			List<String> xAxisLabels,
			int systolicColorRes,
			int diastolicColorRes,
			int gridColorRes) {
		
		// Sắp xếp lại các Entry để đảm bảo chúng được sắp xếp theo chỉ số tăng dần
		sortEntriesByXValue(systolicEntries);
		sortEntriesByXValue(diastolicEntries);
		
		// ✅ FIX: Check if data is empty
		if ((systolicEntries == null || systolicEntries.isEmpty()) && 
			(diastolicEntries == null || diastolicEntries.isEmpty())) {
			chart.clear();
			chart.setNoDataText("Không có dữ liệu huyết áp để hiển thị");
			chart.invalidate();
			return;
		}
		
		// Systolic line
		LineDataSet systolicSet = new LineDataSet(systolicEntries, "Tâm thu");
		systolicSet.setColor(ContextCompat.getColor(context, systolicColorRes));
		systolicSet.setLineWidth(3f);
		systolicSet.setDrawCircles(true);
		systolicSet.setCircleColor(ContextCompat.getColor(context, systolicColorRes));
		systolicSet.setCircleRadius(5f);
		systolicSet.setDrawCircleHole(false);
		systolicSet.setDrawValues(false);
		systolicSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
		systolicSet.setDrawFilled(false);

		// Diastolic line
		LineDataSet diastolicSet = new LineDataSet(diastolicEntries, "Tâm trương");
		diastolicSet.setColor(ContextCompat.getColor(context, diastolicColorRes));
		diastolicSet.setLineWidth(3f);
		diastolicSet.setDrawCircles(true);
		diastolicSet.setCircleColor(ContextCompat.getColor(context, diastolicColorRes));
		diastolicSet.setCircleRadius(5f);
		diastolicSet.setDrawCircleHole(false);
		diastolicSet.setDrawValues(false);
		diastolicSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
		diastolicSet.setDrawFilled(false);

		LineData lineData = new LineData(systolicSet, diastolicSet);
		chart.setData(lineData);

		chart.getDescription().setEnabled(false);

		// Configure legend
		com.github.mikephil.charting.components.Legend legend = chart.getLegend();
		legend.setEnabled(true);
		legend.setTextSize(12f);
		legend.setTextColor(ContextCompat.getColor(context, android.R.color.black));
		legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.LINE);
		legend.setFormLineWidth(3f);

		chart.setTouchEnabled(true);
		chart.setDragEnabled(true);
		chart.setScaleEnabled(false);
		chart.setPinchZoom(false);
		chart.setDrawGridBackground(false);

		XAxis xAxis = chart.getXAxis();
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setDrawGridLines(false);
		xAxis.setGranularity(1f);
		xAxis.setGranularityEnabled(true);
		xAxis.setAvoidFirstLastClipping(true); // Tránh cắt nhãn đầu và cuối
		
		// ✅ FIX: Set proper X-axis range for dual chart
		int dataSize = Math.max(systolicEntries.size(), diastolicEntries.size());
		xAxis.setAxisMinimum(-0.5f); // Mở rộng phần trái để tránh cắt điểm đầu
		xAxis.setAxisMaximum(Math.max(0, dataSize - 0.5f)); // Mở rộng phần phải
		xAxis.setLabelCount(Math.min(dataSize, 10), false);
		xAxis.setAvoidFirstLastClipping(true); // Tránh cắt nhãn đầu và cuối
		
		// ✅ FIX: Rotate labels if too many
		if (dataSize > 7) {
			xAxis.setLabelRotationAngle(-45f);
		}
		
		if (xAxisLabels != null && !xAxisLabels.isEmpty()) {
			xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
			// ✅ FIX: Đảm bảo rằng chúng ta sẽ hiển thị đầy đủ nhãn theo thứ tự đúng
			xAxis.setLabelCount(Math.min(xAxisLabels.size(), 10), false);
		}

		YAxis leftAxis = chart.getAxisLeft();
		leftAxis.setDrawGridLines(true);
		leftAxis.setGridColor(ContextCompat.getColor(context, gridColorRes));
		leftAxis.setAxisMinimum(40f);
		leftAxis.setAxisMaximum(200f);
		leftAxis.setValueFormatter(new YAxisUnitFormatter("mmHg"));
		chart.getAxisRight().setEnabled(false);

		chart.invalidate();
	}

	/**
	 * Custom Y-axis formatter to display unit
	 */
	public static class YAxisUnitFormatter extends ValueFormatter {
		private final String unit;

		public YAxisUnitFormatter(String unit) {
			this.unit = unit;
		}

		@Override
		public String getFormattedValue(float value) {
			return String.format("%.0f %s", value, unit);
		}
	}
	
	/**
	 * Sắp xếp danh sách Entry theo giá trị X tăng dần
	 * Điều này quan trọng để đảm bảo rằng biểu đồ hiển thị chính xác
	 */
	private static void sortEntriesByXValue(List<Entry> entries) {
		if (entries != null && !entries.isEmpty()) {
			entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));
		}
	}
}
