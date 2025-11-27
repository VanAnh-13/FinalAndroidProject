package com.example.healthylifehub.ui.dashboard.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.databinding.ItemReminderBinding;
import com.example.healthylifehub.utils.app.ProgressCalculator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersAdapter extends RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder> {

    private List<Reminder> reminders = new ArrayList<>();
    private OnReminderActionListener listener;
    private com.example.healthylifehub.utils.performance.PerformanceUtils performanceUtils;

    public interface OnReminderActionListener {
        void onSnoozeClicked(Reminder reminder);
        void onDoneClicked(Reminder reminder);
    }

    public RemindersAdapter(OnReminderActionListener listener) {
        this.listener = listener;
    }

    public void setReminders(List<Reminder> reminders) {
        // Filter and limit reminders for dashboard display
        this.reminders = filterRemindersForDashboard(reminders);
        notifyDataSetChanged();
    }
    
    /**
     * Filter reminders for dashboard display:
     * - Only TODAY's reminders (both active and completed)
     * - Maximum 3 most recent completed reminders from today
     */
    private List<Reminder> filterRemindersForDashboard(List<Reminder> allReminders) {
        if (allReminders == null) return new ArrayList<>();
        
        // Get today's date range
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        long startOfDay = today.getTimeInMillis();
        
        today.set(java.util.Calendar.HOUR_OF_DAY, 23);
        today.set(java.util.Calendar.MINUTE, 59);
        today.set(java.util.Calendar.SECOND, 59);
        today.set(java.util.Calendar.MILLISECOND, 999);
        long endOfDay = today.getTimeInMillis();
        
        List<Reminder> todayActiveReminders = new ArrayList<>();
        List<Reminder> todayCompletedReminders = new ArrayList<>();
        
        // Filter reminders for today only
        for (Reminder reminder : allReminders) {
            long reminderTime = reminder.getReminderTime();
            
            // Check if reminder is scheduled for today
            if (reminderTime >= startOfDay && reminderTime <= endOfDay) {
                if (reminder.isActive()) {
                    todayActiveReminders.add(reminder);
                } else {
                    todayCompletedReminders.add(reminder);
                }
            }
        }
        
        // Sort completed reminders by completion time (most recent first)
        todayCompletedReminders.sort((r1, r2) -> Long.compare(r2.getReminderTime(), r1.getReminderTime()));
        
        // Take only the 3 most recent completed reminders from today
        List<Reminder> recentCompleted = todayCompletedReminders.size() > 3 
            ? todayCompletedReminders.subList(0, 3) 
            : todayCompletedReminders;
        
        // Combine today's active + recent completed
        List<Reminder> filteredReminders = new ArrayList<>();
        filteredReminders.addAll(todayActiveReminders);
        filteredReminders.addAll(recentCompleted);
        
        return filteredReminders;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderBinding binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        
        // Initialize performance utils if not already done
        if (performanceUtils == null) {
            performanceUtils = com.example.healthylifehub.utils.performance.PerformanceUtils.getInstance(parent.getContext());
        }
        
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
            
            // Display title with time
            String timeString = formatTime(reminder.getReminderTime());
            String titleWithTime = timeString + " - " + reminder.getTitle();
            binding.tvReminderTitle.setText(titleWithTime);
            
            // Display description if available
            if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
                binding.tvReminderDescription.setText(reminder.getDescription());
                binding.tvReminderDescription.setVisibility(View.VISIBLE);
            } else {
                binding.tvReminderDescription.setVisibility(View.GONE);
            }
            
            // Display frequency
            binding.tvReminderFrequency.setText(getFrequencyText(reminder.getFrequency()));
            
            // Progress tracking implementation
            setupProgressTracking(reminder);
            
            // Status icon implementation
            setupStatusIcon(reminder);
            
            // Deadline display implementation
            setupDeadlineDisplay(reminder);
            
            // Handle completed vs active reminder styling
            if (!isActive) {
                binding.getRoot().setAlpha(0.7f);
                binding.getRoot().setBackgroundColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), R.color.success_green));
                binding.getRoot().getBackground().setAlpha(30);
                binding.btnDone.setVisibility(View.GONE);
                binding.btnSkip.setVisibility(View.GONE);
                if (binding.switchReminder != null) {
                    binding.switchReminder.setEnabled(false);
                }
            } else {
                binding.getRoot().setAlpha(1.0f);
                binding.getRoot().setBackgroundColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), android.R.color.transparent));
                binding.btnDone.setVisibility(View.VISIBLE);
                binding.btnSkip.setVisibility(View.VISIBLE);
                if (binding.switchReminder != null) {
                    binding.switchReminder.setEnabled(true);
                }
            }

            // Set up click listeners with animations
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && isActive) {
                    performanceUtils.animateButtonPress(v, () -> {
                        listener.onDoneClicked(reminder);
                    });
                }
            });
            
            binding.btnDone.setOnClickListener(v -> {
                if (listener != null) {
                    performanceUtils.animateButtonPress(v, () -> {
                        listener.onDoneClicked(reminder);
                    });
                }
            });
            
            binding.btnSkip.setOnClickListener(v -> {
                if (listener != null) {
                    performanceUtils.animateButtonPress(v, () -> {
                        listener.onSnoozeClicked(reminder);
                    });
                }
            });
        }
        
        /**
         * Set up progress tracking UI elements
         */
        private void setupProgressTracking(Reminder reminder) {
            if (reminder.hasDeadline()) {
                // Show progress section
                binding.layoutProgress.setVisibility(View.VISIBLE);
                
                // Calculate and display progress
                float progress = reminder.getProgressPercentage();
                binding.progressBar.setProgress((int) progress);
                
                // Set progress bar color based on completion percentage
                int progressColor = ProgressCalculator.getProgressColor(progress);
                binding.progressBar.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(binding.getRoot().getContext(), progressColor)));
                
                // Set progress text
                String progressText = ProgressCalculator.getProgressText(reminder);
                binding.tvProgressText.setText(progressText);
                binding.tvProgressText.setVisibility(View.VISIBLE);
                
            } else {
                // Hide progress section for unlimited reminders
                binding.layoutProgress.setVisibility(View.GONE);
            }
        }
        
        /**
         * Set up status icon for completed and expired reminders
         */
        private void setupStatusIcon(Reminder reminder) {
            if (reminder.isCompleted()) {
                // Show celebration icon for completed reminders
                binding.ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                binding.ivStatusIcon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.success_green)));
                binding.ivStatusIcon.setVisibility(View.VISIBLE);
                
            } else if (reminder.isExpired()) {
                // Show warning icon for expired reminders
                binding.ivStatusIcon.setImageResource(R.drawable.ic_warning);
                binding.ivStatusIcon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.warning_orange)));
                binding.ivStatusIcon.setVisibility(View.VISIBLE);
                
            } else {
                // Hide status icon for active, non-expired reminders
                binding.ivStatusIcon.setVisibility(View.GONE);
            }
        }
        
        /**
         * Set up deadline display with proper formatting
         */
        private void setupDeadlineDisplay(Reminder reminder) {
            if (reminder.hasDeadline()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String deadlineText = "Hết hạn: " + dateFormat.format(new Date(reminder.getDeadline()));
                
                // Add warning if approaching deadline (within 3 days)
                if (ProgressCalculator.isApproachingDeadline(reminder)) {
                    int daysRemaining = ProgressCalculator.getDaysUntilDeadline(reminder);
                    deadlineText += " (còn " + daysRemaining + " ngày)";
                    binding.tvDeadline.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.warning_orange));
                } else if (reminder.isExpired()) {
                    deadlineText += " - Đã hết hạn";
                    binding.tvDeadline.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.error_red));
                } else {
                    binding.tvDeadline.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.text_secondary));
                }
                
                binding.tvDeadline.setText(deadlineText);
                binding.tvDeadline.setVisibility(View.VISIBLE);
            } else {
                binding.tvDeadline.setVisibility(View.GONE);
            }
        }
        
        /**
         * Get localized frequency text
         */
        private String getFrequencyText(String frequency) {
            if (frequency == null) return "";
            
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
