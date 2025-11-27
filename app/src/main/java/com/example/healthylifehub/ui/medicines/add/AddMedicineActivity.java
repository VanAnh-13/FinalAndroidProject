package com.example.healthylifehub.ui.medicines.add;

import android.app.DatePickerDialog;
import android.view.View;
import android.widget.Toast;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityAddMedicineBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMedicineActivity extends BaseActivity<ActivityAddMedicineBinding> {

    public static final String EXTRA_START_MODE = "start_mode";
    public static final String MODE_OCR = "ocr";
    public static final String MODE_MANUAL = "manual";

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isOcrMode = true;

    public AddMedicineActivity() {
        super(ActivityAddMedicineBinding::inflate);
    }

    @Override
    public void initData() {
        // Initialize mode from intent (default OCR)
        String startMode = getIntent() != null ? getIntent().getStringExtra(EXTRA_START_MODE) : null;
        if (MODE_MANUAL.equals(startMode)) {
            isOcrMode = false;
        } else {
            isOcrMode = true;
        }
        updateSegmentedButtons();
        if (isOcrMode) {
            showOcrMode();
        } else {
            showManualMode();
        }
    }

    @Override
    public void bindData() {
        // No initial data to bind
    }

    @Override
    public void setOnClick() {
        getBinding().ivClose.setOnClickListener(v -> finish());

        // Segmented buttons
        getBinding().btnOcr.setOnClickListener(v -> {
            isOcrMode = true;
            updateSegmentedButtons();
            showOcrMode();
        });

        getBinding().btnManual.setOnClickListener(v -> {
            isOcrMode = false;
            updateSegmentedButtons();
            showManualMode();
        });

        // Camera controls
        getBinding().fabCapture.setOnClickListener(v -> {
            captureImage();
        });

        getBinding().ivGallery.setOnClickListener(v -> {
            openGallery();
        });

        getBinding().ivFlash.setOnClickListener(v -> {
            toggleFlash();
        });

        // Form controls
        getBinding().etStartDate.setOnClickListener(v -> showDatePicker());

        getBinding().btnSaveMedicine.setOnClickListener(v -> saveMedicine());
    }

    private void updateSegmentedButtons() {
        if (isOcrMode) {
            getBinding().btnOcr.setBackgroundColor(getColor(R.color.white));
            getBinding().btnOcr.setTextColor(getColor(R.color.text_primary));
            
            getBinding().btnManual.setBackgroundColor(getColor(android.R.color.transparent));
            getBinding().btnManual.setTextColor(getColor(R.color.text_secondary));
        } else {
            getBinding().btnManual.setBackgroundColor(getColor(R.color.white));
            getBinding().btnManual.setTextColor(getColor(R.color.text_primary));
            
            getBinding().btnOcr.setBackgroundColor(getColor(android.R.color.transparent));
            getBinding().btnOcr.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void showOcrMode() {
        getBinding().llCameraView.setVisibility(View.VISIBLE);
        getBinding().llProcessing.setVisibility(View.GONE);
        getBinding().svForm.setVisibility(View.GONE);
    }

    private void showManualMode() {
        getBinding().llCameraView.setVisibility(View.GONE);
        getBinding().llProcessing.setVisibility(View.GONE);
        getBinding().svForm.setVisibility(View.VISIBLE);
    }

    private void showProcessing() {
        getBinding().llCameraView.setVisibility(View.GONE);
        getBinding().llProcessing.setVisibility(View.VISIBLE);
        getBinding().svForm.setVisibility(View.GONE);

        // Simulate OCR processing
        getBinding().llProcessing.postDelayed(() -> {
            // After processing, show form with OCR results
            fillOcrResults();
            getBinding().svForm.setVisibility(View.VISIBLE);
            getBinding().llProcessing.setVisibility(View.GONE);
        }, 2000);
    }

    private void captureImage() {
        // TODO: Implement camera capture
        Toast.makeText(this, getString(R.string.toast_capture_photo), Toast.LENGTH_SHORT).show();
        showProcessing();
    }

    private void openGallery() {
        // TODO: Implement gallery picker
        Toast.makeText(this, getString(R.string.toast_open_gallery), Toast.LENGTH_SHORT).show();
    }

    private void toggleFlash() {
        // TODO: Implement flash toggle
        Toast.makeText(this, getString(R.string.toast_toggle_flash), Toast.LENGTH_SHORT).show();
    }

    private void fillOcrResults() {
        // TODO: Fill form with actual OCR extracted data
        // This will be populated by MedicineOCRProcessor results
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                getBinding().etStartDate.setText(sdf.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveMedicine() {
        String name = getBinding().etMedicineName.getText().toString().trim();
        String dosage = getBinding().etDosage.getText().toString().trim();
        String instructions = getBinding().etInstructions.getText().toString().trim();
        String startDate = getBinding().etStartDate.getText().toString().trim();

        // Validate
        if (name.isEmpty()) {
            getBinding().tilMedicineName.setError("Vui lòng nhập tên thuốc");
            return;
        }

        if (dosage.isEmpty()) {
            getBinding().tilDosage.setError("Vui lòng nhập liều lượng");
            return;
        }

        // Clear errors
        getBinding().tilMedicineName.setError(null);
        getBinding().tilDosage.setError(null);

        // TODO: Save to database

        Toast.makeText(this, getString(R.string.toast_medicine_saved), Toast.LENGTH_SHORT).show();
        finish();
    }
}
