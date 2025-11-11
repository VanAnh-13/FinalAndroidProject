package com.example.healthylifehub.ui.records.fragment;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.repository.MedicalRecordsRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel for Medical Records
 * Manages loading and displaying medical records
 */
public class RecordsViewModel extends BaseViewModel {
    
    private final MedicalRecordsRepository repository;
    private final MediatorLiveData<List<MedicalRecord>> sortedRecords = new MediatorLiveData<>();
    
    public RecordsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MedicalRecordsRepository(application.getApplicationContext());
        loadAndSortRecords();
    }
    
    /**
     * Load and sort medical records by date (newest first)
     */
    private void loadAndSortRecords() {
        LiveData<List<MedicalRecord>> source = repository.getAllRecords();
        sortedRecords.addSource(source, records -> {
            if (records != null) {
                // Sort by date descending (newest first)
                List<MedicalRecord> sorted = new ArrayList<>(records);
                sorted.sort((r1, r2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        long date1 = sdf.parse(r1.getDate() != null ? r1.getDate() : "01/01/1970").getTime();
                        long date2 = sdf.parse(r2.getDate() != null ? r2.getDate() : "01/01/1970").getTime();
                        return Long.compare(date2, date1); // Descending order
                    } catch (Exception e) {
                        return 0;
                    }
                });
                sortedRecords.setValue(sorted);
            } else {
                sortedRecords.setValue(null);
            }
        });
    }
    
    /**
     * Get all medical records sorted by date (newest first)
     */
    public LiveData<List<MedicalRecord>> getRecords() {
        return sortedRecords;
    }
    
    /**
     * Delete a medical record
     */
    public void deleteRecord(MedicalRecord record) {
        repository.deleteRecord(record)
            .thenAccept(success -> {
                if (success) {
                    android.util.Log.d("RecordsViewModel", "✅ Record deleted");
                } else {
                    android.util.Log.e("RecordsViewModel", "❌ Failed to delete record");
                }
            });
    }
}
