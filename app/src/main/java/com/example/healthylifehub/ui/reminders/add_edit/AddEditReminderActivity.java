package com.example.healthylifehub.ui.reminders.add_edit;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthylifehub.R;
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

    private String selectedTime = "08:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_reminder);

        initViews();
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

        // Set default time
        etTime.setText(selectedTime);
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

        // Get selected reminder type
        int selectedChipId = chipGroupType.getCheckedChipId();
        Chip selectedChip = findViewById(selectedChipId);
        String reminderType = selectedChip != null ? selectedChip.getText().toString() : "";

        String note = etNote.getText().toString().trim();
        String medicine = actMedicine.getText().toString();
        String repeat = actRepeat.getText().toString();

        // TODO: Save reminder to database
        
        Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show();
        finish();
    }
}
