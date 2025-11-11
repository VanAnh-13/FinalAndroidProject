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
            getBinding().tvHospital.setText("Community Health Clinic");
            getBinding().tvDoctor.setText("BS. Eleanor Vance");
            getBinding().tvDiagnosis.setText("Khám sức khỏe định kỳ, không có vấn đề cấp tính. Khuyến nghị tiếp tục tập trung vào chế độ ăn uống và tập thể dục.");
            getBinding().tvNotes.setText(record.getDescription());
        }
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().ivMore.setOnClickListener(v -> {
            // TODO: Show menu with delete option
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
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
            .setTitle("Xóa hồ sơ bệnh án")
            .setMessage("Bạn có chắc chắn muốn xóa hồ sơ này? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa", (dialog, which) -> {
                deleteRecord();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void deleteRecord() {
        // TODO: Delete from database
        Toast.makeText(this, "Đã xóa hồ sơ", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadRecordData() {
        // TODO: Load from database
        // For now, use sample data
        record = new MedicalRecord(
            "26 Tháng 10 2023",
            "Khám sức khỏe định kỳ",
            "Bệnh nhân báo cáo cảm thấy khỏe. Các dấu hiệu sinh tồn đều trong phạm vi bình thường. Huyết áp hơi cao ở mức 130/85 mmHg, sẽ theo dõi. Đã thảo luận các chiến lược giảm căng thẳng và duy trì chế độ ăn uống cân bằng. Đã lên lịch tái khám sau sáu tháng để đánh giá lại huyết áp.",
            null,
            MedicalRecord.RecordType.CHECKUP
        );
    }
}
