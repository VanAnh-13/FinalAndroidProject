package com.example.healthylifehub.ui.reminders.fragment.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.example.healthylifehub.base.BaseAdapter;
import com.example.healthylifehub.base.BaseViewHolder;
import com.example.healthylifehub.databinding.ItemReminderBinding;
import com.example.healthylifehub.data.model.Reminder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersAdapter extends BaseAdapter<Reminder, ItemReminderBinding> {

    private List<Reminder> reminders = new ArrayList<>();
    private List<Reminder> completedReminders = new ArrayList<>(); // Track completed reminders
    private final OnReminderClickListener listener;
    private OnToggleStatusListener toggleStatusListener;
    private static final int MAX_COMPLETED_REMINDERS = 5;

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
    }
    
    public interface OnToggleStatusListener {
        void onToggleStatus(String reminderId, boolean isActive);
    }
    
    public interface OnReminderActionListener {
        void onReminderCompleted(Reminder reminder);
        void onReminderSkipped(Reminder reminder);
        void onReminderDeleted(Reminder reminder);
    }
    
    private OnReminderActionListener actionListener;

    public RemindersAdapter(OnReminderClickListener listener) {
        super(ItemReminderBinding::inflate);
        this.listener = listener;
    }
    
    public void setActionListener(OnReminderActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }
    
    public void setToggleStatusListener(OnToggleStatusListener listener) {
        this.toggleStatusListener = listener;
    }

    @Override
    public void bindData(ItemReminderBinding binding, Reminder reminder, int position) {
        // Không cần implement vì đã xử lý trong onBindViewHolder
    }

    @Override
    public void onItemClick(ItemReminderBinding binding, Reminder reminder, int position) {
        // Không cần implement vì đã xử lý trong onBindViewHolder
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
        // Dùng reminder.isActive() thay vì completedReminders để đồng bộ với Firestore
        boolean isActive = reminder.isActive();

        // Thay đổi icon dựa trên loại nhắc nhở
        updateReminderIcon(binding, reminder);

        // Hiển thị tiêu đề với thời gian
        String timeString = formatTime(reminder.getReminderTime());
        String titleWithTime = timeString + " - " + reminder.getTitle();
        binding.tvReminderTitle.setText(titleWithTime);
        
        // Hiển thị mô tả (nếu có)
        if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
            binding.tvReminderDescription.setText(reminder.getDescription());
            binding.tvReminderDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.tvReminderDescription.setVisibility(android.view.View.GONE);
        }
        
        // Hiển thị tần suất
        String frequencyText = getFrequencyText(reminder.getFrequency());
        binding.tvReminderFrequency.setText(frequencyText);
        
        // Nếu đã hoàn thành (isActive = false): mờ + ẩn nút
        if (!isActive) {
            binding.getRoot().setAlpha(0.6f); // Mờ đi
            binding.switchReminder.setEnabled(false);
            binding.btnDone.setVisibility(android.view.View.GONE); // Ẩn nút hoàn thành
            binding.btnSkip.setVisibility(android.view.View.GONE); // Ẩn nút bỏ
        } else {
            binding.getRoot().setAlpha(1.0f); // Bình thường
            binding.switchReminder.setEnabled(true);
            binding.btnDone.setVisibility(android.view.View.VISIBLE);
            binding.btnSkip.setVisibility(android.view.View.VISIBLE);
        }
        
        // Set trạng thái switch
        binding.switchReminder.setOnCheckedChangeListener(null); // Clear listener trước
        binding.switchReminder.setChecked(reminder.isActive());
        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleStatusListener != null && reminder.getReminderId() != null) {
                toggleStatusListener.onToggleStatus(reminder.getReminderId(), isChecked);
            }
        });
        
        // Xử lý click nút "Hoàn thành" - set isActive = false
        binding.btnDone.setOnClickListener(v -> {
            if (isActive) {
                reminder.setActive(false); // Mark as completed
                notifyItemChanged(position); // Update UI immediately
                if (actionListener != null) {
                    actionListener.onReminderCompleted(reminder);
                }
            }
        });
        
        // Xử lý click nút "Bỏ qua" - xóa nhắc nhở
        binding.btnSkip.setOnClickListener(v -> {
            reminders.remove(reminder);
            completedReminders.remove(reminder);
            notifyItemRemoved(position);
            if (actionListener != null) {
                actionListener.onReminderDeleted(reminder);
            }
        });

        // Click vào item để xem chi tiết
        binding.getRoot().setOnClickListener(v -> {
            if (listener != null && isActive) {
                listener.onReminderClick(reminder);
            }
        });
    }
    
    /**
     * Thay đổi icon dựa trên loại nhắc nhở
     */
    private void updateReminderIcon(ItemReminderBinding binding, Reminder reminder) {
        if (binding.ivReminderIcon == null) return;
        
        String title = reminder.getTitle().toLowerCase();
        int iconRes;
        
        // Detect loại nhắc nhở từ title
        if (title.contains("thuốc") || title.contains("medicine") || title.contains("uống")) {
            iconRes = com.example.healthylifehub.R.drawable.ic_medication;
        } else if (title.contains("khám") || title.contains("bác sĩ") || title.contains("doctor") || title.contains("appointment")) {
            iconRes = com.example.healthylifehub.R.drawable.ic_metrics;
        } else if (title.contains("đo") || title.contains("measure") || title.contains("check")) {
            iconRes = com.example.healthylifehub.R.drawable.ic_metrics;
        } else if (title.contains("ăn") || title.contains("meal") || title.contains("food")) {
            iconRes = com.example.healthylifehub.R.drawable.ic_water_drop;
        } else if (title.contains("nước") || title.contains("water") || title.contains("uống nước")) {
            iconRes = com.example.healthylifehub.R.drawable.ic_water_drop;
        } else if (title.contains("tập") || title.contains("exercise") || title.contains("workout")) {
            iconRes = com.example.healthylifehub.R.drawable.ic_heart;
        } else {
            iconRes = com.example.healthylifehub.R.drawable.ic_reminder;
        }
        
        binding.ivReminderIcon.setImageResource(iconRes);
    }
    
    /**
     * Chuyển đổi tần suất thành tiếng Việt
     */
    private String getFrequencyText(String frequency) {
        if (frequency == null) return "Không xác định";
        
        switch (frequency.toLowerCase()) {
            case "once":
                return "Một lần";
            case "daily":
                return "Hàng ngày";
            case "weekly":
                return "Hàng tuần";
            case "monthly":
                return "Hàng tháng";
            default:
                return frequency;
        }
    }
    
    /**
     * Format thời gian từ milliseconds thành HH:mm
     */
    private String formatTime(long timeInMillis) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timeInMillis));
        } catch (Exception e) {
            return "00:00";
        }
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }
}
