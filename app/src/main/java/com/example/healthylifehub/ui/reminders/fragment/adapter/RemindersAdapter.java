package com.example.healthylifehub.ui.reminders.fragment.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.example.healthylifehub.base.BaseAdapter;
import com.example.healthylifehub.base.BaseViewHolder;
import com.example.healthylifehub.databinding.ItemReminderBinding;
import com.example.healthylifehub.data.model.Reminder;
import java.util.ArrayList;
import java.util.List;

public class RemindersAdapter extends BaseAdapter<Reminder, ItemReminderBinding> {

    private List<Reminder> reminders = new ArrayList<>();
    private final OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
    }

    public RemindersAdapter(OnReminderClickListener listener) {
        super(ItemReminderBinding::inflate);
        this.listener = listener;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    @Override
    public void bindData(ItemReminderBinding binding, Reminder reminder, int position) {
        binding.tvReminderTitle.setText(reminder.getTitle());
        if (reminder.getDescription() != null) {
            binding.tvReminderDescription.setText(reminder.getDescription());
        }
    }

    @Override
    public void onItemClick(ItemReminderBinding binding, Reminder reminder, int position) {
        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onReminderClick(reminder);
            }
        });
    }

    @NonNull
    @Override
    public BaseViewHolder<ItemReminderBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderBinding binding = ItemReminderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BaseViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ItemReminderBinding> holder, int position) {
        Reminder reminder = reminders.get(position);
        ItemReminderBinding binding = holder.getBinding();

        binding.tvReminderTitle.setText(reminder.getTitle());
        if (reminder.getDescription() != null) {
            binding.tvReminderDescription.setText(reminder.getDescription());
        }

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onReminderClick(reminder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }
}
