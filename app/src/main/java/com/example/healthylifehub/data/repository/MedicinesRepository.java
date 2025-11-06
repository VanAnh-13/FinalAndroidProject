package com.example.healthylifehub.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.Medicine;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Medicines data from Firebase Firestore.
 * Collection structure: users/{userId}/medicines/{medicineId}
 */
public class MedicinesRepository extends FirebaseRepository {
    
    private static final String COLLECTION_MEDICINES = "medicines";
    
    /**
     * Load medicines for current user
     * @return LiveData list of medicines
     */
    public LiveData<List<Medicine>> loadMedicines() {
        MutableLiveData<List<Medicine>> medicinesLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            medicinesLiveData.setValue(new ArrayList<>());
            return medicinesLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_MEDICINES)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    medicinesLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<Medicine> medicines = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        String name = doc.getString("name");
                        String dosage = doc.getString("dosage");
                        String frequency = doc.getString("frequency");
                        String instructions = doc.getString("instructions");
                        String startDate = doc.getString("startDate");
                        if (startDate == null) {
                            // Fallback for legacy field name
                            startDate = doc.getString("time");
                        }
                        Boolean isActive = doc.getBoolean("isActive");

                        if (name != null && dosage != null && frequency != null && startDate != null) {
                            Medicine.MedicineStatus statusEnum = (isActive != null && isActive)
                                    ? Medicine.MedicineStatus.ACTIVE
                                    : Medicine.MedicineStatus.STOPPED;

                            medicines.add(new Medicine(
                                    name,
                                    dosage,
                                    frequency,
                                    instructions != null ? instructions : "",
                                    startDate,
                                    statusEnum
                            ));
                        }
                    }
                    medicinesLiveData.setValue(medicines);
                }
            });
        
        return medicinesLiveData;
    }
}

