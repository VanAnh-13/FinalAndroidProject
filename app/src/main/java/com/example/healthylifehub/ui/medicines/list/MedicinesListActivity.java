package com.example.healthylifehub.ui.medicines.list;

import android.content.Intent;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.Medicine;
import com.example.healthylifehub.data.repository.MedicinesRepository;
import com.example.healthylifehub.databinding.ActivityMedicinesListBinding;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;
import com.example.healthylifehub.ui.medicines.add.AddMedicineActivity;
import com.example.healthylifehub.ui.medicines.list.adapter.MedicinesAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MedicinesListActivity extends BaseActivity<ActivityMedicinesListBinding> {

    private List<Medicine> medicines;
    private MedicinesAdapter adapter;
    private MedicinesRepository medicinesRepository;

    public MedicinesListActivity() {
        super(ActivityMedicinesListBinding::inflate);
    }

    @Override
    public void initData() {
        medicines = new ArrayList<>();
        medicinesRepository = new MedicinesRepository();
        adapter = new MedicinesAdapter(new MedicinesAdapter.OnMedicineClickListener() {
            @Override
            public void onMedicineClick(Medicine medicine) {
                // TODO: Navigate to medicine detail
                Toast.makeText(MedicinesListActivity.this, medicine.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCreateReminderClick(Medicine medicine) {
                Intent intent = new Intent(MedicinesListActivity.this, AddEditReminderActivity.class);
                intent.putExtra("medicine_name", medicine.getName());
                startActivity(intent);
            }
        });

        setupTabs();
        setupRecyclerView();
    }

    @Override
    public void bindData() {
        loadMedicines();
    }

    @Override
    public void setOnClick() {
        getBinding().ivBack.setOnClickListener(v -> finish());

        getBinding().ivCamera.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            intent.putExtra(AddMedicineActivity.EXTRA_START_MODE, AddMedicineActivity.MODE_OCR);
            startActivity(intent);
        });

        getBinding().fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            intent.putExtra(AddMedicineActivity.EXTRA_START_MODE, AddMedicineActivity.MODE_MANUAL);
            startActivity(intent);
        });
    }

    private void setupTabs() {
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.active_medicines));
        getBinding().tabLayout.addTab(getBinding().tabLayout.newTab().setText(R.string.stopped_medicines));

        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadMedicines();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        getBinding().rvMedicines.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvMedicines.setAdapter(adapter);
    }

    private void loadMedicines() {
        // Load medicines from Firebase
        LiveData<List<Medicine>> medicinesLiveData = medicinesRepository.loadMedicines();
        medicinesLiveData.observe(this, medicinesList -> {
            if (medicinesList != null) {
                medicines.clear();
                
                // Filter based on selected tab
                int selectedTab = getBinding().tabLayout.getSelectedTabPosition();
                for (Medicine medicine : medicinesList) {
                    if (selectedTab == 0) {
                        // Active medicines
                        if (medicine.isActive()) {
                            medicines.add(medicine);
                        }
                    } else {
                        // Stopped medicines
                        if (!medicine.isActive()) {
                            medicines.add(medicine);
                        }
                    }
                }
                
                adapter.setMedicines(medicines);
            }
        });
    }
}
