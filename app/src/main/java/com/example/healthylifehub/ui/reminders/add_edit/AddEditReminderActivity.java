package com.example.healthylifehub.ui.reminders.add_edit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.utils.error.ErrorHandler;
import com.example.healthylifehub.utils.app.ProgressCalculator;
import com.example.healthylifehub.utils.reminder.ReminderAlarmManager;
import com.example.healthylifehub.utils.reminder.ReminderLogger;
import com.example.healthylifehub.utils.reminder.ReminderValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditReminderActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS";
    
    private ImageView ivClose;
    private TextView btnSave;
    private ChipGroup chipGroupType;
    private TextInputEditText etTitle, etNote, etTime, etDeadline;
    private AutoCompleteTextView actMedicine, actRepeat;
    private MaterialButton btnSaveReminder;
    private MaterialCardView cardDeadlineWarning;
    private TextView tvDeadlineWarning;
    private RemindersRepository remindersRepository;

    private String selectedTime = "08:00";
    private Calendar selectedDeadline = null;
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
        etDeadline = findViewById(R.id.et_deadline);
        actMedicine = findViewById(R.id.act_medicine);
        actRepeat = findViewById(R.id.act_repeat);
        btnSaveReminder = findViewById(R.id.btn_save_reminder);
        cardDeadlineWarning = findViewById(R.id.card_deadline_warning);
        tvDeadlineWarning = findViewById(R.id.tv_deadline_warning);
        
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
                        Toast.makeText(this, getString(R.string.toast_reminder_not_found), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error, throwable.getMessage()), Toast.LENGTH_SHORT).show();
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
        
        // Fill deadline
        if (reminder.getDeadline() != null) {
            selectedDeadline = Calendar.getInstance();
            selectedDeadline.setTimeInMillis(reminder.getDeadline());
            updateDeadlineField();
            checkDeadlineWarning();
        }
        
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
        
        etDeadline.setOnClickListener(v -> showDeadlinePicker());
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

    /**
     * Show deadline date picker with validation
     */
    private void showDeadlinePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // If deadline is already selected, use that date
        if (selectedDeadline != null) {
            calendar = (Calendar) selectedDeadline.clone();
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                selectedDate.set(Calendar.HOUR_OF_DAY, 23);
                selectedDate.set(Calendar.MINUTE, 59);
                selectedDate.set(Calendar.SECOND, 59);
                selectedDate.set(Calendar.MILLISECOND, 999);
                
                // Validate deadline is in the future
                if (validateDeadline(selectedDate)) {
                    selectedDeadline = selectedDate;
                    updateDeadlineField();
                    checkDeadlineWarning();
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to tomorrow
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        datePickerDialog.getDatePicker().setMinDate(tomorrow.getTimeInMillis());
        
        datePickerDialog.show();
    }

    /**
     * Validate that deadline is in the future with comprehensive error handling
     * Requirements: 2.2
     */
    private boolean validateDeadline(Calendar deadline) {
        ReminderValidator.ValidationResult result = ReminderValidator.validateDeadline(deadline);
        
        if (!result.isValid) {
            // Log validation error
            ReminderLogger.logValidation("deadline", false, result.errorMessage);
            
            // Handle validation error
            ErrorHandler.handleValidationError(this, result.errorCode, "deadline");
            
            // Show user-friendly message
            Toast.makeText(this, result.errorMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Log successful validation
        ReminderLogger.logValidation("deadline", true, null);
        return true;
    }

    /**
     * Update deadline field display
     */
    private void updateDeadlineField() {
        if (selectedDeadline != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etDeadline.setText(dateFormat.format(selectedDeadline.getTime()));
        } else {
            etDeadline.setText("");
        }
    }

    /**
     * Check if deadline is within 3 days and show warning
     */
    private void checkDeadlineWarning() {
        if (selectedDeadline == null) {
            cardDeadlineWarning.setVisibility(View.GONE);
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar threeDaysFromNow = Calendar.getInstance();
        threeDaysFromNow.add(Calendar.DAY_OF_MONTH, 3);

        if (selectedDeadline.before(threeDaysFromNow) && selectedDeadline.after(now)) {
            // Show warning for deadlines within 3 days
            long daysUntilDeadline = (selectedDeadline.getTimeInMillis() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24);
            String warningText = String.format("Nhắc nhở này sẽ hết hạn trong %d ngày", daysUntilDeadline + 1);
            tvDeadlineWarning.setText(warningText);
            cardDeadlineWarning.setVisibility(View.VISIBLE);
        } else {
            cardDeadlineWarning.setVisibility(View.GONE);
        }
    }

    private void saveReminder() {
        // Initialize logger if not already done
        ReminderLogger.initialize(this);
        
        // Get form data
        String title = etTitle.getText().toString().trim();
        String description = etNote.getText().toString().trim();
        String repeatText = actRepeat.getText().toString();
        String frequency = convertRepeatToFrequency(repeatText);
        
        // Comprehensive validation using ReminderValidator
        ReminderValidator.ValidationResult validationResult = ReminderValidator.validateCompleteReminder(
            title, description, selectedTime, frequency, selectedDeadline);
        
        if (!validationResult.isValid) {
            // Log validation failure
            ReminderLogger.logValidation("complete_reminder", false, validationResult.errorMessage);
            
            // Handle validation error
            ErrorHandler.handleValidationError(this, validationResult.errorCode, "reminder_form");
            
            // Show user-friendly message
            Toast.makeText(this, validationResult.errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log successful validation
        ReminderLogger.logValidation("complete_reminder", true, null);

        // Request notification permission for new reminders (Android 13+)
        if (!isEditMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                // Log permission request
                ReminderLogger.logInfo(ReminderLogger.LogCategory.NOTIFICATION, 
                    "Requesting notification permission", "Android 13+ permission required");
                
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{NOTIFICATION_PERMISSION},
                    PERMISSION_REQUEST_CODE
                );
                return; // Wait for permission result
            }
        }

        Reminder reminder = createReminderFromForm();
        
        btnSaveReminder.setEnabled(false);
        btnSaveReminder.setText(isEditMode ? "Đang cập nhật..." : "Đang lưu...");
        
        if (isEditMode) {
            remindersRepository.updateReminder(reminder)
                .thenAccept(success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            ReminderAlarmManager.cancelReminder(this, reminder.getReminderId());
                            ReminderAlarmManager.scheduleReminder(this, reminder);
                            
                            Toast.makeText(this, getString(R.string.toast_reminder_updated), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, getString(R.string.update_error), Toast.LENGTH_SHORT).show();
                            btnSaveReminder.setEnabled(true);
                            btnSaveReminder.setText("Cập nhật nhắc nhở");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error, throwable.getMessage()), Toast.LENGTH_SHORT).show();
                        btnSaveReminder.setEnabled(true);
                        btnSaveReminder.setText(getString(R.string.edit_reminder));
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
                            
                            Toast.makeText(this, getString(R.string.toast_reminder_created), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, getString(R.string.toast_create_reminder_error), Toast.LENGTH_SHORT).show();
                            btnSaveReminder.setEnabled(true);
                            btnSaveReminder.setText(getString(R.string.save_reminder));
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error, throwable.getMessage()), Toast.LENGTH_SHORT).show();
                        btnSaveReminder.setEnabled(true);
                        btnSaveReminder.setText(getString(R.string.save_reminder));
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
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with saving
                Toast.makeText(this, getString(R.string.toast_notification_permission_granted), Toast.LENGTH_SHORT).show();
                saveReminderAfterPermission();
            } else {
                // Permission denied
                Toast.makeText(this, getString(R.string.toast_notification_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Create reminder object with all form data
     */
    private Reminder createReminderFromForm() {
        String title = etTitle.getText().toString().trim();
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
            reminder.setCreatedAt(System.currentTimeMillis());
        }
        
        // Set deadline and calculate totalExpected
        if (selectedDeadline != null) {
            reminder.setDeadline(selectedDeadline.getTimeInMillis());
            int totalExpected = ProgressCalculator.calculateTotalExpected(reminder);
            reminder.setTotalExpected(totalExpected);
        } else {
            reminder.setDeadline(null);
            reminder.setTotalExpected(0);
        }
        
        // Set updated timestamp
        reminder.setUpdatedAt(System.currentTimeMillis());
        
        if (medicine != null && !medicine.equals(getString(R.string.no_medicine))) {
            reminder.setMedicineId(medicine);
        }
        
        return reminder;
    }

    /**
     * Save reminder after permission is granted
     * This is called after permission request completes
     * Requirements: 8.5, 2.2
     */
    private void saveReminderAfterPermission() {
        // Log permission granted
        ReminderLogger.logInfo(ReminderLogger.LogCategory.NOTIFICATION, 
            "Permission granted, proceeding with save", null);
        
        Reminder reminder = createReminderFromForm();
        
        btnSaveReminder.setEnabled(false);
        btnSaveReminder.setText(isEditMode ? "Đang cập nhật..." : "Đang lưu...");
        
        if (isEditMode) {
            // Log update attempt
            ReminderLogger.logInfo(ReminderLogger.LogCategory.DATABASE, 
                "Attempting to update reminder after permission", "ReminderId: " + reminder.getReminderId());
            
            remindersRepository.updateReminder(reminder)
                .thenAccept(success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            // Log successful update
                            ReminderLogger.logDatabaseOperation("UPDATE", "reminders", 
                                reminder.getReminderId(), true, "Reminder updated after permission grant");
                            
                            try {
                                ReminderAlarmManager.cancelReminder(this, reminder.getReminderId());
                                ReminderAlarmManager.scheduleReminder(this, reminder);
                                
                                // Log successful scheduling
                                ReminderLogger.logNotificationEvent("scheduled", reminder.getReminderId(), 
                                    "Reminder scheduled after permission grant and update");
                                
                                Toast.makeText(this, getString(R.string.toast_reminder_updated), Toast.LENGTH_SHORT).show();
                                finish();
                            } catch (Exception e) {
                                // Handle scheduling error
                                ErrorHandler.handleNotificationError(this, 
                                    ErrorHandler.ERROR_NOTIFICATION_SEND_FAILED, e, 
                                    reminder.getReminderId(), true);
                                
                                Toast.makeText(this, getString(R.string.toast_updated_schedule_error), 
                                    Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } else {
                            // Log failed update
                            ReminderLogger.logDatabaseOperation("UPDATE", "reminders", 
                                reminder.getReminderId(), false, "Update failed after permission grant");
                            
                            // Handle database error
                            ErrorHandler.handleDatabaseError(this, 
                                ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, null, 
                                "UPDATE", reminder.getReminderId());
                            
                            Toast.makeText(this, getString(R.string.update_error), Toast.LENGTH_SHORT).show();
                            btnSaveReminder.setEnabled(true);
                            btnSaveReminder.setText(getString(R.string.edit_reminder));
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        // Log exception
                        ReminderLogger.logError(ReminderLogger.LogCategory.DATABASE, 
                            "Failed to update reminder after permission", 
                            "ReminderId: " + reminder.getReminderId(), throwable);
                        
                        // Handle database error with exception
                        ErrorHandler.handleDatabaseError(this, 
                            ErrorHandler.ERROR_DATABASE_OPERATION_FAILED, throwable, 
                            "UPDATE", reminder.getReminderId());
                        
                        Toast.makeText(this, getString(R.string.error, throwable.getMessage()), Toast.LENGTH_SHORT).show();
                        btnSaveReminder.setEnabled(true);
                        btnSaveReminder.setText(getString(R.string.edit_reminder));
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
                            
                            Toast.makeText(this, getString(R.string.toast_reminder_created), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, getString(R.string.toast_create_reminder_error), Toast.LENGTH_SHORT).show();
                            btnSaveReminder.setEnabled(true);
                            btnSaveReminder.setText(getString(R.string.save_reminder));
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error, throwable.getMessage()), Toast.LENGTH_SHORT).show();
                        btnSaveReminder.setEnabled(true);
                        btnSaveReminder.setText(getString(R.string.save_reminder));
                    });
                    return null;
                });
        }
    }
}
