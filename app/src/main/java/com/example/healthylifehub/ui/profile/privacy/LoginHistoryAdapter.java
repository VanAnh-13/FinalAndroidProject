package com.example.healthylifehub.ui.profile.privacy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthylifehub.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for login history RecyclerView
 */
public class LoginHistoryAdapter extends RecyclerView.Adapter<LoginHistoryAdapter.ViewHolder> {
    
    private List<LoginHistoryViewModel.LoginHistoryItem> loginHistory = new ArrayList<>();
    
    public void setLoginHistory(List<LoginHistoryViewModel.LoginHistoryItem> history) {
        this.loginHistory = history;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_login_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoginHistoryViewModel.LoginHistoryItem item = loginHistory.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return loginHistory.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDate;
        private TextView tvTime;
        private TextView tvDevice;
        private TextView tvLocation;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDevice = itemView.findViewById(R.id.tvDevice);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
        
        public void bind(LoginHistoryViewModel.LoginHistoryItem item) {
            tvDate.setText(item.getDate());
            tvTime.setText(item.getTime());
            tvDevice.setText(item.getDevice());
            tvLocation.setText("IP: " + item.getLocation());
        }
    }
}