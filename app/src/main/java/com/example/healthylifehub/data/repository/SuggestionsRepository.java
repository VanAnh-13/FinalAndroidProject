package com.example.healthylifehub.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.SmartSuggestion;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SuggestionsRepository extends FirebaseRepository {
    
    private static final String COLLECTION_SUGGESTIONS = "suggestions";
    
    public LiveData<List<SmartSuggestion>> loadPendingSuggestions() {
        MutableLiveData<List<SmartSuggestion>> suggestionsLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            suggestionsLiveData.setValue(new ArrayList<>());
            return suggestionsLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_SUGGESTIONS)
            .whereEqualTo("status", "pending")
            .orderBy("priority")
            .limit(1)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    suggestionsLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<SmartSuggestion> suggestions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        SmartSuggestion suggestion = doc.toObject(SmartSuggestion.class);
                        if (suggestion != null) {
                            suggestions.add(suggestion);
                        }
                    }
                    suggestionsLiveData.setValue(suggestions);
                }
            });
        
        return suggestionsLiveData;
    }
}
