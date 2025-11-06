package com.example.healthylifehub.ui.records.add_edit;

import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityAddEditRecordBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditRecordActivity extends BaseActivity<ActivityAddEditRecordBinding> {

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isEditMode = false;
    private String recordId;

    public AddEditRecordActivity() {
        super(ActivityAddEditRecordBinding::inflate);
    }

    @Override
    public void initData() {
        // Check if editing existing record
        recordId = getIntent().getStringExtra("record_id");
        isEditMode = recordId != null;

        // Update title based on mode
        if (isEditMode) {
            getBinding().tvPageTitle.setText(R.string.edit_medical_record);
        }

        setupHospitalDropdown();
    }

    @Override
    public void bindData() {
        if (isEditMode) {
            loadRecordData();
        }
    }

    @Override
    public void setOnClick() {
        getBinding().tvCancel.setOnClickListener(v -> finish());

        getBinding().tvSave.setOnClickListener(v -> saveRecord());

        getBinding().btnSaveRecord.setOnClickListener(v -> saveRecord());

        getBinding().etDate.setOnClickListener(v -> showDatePicker());
    }

    private void setupHospitalDropdown() {
        String[] hospitals = {
            getString(R.string.hospital_vinmec),
            getString(R.string.hospital_cho_ray),
            getString(R.string.hospital_other)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            hospitals
        );
        
        ((AutoCompleteTextView) getBinding().actHospital).setAdapter(adapter);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                getBinding().etDate.setText(sdf.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadRecordData() {
        // TODO: Load record data from database
        // For now, load sample data
        getBinding().etTitle.setText("Khám sức khỏe tổng quát");
        getBinding().etDate.setText("28/05/2024");
        getBinding().actHospital.setText(getString(R.string.hospital_vinmec), false);
        getBinding().etDoctor.setText("BS. Nguyễn Văn A");
        getBinding().etDiagnosis.setText("Sức khỏe ổn định");
        getBinding().etDetails.setText("Các chỉ số bình thường, không có dấu hiệu bất thường.");
    }

    private void saveRecord() {
        // Get all input values
        String title = getBinding().etTitle.getText().toString().trim();
        String date = getBinding().etDate.getText().toString().trim();
        String hospital = getBinding().actHospital.getText().toString().trim();
        String doctor = getBinding().etDoctor.getText().toString().trim();
        String diagnosis = getBinding().etDiagnosis.getText().toString().trim();
        String details = getBinding().etDetails.getText().toString().trim();

        // Validate required fields
        if (title.isEmpty()) {
            getBinding().tilTitle.setError(getString(R.string.please_enter_name));
            getBinding().etTitle.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            getBinding().tilDate.setError("Vui lòng chọn ngày");
            return;
        }

        // Clear errors
        getBinding().tilTitle.setError(null);
        getBinding().tilDate.setError(null);

        // TODO: Save to database
        
        Toast.makeText(this, 
            isEditMode ? "Đã cập nhật hồ sơ" : "Đã lưu hồ sơ", 
            Toast.LENGTH_SHORT).show();
        
        finish();
    }
}
