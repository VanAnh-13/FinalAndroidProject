package com.example.healthylifehub.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.Reminder;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Reminder data from Firebase Firestore.
 * Collection structure: users/{userId}/reminders/{reminderId}
 */
public class RemindersRepository extends FirebaseRepository {
    
    private static final String COLLECTION_REMINDERS = "reminders";
    
    /**
     * Load reminders for current user from Firestore
     * @return LiveData list of reminders
     */
    public LiveData<List<Reminder>> loadReminders() {
        MutableLiveData<List<Reminder>> remindersLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            remindersLiveData.setValue(new ArrayList<>());
            return remindersLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_REMINDERS)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    remindersLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<Reminder> reminders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        String title = doc.getString("title");
                        String time = doc.getString("time");
                        if (title != null && time != null) {
                            reminders.add(new Reminder(title, time));
                        }
                    }
                    remindersLiveData.setValue(reminders);
                }
            });
        
        return remindersLiveData;
    }
}

