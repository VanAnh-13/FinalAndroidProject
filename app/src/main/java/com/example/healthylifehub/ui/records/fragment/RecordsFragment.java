package com.example.healthylifehub.ui.records.fragment;

import android.content.Intent;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentRecordsBinding;
import com.example.healthylifehub.ui.records.fragment.adapter.RecordsAdapter;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.repository.MedicalRecordsRepository;
import java.util.ArrayList;

/**
 * Fragment for displaying medical records
 * Shows a list of medical records with options to add, edit, and delete
 */
public class RecordsFragment extends BaseFragment<FragmentRecordsBinding> {

    private static final String TAG = "RecordsFragment";
    private RecordsViewModel viewModel;
    private RecordsAdapter adapter;
    private MedicalRecordsRepository medicalRecordsRepository;

    public RecordsFragment() {
        super(FragmentRecordsBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(RecordsViewModel.class);
        adapter = new RecordsAdapter(this::onRecordClick);
        medicalRecordsRepository = new MedicalRecordsRepository(requireContext());
    }

    @Override
    public void bindData() {
        // Setup RecyclerView
        getBinding().rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvRecords.setAdapter(adapter);
    }

    @Override
    public void observeData() {
        // Auto-sync from Firestore when fragment loads
        medicalRecordsRepository.syncFromFirestore()
            .thenAccept(success -> {
                if (success) {
                    Log.d(TAG, "✅ Synced medical records from Firestore");
                } else {
                    Log.w(TAG, "⚠️ Failed to sync records from Firestore");
                }
            })
            .exceptionally(e -> {
                Log.w(TAG, "⚠️ Sync error", e);
                return null;
            });
        
        // Observe medical records
        viewModel.getRecords().observe(getViewLifecycleOwner(), records -> {
            if (records != null && !records.isEmpty()) {
                adapter.setRecords(records);
                getBinding().emptyState.setVisibility(android.view.View.GONE);
                getBinding().rvRecords.setVisibility(android.view.View.VISIBLE);
            } else {
                getBinding().emptyState.setVisibility(android.view.View.VISIBLE);
                getBinding().rvRecords.setVisibility(android.view.View.GONE);
            }
        });
    }

    @Override
    public void setOnClick() {
        getBinding().ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.healthylifehub.utils.navigation.DrawerController) {
                ((com.example.healthylifehub.utils.navigation.DrawerController) getActivity()).openDrawer();
            }
        });

        // FAB to add new record
        getBinding().fabAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.healthylifehub.ui.records.add_edit.AddEditRecordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Handle record click - navigate to detail or edit
     */
    private void onRecordClick(MedicalRecord record) {
        Intent intent = new Intent(requireContext(), com.example.healthylifehub.ui.records.add_edit.AddEditRecordActivity.class);
        intent.putExtra("record_id", record.getId());
        startActivity(intent);
    }
}
