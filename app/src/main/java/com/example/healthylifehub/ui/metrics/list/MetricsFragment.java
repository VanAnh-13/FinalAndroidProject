package com.example.healthylifehub.ui.metrics.list;

import android.content.Intent;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentMetricsBinding;
import com.example.healthylifehub.ui.notifications.center.NotificationsCenterActivity;
import com.example.healthylifehub.ui.metrics.detail.MetricDetailActivity;
import com.example.healthylifehub.ui.metrics.add_edit.AddEditMetricActivity;
import com.example.healthylifehub.ui.metrics.list.adapter.MetricsAdapter;
import com.google.android.material.tabs.TabLayout;
import com.example.healthylifehub.data.model.MetricItem;

public class MetricsFragment extends BaseFragment<FragmentMetricsBinding> {

    private MetricsViewModel viewModel;
    private MetricsAdapter adapter;
    private java.util.List<MetricItem> allMetrics = new java.util.ArrayList<>();
    private java.util.Map<Integer, com.example.healthylifehub.utils.filters.FilterStrategy<MetricItem>> filterStrategies;

    public MetricsFragment() {
        super(FragmentMetricsBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(MetricsViewModel.class);
        adapter = new MetricsAdapter(metric -> {
            // Navigate to metric detail
            Intent intent = new Intent(requireContext(), MetricDetailActivity.class);
            intent.putExtra(MetricDetailActivity.EXTRA_METRIC_TYPE, metric.getType());
            startActivity(intent);
        });
    }

    @Override
    public void bindData() {
        // Setup RecyclerView
        getBinding().rvMetrics.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvMetrics.setAdapter(adapter);

        // Setup Tabs
        setupTabs();

        // Setup filter strategies (Strategy Pattern)
        setupFilterStrategies();
    }

    @Override
    public void observeData() {
        // Observe metrics from Firebase
        viewModel.getMetrics().observe(getViewLifecycleOwner(), metrics -> {
            if (metrics != null) {
                allMetrics.clear();
                allMetrics.addAll(metrics);
                // Re-apply current filter
                filterMetrics(getBinding().tabLayout.getSelectedTabPosition());
            }
        });
    }

    @Override
    public void setOnClick() {
        getBinding().ivSearch.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Tìm kiếm - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        getBinding().ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NotificationsCenterActivity.class);
            startActivity(intent);
        });

        getBinding().btnSort.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Sắp xếp - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnFilter.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Bộ lọc - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        getBinding().fabAddMetric.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditMetricActivity.class);
            startActivity(intent);
        });
    }

    private void setupTabs() {
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.all_metrics));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.blood_pressure_tab));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.blood_sugar_tab));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.weight_tab));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.heart_rate_tab));

        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Filter metrics based on selected tab
                filterMetrics(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void filterMetrics(int position) {
        com.example.healthylifehub.utils.filters.FilterStrategy<MetricItem> strategy =
                filterStrategies != null ? filterStrategies.get(position) : null;
        if (strategy == null) {
            adapter.setMetrics(allMetrics);
            return;
        }
        adapter.setMetrics(strategy.apply(allMetrics));
    }


    private void setupFilterStrategies() {
        filterStrategies = new java.util.HashMap<>();

        // 0: All
        filterStrategies.put(0, source -> source);

        // 1: Blood Pressure
        filterStrategies.put(1, source -> {
            java.util.List<MetricItem> out = new java.util.ArrayList<>();
            for (MetricItem item : source) {
                if (MetricDetailActivity.METRIC_BLOOD_PRESSURE.equals(item.getType())) {
                    out.add(item);
                }
            }
            return out;
        });

        // 2: Blood Sugar
        filterStrategies.put(2, source -> {
            java.util.List<MetricItem> out = new java.util.ArrayList<>();
            for (MetricItem item : source) {
                if (MetricDetailActivity.METRIC_BLOOD_SUGAR.equals(item.getType())) {
                    out.add(item);
                }
            }
            return out;
        });

        // 3: Weight
        filterStrategies.put(3, source -> {
            java.util.List<MetricItem> out = new java.util.ArrayList<>();
            for (MetricItem item : source) {
                if ("weight".equals(item.getType())) {
                    out.add(item);
                }
            }
            return out;
        });

        // 4: Heart Rate
        filterStrategies.put(4, source -> {
            java.util.List<MetricItem> out = new java.util.ArrayList<>();
            for (MetricItem item : source) {
                if (MetricDetailActivity.METRIC_HEART_RATE.equals(item.getType())) {
                    out.add(item);
                }
            }
            return out;
        });
    }
}
