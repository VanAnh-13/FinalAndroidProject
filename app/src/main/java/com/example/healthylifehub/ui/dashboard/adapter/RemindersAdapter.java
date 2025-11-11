package com.example.healthylifehub.ui.dashboard.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.databinding.ItemReminderBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersAdapter extends RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder> {

    private List<Reminder> reminders = new ArrayList<>();
    private OnReminderActionListener listener;

    public interface OnReminderActionListener {
        void onSnoozeClicked(Reminder reminder);
        void onDoneClicked(Reminder reminder);
    }

    public RemindersAdapter(OnReminderActionListener listener) {
        this.listener = listener;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderBinding binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ReminderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        holder.bind(reminders.get(position));
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final ItemReminderBinding binding;

        public ReminderViewHolder(@NonNull ItemReminderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Reminder reminder) {
            boolean isActive = reminder.isActive();
            
            // Hiển thị tiêu đề với thời gian
            String timeString = formatTime(reminder.getReminderTime());
            String titleWithTime = timeString + " - " + reminder.getTitle();
            binding.tvReminderTitle.setText(titleWithTime);
            
            // Hiển thị mô tả nếu có
            if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
                binding.tvReminderDescription.setText(reminder.getDescription());
                binding.tvReminderDescription.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvReminderDescription.setVisibility(android.view.View.GONE);
            }
            
            // Nếu đã hoàn thành: xanh lá + mờ + ẩn nút
            if (!isActive) {
                binding.getRoot().setAlpha(0.6f);
                binding.getRoot().setBackgroundColor(binding.getRoot().getContext().getColor(
                    android.R.color.holo_green_light));
                binding.btnDone.setVisibility(android.view.View.GONE);
                binding.btnSkip.setVisibility(android.view.View.GONE);
                binding.switchReminder.setEnabled(false);
            } else {
                binding.getRoot().setAlpha(1.0f);
                binding.getRoot().setBackgroundColor(binding.getRoot().getContext().getColor(
                    android.R.color.transparent));
                binding.btnDone.setVisibility(android.view.View.VISIBLE);
                binding.btnSkip.setVisibility(android.view.View.VISIBLE);
                binding.switchReminder.setEnabled(true);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && isActive) {
                    listener.onDoneClicked(reminder);
                }
            });
        }
        
        private String formatTime(long timeInMillis) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return sdf.format(new Date(timeInMillis));
            } catch (Exception e) {
                return "00:00";
            }
        }
    }
}
