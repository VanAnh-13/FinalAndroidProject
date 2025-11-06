package com.example.healthylifehub.ui.profile.reports.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.GeneratedReport;

import java.util.ArrayList;
import java.util.List;

public class GeneratedReportsAdapter extends RecyclerView.Adapter<GeneratedReportsAdapter.ReportViewHolder> {

    private List<GeneratedReport> reports = new ArrayList<>();
    private OnReportClickListener listener;

    public interface OnReportClickListener {
        void onReportClick(GeneratedReport report);
        void onMoreClick(GeneratedReport report);
    }

    public GeneratedReportsAdapter(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void setReports(List<GeneratedReport> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generated_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        GeneratedReport report = reports.get(position);
        holder.bind(report);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivReportIcon;
        private TextView tvReportTitle;
        private TextView tvReportInfo;
        private ImageView ivMore;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReportIcon = itemView.findViewById(R.id.ivReportIcon);
            tvReportTitle = itemView.findViewById(R.id.tvReportTitle);
            tvReportInfo = itemView.findViewById(R.id.tvReportInfo);
            ivMore = itemView.findViewById(R.id.ivMore);
        }

        public void bind(GeneratedReport report) {
            tvReportTitle.setText(report.getTitle());
            tvReportInfo.setText(report.getInfo());

            // Set icon based on report type
            if (report.getType() == GeneratedReport.ReportType.PDF) {
                ivReportIcon.setImageResource(R.drawable.ic_description);
            } else {
                ivReportIcon.setImageResource(R.drawable.ic_chart);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(report);
                }
            });

            ivMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMoreClick(report);
                }
            });
        }
    }
}
