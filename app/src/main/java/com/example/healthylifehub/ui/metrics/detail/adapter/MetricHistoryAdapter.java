package com.example.healthylifehub.ui.metrics.detail.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public interface OnHistoryClickListener {
        void onHistoryClick(MetricHistory history);
    }

    public MetricHistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryValue = itemView.findViewById(R.id.tv_history_value);
            tvHistoryDate = itemView.findViewById(R.id.tv_history_date);

            itemView.findViewById(R.id.history_item_container).setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryClick(historyList.get(position));
                }
            });
        }

        public void bind(MetricHistory history) {
            tvHistoryValue.setText(history.getDisplayValue());
            tvHistoryDate.setText(history.getDate());
        }
    }
}
