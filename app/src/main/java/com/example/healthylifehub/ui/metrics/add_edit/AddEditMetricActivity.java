package com.example.healthylifehub.ui.metrics.add_edit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityAddEditMetricBinding;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditMetricActivity extends BaseActivity<ActivityAddEditMetricBinding> {

    private Calendar calendar;
    private String selectedMetricType = "blood_pressure";

    public AddEditMetricActivity() {
        super(ActivityAddEditMetricBinding::inflate);
    }

    @Override
    public void initData() {
        calendar = Calendar.getInstance();
        setupMetricTypeDropdown();
    }

    @Override
    public void bindData() {
        // Set current date and time
        updateDateField();
        updateTimeField();
        
        // Show blood pressure fields by default
        showBloodPressureFields();
    }

    @Override
    public void setOnClick() {
        getBinding().ivClose.setOnClickListener(v -> finish());

        getBinding().btnSave.setOnClickListener(v -> saveMetric());

        getBinding().btnSaveAnalyze.setOnClickListener(v -> saveAndAnalyze());

        getBinding().etDate.setOnClickListener(v -> showDatePicker());

        getBinding().etTime.setOnClickListener(v -> showTimePicker());

        getBinding().actvMetricType.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            handleMetricTypeChange(selected);
        });
    }

    private void setupMetricTypeDropdown() {
        String[] metricTypes = {
            getString(R.string.blood_pressure),
            getString(R.string.blood_sugar),
            getString(R.string.weight),
            getString(R.string.heart_rate)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            metricTypes
        );

        getBinding().actvMetricType.setAdapter(adapter);
        getBinding().actvMetricType.setText(metricTypes[0], false);
    }

    private void handleMetricTypeChange(String metricType) {
        if (metricType.equals(getString(R.string.blood_pressure))) {
            showBloodPressureFields();
            selectedMetricType = "blood_pressure";
        } else {
            showSingleValueField(metricType);
            if (metricType.equals(getString(R.string.blood_sugar))) {
                selectedMetricType = "blood_sugar";
                getBinding().tilSingleValue.setSuffixText("mg/dL");
            } else if (metricType.equals(getString(R.string.weight))) {
                selectedMetricType = "weight";
                getBinding().tilSingleValue.setSuffixText("kg");
            } else if (metricType.equals(getString(R.string.heart_rate))) {
                selectedMetricType = "heart_rate";
                getBinding().tilSingleValue.setSuffixText("bpm");
            }
        }
    }

    private void showBloodPressureFields() {
        getBinding().llBloodPressureFields.setVisibility(View.VISIBLE);
        getBinding().tilSingleValue.setVisibility(View.GONE);
    }

    private void showSingleValueField(String metricType) {
        getBinding().llBloodPressureFields.setVisibility(View.GONE);
        getBinding().tilSingleValue.setVisibility(View.VISIBLE);
        getBinding().tilSingleValue.setHint(metricType);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateField();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                updateTimeField();
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }

    private void updateDateField() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        getBinding().etDate.setText(dateFormat.format(calendar.getTime()));
    }

    private void updateTimeField() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        getBinding().etTime.setText(timeFormat.format(calendar.getTime()));
    }

    private void saveMetric() {
        if (validateInputs()) {
            // TODO: Save to database
            Toast.makeText(this, "Đã lưu chỉ số", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveAndAnalyze() {
        if (validateInputs()) {
            // TODO: Save to database and show analysis
            Toast.makeText(this, "Đã lưu và phân tích", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (selectedMetricType.equals("blood_pressure")) {
            String systolic = getBinding().etSystolic.getText().toString().trim();
            String diastolic = getBinding().etDiastolic.getText().toString().trim();

            if (systolic.isEmpty()) {
                getBinding().tilSystolic.setError(getString(R.string.validation_error));
                isValid = false;
            } else {
                getBinding().tilSystolic.setError(null);
            }

            if (diastolic.isEmpty()) {
                getBinding().tilDiastolic.setError(getString(R.string.validation_error));
                isValid = false;
            } else {
                getBinding().tilDiastolic.setError(null);
            }
        } else {
            String value = getBinding().etSingleValue.getText().toString().trim();
            if (value.isEmpty()) {
                getBinding().tilSingleValue.setError(getString(R.string.validation_error));
                isValid = false;
            } else {
                getBinding().tilSingleValue.setError(null);
            }
        }

        return isValid;
    }
}
