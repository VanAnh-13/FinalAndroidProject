package com.example.healthylifehub.ui.metrics.list.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseAdapter;
import com.example.healthylifehub.base.BaseViewHolder;
import com.example.healthylifehub.databinding.ItemMetricBinding;
import com.example.healthylifehub.data.model.MetricItem;
import com.example.healthylifehub.ui.metrics.detail.MetricDetailActivity;
import java.util.ArrayList;
import java.util.List;

public class MetricsAdapter extends BaseAdapter<MetricItem, ItemMetricBinding> {

    private List<MetricItem> metrics = new ArrayList<>();
    private final OnMetricClickListener listener;

    public interface OnMetricClickListener {
        void onMetricClick(MetricItem metric);
    }

    public MetricsAdapter(OnMetricClickListener listener) {
        super(ItemMetricBinding::inflate);
        this.listener = listener;
    }

    @Override
    public void bindData(ItemMetricBinding binding, MetricItem metric, int position) {
        binding.tvMetricValue.setText(metric.getValue());
        binding.tvMetricNote.setText(metric.getNote());
        binding.tvMetricType.setText(metric.getTypeName());
        binding.tvMetricTime.setText(metric.getTime());

        // Set icon based on metric type
        int iconRes = getIconForType(metric.getType());
        binding.ivMetricIcon.setImageResource(iconRes);
    }

    @Override
    public void onItemClick(ItemMetricBinding binding, MetricItem metric, int position) {
        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onMetricClick(metric);
            }
        });
    }

    public void setMetrics(List<MetricItem> metrics) {
        this.metrics = metrics;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BaseViewHolder<ItemMetricBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMetricBinding binding = ItemMetricBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BaseViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ItemMetricBinding> holder, int position) {
        MetricItem metric = metrics.get(position);
        ItemMetricBinding binding = holder.getBinding();

        binding.tvMetricValue.setText(metric.getValue());
        binding.tvMetricNote.setText(metric.getNote());
        binding.tvMetricType.setText(metric.getTypeName());
        binding.tvMetricTime.setText(metric.getTime());

        // Set icon based on metric type
        int iconRes = getIconForType(metric.getType());
        binding.ivMetricIcon.setImageResource(iconRes);

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onMetricClick(metric);
            }
        });
    }

    @Override
    public int getItemCount() {
        return metrics.size();
    }

    private int getIconForType(String type) {
        switch (type) {
            case MetricDetailActivity.METRIC_BLOOD_PRESSURE:
                return R.drawable.ic_health_tracking;
            case MetricDetailActivity.METRIC_BLOOD_SUGAR:
                return R.drawable.ic_blood_test;
            case MetricDetailActivity.METRIC_HEART_RATE:
                return R.drawable.ic_health_tracking;
            case "weight":
                return R.drawable.ic_metrics;
            default:
                return R.drawable.ic_health_tracking;
        }
    }
}
