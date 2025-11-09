package com.example.healthylifehub.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.UserBehavior;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Reminder data from Firebase Firestore.
 * Collection structure: users/{userId}/reminders/{reminderId}
 * Supports CRUD operations and AI behavior analysis
 */
public class RemindersRepository extends FirebaseRepository {
    
    private static final String COLLECTION_REMINDERS = "reminders";
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // ==================== READ OPERATIONS ====================
    
    /**
     * Load all reminders for current user
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
            .orderBy("reminderTime", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    remindersLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<Reminder> reminders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Reminder reminder = parseReminder(doc);
                        if (reminder != null) {
                            reminders.add(reminder);
                        }
                    }
                    remindersLiveData.setValue(reminders);
                }
            });
        
        return remindersLiveData;
    }
    
    /**
     * Load only active reminders
     * @return LiveData list of active reminders
     */
    public LiveData<List<Reminder>> loadActiveReminders() {
        MutableLiveData<List<Reminder>> remindersLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            remindersLiveData.setValue(new ArrayList<>());
            return remindersLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_REMINDERS)
            .whereEqualTo("isActive", true)
            .orderBy("reminderTime", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    remindersLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<Reminder> reminders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Reminder reminder = parseReminder(doc);
                        if (reminder != null) {
                            reminders.add(reminder);
                        }
                    }
                    remindersLiveData.setValue(reminders);
                }
            });
        
        return remindersLiveData;
    }
    
    /**
     * Get single reminder by ID
     * @param reminderId Reminder ID
     * @return CompletableFuture with Reminder
     */
    public CompletableFuture<Reminder> getReminderById(String reminderId) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return null;
            
            try {
                Task<DocumentSnapshot> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .get();
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    return parseReminder(task.getResult());
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, executorService);
    }
    
    // ==================== CREATE/UPDATE OPERATIONS ====================
    
    /**
     * Create new reminder
     * @param reminder Reminder object
     * @return CompletableFuture with reminder ID
     */
    public CompletableFuture<String> createReminder(Reminder reminder) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return null;
            
            try {
                // Generate new ID
                String reminderId = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document()
                        .getId();
                
                reminder.setReminderId(reminderId);
                reminder.setUserId(userId);
                reminder.setCreatedAt(System.currentTimeMillis());
                reminder.setUpdatedAt(System.currentTimeMillis());
                
                Map<String, Object> reminderData = toMap(reminder);
                
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .set(reminderData);
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                return task.isSuccessful() ? reminderId : null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, executorService);
    }
    
    /**
     * Update existing reminder
     * @param reminder Reminder object with updated data
     * @return CompletableFuture<Boolean> success status
     */
    public CompletableFuture<Boolean> updateReminder(Reminder reminder) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null || reminder.getReminderId() == null) return false;
            
            try {
                reminder.setUpdatedAt(System.currentTimeMillis());
                Map<String, Object> reminderData = toMap(reminder);
                
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminder.getReminderId())
                        .set(reminderData, SetOptions.merge());
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                return task.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
    }
    
    /**
     * Toggle reminder active status
     * @param reminderId Reminder ID
     * @param isActive New active status
     * @return CompletableFuture<Boolean> success status
     */
    public CompletableFuture<Boolean> toggleReminderStatus(String reminderId, boolean isActive) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return false;
            
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("isActive", isActive);
                updates.put("updatedAt", Timestamp.now());
                
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .update(updates);
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                return task.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
    }
    
    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Delete reminder
     * @param reminderId Reminder ID
     * @return CompletableFuture<Boolean> success status
     */
    public CompletableFuture<Boolean> deleteReminder(String reminderId) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return false;
            
            try {
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .delete();
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                return task.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Parse Firestore document to Reminder object
     */
    private Reminder parseReminder(DocumentSnapshot doc) {
        try {
            Reminder reminder = new Reminder();
            reminder.setReminderId(doc.getId());
            reminder.setUserId(doc.getString("userId"));
            reminder.setTitle(doc.getString("title"));
            reminder.setDescription(doc.getString("description"));
            
            // Parse timestamp
            Object timeObj = doc.get("reminderTime");
            if (timeObj instanceof Timestamp) {
                reminder.setReminderTime(((Timestamp) timeObj).toDate().getTime());
            } else if (timeObj instanceof Long) {
                reminder.setReminderTime((Long) timeObj);
            }
            
            reminder.setFrequency(doc.getString("frequency"));
            reminder.setActive(Boolean.TRUE.equals(doc.getBoolean("isActive")));
            reminder.setMedicineId(doc.getString("medicineId"));
            
            // Parse created/updated timestamps
            Object createdObj = doc.get("createdAt");
            if (createdObj instanceof Timestamp) {
                reminder.setCreatedAt(((Timestamp) createdObj).toDate().getTime());
            } else if (createdObj instanceof Long) {
                reminder.setCreatedAt((Long) createdObj);
            }
            
            Object updatedObj = doc.get("updatedAt");
            if (updatedObj instanceof Timestamp) {
                reminder.setUpdatedAt(((Timestamp) updatedObj).toDate().getTime());
            } else if (updatedObj instanceof Long) {
                reminder.setUpdatedAt((Long) updatedObj);
            }
            
            return reminder;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convert Reminder to Firestore map
     */
    private Map<String, Object> toMap(Reminder reminder) {
        Map<String, Object> map = new HashMap<>();
        map.put("reminderId", reminder.getReminderId());
        map.put("userId", reminder.getUserId());
        map.put("title", reminder.getTitle());
        map.put("description", reminder.getDescription());
        map.put("reminderTime", new Timestamp(reminder.getReminderTime() / 1000, 0));
        map.put("frequency", reminder.getFrequency());
        map.put("isActive", reminder.isActive());
        map.put("medicineId", reminder.getMedicineId());
        map.put("createdAt", new Timestamp(reminder.getCreatedAt() / 1000, 0));
        map.put("updatedAt", new Timestamp(reminder.getUpdatedAt() / 1000, 0));
        return map;
    }
}

