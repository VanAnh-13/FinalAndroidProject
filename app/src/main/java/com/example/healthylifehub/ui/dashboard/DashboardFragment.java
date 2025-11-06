package com.example.healthylifehub.ui.dashboard;

import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentDashboardBinding;
import com.example.healthylifehub.ui.actions.QuickActionsActivity;
import com.example.healthylifehub.ui.dashboard.adapter.RemindersAdapter;
import com.example.healthylifehub.ui.notifications.center.NotificationsCenterActivity;
import com.example.healthylifehub.ui.metrics.detail.MetricDetailActivity;

public class DashboardFragment extends BaseFragment<FragmentDashboardBinding> {

    private DashboardViewModel viewModel;
    private RemindersAdapter remindersAdapter;

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
    }

    @Override
    public void observeData() {
        viewModel.getUserName().observe(getViewLifecycleOwner(), userName -> {
            getBinding().tvUserName.setText(userName + "!");
        });

        viewModel.getGreeting().observe(getViewLifecycleOwner(), greeting -> {
            getBinding().tvGreeting.setText(greeting);
        });

        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            remindersAdapter.setReminders(reminders);
        });

        viewModel.getNotificationCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                getBinding().tvNotificationBadge.setText(String.valueOf(count));
            }
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
                Toast.makeText(getContext(), "Đã báo lại nhắc nhở", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDoneClicked(Reminder reminder) {
                viewModel.completeReminder(reminder);
                Toast.makeText(getContext(), "Đã hoàn thành", Toast.LENGTH_SHORT).show();
            }
        });
        getBinding().rvReminders.setLayoutManager(new LinearLayoutManager(getContext()));
        getBinding().rvReminders.setAdapter(remindersAdapter);
    }

    private void setupHeaderClicks() {
        getBinding().ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.healthylifehub.utils.navigation.DrawerController) {
                ((com.example.healthylifehub.utils.navigation.DrawerController) getActivity()).openDrawer();
            }
        });

        getBinding().ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotificationsCenterActivity.class);
            startActivity(intent);
        });

        getBinding().tvPeriodSelector.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Period selector clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupQuickActionClicks() {
        android.view.View.OnClickListener quickActions = v -> openQuickActions();
        getBinding().actionAddMetric.setOnClickListener(quickActions);
        getBinding().actionAnalysis.setOnClickListener(quickActions);
        getBinding().actionReminder.setOnClickListener(quickActions);
        getBinding().actionReports.setOnClickListener(quickActions);
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

    private void openQuickActions() {
        Intent intent = new Intent(getContext(), QuickActionsActivity.class);
        startActivity(intent);
    }
}
