package com.example.healthylifehub.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.SmartSuggestion;
import com.example.healthylifehub.data.model.UserBehavior;
import com.example.healthylifehub.data.model.NotificationSettings;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.data.repository.NotificationSettingsRepository;
import com.example.healthylifehub.services.SmartReminderAI;
import com.example.healthylifehub.utils.notification.NotificationHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background Worker for Smart Reminder Analysis
 * Runs periodically to analyze user behavior and generate suggestions
 * Based on UC-HLH-05 from Project_Sumary.md
 */
public class SmartReminderWorker extends Worker {
    
    private static final String TAG = "SmartReminderWorker";
    private final RemindersRepository repository;
    private final NotificationSettingsRepository settingsRepository;
    private final ExecutorService executor;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    public SmartReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.repository = new RemindersRepository();
        this.settingsRepository = new NotificationSettingsRepository(context);
        this.executor = Executors.newFixedThreadPool(3);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SmartReminderWorker started");
        
        try {
            // Parallel Task 1: Fetch active reminders
            CompletableFuture<List<Reminder>> fetchTask = CompletableFuture.supplyAsync(
                this::fetchActiveReminders, 
                executor
            );
            
            // Parallel Task 2: Fetch all reminders for behavior analysis
            CompletableFuture<List<Reminder>> allRemindersTask = CompletableFuture.supplyAsync(
                this::fetchAllReminders,
                executor
            );
            
            // Wait for both tasks to complete
            CompletableFuture.allOf(fetchTask, allRemindersTask).join();
            
            List<Reminder> activeReminders = fetchTask.get();
            List<Reminder> allReminders = allRemindersTask.get();
            
            Log.d(TAG, "Fetched " + activeReminders.size() + " active reminders");
            Log.d(TAG, "Fetched " + allReminders.size() + " total reminders");
            
            // Task 3: Analyze user behavior
            UserBehavior behavior = SmartReminderAI.analyzeUserBehavior(allReminders);
            Log.d(TAG, "Behavior analyzed - Completion rate: " + behavior.getCompletionRate());
            
            // Task 4: Generate smart suggestions
            List<SmartSuggestion> suggestions = SmartReminderAI.generateSmartSuggestions(
                activeReminders, 
                behavior
            );
            
            Log.d(TAG, "Generated " + suggestions.size() + " suggestions");
            
            // Task 5: Process suggestions (send notifications, save to DB, etc.)
            processSuggestions(suggestions);
            
            Log.d(TAG, "SmartReminderWorker completed successfully");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "SmartReminderWorker failed", e);
            return Result.retry();
        } finally {
            executor.shutdown();
        }
    }
    
    private List<Reminder> fetchActiveReminders() {
        try {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user");
                return new ArrayList<>();
            }
            
            String userId = currentUser.getUid();
            Log.d(TAG, "Fetching active reminders for user: " + userId);
            
            QuerySnapshot snapshot = Tasks.await(
                db.collection("users")
                    .document(userId)
                    .collection("reminders")
                    .whereEqualTo("isActive", true)
                    .get()
            );
            
            List<Reminder> reminders = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                Reminder reminder = parseReminderFromFirestore(doc);
                if (reminder != null) {
                    reminders.add(reminder);
                }
            }
            
            Log.d(TAG, "Fetched " + reminders.size() + " active reminders");
            return reminders;
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching active reminders", e);
            return new ArrayList<>();
        }
    }
    
    private List<Reminder> fetchAllReminders() {
        try {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user");
                return new ArrayList<>();
            }
            
            String userId = currentUser.getUid();
            Log.d(TAG, "Fetching all reminders for user: " + userId);
            
            QuerySnapshot snapshot = Tasks.await(
                db.collection("users")
                    .document(userId)
                    .collection("reminders")
                    .get()
            );
            
            List<Reminder> reminders = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                Reminder reminder = parseReminderFromFirestore(doc);
                if (reminder != null) {
                    reminders.add(reminder);
                }
            }
            
            Log.d(TAG, "Fetched " + reminders.size() + " total reminders");
            return reminders;
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all reminders", e);
            return new ArrayList<>();
        }
    }
    
    private Reminder parseReminderFromFirestore(QueryDocumentSnapshot doc) {
        try {
            Reminder reminder = new Reminder();
            reminder.setReminderId(doc.getId());
            reminder.setUserId(doc.getString("userId"));
            reminder.setTitle(doc.getString("title"));
            reminder.setDescription(doc.getString("description"));
            
            Object timeObj = doc.get("reminderTime");
            if (timeObj instanceof com.google.firebase.Timestamp) {
                reminder.setReminderTime(((com.google.firebase.Timestamp) timeObj).toDate().getTime());
            } else if (timeObj instanceof Long) {
                reminder.setReminderTime((Long) timeObj);
            }
            
            reminder.setFrequency(doc.getString("frequency"));
            reminder.setActive(Boolean.TRUE.equals(doc.getBoolean("isActive")));
            reminder.setMedicineId(doc.getString("medicineId"));
            
            Object createdObj = doc.get("createdAt");
            if (createdObj instanceof com.google.firebase.Timestamp) {
                reminder.setCreatedAt(((com.google.firebase.Timestamp) createdObj).toDate().getTime());
            } else if (createdObj instanceof Long) {
                reminder.setCreatedAt((Long) createdObj);
            }
            
            Object updatedObj = doc.get("updatedAt");
            if (updatedObj instanceof com.google.firebase.Timestamp) {
                reminder.setUpdatedAt(((com.google.firebase.Timestamp) updatedObj).toDate().getTime());
            } else if (updatedObj instanceof Long) {
                reminder.setUpdatedAt((Long) updatedObj);
            }
            
            return reminder;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing reminder", e);
            return null;
        }
    }
    
    private void processSuggestions(List<SmartSuggestion> suggestions) {
        if (suggestions.isEmpty()) {
            Log.d(TAG, "No suggestions to process");
            return;
        }
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user, cannot save suggestions");
            return;
        }
        
        String userId = currentUser.getUid();
        
        // Check notification settings before sending notifications
        checkSettingsAndProcessSuggestions(userId, suggestions);
    }
    
    /**
     * Check notification settings and process suggestions (Requirement 5.5)
     * Only sends notifications if settings allow
     */
    private void checkSettingsAndProcessSuggestions(String userId, List<SmartSuggestion> suggestions) {
        try {
            // Save all suggestions to Firestore
            for (SmartSuggestion suggestion : suggestions) {
                processSingleSuggestion(userId, suggestion);
            }
            
            // Check notification settings before sending notification (Requirement 5.5)
            NotificationSettings settings = settingsRepository.loadNotificationSettings().getValue();
            
            // Send notification if settings allow (Requirement 5.5)
            if (settings != null && settings.areSuggestionsAllowed()) {
                sendSuggestionNotification(suggestions.size());
                Log.d(TAG, "✅ Sent notification for " + suggestions.size() + " suggestions");
            } else {
                Log.d(TAG, "⚠️ Suggestions notifications disabled or quiet hours active");
            }
            
            Log.d(TAG, "Processed " + suggestions.size() + " suggestions successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing suggestions", e);
        }
    }
    
    /**
     * Process and save a single suggestion to Firestore (Requirement 5.4)
     * Creates document in users/{userId}/suggestions collection with all required fields
     */
    private void processSingleSuggestion(String userId, SmartSuggestion suggestion) {
        try {
            Log.d(TAG, "Processing suggestion: " + suggestion.getTitle());
            
            // Generate unique suggestion ID if not already set
            String suggestionId = suggestion.getSuggestionId();
            if (suggestionId == null || suggestionId.isEmpty()) {
                suggestionId = db.collection("users")
                    .document(userId)
                    .collection("suggestions")
                    .document()
                    .getId();
            }
            
            // Set required fields (Requirement 5.4)
            suggestion.setSuggestionId(suggestionId);
            suggestion.setUserId(userId);
            suggestion.setStatus("pending");
            suggestion.setCreatedAt(System.currentTimeMillis());
            suggestion.setAppliedAt(0);
            suggestion.setDismissedAt(0);
            
            // Create Firestore document with all fields
            Map<String, Object> suggestionData = new HashMap<>();
            suggestionData.put("suggestionId", suggestion.getSuggestionId());
            suggestionData.put("userId", suggestion.getUserId());
            suggestionData.put("type", suggestion.getType());
            suggestionData.put("title", suggestion.getTitle());
            suggestionData.put("description", suggestion.getDescription());
            suggestionData.put("reason", suggestion.getReason());
            suggestionData.put("status", suggestion.getStatus());
            suggestionData.put("createdAt", com.google.firebase.Timestamp.now());
            suggestionData.put("appliedAt", suggestion.getAppliedAt());
            suggestionData.put("dismissedAt", suggestion.getDismissedAt());
            suggestionData.put("confidenceScore", suggestion.getConfidenceScore());
            suggestionData.put("priority", suggestion.getPriority());
            
            // Add optional fields if present
            if (suggestion.getReminderId() != null) {
                suggestionData.put("reminderId", suggestion.getReminderId());
            }
            if (suggestion.getReminderIds() != null) {
                suggestionData.put("reminderIds", suggestion.getReminderIds());
            }
            if (suggestion.getSuggestedTime() != null) {
                suggestionData.put("suggestedTime", suggestion.getSuggestedTime());
            }
            if (suggestion.getSuggestedFrequency() != null) {
                suggestionData.put("suggestedFrequency", suggestion.getSuggestedFrequency());
            }
            if (suggestion.getSuggestedTitle() != null) {
                suggestionData.put("suggestedTitle", suggestion.getSuggestedTitle());
            }
            if (suggestion.getCurrentValue() != null) {
                suggestionData.put("currentValue", suggestion.getCurrentValue());
            }
            if (suggestion.getSuggestedValue() != null) {
                suggestionData.put("suggestedValue", suggestion.getSuggestedValue());
            }
            
            // Save to Firestore
            Tasks.await(
                db.collection("users")
                    .document(userId)
                    .collection("suggestions")
                    .document(suggestionId)
                    .set(suggestionData)
            );
            
            Log.d(TAG, "✅ Saved suggestion to Firestore: " + suggestionId + " (" + suggestion.getType() + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving suggestion: " + suggestion.getTitle(), e);
        }
    }
    
    /**
     * Send notification for new suggestions (Requirement 5.5)
     * Includes suggestion count in notification message
     */
    private void sendSuggestionNotification(int count) {
        if (count <= 0) {
            return;
        }
        
        String title = "Gợi ý thông minh mới";
        String message;
        
        // Format message based on count (Requirement 5.5)
        if (count == 1) {
            message = "Bạn có 1 gợi ý cải thiện sức khỏe mới";
        } else {
            message = String.format("Bạn có %d gợi ý cải thiện sức khỏe mới", count);
        }
        
        int notificationId = (int) System.currentTimeMillis();
        
        // Use NotificationHelper.showSuggestionNotification() (Requirement 5.5)
        NotificationHelper.showSuggestionNotification(
            getApplicationContext(),
            title,
            message,
            notificationId
        );
        
        Log.d(TAG, "✅ Sent suggestion notification with count: " + count);
    }
    
    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "SmartReminderWorker stopped");
        executor.shutdownNow();
    }
}
