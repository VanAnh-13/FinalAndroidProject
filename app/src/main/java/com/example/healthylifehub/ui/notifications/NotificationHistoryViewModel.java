package com.example.healthylifehub.ui.notifications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.model.NotificationHistory;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationHistoryViewModel extends AndroidViewModel {
    
    private final AppDatabase database;
    private final ExecutorService executor;
    private final MutableLiveData<Boolean> showUnreadOnly = new MutableLiveData<>(false);
    private LiveData<List<NotificationHistory>> notifications;
    
    public NotificationHistoryViewModel(@NonNull Application application) {
        super(application);
        this.database = AppDatabase.getInstance(application);
        this.executor = Executors.newSingleThreadExecutor();
        
        String userId = getCurrentUserId();
        if (userId != null) {
            notifications = Transformations.switchMap(showUnreadOnly, unreadOnly -> {
                if (unreadOnly) {
                    return database.notificationHistoryDao().getUnreadNotifications(userId);
                } else {
                    return database.notificationHistoryDao().getAllNotifications(userId);
                }
            });
        }
    }
    
    public LiveData<List<NotificationHistory>> getNotifications() {
        return notifications;
    }
    
    public void loadAllNotifications() {
        showUnreadOnly.setValue(false);
    }
    
    public void loadUnreadNotifications() {
        showUnreadOnly.setValue(true);
    }
    
    public void markAsRead(String notificationId) {
        executor.execute(() -> database.notificationHistoryDao().markAsRead(notificationId));
    }
    
    public void markAllAsRead() {
        String userId = getCurrentUserId();
        if (userId != null) {
            executor.execute(() -> database.notificationHistoryDao().markAllAsRead(userId));
        }
    }
    
    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
