package com.example.healthylifehub.ui.reminders.detail;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;
import com.example.healthylifehub.utils.ReminderAlarmManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderDetailActivity extends AppCompatActivity {

    private ImageView ivBack, ivDelete, ivReminderIcon;
    private TextView tvReminderTitle, tvReminderDescription;
    private TextView tvTimeValue, tvRepeatValue, tvMedicationValue, tvNotes;
    private MaterialButton btnMarkTaken, btnSkipReminder, btnReschedule;
    private MaterialButton btnEditReminder, btnDeleteReminder;

    private String reminderId;
    private Reminder currentReminder;
    private RemindersRepository remindersRepository;

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
        
        remindersRepository = new RemindersRepository();
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
            selectSegmentedButton(btnMarkTaken);
            markReminderAsTaken();
        });

        btnSkipReminder.setOnClickListener(v -> {
            selectSegmentedButton(btnSkipReminder);
            skipReminder();
        });

        btnReschedule.setOnClickListener(v -> {
            selectSegmentedButton(btnReschedule);
            showRescheduleDialog();
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
}
