package com.example.healthylifehub.ui.metrics.list;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentMetricsBinding;
import com.example.healthylifehub.databinding.DialogSearchMetricBinding;
import com.example.healthylifehub.databinding.DialogFilterMetricBinding;
import com.example.healthylifehub.ui.notifications.center.NotificationsCenterActivity;
import com.example.healthylifehub.ui.metrics.detail.MetricDetailActivity;
import com.example.healthylifehub.ui.metrics.add_edit.AddEditMetricActivity;
import com.example.healthylifehub.ui.metrics.list.adapter.MetricsAdapter;
import com.google.android.material.tabs.TabLayout;
import com.example.healthylifehub.data.model.MetricItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MetricsFragment extends BaseFragment<FragmentMetricsBinding> {

    private MetricsViewModel viewModel;
    private MetricsAdapter adapter;
    private List<MetricItem> allMetrics = new ArrayList<>();
    private List<MetricItem> filteredMetrics = new ArrayList<>();
    private java.util.Map<Integer, com.example.healthylifehub.utils.filters.FilterStrategy<MetricItem>> filterStrategies;
    
    // Filter and sort state
    private String currentSortOrder = "newest"; // newest, oldest, value_high, value_low
    private Long filterDateFrom = null;
    private Long filterDateTo = null;
    private Double filterValueFrom = null;
    private Double filterValueTo = null;
    private String searchQuery = "";
    private int currentTabPosition = 0;

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
                applyFiltersAndSort();
            }
        });
    }

    @Override
    public void setOnClick() {
        getBinding().ivSearch.setOnClickListener(v -> showSearchDialog());

        getBinding().ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NotificationsCenterActivity.class);
            startActivity(intent);
        });

        getBinding().btnSort.setOnClickListener(v -> showSortOptions());

        getBinding().btnFilter.setOnClickListener(v -> showFilterDialog());

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
                currentTabPosition = tab.getPosition();
                applyFiltersAndSort();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
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
    
    // ==================== SEARCH ====================
    
    private void showSearchDialog() {
        DialogSearchMetricBinding dialogBinding = DialogSearchMetricBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();
        
        dialogBinding.btnSearch.setOnClickListener(v -> {
            searchQuery = dialogBinding.etSearch.getText().toString().trim();
            applyFiltersAndSort();
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.toast_found_results, filteredMetrics.size()), Toast.LENGTH_SHORT).show();
        });
        
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    // ==================== SORT ====================
    
    private void showSortOptions() {
        String[] options = {"Mới nhất", "Cũ nhất", "Giá trị cao nhất", "Giá trị thấp nhất"};
        int currentSelection = currentSortOrder.equals("newest") ? 0 : 
                              currentSortOrder.equals("oldest") ? 1 :
                              currentSortOrder.equals("value_high") ? 2 : 3;
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Sắp xếp theo")
                .setSingleChoiceItems(options, currentSelection, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortOrder = "newest";
                            getBinding().btnSort.setText("Mới nhất");
                            break;
                        case 1:
                            currentSortOrder = "oldest";
                            getBinding().btnSort.setText("Cũ nhất");
                            break;
                        case 2:
                            currentSortOrder = "value_high";
                            getBinding().btnSort.setText("Giá trị cao");
                            break;
                        case 3:
                            currentSortOrder = "value_low";
                            getBinding().btnSort.setText("Giá trị thấp");
                            break;
                    }
                    applyFiltersAndSort();
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    // ==================== FILTER ====================
    
    private void showFilterDialog() {
        DialogFilterMetricBinding dialogBinding = DialogFilterMetricBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // Set current filter values
        if (filterDateFrom != null) {
            dialogBinding.etDateFrom.setText(dateFormat.format(filterDateFrom));
        }
        if (filterDateTo != null) {
            dialogBinding.etDateTo.setText(dateFormat.format(filterDateTo));
        }
        if (filterValueFrom != null) {
            dialogBinding.etValueFrom.setText(String.valueOf(filterValueFrom));
        }
        if (filterValueTo != null) {
            dialogBinding.etValueTo.setText(String.valueOf(filterValueTo));
        }
        
        // Date pickers
        dialogBinding.etDateFrom.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (filterDateFrom != null) {
                calendar.setTimeInMillis(filterDateFrom);
            }
            
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                filterDateFrom = selected.getTimeInMillis();
                dialogBinding.etDateFrom.setText(dateFormat.format(filterDateFrom));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        dialogBinding.etDateTo.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (filterDateTo != null) {
                calendar.setTimeInMillis(filterDateTo);
            }
            
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 23, 59, 59);
                filterDateTo = selected.getTimeInMillis();
                dialogBinding.etDateTo.setText(dateFormat.format(filterDateTo));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        // Apply button
        dialogBinding.btnApply.setOnClickListener(v -> {
            // Get value filters
            String valueFromStr = dialogBinding.etValueFrom.getText().toString().trim();
            String valueToStr = dialogBinding.etValueTo.getText().toString().trim();
            
            filterValueFrom = valueFromStr.isEmpty() ? null : Double.parseDouble(valueFromStr);
            filterValueTo = valueToStr.isEmpty() ? null : Double.parseDouble(valueToStr);
            
            applyFiltersAndSort();
            dialog.dismiss();
            
            int activeFilters = 0;
            if (filterDateFrom != null || filterDateTo != null) activeFilters++;
            if (filterValueFrom != null || filterValueTo != null) activeFilters++;
            
            if (activeFilters > 0) {
                getBinding().btnFilter.setText("Bộ lọc (" + activeFilters + ")");
            } else {
                getBinding().btnFilter.setText("Bộ lọc");
            }
            
            Toast.makeText(requireContext(), getString(R.string.toast_filtered_metrics, filteredMetrics.size()), Toast.LENGTH_SHORT).show();
        });
        
        // Reset button
        dialogBinding.btnReset.setOnClickListener(v -> {
            filterDateFrom = null;
            filterDateTo = null;
            filterValueFrom = null;
            filterValueTo = null;
            dialogBinding.etDateFrom.setText("");
            dialogBinding.etDateTo.setText("");
            dialogBinding.etValueFrom.setText("");
            dialogBinding.etValueTo.setText("");
            getBinding().btnFilter.setText(getString(R.string.filter));
            applyFiltersAndSort();
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.toast_filter_cleared), Toast.LENGTH_SHORT).show();
        });
        
        dialog.show();
    }
    
    // ==================== APPLY FILTERS AND SORT ====================
    
    private void applyFiltersAndSort() {
        // First apply tab filter
        com.example.healthylifehub.utils.filters.FilterStrategy<MetricItem> tabStrategy =
                filterStrategies != null ? filterStrategies.get(currentTabPosition) : null;
        
        List<MetricItem> tabFiltered = (tabStrategy != null) ? tabStrategy.apply(allMetrics) : allMetrics;
        
        // Then apply additional filters
        filteredMetrics = new ArrayList<>();
        
        for (MetricItem metric : tabFiltered) {
            boolean passFilter = true;
            
            // Date filter - skip for now as MetricItem doesn't have timestamp
            // TODO: Add timestamp to MetricItem model
            
            // Value filter
            if (passFilter && (filterValueFrom != null || filterValueTo != null)) {
                try {
                    double value = Double.parseDouble(metric.getValue());
                    if (filterValueFrom != null && value < filterValueFrom) {
                        passFilter = false;
                    }
                    if (filterValueTo != null && value > filterValueTo) {
                        passFilter = false;
                    }
                } catch (NumberFormatException e) {
                    // Skip if value is not numeric
                }
            }
            
            // Search filter
            if (passFilter && !searchQuery.isEmpty()) {
                String searchText = (metric.getValue() + " " + 
                                   (metric.getNote() != null ? metric.getNote() : "")).toLowerCase();
                if (!searchText.contains(searchQuery.toLowerCase())) {
                    passFilter = false;
                }
            }
            
            if (passFilter) {
                filteredMetrics.add(metric);
            }
        }
        
        // Apply sort
        switch (currentSortOrder) {
            case "newest":
                // Sort by time string (assuming format allows string comparison)
                Collections.sort(filteredMetrics, (m1, m2) -> {
                    String t1 = m1.getTime() != null ? m1.getTime() : "";
                    String t2 = m2.getTime() != null ? m2.getTime() : "";
                    return t2.compareTo(t1); // Descending
                });
                break;
            case "oldest":
                Collections.sort(filteredMetrics, (m1, m2) -> {
                    String t1 = m1.getTime() != null ? m1.getTime() : "";
                    String t2 = m2.getTime() != null ? m2.getTime() : "";
                    return t1.compareTo(t2); // Ascending
                });
                break;
            case "value_high":
                Collections.sort(filteredMetrics, (m1, m2) -> {
                    try {
                        double v1 = Double.parseDouble(m1.getValue());
                        double v2 = Double.parseDouble(m2.getValue());
                        return Double.compare(v2, v1); // Descending
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
                break;
            case "value_low":
                Collections.sort(filteredMetrics, (m1, m2) -> {
                    try {
                        double v1 = Double.parseDouble(m1.getValue());
                        double v2 = Double.parseDouble(m2.getValue());
                        return Double.compare(v1, v2); // Ascending
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
                break;
        }
        
        adapter.setMetrics(filteredMetrics);
    }
}
