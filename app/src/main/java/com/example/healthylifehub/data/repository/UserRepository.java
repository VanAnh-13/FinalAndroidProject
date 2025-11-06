package com.example.healthylifehub.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

/**
 * Repository for User Profile data from Firebase Firestore.
 * Collection structure: users/{userId}
 * Matches the provided JSON structure with profile nested object
 */
public class UserRepository extends FirebaseRepository {
    
    private static final String COLLECTION_USERS = "users";
    
    /**
     * Load user profile data
     * @return LiveData of user profile map
     */
    public LiveData<Map<String, Object>> loadUserProfile() {
        MutableLiveData<Map<String, Object>> profileLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            profileLiveData.setValue(null);
            return profileLiveData;
        }
        
        db.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    profileLiveData.setValue(null);
                    return;
                }
                
                if (snapshot != null && snapshot.exists()) {
                    profileLiveData.setValue(snapshot.getData());
                }
            });
        
        return profileLiveData;
    }
    
    /**
     * Get specific profile field
     * @param fieldPath Path to field (e.g., "profile.fullName")
     * @return LiveData of field value
     */
    public LiveData<Object> getProfileField(String fieldPath) {
        MutableLiveData<Object> fieldLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            fieldLiveData.setValue(null);
            return fieldLiveData;
        }
        
        db.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null || snapshot == null || !snapshot.exists()) {
                    fieldLiveData.setValue(null);
                    return;
                }
                
                fieldLiveData.setValue(snapshot.get(fieldPath));
            });
        
        return fieldLiveData;
    }
}

