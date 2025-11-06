package com.example.healthylifehub.ui.notifications.center.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.example.healthylifehub.base.BaseAdapter;
import com.example.healthylifehub.base.BaseViewHolder;
import com.example.healthylifehub.data.model.NotificationItem;
import com.example.healthylifehub.databinding.ItemNotificationBinding;
import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends BaseAdapter<NotificationItem, ItemNotificationBinding> {

    private List<NotificationItem> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
    }

    public NotificationsAdapter(OnNotificationClickListener listener) {
        super(ItemNotificationBinding::inflate);
        this.listener = listener;
    }

    @Override
    public void bindData(ItemNotificationBinding binding, NotificationItem notification, int position) {
        binding.tvNotificationTitle.setText(notification.getTitle());
        binding.tvNotificationMessage.setText(notification.getMessage());
        binding.tvNotificationTime.setText(notification.getTime());

        // Set icon based on notification type
        int iconRes = getIconForType(notification.getType());
        binding.ivNotificationIcon.setImageResource(iconRes);
    }

    @Override
    public void onItemClick(ItemNotificationBinding binding, NotificationItem notification, int position) {
        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BaseViewHolder<ItemNotificationBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BaseViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ItemNotificationBinding> holder, int position) {
        NotificationItem notification = notifications.get(position);
        ItemNotificationBinding binding = holder.getBinding();

        binding.tvNotificationTitle.setText(notification.getTitle());
        binding.tvNotificationMessage.setText(notification.getMessage());
        binding.tvNotificationTime.setText(notification.getTime());

        // Set icon based on notification type
        int iconRes = getIconForType(notification.getType());
        binding.ivNotificationIcon.setImageResource(iconRes);

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    private int getIconForType(NotificationItem.NotificationType type) {
        switch (type) {
            case MEDICATION:
                return com.example.healthylifehub.R.drawable.ic_medication;
            case ACTIVITY:
                return com.example.healthylifehub.R.drawable.ic_directions_run;
            case APPOINTMENT:
                return com.example.healthylifehub.R.drawable.ic_calendar;
            case INSIGHT:
                return com.example.healthylifehub.R.drawable.ic_lightbulb;
            case WARNING:
                return com.example.healthylifehub.R.drawable.ic_notifications_active;
            case GENERAL:
            default:
                return com.example.healthylifehub.R.drawable.ic_notifications;
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}
