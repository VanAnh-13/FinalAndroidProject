package com.example.healthylifehub.ui.reminders.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthylifehub.R;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;
import com.google.android.material.button.MaterialButton;

public class ReminderDetailActivity extends AppCompatActivity {

    private ImageView ivBack, ivDelete, ivReminderIcon;
    private TextView tvReminderTitle, tvReminderDescription;
    private TextView tvTimeValue, tvRepeatValue, tvMedicationValue, tvNotes;
    private MaterialButton btnMarkTaken, btnSkipReminder, btnReschedule;
    private MaterialButton btnEditReminder, btnDeleteReminder;

    private String reminderId;
    private String reminderTitle;
    private String reminderDescription;
    private String reminderTime;
    private String reminderRepeat;
    private String reminderMedication;
    private String reminderNotes;

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
    }

    private void loadReminderData() {
        // Get data from intent
        Intent intent = getIntent();
        reminderId = intent.getStringExtra("reminder_id");
        reminderTitle = intent.getStringExtra("reminder_title");
        reminderDescription = intent.getStringExtra("reminder_description");
        reminderTime = intent.getStringExtra("reminder_time");
        reminderRepeat = intent.getStringExtra("reminder_repeat");
        reminderMedication = intent.getStringExtra("reminder_medication");
        reminderNotes = intent.getStringExtra("reminder_notes");

        // Set default values for demo
        if (reminderTitle == null) reminderTitle = "Morning Medication";
        if (reminderDescription == null) reminderDescription = "Take with a full glass of water, 30 minutes before breakfast.";
        if (reminderTime == null) reminderTime = "08:00 AM, Today";
        if (reminderRepeat == null) reminderRepeat = "Daily";
        if (reminderMedication == null) reminderMedication = "Metformin 500mg";
        if (reminderNotes == null) reminderNotes = "Remember to check blood sugar levels before taking this medication, especially if feeling dizzy.";

        // Display data
        tvReminderTitle.setText(reminderTitle);
        tvReminderDescription.setText(reminderDescription);
        tvTimeValue.setText(reminderTime);
        tvRepeatValue.setText(reminderRepeat);
        tvMedicationValue.setText(reminderMedication);
        tvNotes.setText(reminderNotes);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivDelete.setOnClickListener(v -> showDeleteConfirmation());

        btnMarkTaken.setOnClickListener(v -> {
            selectSegmentedButton(btnMarkTaken);
            Toast.makeText(this, "Marked as taken", Toast.LENGTH_SHORT).show();
        });

        btnSkipReminder.setOnClickListener(v -> {
            selectSegmentedButton(btnSkipReminder);
            Toast.makeText(this, "Reminder skipped", Toast.LENGTH_SHORT).show();
        });

        btnReschedule.setOnClickListener(v -> {
            selectSegmentedButton(btnReschedule);
            Toast.makeText(this, "Reschedule reminder", Toast.LENGTH_SHORT).show();
        });

        btnEditReminder.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditReminderActivity.class);
            intent.putExtra("reminder_id", reminderId);
            intent.putExtra("reminder_title", reminderTitle);
            intent.putExtra("reminder_description", reminderDescription);
            startActivity(intent);
        });

        btnDeleteReminder.setOnClickListener(v -> showDeleteConfirmation());
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
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_reminder)
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                // TODO: Delete reminder from database
                Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}
