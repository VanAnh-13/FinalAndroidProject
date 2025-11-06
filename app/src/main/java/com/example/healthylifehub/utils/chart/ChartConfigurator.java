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
		chart.getAxisRight().setEnabled(false);

		chart.invalidate();
	}
}
