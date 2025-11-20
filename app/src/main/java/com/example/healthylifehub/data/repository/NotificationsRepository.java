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
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        
        com.google.firebase.firestore.Query query = db.collection("users")
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING);

        return new com.example.healthylifehub.data.livedata.FirestoreQueryLiveData<List<NotificationItem>>(query) {
            @Override
            protected List<NotificationItem> parseSnapshot(com.google.firebase.firestore.QuerySnapshot snapshot) {
                List<NotificationItem> notifications = new ArrayList<>();
                if (snapshot != null) {
                    for (QueryDocumentSnapshot doc : snapshot) {
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
                }
                return notifications;
            }
        };
    }
    
    /**
     * Get unread notification count synchronously for parallel execution.
     * This method blocks until count is retrieved from Firestore.
     * Should be called from background thread via CompletableFuture.
     * 
     * Requirements: 6.1
     * - 6.1: Fetch notification count in parallel with other dashboard data
     * 
     * @param userId User ID to get count for
     * @return Integer count of unread notifications
     */
    public Integer getUnreadCountSync(String userId) {
        if (userId == null) {
            return 0;
        }
        
        try {
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = 
                db.collection("users")
                    .document(userId)
                    .collection(COLLECTION_NOTIFICATIONS)
                    .whereEqualTo("isRead", false)
                    .get();
            
            // Wait for task to complete (with timeout)
            long startTime = System.currentTimeMillis();
            long timeout = 3000; // 3 seconds
            while (!task.isComplete() && System.currentTimeMillis() - startTime < timeout) {
                Thread.sleep(50);
            }
            
            if (task.isSuccessful() && task.getResult() != null) {
                int count = task.getResult().size();
                Log.d("NotificationsRepository", "✅ Loaded unread count synchronously: " + count);
                return count;
            }
        } catch (Exception e) {
            Log.e("NotificationsRepository", "❌ Error loading unread count synchronously", e);
        }
        
        return 0;
    }
    
    /**
     * Get unread notification count
     * @return LiveData integer count
     */
    public LiveData<Integer> getUnreadCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new MutableLiveData<>(0);
        }
        
        com.google.firebase.firestore.Query query = db.collection("users")
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("isRead", false);

        return new com.example.healthylifehub.data.livedata.FirestoreQueryLiveData<Integer>(query) {
            @Override
            protected Integer parseSnapshot(com.google.firebase.firestore.QuerySnapshot snapshot) {
                if (snapshot == null) {
                    return 0;
                }
                return snapshot.size();
            }
        };
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

