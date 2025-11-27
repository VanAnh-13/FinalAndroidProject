package com.example.healthylifehub.ui.records.detail;

import android.content.Intent;
import android.widget.Toast;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.databinding.ActivityRecordDetailBinding;
import com.example.healthylifehub.ui.records.add_edit.AddEditRecordActivity;

public class RecordDetailActivity extends BaseActivity<ActivityRecordDetailBinding> {

    private String recordId;
    private MedicalRecord record;

    public RecordDetailActivity() {
        super(ActivityRecordDetailBinding::inflate);
    }

    @Override
    public void initData() {
        recordId = getIntent().getStringExtra("record_id");
        loadRecordData();
    }

    @Override
    public void bindData() {
        if (record != null) {
            getBinding().tvRecordTitle.setText(record.getTitle());
            getBinding().tvRecordDate.setText(record.getDate());
            getBinding().tvHospital.setText(record.getHospital());
            getBinding().tvDoctor.setText(record.getDoctor());
            getBinding().tvDiagnosis.setText(record.getDiagnosis());
            getBinding().tvNotes.setText(record.getDescription());
        }
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().ivMore.setOnClickListener(v -> {
            // TODO: Show menu with delete option
            Toast.makeText(this, getString(R.string.toast_menu), Toast.LENGTH_SHORT).show();
        });

        // Edit button
        getBinding().btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditRecordActivity.class);
            intent.putExtra("record_id", recordId);
            startActivity(intent);
        });

        // Delete button
        getBinding().btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation();
        });
    }

    private void showDeleteConfirmation() {
        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.record_delete_title)
            .setMessage(R.string.record_delete_message)
            .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                deleteRecord();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void deleteRecord() {
        // TODO: Delete from database
        Toast.makeText(this, getString(R.string.toast_record_deleted), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadRecordData() {
        // TODO: Load from database using recordId
        // record will be loaded from repository
    }
}
