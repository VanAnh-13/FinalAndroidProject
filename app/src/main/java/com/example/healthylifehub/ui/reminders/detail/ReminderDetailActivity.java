package com.example.healthylifehub.ui.reminders.detail;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.ReminderHistory;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;
import com.example.healthylifehub.utils.ProgressCalculator;
import com.example.healthylifehub.utils.ReminderAlarmManager;
import com.example.healthylifehub.utils.ReminderStatisticsCalculator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReminderDetailActivity extends AppCompatActivity {

    private ImageView ivBack, ivDelete, ivReminderIcon;
    private TextView tvReminderTitle, tvReminderDescription;
    private TextView tvTimeValue, tvRepeatValue, tvMedicationValue, tvNotes;
    private MaterialButton btnMarkTaken, btnSkipReminder, btnReschedule;
    private MaterialButton btnEditReminder, btnDeleteReminder;
    
    // Progress visualization components
    private MaterialCardView cvProgress;
    private TextView tvProgressText, tvDeadlineStatus;
    private ProgressBar pbProgress;
    private TextView tvCompletionRate, tvCurrentStreak, tvBestStreak;
    private ImageView ivShareProgress;
    private MaterialButton btnAdjustProgress;
    
    // History components
    private TextView tvHistoryTitle, tvNoHistory;
    private RecyclerView rvHistory;
    private ReminderHistoryAdapter historyAdapter;
    
    // Statistics data
    private ReminderStatisticsCalculator.ReminderStatistics currentStatistics;
    
    // Performance optimization components
    private com.example.healthylifehub.utils.PerformanceUtils performanceUtils;

    private String reminderId;
    private Reminder currentReminder;
    private RemindersRepository remindersRepository;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_detail);

        initViews();
        loadReminderData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivDelete = findViewById(R.id.iv_delete);
        ivReminderIcon = findViewById(R.id.iv_reminder_icon);
        tvReminderTitle = findViewById(R.id.tv_reminder_title);
        tvReminderDescription = findViewById(R.id.tv_reminder_description);
        tvTimeValue = findViewById(R.id.tv_time_value);
        tvRepeatValue = findViewById(R.id.tv_repeat_value);
        tvMedicationValue = findViewById(R.id.tv_medication_value);
        tvNotes = findViewById(R.id.tv_notes);
        btnMarkTaken = findViewById(R.id.btn_mark_taken);
        btnSkipReminder = findViewById(R.id.btn_skip_reminder);
        btnReschedule = findViewById(R.id.btn_reschedule);
        btnEditReminder = findViewById(R.id.btn_edit_reminder);
        btnDeleteReminder = findViewById(R.id.btn_delete_reminder);
        
        // Initialize progress visualization components
        // TODO: Re-enable when views are added to layout
        /*cvProgress = findViewById(R.id.cv_progress);
        tvProgressText = findViewById(R.id.tv_progress_text);
        tvDeadlineStatus = findViewById(R.id.tv_deadline_status);
        pbProgress = findViewById(R.id.pb_progress);
        tvCompletionRate = findViewById(R.id.tv_completion_rate);
        tvCurrentStreak = findViewById(R.id.tv_current_streak);
        tvBestStreak = findViewById(R.id.tv_best_streak);
        ivShareProgress = findViewById(R.id.iv_share_progress);
        btnAdjustProgress = findViewById(R.id.btn_adjust_progress);
        
        // Initialize history components
        tvHistoryTitle = findViewById(R.id.tv_history_title);
        tvNoHistory = findViewById(R.id.tv_no_history);*/
        rvHistory = findViewById(R.id.rv_history);
        
        // Setup history RecyclerView
        setupHistoryRecyclerView();
        
        // Initialize performance optimization components
        performanceUtils = com.example.healthylifehub.utils.PerformanceUtils.getInstance(this);
        
        remindersRepository = new RemindersRepository();
        database = AppDatabase.getInstance(this);
        
        // Optimize database performance
        performanceUtils.optimizeDatabase(database);
    }
    
    /**
     * Setup the history RecyclerView with adapter and layout manager
     * Requirements: 4.3, 4.4
     */
    private void setupHistoryRecyclerView() {
        historyAdapter = new ReminderHistoryAdapter(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);
        rvHistory.setNestedScrollingEnabled(false); // Since it's inside a ScrollView
    }
    
    /**
     * Load and display reminder history
     * Requirements: 4.3, 4.4, 4.5
     * 
     * @param reminderId The reminder ID to load history for
     */
    private void loadReminderHistory(String reminderId) {
        if (database == null || reminderId == null) {
            showNoHistoryState();
            return;
        }
        
        // Observe history changes using LiveData
        database.reminderHistoryDao().getHistoryByReminderId(reminderId)
            .observe(this, new Observer<java.util.List<ReminderHistory>>() {
                @Override
                public void onChanged(java.util.List<ReminderHistory> historyList) {
                    if (historyList != null && !historyList.isEmpty()) {
                        // Show history
                        showHistoryState();
                        historyAdapter.updateHistory(historyList);
                        
                        // Update progress visualization with statistics
                        updateProgressVisualization(historyList);
                    } else {
                        // Show no history state
                        showNoHistoryState();
                        
                        // Update progress visualization without history
                        updateProgressVisualization(null);
                    }
                }
            });
    }
    
    /**
     * Show the history section with data
     * Requirements: 4.3
     */
    private void showHistoryState() {
        if (tvHistoryTitle != null) tvHistoryTitle.setVisibility(View.VISIBLE);
        if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
        if (tvNoHistory != null) tvNoHistory.setVisibility(View.GONE);
    }
    
    /**
     * Show the no history state
     * Requirements: 4.3
     */
    private void showNoHistoryState() {
        if (tvHistoryTitle != null) tvHistoryTitle.setVisibility(View.VISIBLE);
        if (rvHistory != null) rvHistory.setVisibility(View.GONE);
        if (tvNoHistory != null) tvNoHistory.setVisibility(View.VISIBLE);
    }

    private void loadReminderData() {
        Intent intent = getIntent();
        reminderId = intent.getStringExtra("reminderId");
        
        if (reminderId != null) {
            remindersRepository.getReminderById(reminderId)
                .thenAccept(reminder -> {
                    runOnUiThread(() -> {
                        if (reminder != null) {
                            currentReminder = reminder;
                            displayReminderData(reminder);
                            loadReminderHistory(reminderId);
                        } else {
                            Toast.makeText(this, "Không tìm thấy nhắc nhở", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                });
        } else {
            String title = intent.getStringExtra("reminderTitle");
            String description = intent.getStringExtra("reminderDescription");
            
            if (title != null) {
                tvReminderTitle.setText(title);
                tvReminderDescription.setText(description != null ? description : "");
            }
        }
    }
    
    private void displayReminderData(Reminder reminder) {
        tvReminderTitle.setText(reminder.getTitle());
        tvReminderDescription.setText(reminder.getDescription() != null ? reminder.getDescription() : "");
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());
        tvTimeValue.setText(timeFormat.format(reminder.getReminderTime()));
        
        String frequency = getFrequencyText(reminder.getFrequency());
        tvRepeatValue.setText(frequency);
        
        if (reminder.getMedicineId() != null && !reminder.getMedicineId().isEmpty()) {
            tvMedicationValue.setText(reminder.getMedicineId());
        } else {
            tvMedicationValue.setText("Không có");
        }
        
        tvNotes.setText(reminder.getDescription() != null ? reminder.getDescription() : "Không có ghi chú");
    }
    
    private String getFrequencyText(String frequency) {
        if (frequency == null) return "Một lần";
        switch (frequency) {
            case "daily": return "Hàng ngày";
            case "weekly": return "Hàng tuần";
            case "monthly": return "Hàng tháng";
            default: return "Một lần";
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivDelete.setOnClickListener(v -> showDeleteConfirmation());

        btnMarkTaken.setOnClickListener(v -> {
            performanceUtils.animateButtonPress(v, () -> {
                selectSegmentedButton(btnMarkTaken);
                markReminderAsTaken();
            });
        });

        btnSkipReminder.setOnClickListener(v -> {
            performanceUtils.animateButtonPress(v, () -> {
                selectSegmentedButton(btnSkipReminder);
                skipReminder();
            });
        });

        btnReschedule.setOnClickListener(v -> {
            performanceUtils.animateButtonPress(v, () -> {
                selectSegmentedButton(btnReschedule);
                showRescheduleDialog();
            });
        });

        btnEditReminder.setOnClickListener(v -> {
            if (currentReminder != null) {
                Intent intent = new Intent(this, AddEditReminderActivity.class);
                intent.putExtra("reminderId", currentReminder.getReminderId());
                intent.putExtra("mode", "edit");
                startActivity(intent);
            }
        });

        btnDeleteReminder.setOnClickListener(v -> showDeleteConfirmation());
        
        // Progress visualization listeners
        if (ivShareProgress != null) {
            ivShareProgress.setOnClickListener(v -> shareProgress());
        }
        if (btnAdjustProgress != null) {
            btnAdjustProgress.setOnClickListener(v -> showManualProgressAdjustment());
        }
    }
    
    private void markReminderAsTaken() {
        if (currentReminder == null) {
            Toast.makeText(this, "Không có dữ liệu nhắc nhở", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if ("once".equals(currentReminder.getFrequency())) {
            currentReminder.setActive(false);
            remindersRepository.updateReminder(currentReminder)
                .thenAccept(success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            ReminderAlarmManager.cancelReminder(this, currentReminder.getReminderId());
                            Toast.makeText(this, "✅ Đã đánh dấu hoàn thành", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "❌ Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(currentReminder.getReminderTime());
            
            switch (currentReminder.getFrequency()) {
                case "daily":
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case "weekly":
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "monthly":
                    calendar.add(Calendar.MONTH, 1);
                    break;
            }
            
            currentReminder.setReminderTime(calendar.getTimeInMillis());
            remindersRepository.updateReminder(currentReminder)
                .thenAccept(success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            ReminderAlarmManager.scheduleReminder(this, currentReminder);
                            Toast.makeText(this, "✅ Đã đánh dấu hoàn thành. Lịch nhắc tiếp theo đã được đặt", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "❌ Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
        }
    }
    
    private void skipReminder() {
        if (currentReminder == null) {
            Toast.makeText(this, "Không có dữ liệu nhắc nhở", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentReminder.getReminderTime());
        
        switch (currentReminder.getFrequency()) {
            case "daily":
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case "weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            default:
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                break;
        }
        
        currentReminder.setReminderTime(calendar.getTimeInMillis());
        remindersRepository.updateReminder(currentReminder)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    if (success) {
                        ReminderAlarmManager.scheduleReminder(this, currentReminder);
                        Toast.makeText(this, "⏭️ Đã bỏ qua. Lịch nhắc tiếp theo đã được đặt", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "❌ Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                    }
                });
            });
    }
    
    private void showRescheduleDialog() {
        if (currentReminder == null) {
            Toast.makeText(this, "Không có dữ liệu nhắc nhở", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minuteOfHour) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minuteOfHour);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                
                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                currentReminder.setReminderTime(calendar.getTimeInMillis());
                remindersRepository.updateReminder(currentReminder)
                    .thenAccept(success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                ReminderAlarmManager.scheduleReminder(this, currentReminder);
                                Toast.makeText(this, "🔔 Đã đặt lại lịch nhắc", Toast.LENGTH_SHORT).show();
                                displayReminderData(currentReminder);
                            } else {
                                Toast.makeText(this, "❌ Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
            },
            hour,
            minute,
            true
        );
        timePickerDialog.show();
    }

    private void selectSegmentedButton(MaterialButton selectedButton) {
        // Reset all buttons
        btnMarkTaken.setBackgroundColor(getColor(android.R.color.transparent));
        btnMarkTaken.setTextColor(getColor(R.color.text_secondary));
        
        btnSkipReminder.setBackgroundColor(getColor(android.R.color.transparent));
        btnSkipReminder.setTextColor(getColor(R.color.text_secondary));
        
        btnReschedule.setBackgroundColor(getColor(android.R.color.transparent));
        btnReschedule.setTextColor(getColor(R.color.text_secondary));

        // Highlight selected button
        selectedButton.setBackgroundColor(getColor(R.color.primary));
        selectedButton.setTextColor(getColor(R.color.white));
    }

    private void showDeleteConfirmation() {
        if (currentReminder == null) {
            Toast.makeText(this, "Không có dữ liệu nhắc nhở", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_reminder)
            .setMessage("Bạn có chắc chắn muốn xóa nhắc nhở này?")
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                remindersRepository.deleteReminder(currentReminder.getReminderId())
                    .thenAccept(success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                ReminderAlarmManager.cancelReminder(this, currentReminder.getReminderId());
                                Toast.makeText(this, "🗑️ Đã xóa nhắc nhở", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "❌ Lỗi khi xóa", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Update progress visualization with current reminder data and statistics
     * Requirements: 7.5 - Progress visualization and statistics
     * 
     * @param historyList List of reminder history entries (can be null)
     */
    private void updateProgressVisualization(List<ReminderHistory> historyList) {
        if (currentReminder == null || cvProgress == null) {
            if (cvProgress != null) cvProgress.setVisibility(View.GONE);
            return;
        }
        
        cvProgress.setVisibility(View.VISIBLE);
        
        // Calculate statistics
        currentStatistics = ReminderStatisticsCalculator.calculateStatistics(currentReminder, historyList);
        
        // Update progress bar and text
        updateProgressBar();
        
        // Update deadline status
        updateDeadlineStatus();
        
        // Update statistics display
        updateStatisticsDisplay();
    }
    
    /**
     * Update progress bar display with smooth animation
     * Requirements: 7.5 - Progress visualization with smooth animations
     */
    private void updateProgressBar() {
        if (currentReminder == null || pbProgress == null || tvProgressText == null) return;
        
        float newProgress = currentReminder.getProgressPercentage();
        String progressText = ProgressCalculator.getProgressText(currentReminder);
        
        // Get current progress for animation
        int currentProgress = pbProgress.getProgress();
        
        // Animate progress bar update with color transition
        int oldColor = ProgressCalculator.getProgressColor(currentProgress);
        int newColor = ProgressCalculator.getProgressColor(newProgress);
        
        performanceUtils.animateProgressWithColor(pbProgress, currentProgress, (int) newProgress, oldColor, newColor);
        
        // Animate text update with fade effect
        performanceUtils.fadeOut(tvProgressText, false, () -> {
            tvProgressText.setText(progressText);
            performanceUtils.fadeIn(tvProgressText, null);
        });
    }
    
    /**
     * Update deadline status display
     * Requirements: 7.5 - Deadline status visualization
     */
    private void updateDeadlineStatus() {
        if (tvDeadlineStatus == null) return;
        
        if (currentReminder == null || !currentReminder.hasDeadline()) {
            tvDeadlineStatus.setVisibility(View.GONE);
            return;
        }
        
        tvDeadlineStatus.setVisibility(View.VISIBLE);
        
        if (currentReminder.isExpired()) {
            tvDeadlineStatus.setText(getString(R.string.expired));
            tvDeadlineStatus.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else {
            int daysRemaining = ProgressCalculator.getDaysUntilDeadline(currentReminder);
            if (daysRemaining >= 0) {
                tvDeadlineStatus.setText(getString(R.string.days_remaining, daysRemaining));
                
                // Color code based on urgency
                if (daysRemaining <= 3) {
                    tvDeadlineStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_orange));
                } else {
                    tvDeadlineStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                }
            }
        }
    }
    
    /**
     * Update statistics display (completion rate, streaks)
     * Requirements: 7.5 - Reminder statistics display
     */
    private void updateStatisticsDisplay() {
        if (tvCompletionRate == null || tvCurrentStreak == null || tvBestStreak == null) return;
        
        if (currentStatistics == null) {
            // Show default values
            tvCompletionRate.setText("0%");
            tvCurrentStreak.setText("0");
            tvBestStreak.setText("0");
            return;
        }
        
        // Update completion rate
        String completionRateText = ReminderStatisticsCalculator.formatCompletionRate(
            currentStatistics.getCompletionRate());
        tvCompletionRate.setText(completionRateText);
        
        // Update current streak
        String currentStreakText = ReminderStatisticsCalculator.formatStreak(
            currentStatistics.getCurrentStreak());
        tvCurrentStreak.setText(currentStreakText);
        
        // Update best streak
        String bestStreakText = ReminderStatisticsCalculator.formatStreak(
            currentStatistics.getBestStreak());
        tvBestStreak.setText(bestStreakText);
    }
    
    /**
     * Show manual progress adjustment dialog
     * Requirements: 7.5 - Manual progress adjustment options
     */
    private void showManualProgressAdjustment() {
        if (currentReminder == null) {
            Toast.makeText(this, "Không có dữ liệu nhắc nhở", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create input dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.adjust_progress_title);
        
        // Create input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(String.valueOf(currentReminder.getCompletedCount()));
        input.setText(String.valueOf(currentReminder.getCompletedCount()));
        
        // Set message with current progress
        String message = getString(R.string.adjust_progress_message) + "\n" +
                        getString(R.string.current_completed, 
                                currentReminder.getCompletedCount(), 
                                currentReminder.getTotalExpected());
        builder.setMessage(message);
        builder.setView(input);
        
        builder.setPositiveButton(R.string.adjust, (dialog, which) -> {
            String inputText = input.getText().toString().trim();
            
            if (inputText.isEmpty()) {
                Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int newCompletedCount = Integer.parseInt(inputText);
                
                // Validate input
                if (newCompletedCount < 0) {
                    Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update reminder
                updateReminderProgress(newCompletedCount);
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Update reminder progress with new completed count
     * Requirements: 7.5 - Manual progress adjustment
     * 
     * @param newCompletedCount New completed count
     */
    private void updateReminderProgress(int newCompletedCount) {
        if (currentReminder == null) return;
        
        currentReminder.setCompletedCount(newCompletedCount);
        
        remindersRepository.updateReminder(currentReminder)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, R.string.progress_updated, Toast.LENGTH_SHORT).show();
                        
                        // Refresh progress visualization
                        updateProgressBar();
                        updateDeadlineStatus();
                        
                        // Recalculate statistics if we have history
                        if (currentStatistics != null) {
                            // Reload history to recalculate statistics
                            loadReminderHistory(reminderId);
                        }
                    } else {
                        Toast.makeText(this, R.string.progress_update_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            });
    }
    
    /**
     * Share progress report
     * Requirements: 7.5 - Sharing functionality for progress reports
     */
    private void shareProgress() {
        if (currentReminder == null) {
            Toast.makeText(this, "Không có dữ liệu nhắc nhở", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Prepare share content
        String reminderTitle = currentReminder.getTitle();
        int completed = currentReminder.getCompletedCount();
        int total = currentReminder.getTotalExpected();
        String progressPercentage = String.format("%.0f%%", currentReminder.getProgressPercentage());
        
        int currentStreak = currentStatistics != null ? currentStatistics.getCurrentStreak() : 0;
        int bestStreak = currentStatistics != null ? currentStatistics.getBestStreak() : 0;
        
        String shareText = getString(R.string.share_progress_text,
                reminderTitle,
                completed,
                total,
                progressPercentage,
                currentStreak,
                bestStreak);
        
        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_progress_title));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        // Show share chooser
        Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_via));
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng để chia sẻ", Toast.LENGTH_SHORT).show();
        }
    }
}
