package com.example.healthylifehub.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.local.dao.ReminderHistoryDao;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.ReminderHistory;
import com.example.healthylifehub.services.ReminderSchedulingService;
import com.example.healthylifehub.utils.app.ApplicationContextProvider;
import com.example.healthylifehub.sync.SyncManager;
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
    private final ReminderHistoryDao reminderHistoryDao;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final SyncManager syncManager;
    private final ReminderSchedulingService schedulingService;

    public RemindersRepository() {
        super();
        this.syncManager = new SyncManager(ApplicationContextProvider.getContext());
        AppDatabase database = AppDatabase.getInstance(ApplicationContextProvider.getContext());
        this.reminderDao = database.reminderDao();
        this.reminderHistoryDao = database.reminderHistoryDao();
        this.schedulingService = ReminderSchedulingService.getInstance(ApplicationContextProvider.getContext());
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
                
                // Trigger immediate sync after reminder create (Requirement 12.1)
                syncManager.triggerImmediateSync();
                Log.d(TAG, "🔄 Triggered immediate sync after reminder create");

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
                
                // Schedule notifications for the new reminder
                schedulingService.onReminderCreated(reminder, new ReminderSchedulingService.SchedulingCallback() {
                    @Override
                    public void onSuccess(int count) {
                        Log.d(TAG, "✅ Scheduled " + count + " notifications for new reminder: " + reminderId);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Failed to schedule notifications for new reminder: " + error);
                    }
                });

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
                
                // Trigger immediate sync after reminder update (Requirement 12.1)
                syncManager.triggerImmediateSync();
                Log.d(TAG, "🔄 Triggered immediate sync after reminder update");

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
                
                // Reschedule notifications for the updated reminder
                schedulingService.onReminderUpdated(reminder, new ReminderSchedulingService.SchedulingCallback() {
                    @Override
                    public void onSuccess(int count) {
                        Log.d(TAG, "✅ Rescheduled " + count + " notifications for updated reminder: " + reminder.getReminderId());
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Failed to reschedule notifications for updated reminder: " + error);
                    }
                });

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
                
                // Handle notification scheduling based on status change
                if (!isActive) {
                    // Cancel notifications when reminder is deactivated
                    schedulingService.onReminderDeactivated(reminderId, new ReminderSchedulingService.SchedulingCallback() {
                        @Override
                        public void onSuccess(int count) {
                            Log.d(TAG, "✅ Cancelled notifications for deactivated reminder: " + reminderId);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Failed to cancel notifications for deactivated reminder: " + error);
                        }
                    });
                } else {
                    // Reschedule notifications when reminder is reactivated
                    Reminder reminder = reminderDao.getReminderById(reminderId);
                    if (reminder != null) {
                        schedulingService.onReminderUpdated(reminder, new ReminderSchedulingService.SchedulingCallback() {
                            @Override
                            public void onSuccess(int count) {
                                Log.d(TAG, "✅ Rescheduled " + count + " notifications for reactivated reminder: " + reminderId);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Failed to reschedule notifications for reactivated reminder: " + error);
                            }
                        });
                    }
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
                
                // Cancel notifications for the deleted reminder
                schedulingService.onReminderDeleted(reminderId, new ReminderSchedulingService.SchedulingCallback() {
                    @Override
                    public void onSuccess(int count) {
                        Log.d(TAG, "✅ Cancelled notifications for deleted reminder: " + reminderId);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Failed to cancel notifications for deleted reminder: " + error);
                    }
                });

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
            
            // Parse deadline
            Object deadlineObj = doc.get("deadline");
            if (deadlineObj instanceof Timestamp) {
                reminder.setDeadline(((Timestamp) deadlineObj).toDate().getTime());
            } else if (deadlineObj instanceof Long) {
                reminder.setDeadline((Long) deadlineObj);
            }

            // Parse progress tracking fields
            Long totalExpected = doc.getLong("totalExpected");
            if (totalExpected != null) {
                reminder.setTotalExpected(totalExpected.intValue());
            }

            Long completedCount = doc.getLong("completedCount");
            if (completedCount != null) {
                reminder.setCompletedCount(completedCount.intValue());
            }

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
        
        // Add deadline if present
        if (reminder.hasDeadline()) {
            long deadlineSeconds = reminder.getDeadline() / 1000;
            map.put("deadline", new Timestamp(deadlineSeconds, 0));
        }

        // Add progress tracking fields
        map.put("totalExpected", reminder.getTotalExpected());
        map.put("completedCount", reminder.getCompletedCount());

        // Convert milliseconds to seconds for Firestore Timestamp
        long createdSeconds = reminder.getCreatedAt() / 1000;
        map.put("createdAt", new Timestamp(createdSeconds, 0));
        
        long updatedSeconds = reminder.getUpdatedAt() / 1000;
        map.put("updatedAt", new Timestamp(updatedSeconds, 0));
        
        return map;
    }

    // ==================== PROGRESS TRACKING METHODS ====================
    // Requirements: 5.4, 4.5

    /**
     * Get all reminders with progress tracking data
     * Requirements: 5.4
     *
     * @return LiveData list of reminders with updated progress
     */
    public LiveData<List<Reminder>> getAllRemindersWithProgress() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MediatorLiveData<>();
        }

        // Get reminders and update their progress from history
        LiveData<List<Reminder>> reminders = reminderDao.getAllReminders(userId);

        // Update progress for each reminder in background
        executorService.execute(() -> {
            try {
                List<Reminder> reminderList = reminderDao.getAllRemindersSync(userId);
                for (Reminder reminder : reminderList) {
                    updateReminderProgressFromHistory(reminder.getReminderId());
                }
                Log.d(TAG, "✅ Updated progress for " + reminderList.size() + " reminders");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to update reminder progress", e);
            }
        });

        return reminders;
    }

    /**
     * Get reminder history for a specific reminder
     * Requirements: 4.5
     *
     * @param reminderId The reminder ID
     * @return LiveData list of history entries
     */
    public LiveData<List<ReminderHistory>> getReminderHistory(String reminderId) {
        if (reminderId == null || reminderId.trim().isEmpty()) {
            return new MediatorLiveData<>();
        }

        return reminderHistoryDao.getHistoryByReminderId(reminderId);
    }

    /**
     * Update reminder progress by recalculating from history
     * Requirements: 5.4
     *
     * @param reminderId The reminder ID to update
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> updateReminderProgress(String reminderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return updateReminderProgressFromHistory(reminderId);
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to update reminder progress: " + reminderId, e);
                return false;
            }
        }, executorService);
    }

    /**
     * Internal method to update reminder progress from history
     *
     * @param reminderId The reminder ID
     * @return true if updated successfully
     */
    private boolean updateReminderProgressFromHistory(String reminderId) {
        try {
            // Get current reminder
            Reminder reminder = reminderDao.getReminderById(reminderId);
            if (reminder == null) {
                Log.w(TAG, "⚠️ Reminder not found for progress update: " + reminderId);
                return false;
            }

            // Get completed count from history
            int completedCount = reminderHistoryDao.getCompletedCountByReminderId(reminderId);

            // Update reminder if count changed
            if (reminder.getCompletedCount() != completedCount) {
                reminder.setCompletedCount(completedCount);
                reminder.setUpdatedAt(System.currentTimeMillis());

                reminderDao.update(reminder);
                Log.d(TAG, "✅ Updated progress for reminder: " + reminderId +
                          " -> " + completedCount + "/" + reminder.getTotalExpected());

                // Sync to Firebase if online
                syncProgressToFirebase(reminder);

                return true;
            }

            return true; // No update needed, but not an error

        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating reminder progress from history", e);
            return false;
        }
    }

    /**
     * Sync progress data to Firebase
     *
     * @param reminder The reminder with updated progress
     */
    private void syncProgressToFirebase(Reminder reminder) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        try {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("completedCount", reminder.getCompletedCount());
            progressData.put("totalExpected", reminder.getTotalExpected());
            progressData.put("updatedAt", new Timestamp(reminder.getUpdatedAt() / 1000, 0));

            db.collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDERS)
                .document(reminder.getReminderId())
                .update(progressData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "☁️ Synced progress to Firebase: " + reminder.getReminderId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Failed to sync progress to Firebase (offline mode): " + reminder.getReminderId());
                });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing progress to Firebase", e);
        }
    }

    /**
     * Get reminders that are approaching their deadline (within 3 days)
     * Requirements: 2.5, 7.6
     *
     * @return CompletableFuture with list of approaching deadline reminders
     */
    public CompletableFuture<List<Reminder>> getApproachingDeadlineReminders() {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return new java.util.ArrayList<>();

            try {
                long currentTime = System.currentTimeMillis();
                long threeDaysFromNow = currentTime + (3 * 24 * 60 * 60 * 1000L);

                List<Reminder> allReminders = reminderDao.getAllRemindersSync(userId);
                List<Reminder> approachingReminders = new java.util.ArrayList<>();

                for (Reminder reminder : allReminders) {
                    if (reminder.isActive() &&
                        reminder.hasDeadline() &&
                        !reminder.isExpired() &&
                        !reminder.isCompleted() &&
                        reminder.getDeadline() <= threeDaysFromNow) {

                        approachingReminders.add(reminder);
                    }
                }

                Log.d(TAG, "📅 Found " + approachingReminders.size() + " reminders approaching deadline");
                return approachingReminders;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting approaching deadline reminders", e);
                return new java.util.ArrayList<>();
            }
        }, executorService);
    }

    /**
     * Get completion statistics for a reminder
     * Requirements: 4.5
     *
     * @param reminderId The reminder ID
     * @return CompletableFuture with completion rate (0.0 to 100.0)
     */
    public CompletableFuture<Float> getCompletionRate(String reminderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reminderHistoryDao.getCompletionRate(reminderId);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting completion rate for: " + reminderId, e);
                return 0.0f;
            }
        }, executorService);
    }

    /**
     * Add database transaction support for consistent updates
     * Requirements: 5.4
     *
     * @param reminderId The reminder ID
     * @param actionType "completed" or "skipped"
     * @param timestamp Action timestamp
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> recordReminderAction(String reminderId, String actionType, long timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use database transaction for consistency
                AppDatabase database = AppDatabase.getInstance(ApplicationContextProvider.getContext());

                database.runInTransaction(() -> {
                    // Create history record
                    ReminderHistory history = new ReminderHistory();
                    history.setId(java.util.UUID.randomUUID().toString());
                    history.setReminderId(reminderId);
                    history.setActionType(actionType);
                    history.setTimestamp(timestamp);
                    history.setScheduledTime(timestamp);

                    reminderHistoryDao.insert(history);

                    // Update reminder progress if completed
                    if ("completed".equals(actionType)) {
                        updateReminderProgressFromHistory(reminderId);
                    }

                    return null;
                });

                Log.d(TAG, "✅ Recorded " + actionType + " action for reminder: " + reminderId);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to record reminder action", e);
                return false;
            }
        }, executorService);
    }

    // ==================== DEADLINE MANAGEMENT METHODS ====================
    // Requirements: 2.4, 2.5, 7.6

    /**
     * Get expired reminders for automatic deactivation
     * Requirements: 2.4
     *
     * @return CompletableFuture with list of expired reminders
     */
    public CompletableFuture<List<Reminder>> getExpiredReminders() {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return new java.util.ArrayList<>();

            try {
                long currentTime = System.currentTimeMillis();
                List<Reminder> expiredReminders = reminderDao.getExpiredReminders(userId, currentTime);

                Log.d(TAG, "📅 Found " + expiredReminders.size() + " expired reminders for user: " + userId);
                return expiredReminders;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting expired reminders", e);
                return new java.util.ArrayList<>();
            }
        }, executorService);
    }

    /**
     * Deactivate expired reminders and calculate final progress
     * Requirements: 2.4, 7.6
     *
     * @return CompletableFuture with number of reminders deactivated
     */
    public CompletableFuture<Integer> deactivateExpiredReminders() {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return 0;

            try {
                long currentTime = System.currentTimeMillis();
                List<Reminder> expiredReminders = reminderDao.getExpiredReminders(userId, currentTime);

                if (expiredReminders.isEmpty()) {
                    Log.d(TAG, "📭 No expired reminders found for deactivation");
                    return 0;
                }

                int deactivatedCount = 0;

                for (Reminder reminder : expiredReminders) {
                    if (reminder.isActive()) {
                        // Calculate final progress before deactivation
                        float finalProgress = reminder.getProgressPercentage();

                        // Deactivate the reminder
                        reminder.setActive(false);
                        reminder.setUpdatedAt(currentTime);

                        reminderDao.update(reminder);
                        deactivatedCount++;

                        Log.d(TAG, "🔒 Deactivated expired reminder: " + reminder.getTitle() +
                                  " (Final progress: " + String.format("%.1f", finalProgress) + "%)");

                        // Sync to Firebase
                        syncReminderToFirebase(reminder);
                    }
                }

                Log.d(TAG, "✅ Deactivated " + deactivatedCount + " expired reminders");
                return deactivatedCount;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error deactivating expired reminders", e);
                return 0;
            }
        }, executorService);
    }

    /**
     * Extend deadline for a reminder
     * Requirements: 2.4 (deadline extension functionality)
     *
     * @param reminderId The reminder ID
     * @param newDeadline The new deadline timestamp
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> extendDeadline(String reminderId, long newDeadline) {
        return CompletableFuture.supplyAsync(() -> {
            if (reminderId == null || reminderId.trim().isEmpty()) {
                Log.w(TAG, "⚠️ Invalid reminder ID for deadline extension");
                return false;
            }

            if (newDeadline <= System.currentTimeMillis()) {
                Log.w(TAG, "⚠️ New deadline must be in the future");
                return false;
            }

            try {
                Reminder reminder = reminderDao.getReminderById(reminderId);
                if (reminder == null) {
                    Log.w(TAG, "⚠️ Reminder not found: " + reminderId);
                    return false;
                }

                Long oldDeadline = reminder.getDeadline();

                // Update deadline and recalculate total expected
                reminder.setDeadline(newDeadline);
                reminder.setUpdatedAt(System.currentTimeMillis());

                // Recalculate total expected based on new deadline
                // Note: You'll need to implement ProgressCalculator.calculateTotalExpected
                // For now, we'll use a simple calculation
                int newTotalExpected = calculateTotalExpectedForReminder(reminder);
                reminder.setTotalExpected(newTotalExpected);

                reminderDao.update(reminder);

                Log.d(TAG, "✅ Extended deadline for reminder: " + reminder.getTitle() +
                          " from " + (oldDeadline != null ? new java.util.Date(oldDeadline) : "None") +
                          " to " + new java.util.Date(newDeadline) +
                          " (New total expected: " + newTotalExpected + ")");

                // Sync to Firebase
                syncReminderToFirebase(reminder);

                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to extend deadline for reminder: " + reminderId, e);
                return false;
            }
        }, executorService);
    }

    /**
     * Get reminders that need deadline warnings (within 3 days)
     * Requirements: 2.5
     *
     * @return CompletableFuture with list of reminders needing warnings
     */
    public CompletableFuture<List<Reminder>> getRemindersNeedingDeadlineWarnings() {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) return new java.util.ArrayList<>();

            try {
                long currentTime = System.currentTimeMillis();
                long threeDaysFromNow = currentTime + (3 * 24 * 60 * 60 * 1000L);

                List<Reminder> warningReminders = reminderDao.getRemindersExpiringSoon(
                        userId, currentTime, threeDaysFromNow);

                // Filter to only include active, non-completed reminders
                List<Reminder> filteredReminders = new java.util.ArrayList<>();
                for (Reminder reminder : warningReminders) {
                    if (reminder.isActive() && !reminder.isCompleted()) {
                        filteredReminders.add(reminder);
                    }
                }

                Log.d(TAG, "⚠️ Found " + filteredReminders.size() +
                          " reminders needing deadline warnings for user: " + userId);

                return filteredReminders;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting reminders needing deadline warnings", e);
                return new java.util.ArrayList<>();
            }
        }, executorService);
    }

    /**
     * Calculate total expected reminders based on frequency and deadline
     *
     * @param reminder The reminder
     * @return Total expected count
     */
    private int calculateTotalExpectedForReminder(Reminder reminder) {
        if (!reminder.hasDeadline()) {
            return 0; // Unlimited reminders
        }

        long startTime = reminder.getCreatedAt();
        long endTime = reminder.getDeadline();
        String frequency = reminder.getFrequency();

        switch (frequency.toLowerCase()) {
            case "daily":
                return (int) ((endTime - startTime) / (24 * 60 * 60 * 1000L)) + 1;
            case "weekly":
                return (int) ((endTime - startTime) / (7 * 24 * 60 * 60 * 1000L)) + 1;
            case "monthly":
                // Approximate calculation (30 days per month)
                return (int) ((endTime - startTime) / (30 * 24 * 60 * 60 * 1000L)) + 1;
            case "once":
                return 1;
            default:
                return 1;
        }
    }

    /**
     * Sync reminder to Firebase (helper method)
     *
     * @param reminder The reminder to sync
     */
    private void syncReminderToFirebase(Reminder reminder) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        try {
            Map<String, Object> reminderData = toMap(reminder);

            db.collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDERS)
                .document(reminder.getReminderId())
                .set(reminderData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "☁️ Synced reminder to Firebase: " + reminder.getReminderId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Failed to sync reminder to Firebase (offline mode): " + reminder.getReminderId());
                });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing reminder to Firebase", e);
        }
    }

    /**
     * Load reminders synchronously for parallel execution.
     * This method blocks until reminders are loaded from Room database.
     * Should be called from background thread via CompletableFuture.
     *
     * Requirements: 6.1
     * - 6.1: Fetch reminders in parallel with other dashboard data
     *
     * @param userId User ID to load reminders for
     * @return List of reminders from local database
     */
    public List<Reminder> loadRemindersSync(String userId) {
        if (userId == null) {
            return new java.util.ArrayList<>();
        }

        try {
            // Load from Room database synchronously
            List<Reminder> reminders = reminderDao.getAllRemindersSync(userId);
            Log.d(TAG, "✅ Loaded " + reminders.size() + " reminders synchronously for parallel execution");
            return reminders;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading reminders synchronously", e);
            return new java.util.ArrayList<>();
        }
    }
}
