package com.example.healthylifehub.ui.dashboard;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.utils.navigation.DrawerController;
import com.example.healthylifehub.databinding.FragmentDashboardBinding;
import com.example.healthylifehub.ui.dashboard.adapter.RemindersAdapter;
import com.example.healthylifehub.ui.notifications.center.NotificationsCenterActivity;
import com.example.healthylifehub.ui.metrics.detail.MetricDetailActivity;
import com.example.healthylifehub.ui.metrics.add_edit.AddEditMetricActivity;
import com.example.healthylifehub.ui.metrics.analysis.MetricAnalysisActivity;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;
import com.example.healthylifehub.ui.profile.reports.ExportReportsActivity;
import com.example.healthylifehub.ui.analytics.enhanced.EnhancedAnalyticsActivity;
import com.example.healthylifehub.ui.medicines.ocr.MedicineOCRActivity;
import com.example.healthylifehub.utils.chart.ChartConfigurator;
import com.example.healthylifehub.utils.chart.ChartDataProcessor;
import com.example.healthylifehub.utils.app.AccessibilityUtils;
import com.example.healthylifehub.utils.app.ResponsiveDesignUtils;
import com.example.healthylifehub.utils.animation.AnimationUtils;
import com.example.healthylifehub.utils.animation.ButtonAnimationHelper;
import com.example.healthylifehub.utils.animation.LoadingAnimationManager;
import com.example.healthylifehub.data.model.MetricHistory;
import com.example.healthylifehub.R;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends BaseFragment<FragmentDashboardBinding> {

    private DashboardViewModel viewModel;
    private RemindersAdapter remindersAdapter;
    private String currentPeriod = "month"; // Default period

    public DashboardFragment() {
        super(FragmentDashboardBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
    }

    @Override
    public void bindData() {
        setupRemindersList();
        // Load icons from local vector drawables (no network)
        loadDashboardIcons();
        // Setup blood pressure chart
        setupBloodPressureChart();
        // Setup accessibility and responsive design
        setupAccessibilityAndResponsiveDesign();
        // Setup animations for UI elements
        setupAnimations();
    }

    private void loadDashboardIcons() {
        // Metric icons
        getBinding().ivBloodPressureIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_blood_test);
        getBinding().ivBloodSugarIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_water_drop);
        getBinding().ivHeartRateIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_heart);
        getBinding().ivBmiIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_metrics);

        // Quick action icons
        getBinding().ivAddMetricIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_add_circle);
        getBinding().ivAnalysisIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_analytics);
        getBinding().ivReminderIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_reminder);
        getBinding().ivReportsIcon.setImageResource(com.example.healthylifehub.R.drawable.ic_report);
        
        // Log metric types to verify they're correct
        Log.d("DashboardFragment", "Metric types: Heart Rate=" + MetricDetailActivity.METRIC_HEART_RATE + 
                ", Weight=" + MetricDetailActivity.METRIC_BMI);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning to dashboard
        // This ensures name is refreshed after profile edit
        if (viewModel != null) {
            viewModel.refreshUserData();
        }
    }
    
    @Override
    public void observeData() {
        viewModel.getUserName().observe(getViewLifecycleOwner(), userName -> {
            getBinding().tvUserName.setText(userName + "!");
        });

        viewModel.getGreeting().observe(getViewLifecycleOwner(), greeting -> {
            getBinding().tvGreeting.setText(greeting);
        });

        // Observe reminders with loading state handling
        // Requirements: 6.1, 6.3, 6.4 - Update UI when each CompletableFuture completes
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            if (reminders != null) {
                remindersAdapter.setReminders(reminders);
                // Hide loading indicator for reminders section
                if (reminders.isEmpty()) {
                    // Show empty state message
                    Log.d("DashboardFragment", "No reminders available");
                } else {
                    Log.d("DashboardFragment", "Loaded " + reminders.size() + " reminders");
                }
            }
            
            // Handle empty state for reminders
            if (reminders == null || reminders.isEmpty()) {
                // Show empty state message
                Log.d("DashboardFragment", "No reminders for today - showing empty state");
                // Note: Empty state is handled by the adapter showing appropriate message
            } else {
                Log.d("DashboardFragment", "Found " + reminders.size() + " reminders for today");
            }
        });

        // Observe notification count with partial failure handling
        // Requirements: 6.3 - Handle partial failures gracefully
        viewModel.getNotificationCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                getBinding().tvNotificationBadge.setText(String.valueOf(count));
                getBinding().tvNotificationBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                getBinding().tvNotificationBadge.setVisibility(android.view.View.GONE);
            }
        });

        // Observe blood pressure history for chart updates
        viewModel.getBloodPressureHistory().observe(getViewLifecycleOwner(), bloodPressureData -> {
            // When blood pressure data changes, refresh the chart
            setupBloodPressureChart();
            Log.d("DashboardFragment", "Blood pressure data updated, refreshing chart");
        });
    }

    @Override
    public void setOnClick() {
        setupHeaderClicks();
        setupQuickActionClicks();
        setupMetricCardsClicks();
    }

    private void setupRemindersList() {
        remindersAdapter = new RemindersAdapter(new RemindersAdapter.OnReminderActionListener() {
            @Override
            public void onSnoozeClicked(Reminder reminder) {
                viewModel.snoozeReminder(reminder);
                Toast.makeText(getContext(), getString(R.string.reminder_snoozed), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDoneClicked(Reminder reminder) {
                viewModel.completeReminder(reminder);
                Toast.makeText(getContext(), getString(R.string.reminder_completed), Toast.LENGTH_SHORT).show();
            }
        });
        getBinding().rvReminders.setLayoutManager(new LinearLayoutManager(getContext()));
        getBinding().rvReminders.setAdapter(remindersAdapter);
    }

    private void setupHeaderClicks() {
        getBinding().ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof DrawerController) {
                ((DrawerController) getActivity()).openDrawer();
            }
        });

        getBinding().ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotificationsCenterActivity.class);
            startActivity(intent);
        });

        getBinding().tvPeriodSelector.setOnClickListener(v -> {
            showPeriodSelector();
        });
    }

    private void setupQuickActionClicks() {
        getBinding().actionAddMetric.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEditMetricActivity.class);
            startActivity(intent);
        });
        getBinding().actionAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MetricAnalysisActivity.class);
            startActivity(intent);
        });
        getBinding().actionReminder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEditReminderActivity.class);
            startActivity(intent);
        });
        getBinding().actionReports.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ExportReportsActivity.class);
            startActivity(intent);
        });
        getBinding().actionStartActivity.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EnhancedAnalyticsActivity.class);
            startActivity(intent);
        });
        getBinding().actionScanFood.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MedicineOCRActivity.class);
            startActivity(intent);
        });
    }

    private void setupMetricCardsClicks() {
        getBinding().cardBloodPressure.setOnClickListener(v -> openMetricDetail(MetricDetailActivity.METRIC_BLOOD_PRESSURE));
        getBinding().cardBloodSugar.setOnClickListener(v -> openMetricDetail(MetricDetailActivity.METRIC_BLOOD_SUGAR));
        getBinding().cardHeartRate.setOnClickListener(v -> openMetricDetail(MetricDetailActivity.METRIC_HEART_RATE));
        getBinding().cardBmi.setOnClickListener(v -> openMetricDetail(MetricDetailActivity.METRIC_BMI));
    }

    private void openMetricDetail(String metricType) {
        Intent intent = new Intent(getContext(), MetricDetailActivity.class);
        intent.putExtra(MetricDetailActivity.EXTRA_METRIC_TYPE, metricType);
        startActivity(intent);
    }

    /**
     * Setup blood pressure chart on dashboard
     */
    private void setupBloodPressureChart() {
        // Use only actual data from ViewModel - no hardcoded sample data
        List<MetricHistory> actualData = viewModel.getBloodPressureHistory().getValue();
        
        if (actualData == null || actualData.isEmpty()) {
            // Show proper empty state
            getBinding().lineChartBloodPressure.clear();
            getBinding().lineChartBloodPressure.setNoDataText("Chưa có dữ liệu huyết áp\nThêm chỉ số đầu tiên để xem biểu đồ");
            getBinding().lineChartBloodPressure.setNoDataTextColor(getResources().getColor(R.color.text_secondary, null));
            getBinding().lineChartBloodPressure.invalidate();
            Log.d("DashboardFragment", "No blood pressure data available - showing empty state");
            return;
        }

        ChartDataProcessor.BloodPressureData bpData = 
            ChartDataProcessor.processBloodPressureData(actualData, currentPeriod);
        
        if (bpData.isEmpty()) {
            getBinding().lineChartBloodPressure.clear();
            getBinding().lineChartBloodPressure.setNoDataText("Chưa có dữ liệu huyết áp hợp lệ\nKiểm tra định dạng dữ liệu");
            getBinding().lineChartBloodPressure.setNoDataTextColor(getResources().getColor(R.color.text_secondary, null));
            getBinding().lineChartBloodPressure.invalidate();
            Log.d("DashboardFragment", "Invalid blood pressure data format - showing error state");
            return;
        }

        ChartConfigurator.configureDualLineChart(
            getBinding().lineChartBloodPressure,
            getContext(),
            bpData.systolicEntries,
            bpData.diastolicEntries,
            bpData.labels,
            R.color.error_red,     // Systolic color
            R.color.primary_blue,  // Diastolic color
            R.color.divider
        );
        
        Log.d("DashboardFragment", "Blood pressure chart setup completed with " + 
              bpData.systolicEntries.size() + " actual data points");
    }

    /**
     * Show period selector dialog
     */
    private void showPeriodSelector() {
        String[] periods = {"7 Ngày", "30 Ngày", "3 Tháng", "1 Năm"};
        String[] periodValues = {"week", "month", "quarter", "year"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.select_time_period));
        builder.setItems(periods, (dialog, which) -> {
            currentPeriod = periodValues[which];
            getBinding().tvPeriodSelector.setText(periods[which]);
            // Reload chart with new period
            setupBloodPressureChart();
            Log.d("DashboardFragment", "Period changed to: " + currentPeriod);
        });
        builder.show();
    }

    /**
     * Setup accessibility and responsive design enhancements - Stub
     */
    private void setupAccessibilityAndResponsiveDesign() {
        // Stub implementation - removed complex accessibility code
        Log.d("DashboardFragment", "Accessibility setup skipped");
    }
    
    /**
     * Setup smooth animations for UI elements - Stub
     */
    private void setupAnimations() {
        // Stub implementation - removed complex animation code
        Log.d("DashboardFragment", "Animations setup skipped");
    }

}
