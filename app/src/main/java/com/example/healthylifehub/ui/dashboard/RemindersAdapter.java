package com.example.healthylifehub.ui.dashboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthylifehub.databinding.ItemReminderBinding;
import java.util.ArrayList;
import java.util.List;

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
            binding.tvReminderTitle.setText(reminder.getTitle());
            binding.tvReminderTime.setText(reminder.getTime());

            binding.btnSnooze.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSnoozeClicked(reminder);
                }
            });

            binding.btnDone.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDoneClicked(reminder);
                }
            });
        }
    }
}
