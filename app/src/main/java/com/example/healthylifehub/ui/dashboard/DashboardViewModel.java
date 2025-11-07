package com.example.healthylifehub.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.repository.NotificationsRepository;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class DashboardViewModel extends BaseViewModel {

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<String> greeting = new MutableLiveData<>();
    private final MediatorLiveData<List<Reminder>> reminders = new MediatorLiveData<>();
    private final MediatorLiveData<Integer> notificationCount = new MediatorLiveData<>();
    
    private final RemindersRepository remindersRepository;
    private final NotificationsRepository notificationsRepository;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        
        remindersRepository = new RemindersRepository();
        notificationsRepository = new NotificationsRepository();
        
        loadUserName();
        updateGreeting();
        loadReminders();
        loadNotificationCount();
    }
    
    private void loadUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            
            if (displayName != null && !displayName.isEmpty()) {
                userName.setValue(displayName);
            } else {
                // Load from Firestore if displayName is null
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("displayName");
                                userName.setValue(name != null ? name : "User");
                            } else {
                                userName.setValue("User");
                            }
                        })
                        .addOnFailureListener(e -> userName.setValue("User"));
            }
        } else {
            userName.setValue("User");
        }
    }

    private void updateGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            greeting.setValue("Chào buổi sáng,");
        } else if (hour < 18) {
            greeting.setValue("Chào buổi chiều,");
        } else {
            greeting.setValue("Chào buổi tối,");
        }
    }

    private void loadReminders() {
        LiveData<List<Reminder>> source = remindersRepository.loadReminders();
        reminders.addSource(source, reminders::setValue);
    }
    
    private void loadNotificationCount() {
        LiveData<Integer> source = notificationsRepository.getUnreadCount();
        notificationCount.addSource(source, notificationCount::setValue);
    }

    public void snoozeReminder(Reminder reminder) {
    }

    public void completeReminder(Reminder reminder) {
        List<Reminder> currentReminders = reminders.getValue();
        if (currentReminders != null) {
            currentReminders.remove(reminder);
            reminders.setValue(currentReminders);
        }
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<String> getGreeting() {
        return greeting;
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    public LiveData<Integer> getNotificationCount() {
        return notificationCount;
    }
}
