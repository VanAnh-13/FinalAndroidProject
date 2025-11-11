package com.example.healthylifehub.ui.notifications.center;

import android.os.Bundle;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.NotificationItem;
import com.example.healthylifehub.data.repository.NotificationsRepository;
import com.example.healthylifehub.databinding.ActivityNotificationsCenterBinding;
import com.example.healthylifehub.ui.notifications.center.adapter.NotificationsAdapter;
import com.example.healthylifehub.utils.filters.FilterStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsCenterActivity extends BaseActivity<ActivityNotificationsCenterBinding> {

    private NotificationsAdapter adapter;
    private List<NotificationItem> allNotifications = new ArrayList<>();
    private Map<Integer, FilterStrategy<NotificationItem>> chipStrategies;
    private NotificationsRepository notificationsRepository;

    public NotificationsCenterActivity() {
        super(ActivityNotificationsCenterBinding::inflate);
    }

    @Override
    public void initData() {
        notificationsRepository = new NotificationsRepository();
        adapter = new NotificationsAdapter(notification -> {
            Toast.makeText(this, "Clicked: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
        });
        
        // Set delete listener
        adapter.setDeleteListener((notification, position) -> {
            // Delete from Firestore using document ID
            if (notification.getId() != null) {
                notificationsRepository.deleteNotification(notification.getId())
                    .thenAccept(success -> {
                        if (success) {
                            Toast.makeText(NotificationsCenterActivity.this, "✅ Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                            allNotifications.remove(position);
                            adapter.notifyItemRemoved(position);
                        } else {
                            Toast.makeText(NotificationsCenterActivity.this, "❌ Lỗi khi xóa", Toast.LENGTH_SHORT).show();
                        }
                    });
            } else {
                Toast.makeText(NotificationsCenterActivity.this, "❌ Không thể xóa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void bindData() {
        getBinding().tvTitle.setText("Thông báo");
        getBinding().rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvNotifications.setAdapter(adapter);
        
        // Load notifications from Firebase
        loadNotifications();

        // Default filter: All
        applyFilter(null);

        // Setup filter strategies (Strategy Pattern)
        setupChipStrategies();
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());
        getBinding().chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                applyFilter(null);
                return;
            }
            int id = checkedIds.get(0);
            FilterStrategy<NotificationItem> strategy = chipStrategies != null ? chipStrategies.get(id) : null;
            if (strategy == null) {
                applyFilter(null);
            } else {
                List<NotificationItem> filtered = strategy.apply(allNotifications);
                adapter.setNotifications(filtered);
            }
        });
    }

    private void loadNotifications() {
        // Load notifications from Firebase
        LiveData<List<NotificationItem>> notificationsLiveData = notificationsRepository.loadNotifications();
        
        notificationsLiveData.observe(this, notifications -> {
            if (notifications != null) {
                allNotifications.clear();
                allNotifications.addAll(notifications);
                applyFilter(null);
            }
        });
        
    }

    private void applyFilter(NotificationItem.NotificationType type) {
        if (type == null) {
            adapter.setNotifications(allNotifications);
            return;
        }
        List<NotificationItem> filtered = new ArrayList<>();
        for (NotificationItem item : allNotifications) {
            if (item.getType() == type) {
                filtered.add(item);
            }
        }
        adapter.setNotifications(filtered);
    }

    private void setupChipStrategies() {
        chipStrategies = new HashMap<>();
        // All
        chipStrategies.put(R.id.chip_all, source -> source);
        chipStrategies.put(R.id.chip_medication, source -> {
            List<NotificationItem> out = new ArrayList<>();
            for (NotificationItem item : source) {
                if (item.getType() == NotificationItem.NotificationType.MEDICATION) out.add(item);
            }
            return out;
        });
        chipStrategies.put(R.id.chip_activity, source -> {
            List<NotificationItem> out = new ArrayList<>();
            for (NotificationItem item : source) {
                if (item.getType() == NotificationItem.NotificationType.ACTIVITY) out.add(item);
            }
            return out;
        });
        chipStrategies.put(R.id.chip_appointments, source -> {
            List<NotificationItem> out = new ArrayList<>();
            for (NotificationItem item : source) {
                if (item.getType() == NotificationItem.NotificationType.APPOINTMENT) out.add(item);
            }
            return out;
        });
        chipStrategies.put(R.id.chip_insights, source -> {
            List<NotificationItem> out = new ArrayList<>();
            for (NotificationItem item : source) {
                if (item.getType() == NotificationItem.NotificationType.INSIGHT) out.add(item);
            }
            return out;
        });
    }
}
