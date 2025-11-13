package com.example.healthylifehub.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.utils.ApplicationContextProvider;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemindersRepository extends FirebaseRepository {
    
    private static final String TAG = "RemindersRepository";
    private static final String COLLECTION_REMINDERS = "reminders";
    
    private final ReminderDao reminderDao;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    public RemindersRepository() {
        super();
        this.reminderDao = AppDatabase.getInstance(ApplicationContextProvider.getContext()).reminderDao();
    }
    
    public LiveData<List<Reminder>> loadReminders() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MediatorLiveData<>();
        }
        
        LiveData<List<Reminder>> localData = reminderDao.getAllReminders(userId);
        
        fetchRemindersFromFirebase(userId);
        
        return localData;
    }
    
    public LiveData<List<Reminder>> loadActiveReminders() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MediatorLiveData<>();
        }
        
        LiveData<List<Reminder>> localData = reminderDao.getActiveReminders(userId);
        
        fetchRemindersFromFirebase(userId);
        
        return localData;
    }
    
    private void fetchRemindersFromFirebase(String userId) {
        executorService.execute(() -> {
            db.collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDERS)
                .orderBy("reminderTime", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    executorService.execute(() -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Reminder reminder = parseReminder(doc);
                            if (reminder != null) {
                                reminderDao.insert(reminder);
                            }
                        }
                        Log.d(TAG, "✅ Synced " + querySnapshot.size() + " reminders from Firebase");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch from Firebase (offline mode)", e);
                });
        });
    }
    
    public CompletableFuture<Reminder> getReminderById(String reminderId) {
        return CompletableFuture.supplyAsync(() -> {
            Reminder localReminder = reminderDao.getReminderById(reminderId);
            if (localReminder != null) {
                Log.d(TAG, "📱 Loaded from local cache: " + reminderId);
                return localReminder;
            }
            
            String userId = getCurrentUserId();
            if (userId == null) return null;
            
            try {
                Task<DocumentSnapshot> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .get();
                
                // Add 5 second timeout to prevent blocking
                long startTime = System.currentTimeMillis();
                long timeout = 5000; // 5 seconds
                while (!task.isComplete() && System.currentTimeMillis() - startTime < timeout) {
                    Thread.sleep(50);
                }
                
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Reminder reminder = parseReminder(task.getResult());
                    if (reminder != null) {
                        reminderDao.insert(reminder);
                        Log.d(TAG, "☁️ Fetched from Firebase and cached: " + reminderId);
                    }
                    return reminder;
                }
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error fetching reminder (offline mode)", e);
                return null;
            }
        }, executorService);
    }
    
    public CompletableFuture<String> createReminder(Reminder reminder) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return null;
            
            try {
                String reminderId = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document()
                        .getId();
                
                reminder.setReminderId(reminderId);
                reminder.setUserId(userId);
                reminder.setCreatedAt(System.currentTimeMillis());
                reminder.setUpdatedAt(System.currentTimeMillis());
                
                reminderDao.insert(reminder);
                Log.d(TAG, "📱 Saved to local DB first: " + reminderId);
                
                Map<String, Object> reminderData = toMap(reminder);
                
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .set(reminderData);
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                if (task.isSuccessful()) {
                    Log.d(TAG, "☁️ Synced to Firebase: " + reminderId);
                } else {
                    Log.w(TAG, "⚠️ Offline mode - will sync later: " + reminderId);
                }
                
                return reminderId;
            } catch (Exception e) {
                Log.e(TAG, "Error creating reminder", e);
                return null;
            }
        }, executorService);
    }
    
    public CompletableFuture<Boolean> updateReminder(Reminder reminder) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null || reminder.getReminderId() == null) return false;
            
            try {
                reminder.setUpdatedAt(System.currentTimeMillis());
                
                reminderDao.update(reminder);
                Log.d(TAG, "📱 Updated local DB: " + reminder.getReminderId());
                
                Map<String, Object> reminderData = toMap(reminder);
                
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminder.getReminderId())
                        .set(reminderData, SetOptions.merge());
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                if (task.isSuccessful()) {
                    Log.d(TAG, "☁️ Synced update to Firebase");
                } else {
                    Log.w(TAG, "⚠️ Offline mode - will sync later");
                }
                
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error updating reminder", e);
                return false;
            }
        }, executorService);
    }
    
    public CompletableFuture<Boolean> toggleReminderStatus(String reminderId, boolean isActive) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return false;
            
            try {
                reminderDao.updateStatus(reminderId, isActive);
                Log.d(TAG, "📱 Toggled status in local DB: " + reminderId);
                
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
                
                if (task.isSuccessful()) {
                    Log.d(TAG, "☁️ Synced status to Firebase");
                } else {
                    Log.w(TAG, "⚠️ Offline mode - will sync later");
                }
                
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error toggling status", e);
                return false;
            }
        }, executorService);
    }
    
    public CompletableFuture<Boolean> deleteReminder(String reminderId) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return false;
            
            try {
                reminderDao.deleteById(reminderId);
                Log.d(TAG, "📱 Deleted from local DB: " + reminderId);
                
                Task<Void> task = db.collection("users")
                        .document(userId)
                        .collection(COLLECTION_REMINDERS)
                        .document(reminderId)
                        .delete();
                
                while (!task.isComplete()) {
                    Thread.sleep(50);
                }
                
                if (task.isSuccessful()) {
                    Log.d(TAG, "☁️ Deleted from Firebase");
                } else {
                    Log.w(TAG, "⚠️ Offline mode - deletion marked locally");
                }
                
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error deleting reminder", e);
                return false;
            }
        }, executorService);
    }
    
    private Reminder parseReminder(DocumentSnapshot doc) {
        try {
            // Validate critical fields
            String reminderId = doc.getId();
            String userId = doc.getString("userId");
            String title = doc.getString("title");
            String frequency = doc.getString("frequency");
            
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "Invalid userId in reminder document");
                return null;
            }
            if (title == null || title.isEmpty()) {
                Log.e(TAG, "Invalid title in reminder document");
                return null;
            }
            if (frequency == null || frequency.isEmpty()) {
                Log.e(TAG, "Invalid frequency in reminder document");
                return null;
            }
            
            Reminder reminder = new Reminder();
            reminder.setReminderId(reminderId);
            reminder.setUserId(userId);
            reminder.setTitle(title);
            reminder.setDescription(doc.getString("description"));
            
            // Parse reminderTime
            Object timeObj = doc.get("reminderTime");
            if (timeObj instanceof Timestamp) {
                reminder.setReminderTime(((Timestamp) timeObj).toDate().getTime());
            } else if (timeObj instanceof Long) {
                reminder.setReminderTime((Long) timeObj);
            } else {
                Log.w(TAG, "Invalid reminderTime format");
                return null;
            }
            
            reminder.setFrequency(frequency);
            reminder.setActive(Boolean.TRUE.equals(doc.getBoolean("isActive")));
            reminder.setMedicineId(doc.getString("medicineId"));
            
            // Parse createdAt
            Object createdObj = doc.get("createdAt");
            if (createdObj instanceof Timestamp) {
                reminder.setCreatedAt(((Timestamp) createdObj).toDate().getTime());
            } else if (createdObj instanceof Long) {
                reminder.setCreatedAt((Long) createdObj);
            }
            
            // Parse updatedAt
            Object updatedObj = doc.get("updatedAt");
            if (updatedObj instanceof Timestamp) {
                reminder.setUpdatedAt(((Timestamp) updatedObj).toDate().getTime());
            } else if (updatedObj instanceof Long) {
                reminder.setUpdatedAt((Long) updatedObj);
            }
            
            return reminder;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing reminder", e);
            return null;
        }
    }
    
    private Map<String, Object> toMap(Reminder reminder) {
        Map<String, Object> map = new HashMap<>();
        map.put("reminderId", reminder.getReminderId());
        map.put("userId", reminder.getUserId());
        map.put("title", reminder.getTitle());
        map.put("description", reminder.getDescription());
        
        // Convert milliseconds to seconds for Firestore Timestamp
        long reminderSeconds = reminder.getReminderTime() / 1000;
        map.put("reminderTime", new Timestamp(reminderSeconds, 0));
        
        map.put("frequency", reminder.getFrequency());
        map.put("isActive", reminder.isActive());
        map.put("medicineId", reminder.getMedicineId());
        
        // Convert milliseconds to seconds for Firestore Timestamp
        long createdSeconds = reminder.getCreatedAt() / 1000;
        map.put("createdAt", new Timestamp(createdSeconds, 0));
        
        long updatedSeconds = reminder.getUpdatedAt() / 1000;
        map.put("updatedAt", new Timestamp(updatedSeconds, 0));
        
        return map;
    }
}
