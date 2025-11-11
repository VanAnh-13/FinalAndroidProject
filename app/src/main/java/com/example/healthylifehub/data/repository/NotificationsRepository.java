package com.example.healthylifehub.data.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.NotificationItem;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for Notifications data from Firebase Firestore.
 * Collection structure: users/{userId}/notifications/{notificationId}
 */
public class NotificationsRepository extends FirebaseRepository {
    
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    
    /**
     * Load notifications for current user
     * @return LiveData list of notifications
     */
    public LiveData<List<NotificationItem>> loadNotifications() {
        MutableLiveData<List<NotificationItem>> notificationsLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            notificationsLiveData.setValue(new ArrayList<>());
            return notificationsLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    notificationsLiveData.setValue(new ArrayList<>());
                    return;
                }
                
                if (value != null) {
                    List<NotificationItem> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        String id = doc.getId(); // Get document ID
                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        String time = doc.getString("time");
                        String type = doc.getString("type");
                        Boolean isRead = doc.getBoolean("isRead");
                        
                        if (title != null && message != null && time != null && type != null) {
                            NotificationItem.NotificationType notificationType;
                            try {
                                notificationType = NotificationItem.NotificationType.valueOf(type.toUpperCase());
                            } catch (IllegalArgumentException ex) {
                                notificationType = NotificationItem.NotificationType.GENERAL;
                            }

                            NotificationItem item = new NotificationItem(
                                title,
                                message,
                                time,
                                isRead != null ? isRead : false,
                                notificationType
                            );
                            item.setId(id); // Set document ID
                            notifications.add(item);
                        }
                    }
                    notificationsLiveData.setValue(notifications);
                }
            });
        
        return notificationsLiveData;
    }
    
    /**
     * Get unread notification count
     * @return LiveData integer count
     */
    public LiveData<Integer> getUnreadCount() {
        MutableLiveData<Integer> countLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            countLiveData.setValue(0);
            return countLiveData;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("isRead", false)
            .addSnapshotListener((value, error) -> {
                if (error != null || value == null) {
                    countLiveData.setValue(0);
                    return;
                }
                countLiveData.setValue(value.size());
            });
        
        return countLiveData;
    }
    
    /**
     * Delete notification by ID
     * @param notificationId ID of notification to delete
     * @return CompletableFuture<Boolean> success status
     */
    public CompletableFuture<Boolean> deleteNotification(String notificationId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            future.complete(false);
            return future;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d("NotificationsRepository", "✅ Deleted notification: " + notificationId);
                future.complete(true);
            })
            .addOnFailureListener(e -> {
                Log.e("NotificationsRepository", "❌ Failed to delete notification", e);
                future.complete(false);
            });
        
        return future;
    }
    
    /**
     * Mark notification as read
     * @param notificationId ID of notification
     * @return CompletableFuture<Boolean> success status
     */
    public CompletableFuture<Boolean> markAsRead(String notificationId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            future.complete(false);
            return future;
        }
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener(aVoid -> {
                Log.d("NotificationsRepository", "✅ Marked as read: " + notificationId);
                future.complete(true);
            })
            .addOnFailureListener(e -> {
                Log.e("NotificationsRepository", "❌ Failed to mark as read", e);
                future.complete(false);
            });
        
        return future;
    }
}

