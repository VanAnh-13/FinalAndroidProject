package com.example.healthylifehub.ui.shared;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.UserProfile;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized shared ViewModel for all app-wide data synchronization.
 * Single source of truth for reminders, user profile, and other shared data.
 * Replaces multiple SharedXxxViewModel classes with one unified approach.
 */
public class AppSharedViewModel extends BaseViewModel {

    // Reminders data
    private final MediatorLiveData<List<Reminder>> reminders = new MediatorLiveData<>();
    
    // User data
    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    
    // Repositories
    private final RemindersRepository remindersRepository;
    private final UserRepository userRepository;
    private final FirebaseFirestore db;

    public AppSharedViewModel(@NonNull Application application) {
        super(application);
        remindersRepository = new RemindersRepository();
        userRepository = new UserRepository(application);
        db = FirebaseFirestore.getInstance();
        
        // Initialize all shared data
        loadReminders();
        loadUserData();
    }

    // ==================== REMINDERS ====================

    private void loadReminders() {
        LiveData<List<Reminder>> source = remindersRepository.loadReminders();
        reminders.addSource(source, reminders::setValue);
    }

    public void completeReminder(Reminder reminder) {
        reminder.setActive(false);
        remindersRepository.updateReminder(reminder)
            .thenAccept(success -> {
                if (success) {
                    updateRemindersList();
                }
            });
    }

    public void deleteReminder(Reminder reminder) {
        remindersRepository.deleteReminder(reminder.getReminderId())
            .thenAccept(success -> {
                if (success) {
                    updateRemindersList();
                }
            });
    }

    public void snoozeReminder(Reminder reminder) {
        long newTime = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes
        reminder.setReminderTime(newTime);
        remindersRepository.updateReminder(reminder)
            .thenAccept(success -> {
                if (success) {
                    updateRemindersList();
                }
            });
    }

    private void updateRemindersList() {
        List<Reminder> current = reminders.getValue();
        if (current != null) {
            reminders.setValue(new ArrayList<>(current));
        }
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    // ==================== USER DATA ====================

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Real-time listener for user data
            db.collection("users")
                .document(user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        userName.setValue("User");
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        String displayName = snapshot.getString("displayName");
                        userName.setValue(displayName != null ? displayName : "User");
                        
                        try {
                            UserProfile profile = snapshot.toObject(UserProfile.class);
                            if (profile != null) {
                                userProfile.setValue(profile);
                            }
                        } catch (Exception e) {
                            // Profile parsing failed
                        }
                    } else {
                        userName.setValue("User");
                    }
                });
        } else {
            userName.setValue("User");
        }
    }

    public void updateUserName(String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users")
                .document(user.getUid())
                .update("displayName", newName)
                .addOnSuccessListener(aVoid -> {
                    userName.setValue(newName);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AppSharedViewModel", "❌ Failed to update name", e);
                });
        }
    }

    public void updateUserProfile(UserProfile profile) {
        userRepository.updateUserProfile(profile)
            .thenAccept(success -> {
                if (success) {
                    userProfile.setValue(profile);
                    userName.setValue(profile.getFullName());
                }
            });
    }

    public void reloadReminders() {
        reminders.removeSource(reminders);
        loadReminders();
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }
}
