package com.example.healthylifehub.ui.reminders.add_edit;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.utils.ReminderAlarmManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

public class AddEditReminderActivity extends AppCompatActivity {

    private ImageView ivClose;
    private TextView btnSave;
    private ChipGroup chipGroupType;
    private TextInputEditText etTitle, etNote, etTime;
    private AutoCompleteTextView actMedicine, actRepeat;
    private MaterialButton btnSaveReminder;
    private RemindersRepository remindersRepository;

    private String selectedTime = "08:00";
    private boolean isEditMode = false;
    private String editingReminderId = null;
    private Reminder currentReminder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_reminder);

        initViews();
        checkEditMode();
        setupListeners();
        setupDropdowns();
    }

    private void initViews() {
        ivClose = findViewById(R.id.iv_close);
        btnSave = findViewById(R.id.btn_save);
        chipGroupType = findViewById(R.id.chip_group_type);
        etTitle = findViewById(R.id.et_title);
        etNote = findViewById(R.id.et_note);
        etTime = findViewById(R.id.et_time);
        actMedicine = findViewById(R.id.act_medicine);
        actRepeat = findViewById(R.id.act_repeat);
        btnSaveReminder = findViewById(R.id.btn_save_reminder);
        
        remindersRepository = new RemindersRepository();

        etTime.setText(selectedTime);
    }
    
    private void checkEditMode() {
        Intent intent = getIntent();
        editingReminderId = intent.getStringExtra("reminderId");
        String mode = intent.getStringExtra("mode");
        
        if (editingReminderId != null && "edit".equals(mode)) {
            isEditMode = true;
            btnSave.setText("Cập nhật");
            btnSaveReminder.setText("Cập nhật nhắc nhở");
            
            loadReminderData(editingReminderId);
        } else {
            isEditMode = false;
            btnSaveReminder.setText("Lưu nhắc nhở");
        }
    }
    
    private void loadReminderData(String reminderId) {
        remindersRepository.getReminderById(reminderId)
            .thenAccept(reminder -> {
                runOnUiThread(() -> {
                    if (reminder != null) {
                        currentReminder = reminder;
                        fillFormWithReminderData(reminder);
                    } else {
                        Toast.makeText(this, "Không tìm thấy nhắc nhở", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
                return null;
            });
    }
    
    private void fillFormWithReminderData(Reminder reminder) {
        // Fill title
        etTitle.setText(reminder.getTitle());
        
        // Fill description
        etNote.setText(reminder.getDescription());
        
        // Fill time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(reminder.getReminderTime());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        etTime.setText(selectedTime);
        
        // Fill medicine
        if (reminder.getMedicineId() != null && !reminder.getMedicineId().isEmpty()) {
            actMedicine.setText(reminder.getMedicineId(), false);
        }
        
        // Fill repeat frequency
        String frequencyText = convertFrequencyToRepeatText(reminder.getFrequency());
        actRepeat.setText(frequencyText, false);
        
        // Select reminder type chip (default to medication)
        selectReminderTypeChip("Thuốc");
    }
    
    /**
     * Select chip based on reminder type
     */
    private void selectReminderTypeChip(String reminderType) {
        for (int i = 0; i < chipGroupType.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupType.getChildAt(i);
            if (chip.getText().toString().equals(reminderType)) {
                chip.setChecked(true);
                break;
            }
        }
    }
    
    private String convertFrequencyToRepeatText(String frequency) {
        if (frequency == null) return getString(R.string.no_repeat);
        switch (frequency) {
            case "daily": return getString(R.string.daily);
            case "weekly": return getString(R.string.weekly);
            case "monthly": return getString(R.string.monthly);
            default: return getString(R.string.no_repeat);
        }
    }

    private void setupListeners() {
        ivClose.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> saveReminder());
        
        btnSaveReminder.setOnClickListener(v -> saveReminder());

        etTime.setOnClickListener(v -> showTimePicker());
    }

    private void setupDropdowns() {
        // Medicine dropdown
        String[] medicines = {
            getString(R.string.no_medicine),
            "Paracetamol 500mg",
            "Amoxicillin 250mg",
            "Metformin 500mg",
            "Aspirin 100mg"
        };
        ArrayAdapter<String> medicineAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            medicines
        );
        actMedicine.setAdapter(medicineAdapter);
        actMedicine.setText(medicines[0], false);

        // Repeat dropdown
        String[] repeatOptions = {
            getString(R.string.no_repeat),
            getString(R.string.daily),
            getString(R.string.weekly),
            getString(R.string.monthly),
            getString(R.string.custom)
        };
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            repeatOptions
        );
        actRepeat.setAdapter(repeatAdapter);
        actRepeat.setText(repeatOptions[0], false);
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minuteOfHour) -> {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                etTime.setText(selectedTime);
            },
            hour,
            minute,
            true
        );
        timePickerDialog.show();
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedChipId = chipGroupType.getCheckedChipId();
        Chip selectedChip = findViewById(selectedChipId);
        String reminderType = selectedChip != null ? selectedChip.getText().toString() : "";

        String description = etNote.getText().toString().trim();
        String medicine = actMedicine.getText().toString();
        String repeatText = actRepeat.getText().toString();
        
        String frequency = convertRepeatToFrequency(repeatText);
        long reminderTime = convertTimeToTimestamp(selectedTime);

        Reminder reminder;
        if (isEditMode && currentReminder != null) {
            reminder = currentReminder;
            reminder.setTitle(title);
            reminder.setDescription(description);
            reminder.setReminderTime(reminderTime);
            reminder.setFrequency(frequency);
        } else {
            reminder = new Reminder();
            reminder.setTitle(title);
            reminder.setDescription(description);
            reminder.setReminderTime(reminderTime);
            reminder.setFrequency(frequency);
            reminder.setActive(true);
        }
        
        if (medicine != null && !medicine.equals(getString(R.string.no_medicine))) {
            reminder.setMedicineId(medicine);
        }
        
        btnSaveReminder.setEnabled(false);
        btnSaveReminder.setText(isEditMode ? "Đang cập nhật..." : "Đang lưu...");
        
        if (isEditMode) {
            remindersRepository.updateReminder(reminder)
                .thenAccept(success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            ReminderAlarmManager.cancelReminder(this, reminder.getReminderId());
                            ReminderAlarmManager.scheduleReminder(this, reminder);
                            
                            Toast.makeText(this, "✅ Đã cập nhật nhắc nhở!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "❌ Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                            btnSaveReminder.setEnabled(true);
                            btnSaveReminder.setText("Cập nhật nhắc nhở");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSaveReminder.setEnabled(true);
                        btnSaveReminder.setText("Cập nhật nhắc nhở");
                    });
                    return null;
                });
        } else {
            remindersRepository.createReminder(reminder)
                .thenAccept(reminderId -> {
                    runOnUiThread(() -> {
                        if (reminderId != null) {
                            reminder.setReminderId(reminderId);
                            
                            ReminderAlarmManager.scheduleReminder(this, reminder);
                            
                            Toast.makeText(this, "✅ Đã tạo nhắc nhở thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "❌ Lỗi khi tạo nhắc nhở", Toast.LENGTH_SHORT).show();
                            btnSaveReminder.setEnabled(true);
                            btnSaveReminder.setText("Lưu nhắc nhở");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSaveReminder.setEnabled(true);
                        btnSaveReminder.setText("Lưu nhắc nhở");
                    });
                    return null;
                });
        }
    }
    
    /**
     * Chuyển đổi text repeat thành frequency code
     */
    private String convertRepeatToFrequency(String repeatText) {
        if (repeatText.equals(getString(R.string.daily))) {
            return "daily";
        } else if (repeatText.equals(getString(R.string.weekly))) {
            return "weekly";
        } else if (repeatText.equals(getString(R.string.monthly))) {
            return "monthly";
        } else {
            return "once";
        }
    }
    
    /**
     * Chuyển đổi thời gian "HH:mm" thành timestamp (milliseconds)
     */
    private long convertTimeToTimestamp(String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            // Nếu thời gian đã qua trong ngày hôm nay, chuyển sang ngày mai
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
