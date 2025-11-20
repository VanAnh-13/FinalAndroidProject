package com.example.healthylifehub.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.NotificationHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHistoryAdapter extends ListAdapter<NotificationHistory, NotificationHistoryAdapter.ViewHolder> {
    
    private final OnNotificationClickListener listener;
    
    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationHistory notification);
    }
    
    public NotificationHistoryAdapter(OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    private static final DiffUtil.ItemCallback<NotificationHistory> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<NotificationHistory>() {
            @Override
            public boolean areItemsTheSame(@NonNull NotificationHistory oldItem, @NonNull NotificationHistory newItem) {
                return oldItem.getNotificationId().equals(newItem.getNotificationId());
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull NotificationHistory oldItem, @NonNull NotificationHistory newItem) {
                return oldItem.isRead() == newItem.isRead() &&
                       oldItem.getTitle().equals(newItem.getTitle()) &&
                       oldItem.getMessage().equals(newItem.getMessage());
            }
        };
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_notification_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationHistory notification = getItem(position);
        holder.bind(notification, listener);
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView messageView;
        private final TextView timeView;
        private final View unreadIndicator;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.iconNotification);
            titleView = itemView.findViewById(R.id.textTitle);
            messageView = itemView.findViewById(R.id.textMessage);
            timeView = itemView.findViewById(R.id.textTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
        
        public void bind(NotificationHistory notification, OnNotificationClickListener listener) {
            titleView.setText(notification.getTitle());
            messageView.setText(notification.getMessage());
            timeView.setText(formatTime(notification.getTimestamp()));
            
            // Set icon based on type
            int iconRes = R.drawable.ic_notifications_active;
            if (NotificationHistory.TYPE_REMINDER.equals(notification.getType())) {
                iconRes = R.drawable.ic_notifications_active;
            } else if (NotificationHistory.TYPE_PROMOTIONAL.equals(notification.getType())) {
                iconRes = R.drawable.ic_star;
            }
            iconView.setImageResource(iconRes);
            
            // Show/hide unread indicator
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            
            // Set background for unread
            itemView.setAlpha(notification.isRead() ? 0.7f : 1.0f);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }
        
        private String formatTime(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            if (diff < 60000) { // < 1 minute
                return "Vừa xong";
            } else if (diff < 3600000) { // < 1 hour
                return (diff / 60000) + " phút trước";
            } else if (diff < 86400000) { // < 1 day
                return (diff / 3600000) + " giờ trước";
            } else if (diff < 604800000) { // < 1 week
                return (diff / 86400000) + " ngày trước";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
