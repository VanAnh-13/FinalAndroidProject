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
		if (xAxisLabels != null && !xAxisLabels.isEmpty()) {
			xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
		}

		YAxis leftAxis = chart.getAxisLeft();
		leftAxis.setDrawGridLines(true);
		leftAxis.setGridColor(ContextCompat.getColor(context, gridColorRes));
		
		// Set Y-axis range if provided
		if (yAxisMax > 0) {
			leftAxis.setAxisMinimum(yAxisMin);
			leftAxis.setAxisMaximum(yAxisMax);
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
		if (xAxisLabels != null && !xAxisLabels.isEmpty()) {
			xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
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
}
