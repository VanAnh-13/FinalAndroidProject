package com.example.healthylifehub.ui.records.add_edit;

import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityAddEditRecordBinding;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.repository.MedicalRecordsRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for adding/editing medical records
 * Supports create and edit modes with full CRUD operations
 */
public class AddEditRecordActivity extends BaseActivity<ActivityAddEditRecordBinding> {

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isEditMode = false;
    private String recordId;
    private MedicalRecord currentRecord;
    private MedicalRecordsRepository repository;

    public AddEditRecordActivity() {
        super(ActivityAddEditRecordBinding::inflate);
    }

    @Override
    public void initData() {
        // Initialize repository
        repository = new MedicalRecordsRepository(this);
        
        // Check if editing existing record
        recordId = getIntent().getStringExtra("record_id");
        isEditMode = recordId != null;

        // Update title based on mode
        if (isEditMode) {
            getBinding().tvPageTitle.setText(R.string.edit_medical_record);
            getBinding().btnSaveRecord.setText("Cập nhật hồ sơ");
            // Show delete button in edit mode
            getBinding().tvDelete.setVisibility(android.view.View.VISIBLE);
        } else {
            getBinding().tvPageTitle.setText(R.string.add_medical_record);
            getBinding().btnSaveRecord.setText("Lưu hồ sơ");
            // Hide delete button in add mode
            getBinding().tvDelete.setVisibility(android.view.View.GONE);
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
        
        // Delete button
        getBinding().tvDelete.setOnClickListener(v -> showDeleteConfirmation());
    }
    
    private void showDeleteConfirmation() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Xóa hồ sơ")
            .setMessage("Bạn có chắc chắn muốn xóa hồ sơ này không?")
            .setPositiveButton("Xóa", (dialog, which) -> deleteRecord())
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    private void deleteRecord() {
        if (currentRecord == null) return;
        
        getBinding().btnSaveRecord.setEnabled(false);
        getBinding().btnSaveRecord.setText("Đang xóa...");
        
        repository.deleteRecord(currentRecord)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(AddEditRecordActivity.this, "✅ Đã xóa hồ sơ", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditRecordActivity.this, "❌ Lỗi khi xóa", Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditRecordActivity.this, "❌ Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
                return null;
            });
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
        // Load record from database
        repository.getRecordById(recordId).observe(this, record -> {
            if (record != null) {
                currentRecord = record;
                fillFormWithRecordData(record);
            } else {
                Toast.makeText(this, "Không tìm thấy hồ sơ", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void fillFormWithRecordData(MedicalRecord record) {
        getBinding().etTitle.setText(record.getTitle());
        getBinding().etDate.setText(record.getDate());
        getBinding().actHospital.setText(record.getHospital() != null ? record.getHospital() : "", false);
        getBinding().etDoctor.setText(record.getDoctor() != null ? record.getDoctor() : "");
        getBinding().etDiagnosis.setText(record.getDiagnosis() != null ? record.getDiagnosis() : "");
        getBinding().etDetails.setText(record.getDescription() != null ? record.getDescription() : "");
    }

    private void saveRecord() {
        // Get all input values
        String title = getBinding().etTitle.getText().toString().trim();
        String date = getBinding().etDate.getText().toString().trim();
        String hospital = getBinding().actHospital.getText().toString().trim();
        String doctor = getBinding().etDoctor.getText().toString().trim();
        String diagnosis = getBinding().etDiagnosis.getText().toString().trim();
        String details = getBinding().etDetails.getText().toString().trim();

        // Simple validation - required fields
        if (title.isEmpty()) {
            Toast.makeText(this, "❌ Tiêu đề không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) {
            Toast.makeText(this, "❌ Ngày khám không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear errors
        getBinding().tilTitle.setError(null);
        getBinding().tilDate.setError(null);

        // Disable button during save
        getBinding().btnSaveRecord.setEnabled(false);
        getBinding().btnSaveRecord.setText(isEditMode ? "Đang cập nhật..." : "Đang lưu...");

        // Create or update record
        if (isEditMode) {
            // Update existing record
            currentRecord.setTitle(title);
            currentRecord.setDate(date);
            currentRecord.setHospital(hospital);
            currentRecord.setDoctor(doctor);
            currentRecord.setDiagnosis(diagnosis);
            currentRecord.setDescription(details);
            
            repository.updateRecord(currentRecord)
                .thenAccept(success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(AddEditRecordActivity.this, "✅ Đã cập nhật hồ sơ", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddEditRecordActivity.this, "❌ Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditRecordActivity.this, "❌ Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButton();
                    });
                    return null;
                });
        } else {
            // Create new record
            MedicalRecord newRecord = new MedicalRecord();
            newRecord.setId(UUID.randomUUID().toString());
            newRecord.setTitle(title);
            newRecord.setDate(date);
            newRecord.setHospital(hospital);
            newRecord.setDoctor(doctor);
            newRecord.setDiagnosis(diagnosis);
            newRecord.setDescription(details);
            newRecord.setType(MedicalRecord.RecordType.CHECKUP);  // Default type
            
            repository.createRecord(newRecord)
                .thenAccept(recordId -> {
                    runOnUiThread(() -> {
                        if (recordId != null) {
                            Toast.makeText(AddEditRecordActivity.this, "✅ Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddEditRecordActivity.this, "❌ Lỗi khi lưu", Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditRecordActivity.this, "❌ Lỗi: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButton();
                    });
                    return null;
                });
        }
    }
    
    private void resetButton() {
        getBinding().btnSaveRecord.setEnabled(true);
        getBinding().btnSaveRecord.setText(isEditMode ? "Cập nhật hồ sơ" : "Lưu hồ sơ");
    }
}
