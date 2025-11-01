package com.example.healthylifehub.ui.dashboard;

import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentDashboardBinding;
import com.example.healthylifehub.ui.actions.QuickActionsActivity;
import com.example.healthylifehub.ui.metrics.MetricDetailActivity;

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

        // Load icons from internet using Glide
        loadDashboardIcons();
    }

    private void loadDashboardIcons() {
        // Load metric icons - Using high quality medical icons from CDN
        // Blood Pressure Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/blood-pressure.png")
                .into(getBinding().ivBloodPressureIcon);

        // Blood Sugar Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/drop-of-blood.png")
                .into(getBinding().ivBloodSugarIcon);

        // Heart Rate Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/cardiogram.png")
                .into(getBinding().ivHeartRateIcon);

        // BMI Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/weight-scale.png")
                .into(getBinding().ivBmiIcon);

        // Quick action icons
        // Add Metric Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/plus-math.png")
                .into(getBinding().ivAddMetricIcon);

        // Analysis Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/combo-chart.png")
                .into(getBinding().ivAnalysisIcon);

        // Reminder Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/appointment-reminders.png")
                .into(getBinding().ivReminderIcon);

        // Reports Icon
        Glide.with(this)
                .load("https://img.icons8.com/color/96/medical-report.png")
                .into(getBinding().ivReportsIcon);
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
        getBinding().ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.healthylifehub.MainActivity) {
                ((com.example.healthylifehub.MainActivity) getActivity()).openDrawer();
            }
        });

        getBinding().ivNotifications.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notifications clicked", Toast.LENGTH_SHORT).show();
        });

        getBinding().tvPeriodSelector.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Period selector clicked", Toast.LENGTH_SHORT).show();
        });

        getBinding().actionAddMetric.setOnClickListener(v -> {
            openQuickActions();
        });

        getBinding().actionAnalysis.setOnClickListener(v -> {
            openQuickActions();
        });

        getBinding().actionReminder.setOnClickListener(v -> {
            openQuickActions();
        });

        getBinding().actionReports.setOnClickListener(v -> {
            openQuickActions();
        });

        // Metric cards click listeners
        getBinding().cardBloodPressure.setOnClickListener(v -> {
            openMetricDetail(MetricDetailActivity.METRIC_BLOOD_PRESSURE);
        });

        getBinding().cardBloodSugar.setOnClickListener(v -> {
            openMetricDetail(MetricDetailActivity.METRIC_BLOOD_SUGAR);
        });

        getBinding().cardHeartRate.setOnClickListener(v -> {
            openMetricDetail(MetricDetailActivity.METRIC_HEART_RATE);
        });

        getBinding().cardBmi.setOnClickListener(v -> {
            openMetricDetail(MetricDetailActivity.METRIC_BMI);
        });
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
