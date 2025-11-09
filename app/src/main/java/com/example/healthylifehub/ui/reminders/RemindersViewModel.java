package com.example.healthylifehub.ui.reminders;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.repository.RemindersRepository;
import java.util.List;

public class RemindersViewModel extends BaseViewModel {

    private final MediatorLiveData<List<Reminder>> reminders = new MediatorLiveData<>();
    private final RemindersRepository remindersRepository;

    public RemindersViewModel(@NonNull Application application) {
        super(application);
        remindersRepository = new RemindersRepository();
        loadReminders();
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    private void loadReminders() {
        LiveData<List<Reminder>> source = remindersRepository.loadReminders();
        reminders.addSource(source, reminders::setValue);
    }

    public void toggleReminderComplete(int position) {
        List<Reminder> currentList = reminders.getValue();
        if (currentList != null && position < currentList.size()) {
            Reminder reminder = currentList.get(position);
            reminder.setActive(!reminder.isActive());
            reminders.setValue(currentList);
        }
    }
}
