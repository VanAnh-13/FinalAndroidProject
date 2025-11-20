package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.healthylifehub.data.cache.CacheManager;
import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.MedicalRecordDao;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Repository for Medical Records with OFFLINE-FIRST architecture
 * 
 * Data Flow:
 * 1. UI reads from Room (instant, works offline)
 * 2. Background sync from Firestore to Room
 * 3. UI automatically updates when Room data changes
 * 
 * Follows the structure defined in Project_Summary.md
 */
public class MedicalRecordsRepository extends FirebaseRepository {
    
    private static final String TAG = "MedicalRecordsRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_RECORDS = "medicalRecords";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MedicalRecordDao dao;
    private final CacheManager cacheManager;
    private final ExecutorService executorService;
    
    public MedicalRecordsRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dao = AppDatabase.getInstance(context).medicalRecordDao();
        this.cacheManager = CacheManager.getInstance(context);
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Create a new medical record (OFFLINE-FIRST)
     * 1. Save to Room immediately (works offline)
     * 2. Sync to Firestore in background
     * 
     * @param record MedicalRecord object to create
     * @return CompletableFuture<String> with record ID
     */
    public CompletableFuture<String> createRecord(MedicalRecord record) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            future.complete(null);
            return future;
        }
        
        // Generate ID if not exists
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(db.collection("temp").document().getId());
        }
        
        // Step 1: Save to Room (instant, works offline) + Cache
        executorService.execute(() -> {
            try {
                dao.insert(record);
                Log.d(TAG, "✅ Saved medical record to local database: " + record.getId());
                
                // Cache the record
                cacheManager.cacheMedicalRecord(record)
                    .subscribe(
                        () -> Log.d(TAG, "✅ Cached medical record"),
                        error -> Log.w(TAG, "⚠️ Cache failed", error)
                    );
                
                // Step 2: Sync to Firestore in background
                syncToFirestore(record)
                    .thenAccept(success -> {
                        if (success) {
                            Log.d(TAG, "✅ Synced medical record to Firestore");
                        }
                        future.complete(record.getId());
                    })
                    .exceptionally(throwable -> {
                        Log.w(TAG, "⚠️ Firestore sync failed (will retry later)", throwable);
                        future.complete(record.getId()); // Return success even if Firestore fails
                        return null;
                    });
                    
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving medical record to local database", e);
                future.complete(null);
            }
        });
        
        return future;
    }
    
    /**
     * Update an existing medical record
     */
    public CompletableFuture<Boolean> updateRecord(MedicalRecord record) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            future.complete(false);
            return future;
        }
        
        executorService.execute(() -> {
            try {
                dao.update(record);
                Log.d(TAG, "✅ Updated medical record in local database");
                
                syncToFirestore(record)
                    .thenAccept(success -> {
                        if (success) {
                            Log.d(TAG, "✅ Synced updated medical record to Firestore");
                        }
                        future.complete(true);
                    })
                    .exceptionally(throwable -> {
                        Log.w(TAG, "⚠️ Firestore sync failed", throwable);
                        future.complete(true);
                        return null;
                    });
                    
            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating medical record", e);
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Delete a medical record
     */
    public CompletableFuture<Boolean> deleteRecord(MedicalRecord record) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        executorService.execute(() -> {
            try {
                dao.delete(record);
                Log.d(TAG, "✅ Deleted medical record from local database");
                
                // Delete from Firestore
                String userId = getCurrentUserId();
                if (userId != null) {
                    db.collection(COLLECTION_USERS)
                        .document(userId)
                        .collection(SUBCOLLECTION_RECORDS)
                        .document(record.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Deleted medical record from Firestore");
                            future.complete(true);
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "⚠️ Failed to delete from Firestore", e);
                            future.complete(true); // Still success locally
                        });
                } else {
                    future.complete(true);
                }
                    
            } catch (Exception e) {
                Log.e(TAG, "❌ Error deleting medical record", e);
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Get all medical records (LiveData)
     */
    public LiveData<List<MedicalRecord>> getAllRecords() {
        return dao.getAllRecords();
    }
    
    /**
     * Get medical record by ID (LiveData)
     */
    public LiveData<MedicalRecord> getRecordById(String recordId) {
        return dao.getRecordById(recordId);
    }
    
    /**
     * Get all medical records (RxJava - Flowable)
     */
    public Flowable<List<MedicalRecord>> getAllRecordsRx() {
        return dao.getAllRecordsRx()
            .subscribeOn(Schedulers.io());
    }
    
    /**
     * Search medical records by query
     * Searches across title, diagnosis, description, doctor, and hospital fields
     * 
     * Requirements: 8.2, 8.4
     * 
     * @param query Search query string
     * @return List of matching medical records
     */
    public List<MedicalRecord> searchRecords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return dao.searchRecords(query.trim());
    }
    
    /**
     * Sync medical record to Firestore
     */
    private CompletableFuture<Boolean> syncToFirestore(MedicalRecord record) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            future.complete(false);
            return future;
        }
        
        // Prepare data according to Project_Summary.md structure
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("title", record.getTitle());
        recordData.put("description", record.getDescription());
        recordData.put("type", record.getType() != null ? record.getType().toString() : "OTHER");
        recordData.put("attachment", record.getAttachment());
        recordData.put("date", record.getDate());
        recordData.put("createdAt", Timestamp.now());
        
        // Save to Firestore
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_RECORDS)
            .document(record.getId())
            .set(recordData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Synced medical record to Firestore");
                future.complete(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to sync medical record to Firestore", e);
                future.complete(false);
            });
        
        return future;
    }
    
    /**
     * Sync medical records from Firestore to Room
     */
    public CompletableFuture<Boolean> syncFromFirestore() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String userId = getCurrentUserId();
        if (userId == null) {
            future.complete(false);
            return future;
        }
        
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_RECORDS)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<MedicalRecord> records = new ArrayList<>();
                querySnapshot.getDocuments().forEach(doc -> {
                    try {
                        MedicalRecord record = doc.toObject(MedicalRecord.class);
                        if (record != null) {
                            record.setId(doc.getId());
                            records.add(record);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Failed to parse medical record: " + doc.getId(), e);
                        // Skip invalid records instead of crashing
                    }
                });
                
                // Save all to Room (upsert to handle duplicates)
                executorService.execute(() -> {
                    try {
                        dao.upsertAll(records);
                        Log.d(TAG, "✅ Synced " + records.size() + " medical records from Firestore");
                        future.complete(true);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error saving synced records", e);
                        future.complete(false);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to sync from Firestore", e);
                future.complete(false);
            });
        
        return future;
    }
}
