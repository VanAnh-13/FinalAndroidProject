package com.example.healthylifehub.ui.metrics.add_edit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.utils.network.SimpleNetworkManager;
import com.example.healthylifehub.utils.error.ErrorHandler;
import com.example.healthylifehub.utils.error.CrashPreventionHandler;
import com.example.healthylifehub.utils.validation.InputValidationHandler;
import com.example.healthylifehub.utils.network.NetworkErrorHandler;
import com.example.healthylifehub.utils.ui.UserFeedbackManager;
import com.example.healthylifehub.utils.ui.ErrorStateManager;
import com.example.healthylifehub.databinding.ActivityAddEditMetricBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditMetricActivity extends BaseActivity<ActivityAddEditMetricBinding> {

    private Calendar calendar;
    private String selectedMetricType = "blood_pressure";
    private AddEditMetricViewModel viewModel;
    private String editMetricId = null; // For edit mode
    private boolean isEditMode = false;
    private SimpleNetworkManager networkManager;

    public AddEditMetricActivity() {
        super(ActivityAddEditMetricBinding::inflate);
    }

    @Override
    public void initData() {
        CrashPreventionHandler.safeUIExecute(this, () -> {
            calendar = Calendar.getInstance();
            viewModel = new ViewModelProvider(this).get(AddEditMetricViewModel.class);
            
            // Initialize simple network manager with error handling
            networkManager = new SimpleNetworkManager(this);
            networkManager.startMonitoring(this);
            
            // Check if edit mode
            editMetricId = getIntent().getStringExtra("METRIC_ID");
            isEditMode = editMetricId != null;
            
            setupMetricTypeDropdown();
            observeViewModel();
            setupInputValidation();
            
            // Load metric data if edit mode
            if (isEditMode) {
                loadMetricForEdit(editMetricId);
            }
        }, "Không thể khởi tạo màn hình thêm chỉ số");
    }

    @Override
    public void bindData() {
        // Set current date and time
        updateDateField();
        updateTimeField();
        
        // Show blood pressure fields by default
        showBloodPressureFields();
        
        // Update title based on mode
        if (isEditMode) {
            getBinding().tvTitle.setText("Chỉnh sửa chỉ số");
            getBinding().btnSave.setText("Cập nhật");
        } else {
            getBinding().tvTitle.setText("Thêm chỉ số");
            getBinding().btnSave.setText("Lưu");
        }
    }
    
    /**
     * Observe ViewModel LiveData
     */
    private void observeViewModel() {
        // Observe saving state
        viewModel.getIsSaving().observe(this, isSaving -> {
            updateSaveButtonState(isSaving);
        });
        
        // Observe save success
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, getString(R.string.toast_metric_saved), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorDialog(error);
            }
        });
        
        // Observe network connectivity to enable/disable save functionality
        viewModel.getNetworkConnectivity().observe(this, isConnected -> {
            updateNetworkUI(isConnected);
        });
        
        // Observe can save state
        viewModel.getCanSave().observe(this, canSave -> {
            // This will be handled by updateSaveButtonState
        });
    }
    
    /**
     * Update save button state based on saving and network status
     */
    private void updateSaveButtonState(boolean isSaving) {
        Boolean canSave = viewModel.getCanSave().getValue();
        boolean networkAvailable = canSave != null ? canSave : true;
        
        if (isSaving) {
            getBinding().btnSave.setEnabled(false);
            getBinding().btnSaveAnalyze.setEnabled(false);
            getBinding().btnSave.setText("Đang lưu...");
        } else {
            // Enable buttons based on network availability
            getBinding().btnSave.setEnabled(networkAvailable);
            getBinding().btnSaveAnalyze.setEnabled(networkAvailable);
            
            if (networkAvailable) {
                getBinding().btnSave.setText(isEditMode ? "Cập nhật" : "Lưu");
            } else {
                getBinding().btnSave.setText("Lưu offline");
            }
        }
    }
    
    /**
     * Update UI based on network connectivity - Simplified
     */
    private void updateNetworkUI(boolean isConnected) {
        // SimpleNetworkManager sẽ tự động hiển thị dialog
        // Chỉ cần update button state
        updateSaveButtonState(viewModel.getIsSaving().getValue());
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
        
        // ✅ VALIDATION: Không cho chọn ngày tương lai
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
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
        if (!validateBasicInputs()) {
            return;
        }
        
        String notes = getBinding().etNotes.getText().toString().trim();
        
        switch (selectedMetricType) {
            case "blood_pressure":
                saveBloodPressureMetric(notes);
                break;
            case "blood_sugar":
                saveBloodSugarMetric(notes);
                break;
            case "weight":
                saveWeightMetric(notes);
                break;
            case "heart_rate":
                saveHeartRateMetric(notes);
                break;
        }
    }

    private void saveAndAnalyze() {
        // For now, just save. Analysis feature can be added later
        saveMetric();
    }
    
    /**
     * Save blood pressure metric
     */
    private void saveBloodPressureMetric(String notes) {
        String systolicStr = getBinding().etSystolic.getText().toString().trim();
        String diastolicStr = getBinding().etDiastolic.getText().toString().trim();
        
        try {
            int systolic = Integer.parseInt(systolicStr);
            int diastolic = Integer.parseInt(diastolicStr);
            
            viewModel.saveBloodPressure(systolic, diastolic, calendar.getTime(), notes);
        } catch (NumberFormatException e) {
            showErrorDialog("Giá trị không hợp lệ. Vui lòng nhập số nguyên.");
        }
    }
    
    /**
     * Save blood sugar metric
     */
    private void saveBloodSugarMetric(String notes) {
        String valueStr = getBinding().etSingleValue.getText().toString().trim();
        
        try {
            double value = Double.parseDouble(valueStr);
            viewModel.saveBloodSugar(value, calendar.getTime(), notes);
        } catch (NumberFormatException e) {
            showErrorDialog("Giá trị không hợp lệ. Vui lòng nhập số.");
        }
    }
    
    /**
     * Save weight metric
     */
    private void saveWeightMetric(String notes) {
        String valueStr = getBinding().etSingleValue.getText().toString().trim();
        
        try {
            double value = Double.parseDouble(valueStr);
            viewModel.saveWeight(value, calendar.getTime(), notes);
        } catch (NumberFormatException e) {
            showErrorDialog("Giá trị không hợp lệ. Vui lòng nhập số.");
        }
    }
    
    /**
     * Save heart rate metric
     */
    private void saveHeartRateMetric(String notes) {
        String valueStr = getBinding().etSingleValue.getText().toString().trim();
        
        try {
            double value = Double.parseDouble(valueStr);
            viewModel.saveHeartRate(value, calendar.getTime(), notes);
        } catch (NumberFormatException e) {
            showErrorDialog("Giá trị không hợp lệ. Vui lòng nhập số.");
        }
    }

    /**
     * Setup real-time input validation
     */
    private void setupInputValidation() {
        CrashPreventionHandler.safeUIExecute(this, () -> {
            // Setup real-time validation for blood pressure fields
            InputValidationHandler.setupRealTimeValidation(getBinding().tilSystolic, 
                () -> validateBloodPressureField(getBinding().tilSystolic, "Huyết áp tâm thu", 70, 250));
            
            InputValidationHandler.setupRealTimeValidation(getBinding().tilDiastolic, 
                () -> validateBloodPressureField(getBinding().tilDiastolic, "Huyết áp tâm trương", 40, 150));
            
            // Setup validation for single value field
            InputValidationHandler.setupRealTimeValidation(getBinding().tilSingleValue, 
                () -> validateSingleValueField());
            
            // Setup validation for notes field
            InputValidationHandler.setupRealTimeValidation(getBinding().tilNotes, 
                () -> InputValidationHandler.validateReminderDescription(this, getBinding().tilNotes));
        }, null);
    }
    
    /**
     * Validate blood pressure field with range checking
     */
    private void validateBloodPressureField(com.google.android.material.textfield.TextInputLayout layout, 
                                          String fieldName, double min, double max) {
        InputValidationHandler.validateNumericRange(this, layout, min, max, fieldName);
    }
    
    /**
     * Validate single value field based on metric type
     */
    private void validateSingleValueField() {
        switch (selectedMetricType) {
            case "blood_sugar":
                InputValidationHandler.validateBloodSugar(this, getBinding().tilSingleValue);
                break;
            case "weight":
                InputValidationHandler.validateWeight(this, getBinding().tilSingleValue);
                break;
            case "heart_rate":
                InputValidationHandler.validateHeartRate(this, getBinding().tilSingleValue);
                break;
        }
    }

    /**
     * Comprehensive validation for all inputs
     */
    private boolean validateBasicInputs() {
        CrashPreventionHandler.safeExecute(this, () -> {
            InputValidationHandler.ValidationResult result = new InputValidationHandler.ValidationResult();
            
            if (selectedMetricType.equals("blood_pressure")) {
                // Validate blood pressure with comprehensive checking
                InputValidationHandler.ValidationResult bpResult = 
                    InputValidationHandler.validateBloodPressure(this, 
                        getBinding().tilSystolic, getBinding().tilDiastolic);
                
                if (!bpResult.isValid()) {
                    InputValidationHandler.showValidationSummary(this, bpResult);
                    return false;
                }
            } else {
                // Validate single value field
                boolean isValidSingle = false;
                switch (selectedMetricType) {
                    case "blood_sugar":
                        isValidSingle = InputValidationHandler.validateBloodSugar(this, getBinding().tilSingleValue);
                        break;
                    case "weight":
                        isValidSingle = InputValidationHandler.validateWeight(this, getBinding().tilSingleValue);
                        break;
                    case "heart_rate":
                        isValidSingle = InputValidationHandler.validateHeartRate(this, getBinding().tilSingleValue);
                        break;
                }
                
                if (!isValidSingle) {
                    UserFeedbackManager.showError(this, "Vui lòng nhập giá trị hợp lệ");
                    return false;
                }
            }
            
            // Validate notes field
            if (!InputValidationHandler.validateReminderDescription(this, getBinding().tilNotes)) {
                return false;
            }
            
            return true;
        }, false);
        
        return false; // Default return for safety
    }
    
    /**
     * Show error dialog
     */
    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Lỗi")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    /**
     * Load metric data for edit mode
     */
    private void loadMetricForEdit(String metricId) {
        viewModel.loadMetricById(metricId).observe(this, metric -> {
            if (metric != null) {
                // Set calendar to metric's measured date
                calendar.setTime(metric.getMeasuredAt());
                updateDateField();
                updateTimeField();
                
                // Set metric type
                selectedMetricType = metric.getType();
                
                // Set values based on type
                switch (metric.getType()) {
                    case "blood_pressure":
                        getBinding().actvMetricType.setText(getString(R.string.blood_pressure), false);
                        showBloodPressureFields();
                        getBinding().etSystolic.setText(String.valueOf((int) metric.getSystolic()));
                        getBinding().etDiastolic.setText(String.valueOf((int) metric.getDiastolic()));
                        break;
                        
                    case "blood_sugar":
                        getBinding().actvMetricType.setText(getString(R.string.blood_sugar), false);
                        showSingleValueField(getString(R.string.blood_sugar));
                        getBinding().tilSingleValue.setSuffixText("mg/dL");
                        getBinding().etSingleValue.setText(String.valueOf(metric.getValue()));
                        break;
                        
                    case "weight":
                        getBinding().actvMetricType.setText(getString(R.string.weight), false);
                        showSingleValueField(getString(R.string.weight));
                        getBinding().tilSingleValue.setSuffixText("kg");
                        getBinding().etSingleValue.setText(String.valueOf(metric.getValue()));
                        break;
                        
                    case "heart_rate":
                        getBinding().actvMetricType.setText(getString(R.string.heart_rate), false);
                        showSingleValueField(getString(R.string.heart_rate));
                        getBinding().tilSingleValue.setSuffixText("bpm");
                        getBinding().etSingleValue.setText(String.valueOf((int) metric.getValue()));
                        break;
                }
                
                // Set notes
                if (metric.getNotes() != null && !metric.getNotes().isEmpty()) {
                    getBinding().etNotes.setText(metric.getNotes());
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkManager != null) {
            networkManager.destroy();
        }
    }
}
