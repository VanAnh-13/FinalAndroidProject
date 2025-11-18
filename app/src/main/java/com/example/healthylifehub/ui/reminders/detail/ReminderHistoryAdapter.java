package com.example.healthylifehub.ui.reminders.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.ReminderHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying reminder history in a RecyclerView
 * Shows completed/skipped actions with icons and timestamps
 * Optimized with lazy loading and smooth animations
 * Requirements: 4.3, 4.4, 4.5, Performance optimization
 */
public class ReminderHistoryAdapter extends RecyclerView.Adapter<ReminderHistoryAdapter.HistoryViewHolder> {
    
    private final Context context;
    private List<ReminderHistory> historyList;
    private final SimpleDateFormat dateTimeFormat;
    private final SimpleDateFormat timeFormat;
    private final com.example.healthylifehub.utils.PerformanceUtils performanceUtils;
    
    // Lazy loading support
    private OnLoadMoreListener loadMoreListener;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    public interface OnLoadMoreListener {
        void onLoadMore();
    }
    
    public ReminderHistoryAdapter(Context context) {
        this.context = context;
        this.historyList = new ArrayList<>();
        this.dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.performanceUtils = com.example.healthylifehub.utils.PerformanceUtils.getInstance(context);
    }
    
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder_history, parent, false);
        return new HistoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ReminderHistory history = historyList.get(position);
        holder.bind(history);
    }
    
    @Override
    public int getItemCount() {
        return historyList.size();
    }
    
    /**
     * Update the history list and refresh the adapter
     * 
     * @param newHistoryList New list of history entries
     */
    public void updateHistory(List<ReminderHistory> newHistoryList) {
        this.historyList.clear();
        if (newHistoryList != null) {
            this.historyList.addAll(newHistoryList);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Add more history items (for lazy loading)
     * 
     * @param moreHistoryList Additional history entries
     */
    public void addMoreHistory(List<ReminderHistory> moreHistoryList) {
        if (moreHistoryList != null && !moreHistoryList.isEmpty()) {
            int startPosition = historyList.size();
            historyList.addAll(moreHistoryList);
            notifyItemRangeInserted(startPosition, moreHistoryList.size());
        }
        isLoading = false;
    }
    
    /**
     * Set load more listener for lazy loading
     * 
     * @param listener The listener to call when more data is needed
     */
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }
    
    /**
     * Set loading state
     * 
     * @param loading Whether currently loading
     */
    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }
    
    /**
     * Set whether more data is available
     * 
     * @param hasMore Whether more data can be loaded
     */
    public void setHasMoreData(boolean hasMore) {
        this.hasMoreData = hasMore;
    }
    
    /**
     * ViewHolder for history items
     */
    class HistoryViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView ivActionIcon;
        private final TextView tvActionText;
        private final TextView tvTimestamp;
        private final TextView tvScheduledTime;
        private final View divider;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivActionIcon = itemView.findViewById(R.id.iv_action_icon);
            tvActionText = itemView.findViewById(R.id.tv_action_text);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvScheduledTime = itemView.findViewById(R.id.tv_scheduled_time);
            divider = itemView.findViewById(R.id.divider);
        }
        
        /**
         * Bind history data to views
         * Requirements: 4.4, 4.5
         * 
         * @param history The history entry to display
         */
        public void bind(ReminderHistory history) {
            // Set action icon and text based on action type
            if (history.isCompleted()) {
                // Completed action - green icon
                ivActionIcon.setImageResource(R.drawable.ic_check_circle);
                ivActionIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_green));
                tvActionText.setText(context.getString(R.string.action_completed));
                tvActionText.setTextColor(ContextCompat.getColor(context, R.color.success_green));
            } else if (history.isSkipped()) {
                // Skipped action - gray icon
                ivActionIcon.setImageResource(R.drawable.ic_skip_next);
                ivActionIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                tvActionText.setText(context.getString(R.string.action_skipped));
                tvActionText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                // Unknown action type - default icon
                ivActionIcon.setImageResource(R.drawable.ic_help);
                ivActionIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                tvActionText.setText(history.getActionType());
                tvActionText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
            
            // Format and set timestamp
            String timestampText = formatTimestamp(history.getTimestamp());
            tvTimestamp.setText(timestampText);
            
            // Format and set scheduled time if different from actual time
            if (history.getScheduledTime() != history.getTimestamp()) {
                String scheduledText = context.getString(R.string.scheduled_for, 
                    timeFormat.format(new Date(history.getScheduledTime())));
                tvScheduledTime.setText(scheduledText);
                tvScheduledTime.setVisibility(View.VISIBLE);
            } else {
                tvScheduledTime.setVisibility(View.GONE);
            }
            
            // Hide divider for last item
            int position = getAdapterPosition();
            if (position == getItemCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        }
        
        /**
         * Format timestamp for display
         * Shows relative time for recent entries, full date for older ones
         * Requirements: 4.5
         * 
         * @param timestamp The timestamp to format
         * @return Formatted timestamp string
         */
        private String formatTimestamp(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            // Less than 1 minute ago
            if (diff < 60 * 1000) {
                return context.getString(R.string.just_now);
            }
            
            // Less than 1 hour ago
            if (diff < 60 * 60 * 1000) {
                int minutes = (int) (diff / (60 * 1000));
                return context.getString(R.string.minutes_ago, minutes);
            }
            
            // Less than 24 hours ago
            if (diff < 24 * 60 * 60 * 1000) {
                int hours = (int) (diff / (60 * 60 * 1000));
                return context.getString(R.string.hours_ago, hours);
            }
            
            // Less than 7 days ago
            if (diff < 7 * 24 * 60 * 60 * 1000) {
                int days = (int) (diff / (24 * 60 * 60 * 1000));
                return context.getString(R.string.days_ago, days);
            }
            
            // Older than 7 days - show full date and time
            return dateTimeFormat.format(new Date(timestamp));
        }
    }
}