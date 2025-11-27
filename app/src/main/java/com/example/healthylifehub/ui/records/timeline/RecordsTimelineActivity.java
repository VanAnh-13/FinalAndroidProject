package com.example.healthylifehub.ui.records.timeline;

import android.view.View;
import android.widget.Toast;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.databinding.ActivityRecordsTimelineBinding;
import com.example.healthylifehub.ui.records.fragment.adapter.RecordsAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class RecordsTimelineActivity extends BaseActivity<ActivityRecordsTimelineBinding> {

    private List<MedicalRecord> records;
    private RecordsAdapter adapter;

    public RecordsTimelineActivity() {
        super(ActivityRecordsTimelineBinding::inflate);
    }

    @Override
    public void initData() {
        records = new ArrayList<>();
        adapter = new RecordsAdapter(record -> {
            // TODO: Navigate to record detail
            Toast.makeText(this, record.getTitle(), Toast.LENGTH_SHORT).show();
        });
        
        setupTabs();
    }

    @Override
    public void bindData() {
        loadRecords();
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().fabAdd.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_add_new_record), Toast.LENGTH_SHORT).show();
            // TODO: Open add record screen
        });

        getBinding().btnAddFirstRecord.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_add_first_record), Toast.LENGTH_SHORT).show();
            // TODO: Open add record screen
        });
    }

    private void setupTabs() {
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.timeline));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.medicines));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.allergies));

        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadRecords();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadRecords() {
        // TODO: Load records from database based on selected tab
        records.clear();
        
        // Sample data for Timeline tab
        if (getBinding().tabLayout.getSelectedTabPosition() == 0) {
            records.add(new MedicalRecord(
                getString(R.string.record_date_1),
                getString(R.string.cardiology_consultation),
                getString(R.string.record_desc_1),
                "report.pdf",
                MedicalRecord.RecordType.CARDIOLOGY
            ));
            
            records.add(new MedicalRecord(
                getString(R.string.record_date_2),
                getString(R.string.annual_checkup),
                getString(R.string.record_desc_2),
                null,
                MedicalRecord.RecordType.CHECKUP
            ));
            
            records.add(new MedicalRecord(
                getString(R.string.record_date_3),
                getString(R.string.allergy_test),
                getString(R.string.record_desc_3),
                "allergy_results.jpg",
                MedicalRecord.RecordType.ALLERGY
            ));
        }

        if (records.isEmpty()) {
            showEmptyState();
        } else {
            showRecords();
        }
    }

    private void showEmptyState() {
        getBinding().llTimelineContainer.setVisibility(View.GONE);
        getBinding().llEmptyState.setVisibility(View.VISIBLE);
    }

    private void showRecords() {
        getBinding().llTimelineContainer.setVisibility(View.VISIBLE);
        getBinding().llEmptyState.setVisibility(View.GONE);
        adapter.setRecords(records);
    }
}
