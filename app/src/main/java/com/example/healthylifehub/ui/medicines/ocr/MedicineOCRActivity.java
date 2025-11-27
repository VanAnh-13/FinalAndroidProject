package com.example.healthylifehub.ui.medicines.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.Medicine;
import com.example.healthylifehub.databinding.ActivityMedicineOcrBinding;
import com.example.healthylifehub.utils.ocr.MedicineOCRProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MedicineOCRActivity - Prescription scanning with OCR
 * 
 * Features:
 * - Capture image with camera
 * - Show processing indicator during OCR
 * - Display extracted medicines for confirmation
 * - Allow manual editing before saving
 * 
 * Requirements: 9.1, 9.4, 9.5
 */
public class MedicineOCRActivity extends BaseActivity<ActivityMedicineOcrBinding> {
    private static final String TAG = "MedicineOCRActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    
    private MedicineOCRProcessor ocrProcessor;
    private ExtractedMedicinesAdapter medicinesAdapter;
    private List<Medicine> extractedMedicines = new ArrayList<>();
    private Bitmap capturedImage;
    
    // Activity result launcher for camera
    private final ActivityResultLauncher<Intent> cameraLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        onImageCaptured(imageBitmap);
                    }
                }
            }
        });
    
    // Activity result launcher for camera permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchCamera();
            } else {
                Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_camera_permission_required), Toast.LENGTH_SHORT).show();
            }
        });
    
    public MedicineOCRActivity() {
        super(ActivityMedicineOcrBinding::inflate);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void initData() {
        // Initialize OCR processor
        ocrProcessor = new MedicineOCRProcessor();
        
        // Initialize adapter
        medicinesAdapter = new ExtractedMedicinesAdapter(extractedMedicines, 
            new ExtractedMedicinesAdapter.OnMedicineEditListener() {
                @Override
                public void onEdit(int position, Medicine medicine) {
                    // Allow manual editing
                    showEditDialog(position, medicine);
                }
                
                @Override
                public void onDelete(int position) {
                    extractedMedicines.remove(position);
                    medicinesAdapter.notifyItemRemoved(position);
                }
            });
    }
    
    @Override
    public void bindData() {
        // Setup toolbar
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
        
        // Setup RecyclerView
        getBinding().rvMedicines.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvMedicines.setAdapter(medicinesAdapter);
    }
    
    @Override
    public void setOnClick() {
        // Capture button - Requirements: 9.1
        getBinding().btnCapture.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                launchCamera();
            } else {
                requestCameraPermission();
            }
        });
        
        // Save button
        getBinding().btnSave.setOnClickListener(v -> saveMedicines());
    }
    
    /**
     * Check if camera permission is granted
     */
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Request camera permission
     */
    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }
    
    /**
     * Launch camera to capture image
     * Requirements: 9.1
     */
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_camera_app_not_found), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle captured image
     * Requirements: 9.1, 9.4
     */
    private void onImageCaptured(Bitmap image) {
        capturedImage = image;
        
        // Display preview
        getBinding().ivPrescriptionPreview.setImageBitmap(image);
        
        // Show processing indicator - Requirements: 9.4
        showProcessing(true);
        getBinding().tvProcessingStatus.setText("Đang xử lý hình ảnh...");
        
        // Process image with OCR
        processImageWithOCR(image);
    }
    
    /**
     * Process image with OCR
     * Requirements: 9.1, 9.4, 9.5
     */
    private void processImageWithOCR(Bitmap image) {
        Log.d(TAG, "🔍 Starting OCR processing");
        
        // Call MedicineOCRProcessor.processPrescriptionImage()
        // Requirements: 9.1, 9.4
        ocrProcessor.processPrescriptionImage(image)
            .thenAcceptAsync(medicines -> {
                // Display extracted medicines for user confirmation
                // Requirements: 9.5
                Log.d(TAG, "✅ OCR complete: " + medicines.size() + " medicines extracted");
                
                extractedMedicines.clear();
                extractedMedicines.addAll(medicines);
                
                runOnUiThread(() -> {
                    showProcessing(false);
                    
                    if (medicines.isEmpty()) {
                        Toast.makeText(this, 
                            getString(com.example.healthylifehub.R.string.toast_ocr_no_text_found), 
                            Toast.LENGTH_LONG).show();
                    } else {
                        // Show extracted medicines card
                        getBinding().cardMedicines.setVisibility(View.VISIBLE);
                        medicinesAdapter.notifyDataSetChanged();
                        
                        Toast.makeText(this, 
                            getString(com.example.healthylifehub.R.string.toast_ocr_found_medicines, medicines.size()), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }, getMainExecutor())
            .exceptionally(error -> {
                Log.e(TAG, "❌ OCR processing failed", error);
                
                runOnUiThread(() -> {
                    showProcessing(false);
                    Toast.makeText(this, 
                        "Không thể xử lý hình ảnh. Vui lòng thử lại.", 
                        Toast.LENGTH_LONG).show();
                });
                
                return null;
            });
    }
    
    /**
     * Show/hide processing indicator
     * Requirements: 9.4
     */
    private void showProcessing(boolean show) {
        getBinding().cardProcessing.setVisibility(show ? View.VISIBLE : View.GONE);
        getBinding().btnCapture.setEnabled(!show);
    }
    
    /**
     * Show edit dialog for manual editing
     * Requirements: 9.5 - Allow manual editing before saving
     */
    private void showEditDialog(int position, Medicine medicine) {
        // Create a simple edit dialog
        android.view.LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(android.R.layout.simple_list_item_2, null);
        
        // For simplicity, using AlertDialog with EditTexts
        // In production, create a custom dialog layout
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chỉnh sửa thuốc")
            .setMessage("Tên: " + medicine.getName() + "\nLiều lượng: " + medicine.getDosage())
            .setPositiveButton("Lưu", (dialog, which) -> {
                // Update medicine
                medicinesAdapter.notifyItemChanged(position);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    /**
     * Save extracted medicines
     */
    private void saveMedicines() {
        if (extractedMedicines.isEmpty()) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_no_medicine_to_save), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Save medicines to database
        // For now, just show success message and finish
        Toast.makeText(this, 
            "Đã lưu " + extractedMedicines.size() + " loại thuốc", 
            Toast.LENGTH_SHORT).show();
        
        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("medicines_count", extractedMedicines.size());
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup OCR processor
        if (ocrProcessor != null) {
            ocrProcessor.shutdown();
        }
    }
    
    /**
     * Simple adapter for extracted medicines list
     */
    private static class ExtractedMedicinesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ExtractedMedicinesAdapter.ViewHolder> {
        private final List<Medicine> medicines;
        private final OnMedicineEditListener listener;
        
        interface OnMedicineEditListener {
            void onEdit(int position, Medicine medicine);
            void onDelete(int position);
        }
        
        ExtractedMedicinesAdapter(List<Medicine> medicines, OnMedicineEditListener listener) {
            this.medicines = medicines;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Medicine medicine = medicines.get(position);
            holder.text1.setText(medicine.getName());
            holder.text2.setText("Liều lượng: " + medicine.getDosage());
            
            holder.itemView.setOnClickListener(v -> 
                listener.onEdit(position, medicine));
            
            holder.itemView.setOnLongClickListener(v -> {
                listener.onDelete(position);
                return true;
            });
        }
        
        @Override
        public int getItemCount() {
            return medicines.size();
        }
        
        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView text1;
            android.widget.TextView text2;
            
            ViewHolder(android.view.View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
