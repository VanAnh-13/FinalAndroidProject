package com.example.healthylifehub.ui.metrics.analysis.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.AlertItem;

public class AlertsAdapter extends ListAdapter<AlertItem, RecyclerView.ViewHolder> {
	private final OnItemClickListener<AlertItem> listener;

	public AlertsAdapter(OnItemClickListener<AlertItem> listener) {
		super(new DiffUtil.ItemCallback<AlertItem>() {
			@Override
			public boolean areItemsTheSame(AlertItem oldItem, AlertItem newItem) {
				return oldItem.getTitle().equals(newItem.getTitle());
			}

			@Override
			public boolean areContentsTheSame(AlertItem oldItem, AlertItem newItem) {
				return oldItem.getTitle().equals(newItem.getTitle());
			}
		});
		this.listener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new RecyclerView.ViewHolder(
				LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false)
		) {};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		AlertItem item = getItem(position);
		holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
	}
}
