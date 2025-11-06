package com.example.healthylifehub.ui.metrics.analysis.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.RecommendationItem;

public class RecommendationsAdapter extends ListAdapter<RecommendationItem, RecyclerView.ViewHolder> {
	private final OnItemClickListener<RecommendationItem> listener;

	public RecommendationsAdapter(OnItemClickListener<RecommendationItem> listener) {
		super(new DiffUtil.ItemCallback<RecommendationItem>() {
			@Override
			public boolean areItemsTheSame(RecommendationItem oldItem, RecommendationItem newItem) {
				return oldItem.getTitle().equals(newItem.getTitle());
			}

			@Override
			public boolean areContentsTheSame(RecommendationItem oldItem, RecommendationItem newItem) {
				return oldItem.getTitle().equals(newItem.getTitle());
			}
		});
		this.listener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new RecyclerView.ViewHolder(
				LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation, parent, false)
		) {};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		RecommendationItem item = getItem(position);
		holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
	}
}
