package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.cache.CacheManager;
import com.example.healthylifehub.data.model.User;
import com.example.healthylifehub.data.model.UserProfile;
import com.example.healthylifehub.data.sync.SyncManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Repository for User Profile data from Firebase Firestore.
 * Collection structure: users/{userId}
 * Matches the provided JSON structure with profile nested object
 * 
 * Architecture: Offline-First with Room + RxJava + Network Awareness
 * - All reads from Room (instant, works offline)
 * - All writes to Room first, then sync to Firestore
 * - Uses CacheManager for centralized cache logic
 * - Uses SyncManager for background sync
 * - Network-aware operations with proper error handling
 */
public class UserRepository extends FirebaseRepository {
    
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final CacheManager cacheManager;
    private final SyncManager syncManager;
    
    /**
     * Constructor with dependency injection
     */
    public UserRepository(Context context) {
        this.cacheManager = CacheManager.getInstance(context);
        this.syncManager = SyncManager.getInstance(context);
    }
    
    /**
     * Load user profile data with Firestore listener on background thread
     * Requirements 3.2, 3.3: Firestore listener uses ExecutorService for background processing
     * @return LiveData of user profile map
     */
    public LiveData<Map<String, Object>> loadUserProfile() {
        MutableLiveData<Map<String, Object>> profileLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            profileLiveData.setValue(null);
            return profileLiveData;
        }
        
        // Pass executor to addSnapshotListener for background processing
        db.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener(executorService, (snapshot, error) -> {
                if (error != null) {
                    // Post to main thread
                    profileLiveData.postValue(null);
                    return;
                }
                
                if (snapshot != null && snapshot.exists()) {
                    // Process snapshot on background thread, post to main thread
                    profileLiveData.postValue(snapshot.getData());
                } else {
                    // Document doesn't exist, create it with default structure
                    createDefaultUserProfile(userId);
                }
            });
        
        return profileLiveData;
    }
    
    /**
     * Create default user profile structure if it doesn't exist
     * This handles cases where user was created before profile structure was implemented
     */
    private void createDefaultUserProfile(String userId) {
        if (auth.getCurrentUser() == null) {
            return;
        }
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("email", auth.getCurrentUser().getEmail());
        userData.put("displayName", auth.getCurrentUser().getDisplayName());
        userData.put("photoURL", auth.getCurrentUser().getPhotoUrl() != null ? 
                auth.getCurrentUser().getPhotoUrl().toString() : null);
        userData.put("role", "user");
        userData.put("createdAt", Timestamp.now());
        userData.put("updatedAt", Timestamp.now());
        
        // Create empty profile nested object
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", auth.getCurrentUser().getDisplayName() != null ? 
                auth.getCurrentUser().getDisplayName() : "");
        profile.put("dateOfBirth", null);
        profile.put("gender", "");
        profile.put("height", 0);
        profile.put("weight", 0);
        profile.put("bloodType", "");
        profile.put("medicalHistory", "");
        
        userData.put("profile", profile);
        
        db.collection(COLLECTION_USERS)
            .document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> 
                android.util.Log.d("UserRepository", "Default profile created for user: " + userId))
            .addOnFailureListener(e -> 
                android.util.Log.e("UserRepository", "Error creating default profile", e));
    }
    
    /**
     * Get specific profile field with Firestore listener on background thread
     * Requirements 3.2, 3.3: Firestore listener uses ExecutorService
     * @param fieldPath Path to field (e.g., "profile.fullName")
     * @return LiveData of field value
     */
    public LiveData<Object> getProfileField(String fieldPath) {
        MutableLiveData<Object> fieldLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            fieldLiveData.setValue(null);
            return fieldLiveData;
        }
        
        // Pass executor to addSnapshotListener for background processing
        db.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener(executorService, (snapshot, error) -> {
                if (error != null || snapshot == null || !snapshot.exists()) {
                    fieldLiveData.postValue(null);
                    return;
                }
                
                // Process on background thread, post to main thread
                fieldLiveData.postValue(snapshot.get(fieldPath));
            });
        
        return fieldLiveData;
    }
    
    /**
     * Update user profile in Firestore
     * Following the structure from Project_Summary.md:
     * users/{userId} with nested profile object
     * 
     * @param userProfile UserProfile object with updated data
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateUserProfile(UserProfile userProfile) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) {
                return false;
            }
            
            try {
                // Create profile nested object according to Firestore structure
                Map<String, Object> profileData = new HashMap<>();
                
                if (userProfile.getFullName() != null) {
                    profileData.put("fullName", userProfile.getFullName());
                }
                if (userProfile.getDateOfBirth() != null) {
                    // Convert date string to Timestamp
                    Timestamp birthTimestamp = convertDateStringToTimestamp(userProfile.getDateOfBirth());
                    if (birthTimestamp != null) {
                        profileData.put("dateOfBirth", birthTimestamp);
                    }
                }
                if (userProfile.getGender() != null) {
                    // Convert to lowercase English for Firestore
                    String gender = convertGenderToEnglish(userProfile.getGender());
                    profileData.put("gender", gender);
                }
                if (userProfile.getHeight() > 0) {
                    profileData.put("height", userProfile.getHeight());
                }
                if (userProfile.getWeight() > 0) {
                    profileData.put("weight", userProfile.getWeight());
                }
                if (userProfile.getBloodType() != null) {
                    profileData.put("bloodType", userProfile.getBloodType());
                }
                if (userProfile.getMedicalHistory() != null) {
                    profileData.put("medicalHistory", userProfile.getMedicalHistory());
                }
                
                // Update the nested profile object and updatedAt timestamp
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("profile", profileData);
                updateData.put("updatedAt", Timestamp.now());
                
                // Also update displayName at root level if fullName is provided
                if (userProfile.getFullName() != null) {
                    updateData.put("displayName", userProfile.getFullName());
                }
                
                // Perform Firestore update with merge option
                Task<Void> task = db.collection(COLLECTION_USERS)
                        .document(userId)
                        .set(updateData, SetOptions.merge());
                
                // Wait for completion (blocking on background thread)
                while (!task.isComplete()) {
                    Thread.sleep(100);
                }
                
                return task.isSuccessful();
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
    }
    
    /**
     * Update user profile with email and photo URL
     * Also updates Firebase Auth profile
     * 
     * @param fullName User's full name
     * @param email User's email
     * @param photoUrl User's photo URL
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateUserProfileWithAuth(String fullName, String email, String photoUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null || auth.getCurrentUser() == null) {
                return false;
            }
            
            try {
                // Update Firestore
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("displayName", fullName);
                updateData.put("email", email);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    updateData.put("photoURL", photoUrl);
                }
                updateData.put("updatedAt", Timestamp.now());
                
                Task<Void> firestoreTask = db.collection(COLLECTION_USERS)
                        .document(userId)
                        .set(updateData, SetOptions.merge());
                
                // Wait for Firestore update
                while (!firestoreTask.isComplete()) {
                    Thread.sleep(100);
                }
                
                if (!firestoreTask.isSuccessful()) {
                    return false;
                }
                
                // Update Firebase Auth profile
                com.google.firebase.auth.UserProfileChangeRequest.Builder profileUpdates = 
                    new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName);
                
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    profileUpdates.setPhotoUri(android.net.Uri.parse(photoUrl));
                }
                
                Task<Void> authTask = auth.getCurrentUser()
                        .updateProfile(profileUpdates.build());
                
                // Wait for Auth update
                while (!authTask.isComplete()) {
                    Thread.sleep(100);
                }
                
                return authTask.isSuccessful();
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
    }
    
    /**
     * Convert date string (dd/MM/yyyy) to Firestore Timestamp
     */
    private Timestamp convertDateStringToTimestamp(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateString);
            return date != null ? new Timestamp(date) : null;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convert Vietnamese gender to English lowercase
     */
    private String convertGenderToEnglish(String gender) {
        if (gender == null) return "other";
        
        String lowerGender = gender.toLowerCase();
        if (lowerGender.contains("nam") || lowerGender.equals("male")) {
            return "male";
        } else if (lowerGender.contains("nữ") || lowerGender.contains("nu") || lowerGender.equals("female")) {
            return "female";
        } else {
            return "other";
        }
    }
    
    /**
     * Extract numeric value from string (e.g., "175 cm" -> 175.0)
     */
    private double extractNumericValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0.0;
        }
        
        try {
            // Remove all non-numeric characters except decimal point
            String numericValue = value.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numericValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0.0;
        }
    }
    
    // ==================== MEDICAL HISTORY METHODS ====================
    
    /**
     * Load medical history for current user (as simple String)
     * Requirements 3.2, 3.3: Firestore listener uses ExecutorService
     * @return LiveData of medical history string
     */
    public LiveData<String> loadMedicalHistory() {
        MutableLiveData<String> historyLiveData = new MutableLiveData<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            historyLiveData.setValue("");
            return historyLiveData;
        }
        
        // Pass executor to addSnapshotListener for background processing
        db.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener(executorService, (snapshot, error) -> {
                if (error != null || snapshot == null || !snapshot.exists()) {
                    historyLiveData.postValue("");
                    return;
                }
                
                // Get from nested profile.medicalHistory - process on background thread
                Map<String, Object> profileData = (Map<String, Object>) snapshot.get("profile");
                if (profileData != null && profileData.containsKey("medicalHistory")) {
                    Object history = profileData.get("medicalHistory");
                    if (history instanceof String) {
                        historyLiveData.postValue((String) history);
                    } else {
                        historyLiveData.postValue("");
                    }
                } else {
                    historyLiveData.postValue("");
                }
            });
        
        return historyLiveData;
    }
    
    /**
     * Update medical history for current user (as simple String)
     * @param medicalHistory Medical history text
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateMedicalHistory(String medicalHistory) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = getCurrentUserId();
            if (userId == null) {
                return false;
            }
            
            try {
                // Get current profile data first
                Task<DocumentSnapshot> getTask = db.collection(COLLECTION_USERS)
                        .document(userId)
                        .get();
                
                while (!getTask.isComplete()) {
                    Thread.sleep(50);
                }
                
                if (!getTask.isSuccessful()) {
                    return false;
                }
                
                DocumentSnapshot snapshot = getTask.getResult();
                Map<String, Object> profileData = (Map<String, Object>) snapshot.get("profile");
                if (profileData == null) {
                    profileData = new HashMap<>();
                }
                
                // Update medicalHistory in profile
                profileData.put("medicalHistory", medicalHistory != null ? medicalHistory : "");
                
                // Update entire profile object
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("profile", profileData);
                updateData.put("updatedAt", Timestamp.now());
                
                // Perform Firestore update
                Task<Void> task = db.collection(COLLECTION_USERS)
                        .document(userId)
                        .set(updateData, SetOptions.merge());
                
                // Wait for completion
                while (!task.isComplete()) {
                    Thread.sleep(100);
                }
                
                return task.isSuccessful();
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
    }
    
    // ==================== RxJava METHODS (Offline-First) ====================
    
    /**
     * Get current user (reactive - from cache)
     */
    public Flowable<User> getCurrentUserRx() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Flowable.empty();
        }
        
        return cacheManager.getUser(userId)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(user -> Log.d(TAG, "📱 Loaded user: " + user.getUid()));
    }
    
    /**
     * Cache current user
     */
    public Completable cacheCurrentUser(User user) {
        return cacheManager.cacheUser(user)
            .doOnComplete(() -> {
                Log.d(TAG, "✅ Cached user: " + user.getUid());
                // Trigger background sync if network available
                if (syncManager.isNetworkAvailable()) {
                    syncManager.syncAll()
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            () -> Log.d(TAG, "✅ User synced to Firestore"),
                            error -> Log.w(TAG, "⚠️ User sync failed", error)
                        );
                }
            });
    }
    
    /**
     * Update user profile (offline-first)
     */
    public Completable updateUserProfileRx(UserProfile userProfile) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Completable.error(new IllegalStateException("User not logged in"));
        }
        
        return Completable.fromRunnable(() -> {
            // Create User object from profile
            User user = new User();
            user.setUid(userId);
            user.setDisplayName(userProfile.getFullName());
            user.setNeedsSync(true);
            
            // Cache to Room
            cacheManager.cacheUser(user)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    () -> Log.d(TAG, "✅ Updated user profile in cache"),
                    error -> Log.e(TAG, "❌ Error updating user profile", error)
                );
        })
        .subscribeOn(Schedulers.io())
        .doOnComplete(() -> {
            Log.d(TAG, "✅ Updated user profile");
            // Trigger background sync
            if (syncManager.isNetworkAvailable()) {
                syncManager.syncAll()
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        () -> Log.d(TAG, "✅ Profile update synced"),
                        error -> Log.w(TAG, "⚠️ Profile update sync failed", error)
                    );
            }
        });
    }
    
    /**
     * Sync all pending user changes
     */
    public Completable syncPendingChanges() {
        return syncManager.syncAll()
            .doOnComplete(() -> Log.d(TAG, "✅ Synced pending user changes"));
    }
    
    /**
     * Get sync events (for UI updates)
     */
    public io.reactivex.rxjava3.core.Observable<SyncManager.SyncEvent> getSyncEvents() {
        return syncManager.getSyncEvents();
    }
}

