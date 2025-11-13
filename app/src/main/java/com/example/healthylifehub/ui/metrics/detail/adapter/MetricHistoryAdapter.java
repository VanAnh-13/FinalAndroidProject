package com.example.healthylifehub.ui.metrics.detail.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.MetricHistory;
import java.util.ArrayList;
import java.util.List;

public class MetricHistoryAdapter extends RecyclerView.Adapter<MetricHistoryAdapter.ViewHolder> {

    private List<MetricHistory> historyList = new ArrayList<>();
    private OnHistoryClickListener listener;
    private OnHistoryActionListener actionListener;

    public interface OnHistoryClickListener {
        void onHistoryClick(MetricHistory history);
    }

    public interface OnHistoryActionListener {
        void onViewDetail(MetricHistory history);
        void onDelete(MetricHistory history);
    }

    public MetricHistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    public void setActionListener(OnHistoryActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_metric_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MetricHistory history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<MetricHistory> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvHistoryValue;
        private TextView tvHistoryDate;
        private ImageView ivMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryValue = itemView.findViewById(R.id.tv_history_value);
            tvHistoryDate = itemView.findViewById(R.id.tv_history_date);
            ivMore = itemView.findViewById(R.id.iv_more);

            itemView.findViewById(R.id.history_item_container).setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryClick(historyList.get(position));
                }
            });

            // Menu button
            ivMore.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(v, historyList.get(position));
                }
            });
        }

        public void bind(MetricHistory history) {
            // Display value only (unit is shown in metric label)
            tvHistoryValue.setText(history.getValue());
            tvHistoryDate.setText(history.getDate());
        }

        private void showPopupMenu(View view, MetricHistory history) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_history_item, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_view_detail) {
                    if (actionListener != null) {
                        actionListener.onViewDetail(history);
                    }
                    return true;
                } else if (item.getItemId() == R.id.action_delete) {
                    if (actionListener != null) {
                        actionListener.onDelete(history);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
