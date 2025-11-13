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
import com.example.healthylifehub.utils.NotificationHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
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
    
    private void checkSettingsAndProcessSuggestions(String userId, List<SmartSuggestion> suggestions) {
        // Get notification settings synchronously (we're already in background thread)
        try {
            // Note: In a real implementation, you might want to cache settings or use a different approach
            // For now, we'll assume suggestions are allowed and just save them
            for (SmartSuggestion suggestion : suggestions) {
                processSingleSuggestion(userId, suggestion);
            }
            
            // Send notification about new suggestions if settings allow
            sendSuggestionNotificationIfAllowed(suggestions.size());
            
            Log.d(TAG, "Processed " + suggestions.size() + " suggestions successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing suggestions", e);
        }
    }
    
    private void processSingleSuggestion(String userId, SmartSuggestion suggestion) {
        try {
            Log.d(TAG, "Processing suggestion: " + suggestion.getTitle());
            
            String suggestionId = db.collection("users")
                .document(userId)
                .collection("suggestions")
                .document()
                .getId();
            
            suggestion.setSuggestionId(suggestionId);
            suggestion.setUserId(userId);
            suggestion.setCreatedAt(System.currentTimeMillis());
            suggestion.setStatus("pending");
            suggestion.setAppliedAt(0);
            suggestion.setDismissedAt(0);
            
            Tasks.await(
                db.collection("users")
                    .document(userId)
                    .collection("suggestions")
                    .document(suggestionId)
                    .set(suggestion)
            );
            
            Log.d(TAG, "Saved suggestion: " + suggestionId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving suggestion: " + suggestion.getTitle(), e);
        }
    }
    
    private void sendSuggestionNotificationIfAllowed(int count) {
        // For now, we'll send a simple notification
        // In a production app, you'd check the NotificationSettings here
        if (count > 0) {
            String title = "Gợi ý mới từ HealthyLife Hub";
            String message = String.format("Bạn có %d gợi ý cải thiện sức khỏe mới", count);
            int notificationId = (int) System.currentTimeMillis();
            
            NotificationHelper.showSuggestionNotification(
                getApplicationContext(),
                title,
                message,
                notificationId
            );
            
            Log.d(TAG, "✅ Sent suggestion notification");
        }
    }
    
    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "SmartReminderWorker stopped");
        executor.shutdownNow();
    }
}
