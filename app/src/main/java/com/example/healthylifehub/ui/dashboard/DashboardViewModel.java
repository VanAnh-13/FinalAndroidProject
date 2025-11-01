package com.example.healthylifehub.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends BaseViewModel {

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<String> greeting = new MutableLiveData<>();
    private final MutableLiveData<List<Reminder>> reminders = new MutableLiveData<>();
    private final MutableLiveData<Integer> notificationCount = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        userName.setValue("Nguyễn Văn A");
        notificationCount.setValue(3);
        updateGreeting();
        loadReminders();
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
        List<Reminder> reminderList = new ArrayList<>();
        reminderList.add(new Reminder("Uống thuốc huyết áp", "08:00 AM"));
        reminderList.add(new Reminder("Đo đường huyết", "02:00 PM"));
        reminders.setValue(reminderList);
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
