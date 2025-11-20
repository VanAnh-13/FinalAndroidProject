package com.example.healthylifehub.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.healthylifehub.utils.AsyncErrorLogger;
import com.example.healthylifehub.utils.AsyncPerformanceLogger;

import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.local.dao.MedicalRecordDao;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.local.dao.SyncStatusDao;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.SyncStatus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Background Worker for Data Synchronization
 * Implements UC-HLH-12: Offline-First Data Sync
 * 
 * Syncs data between Room (local) and Firestore (cloud)
 * - Upload local changes to Firestore
 * - Download remote changes to Room
 * - Handle conflicts with configurable strategies
 */
public class DataSyncWorker extends Worker {
    
    private static final String TAG = "DataSyncWorker";
    private static final int THREAD_POOL_SIZE = 4;
    private static final int SYNC_TIMEOUT_MINUTES = 5;
    
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final ExecutorService executor;
    
    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.db = AppDatabase.getInstance(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DataSyncWorker started");
        
        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, skipping sync");
            return Result.success();
        }
        
        String userId = currentUser.getUid();
        
        // Start performance tracking
        String operationId = AsyncPerformanceLogger.operation("data_sync_worker")
            .withContext("userId", userId)
            .start();
        
        try {
            // Log thread pool metrics before sync
            AsyncPerformanceLogger.logThreadPoolMetrics("DataSync", executor);
            
            // Parallel sync of 4 collections with error logging
            CompletableFuture<Void> syncMetricsTask = CompletableFuture.runAsync(
                () -> syncHealthMetrics(userId), 
                executor
            ).exceptionally(throwable -> {
                AsyncErrorLogger.context()
                    .put("userId", userId)
                    .put("collection", "healthMetrics")
                    .log("sync_health_metrics", throwable);
                throw new RuntimeException(throwable);
            });
            
            CompletableFuture<Void> syncRemindersTask = CompletableFuture.runAsync(
                () -> syncReminders(userId), 
                executor
            ).exceptionally(throwable -> {
                AsyncErrorLogger.context()
                    .put("userId", userId)
                    .put("collection", "reminders")
                    .log("sync_reminders", throwable);
                throw new RuntimeException(throwable);
            });
            
            CompletableFuture<Void> syncMedicinesTask = CompletableFuture.runAsync(
                () -> syncMedicines(userId), 
                executor
            ).exceptionally(throwable -> {
                AsyncErrorLogger.context()
                    .put("userId", userId)
                    .put("collection", "medicines")
                    .log("sync_medicines", throwable);
                throw new RuntimeException(throwable);
            });
            
            CompletableFuture<Void> syncMedicalRecordsTask = CompletableFuture.runAsync(
                () -> syncMedicalRecords(userId), 
                executor
            ).exceptionally(throwable -> {
                AsyncErrorLogger.context()
                    .put("userId", userId)
                    .put("collection", "medicalRecords")
                    .log("sync_medical_records", throwable);
                throw new RuntimeException(throwable);
            });
            
            // Wait for all syncs to complete (with timeout)
            CompletableFuture.allOf(syncMetricsTask, syncRemindersTask, syncMedicinesTask, syncMedicalRecordsTask)
                .exceptionally(throwable -> {
                    AsyncErrorLogger.context()
                        .put("userId", userId)
                        .put("operation", "parallel_sync")
                        .log("data_sync_worker", throwable);
                    throw new RuntimeException(throwable);
                })
                .get(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            // Log thread pool metrics after sync
            AsyncPerformanceLogger.logThreadPoolMetrics("DataSync", executor);
            
            // Log successful completion
            AsyncPerformanceLogger.logEnd(operationId, "data_sync_worker", true);
            
            Log.d(TAG, "DataSyncWorker completed successfully");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "DataSyncWorker failed", e);
            
            // Log failed completion
            AsyncPerformanceLogger.logEnd(operationId, "data_sync_worker", false);
            
            AsyncErrorLogger.context()
                .put("userId", userId)
                .log("data_sync_worker_failed", e);
            return Result.retry();
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Sync Health Metrics
     * 1. Upload local changes to Firestore
     * 2. Download remote changes to Room
     * 3. Handle conflicts with last-write-wins
     */
    private void syncHealthMetrics(String userId) {
        Log.d(TAG, "Syncing health metrics for user: " + userId);
        
        HealthMetricDao metricDao = db.healthMetricDao();
        SyncStatusDao syncDao = db.syncStatusDao();
        
        try {
            // Step 1: Get entities that need sync
            List<SyncStatus> needsSync = syncDao.getEntitiesNeedingSyncByType(userId, "health_metric");
            Log.d(TAG, "Found " + needsSync.size() + " health metrics needing sync");
            
            // Step 2: Upload each to Firestore
            for (SyncStatus status : needsSync) {
                try {
                    syncDao.markSyncing(status.getEntityId());
                    
                    HealthMetric metric = metricDao.getMetricById(status.getEntityId());
                    if (metric != null) {
                        uploadHealthMetricToFirestore(userId, metric);
                        syncDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
                        Log.d(TAG, "Uploaded health metric: " + status.getEntityId());
                    }
                } catch (Exception e) {
                    syncDao.markSyncFailed(status.getEntityId(), e.getMessage());
                    Log.e(TAG, "Failed to upload health metric: " + status.getEntityId(), e);
                }
            }
            
            // Step 3: Download recent changes from Firestore and resolve conflicts
            downloadAndResolveHealthMetrics(userId, metricDao, syncDao);
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing health metrics", e);
        }
    }
    
    /**
     * Sync Reminders
     * 1. Upload local changes to Firestore
     * 2. Download remote changes to Room
     * 3. Handle conflicts with last-write-wins
     */
    private void syncReminders(String userId) {
        Log.d(TAG, "Syncing reminders for user: " + userId);
        
        ReminderDao reminderDao = db.reminderDao();
        SyncStatusDao syncDao = db.syncStatusDao();
        
        try {
            // Step 1: Get entities that need sync
            List<SyncStatus> needsSync = syncDao.getEntitiesNeedingSyncByType(userId, "reminder");
            Log.d(TAG, "Found " + needsSync.size() + " reminders needing sync");
            
            // Step 2: Upload each to Firestore
            for (SyncStatus status : needsSync) {
                try {
                    syncDao.markSyncing(status.getEntityId());
                    
                    Reminder reminder = reminderDao.getReminderById(status.getEntityId());
                    if (reminder != null) {
                        uploadReminderToFirestore(userId, reminder);
                        syncDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
                        Log.d(TAG, "Uploaded reminder: " + status.getEntityId());
                    }
                } catch (Exception e) {
                    syncDao.markSyncFailed(status.getEntityId(), e.getMessage());
                    Log.e(TAG, "Failed to upload reminder: " + status.getEntityId(), e);
                }
            }
            
            // Step 3: Download recent changes from Firestore and resolve conflicts
            downloadAndResolveReminders(userId, reminderDao, syncDao);
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing reminders", e);
        }
    }
    
    /**
     * Sync Medicines
     * Note: Medicines are currently Firestore-only (no Room entity)
     * This method ensures Firestore data is consistent
     */
    private void syncMedicines(String userId) {
        Log.d(TAG, "Syncing medicines for user: " + userId);
        
        SyncStatusDao syncDao = db.syncStatusDao();
        
        try {
            // Step 1: Get entities that need sync
            List<SyncStatus> needsSync = syncDao.getEntitiesNeedingSyncByType(userId, "medicine");
            Log.d(TAG, "Found " + needsSync.size() + " medicines needing sync");
            
            // Step 2: Upload each to Firestore
            for (SyncStatus status : needsSync) {
                try {
                    syncDao.markSyncing(status.getEntityId());
                    
                    // Note: Medicine data would need to be retrieved from a local source
                    // For now, mark as success since medicines are managed directly in Firestore
                    syncDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
                    Log.d(TAG, "Synced medicine: " + status.getEntityId());
                } catch (Exception e) {
                    syncDao.markSyncFailed(status.getEntityId(), e.getMessage());
                    Log.e(TAG, "Failed to sync medicine: " + status.getEntityId(), e);
                }
            }
            
            // Step 3: Download recent changes from Firestore
            // Medicines are accessed via MedicinesRepository with real-time listeners
            Log.d(TAG, "Medicines sync completed (Firestore-only collection)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing medicines", e);
        }
    }
    
    /**
     * Sync Medical Records
     * 1. Upload local changes to Firestore
     * 2. Download remote changes to Room
     * 3. Handle conflicts with last-write-wins
     */
    private void syncMedicalRecords(String userId) {
        Log.d(TAG, "Syncing medical records for user: " + userId);
        
        MedicalRecordDao recordDao = db.medicalRecordDao();
        SyncStatusDao syncDao = db.syncStatusDao();
        
        try {
            // Step 1: Get entities that need sync
            List<SyncStatus> needsSync = syncDao.getEntitiesNeedingSyncByType(userId, "medical_record");
            Log.d(TAG, "Found " + needsSync.size() + " medical records needing sync");
            
            // Step 2: Upload each to Firestore
            for (SyncStatus status : needsSync) {
                try {
                    syncDao.markSyncing(status.getEntityId());
                    
                    MedicalRecord record = recordDao.getRecordByIdRx(status.getEntityId()).blockingFirst();
                    if (record != null) {
                        uploadMedicalRecordToFirestore(userId, record);
                        syncDao.markSyncSuccess(status.getEntityId(), System.currentTimeMillis());
                        Log.d(TAG, "Uploaded medical record: " + status.getEntityId());
                    }
                } catch (Exception e) {
                    syncDao.markSyncFailed(status.getEntityId(), e.getMessage());
                    Log.e(TAG, "Failed to upload medical record: " + status.getEntityId(), e);
                }
            }
            
            // Step 3: Download recent changes from Firestore and resolve conflicts
            downloadAndResolveMedicalRecords(userId, recordDao, syncDao);
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing medical records", e);
        }
    }
    
    // ==================== UPLOAD METHODS ====================
    
    private void uploadHealthMetricToFirestore(String userId, HealthMetric metric) throws Exception {
        Log.d(TAG, "Uploading health metric to Firestore: " + metric.getId());
        
        firestore.collection("users")
            .document(userId)
            .collection("healthMetrics")
            .document(metric.getId())
            .set(metric)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Health metric uploaded successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to upload health metric", e))
            .getResult();
    }
    
    private void uploadReminderToFirestore(String userId, Reminder reminder) throws Exception {
        Log.d(TAG, "Uploading reminder to Firestore: " + reminder.getReminderId());
        
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.getReminderId())
            .set(reminder)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Reminder uploaded successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to upload reminder", e))
            .getResult();
    }
    
    private void uploadMedicalRecordToFirestore(String userId, MedicalRecord record) throws Exception {
        Log.d(TAG, "Uploading medical record to Firestore: " + record.getId());
        
        firestore.collection("users")
            .document(userId)
            .collection("medicalRecords")
            .document(record.getId())
            .set(record)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Medical record uploaded successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to upload medical record", e))
            .getResult();
    }
    
    // ==================== DOWNLOAD AND CONFLICT RESOLUTION METHODS ====================
    
    private void downloadAndResolveHealthMetrics(String userId, HealthMetricDao dao, SyncStatusDao syncDao) {
        Log.d(TAG, "Downloading health metrics from Firestore");
        
        try {
            // Get last sync time for this collection
            long lastSyncTime = getLastSyncTime(syncDao, userId, "health_metric");
            
            // Download recent changes from Firestore
            QuerySnapshot snapshot = firestore.collection("users")
                .document(userId)
                .collection("healthMetrics")
                .whereGreaterThan("lastSyncedAt", new java.util.Date(lastSyncTime))
                .get()
                .getResult();
            
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    HealthMetric remoteMetric = doc.toObject(HealthMetric.class);
                    if (remoteMetric != null) {
                        // Check for local version
                        HealthMetric localMetric = dao.getMetricById(remoteMetric.getId());
                        
                        if (localMetric != null) {
                            // Conflict detected - resolve using last-write-wins
                            HealthMetric resolved = resolveConflictWithTimestamp(
                                localMetric, 
                                remoteMetric,
                                localMetric.getLastSyncedAt() != null ? localMetric.getLastSyncedAt().getTime() : 0,
                                remoteMetric.getLastSyncedAt() != null ? remoteMetric.getLastSyncedAt().getTime() : 0
                            );
                            
                            if (resolved == remoteMetric) {
                                // Remote is newer - save locally
                                dao.insertMetric(remoteMetric);
                                Log.d(TAG, "Resolved conflict: kept remote version for metric " + remoteMetric.getId());
                            } else {
                                // Local is newer - upload to Firestore
                                uploadHealthMetricToFirestore(userId, localMetric);
                                Log.d(TAG, "Resolved conflict: kept local version for metric " + localMetric.getId());
                            }
                        } else {
                            // No conflict - just save
                            dao.insertMetric(remoteMetric);
                            Log.d(TAG, "Downloaded new health metric: " + remoteMetric.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading health metrics", e);
        }
    }
    
    private void downloadAndResolveReminders(String userId, ReminderDao dao, SyncStatusDao syncDao) {
        Log.d(TAG, "Downloading reminders from Firestore");
        
        try {
            // Get last sync time for this collection
            long lastSyncTime = getLastSyncTime(syncDao, userId, "reminder");
            
            // Download recent changes from Firestore
            QuerySnapshot snapshot = firestore.collection("users")
                .document(userId)
                .collection("reminders")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
                .getResult();
            
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Reminder remoteReminder = doc.toObject(Reminder.class);
                    if (remoteReminder != null) {
                        // Check for local version
                        Reminder localReminder = dao.getReminderById(remoteReminder.getReminderId());
                        
                        if (localReminder != null) {
                            // Conflict detected - resolve using last-write-wins
                            Reminder resolved = resolveConflictWithTimestamp(
                                localReminder, 
                                remoteReminder,
                                localReminder.getUpdatedAt(),
                                remoteReminder.getUpdatedAt()
                            );
                            
                            if (resolved == remoteReminder) {
                                // Remote is newer - save locally
                                dao.insert(remoteReminder);
                                Log.d(TAG, "Resolved conflict: kept remote version for reminder " + remoteReminder.getReminderId());
                            } else {
                                // Local is newer - upload to Firestore
                                uploadReminderToFirestore(userId, localReminder);
                                Log.d(TAG, "Resolved conflict: kept local version for reminder " + localReminder.getReminderId());
                            }
                        } else {
                            // No conflict - just save
                            dao.insert(remoteReminder);
                            Log.d(TAG, "Downloaded new reminder: " + remoteReminder.getReminderId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading reminders", e);
        }
    }
    
    private void downloadAndResolveMedicalRecords(String userId, MedicalRecordDao dao, SyncStatusDao syncDao) {
        Log.d(TAG, "Downloading medical records from Firestore");
        
        try {
            // Get last sync time for this collection
            long lastSyncTime = getLastSyncTime(syncDao, userId, "medical_record");
            
            // Download recent changes from Firestore
            QuerySnapshot snapshot = firestore.collection("users")
                .document(userId)
                .collection("medicalRecords")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get()
                .getResult();
            
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    MedicalRecord remoteRecord = doc.toObject(MedicalRecord.class);
                    if (remoteRecord != null) {
                        // Check for local version
                        MedicalRecord localRecord = dao.getRecordByIdRx(remoteRecord.getId()).blockingFirst();
                        
                        if (localRecord != null) {
                            // Conflict detected - resolve using last-write-wins
                            MedicalRecord resolved = resolveConflictWithTimestamp(
                                localRecord, 
                                remoteRecord,
                                localRecord.getUpdatedAt() != null ? localRecord.getUpdatedAt() : 0,
                                remoteRecord.getUpdatedAt() != null ? remoteRecord.getUpdatedAt() : 0
                            );
                            
                            if (resolved == remoteRecord) {
                                // Remote is newer - save locally
                                dao.upsertAll(java.util.Collections.singletonList(remoteRecord));
                                Log.d(TAG, "Resolved conflict: kept remote version for record " + remoteRecord.getId());
                            } else {
                                // Local is newer - upload to Firestore
                                uploadMedicalRecordToFirestore(userId, localRecord);
                                Log.d(TAG, "Resolved conflict: kept local version for record " + localRecord.getId());
                            }
                        } else {
                            // No conflict - just save
                            dao.upsertAll(java.util.Collections.singletonList(remoteRecord));
                            Log.d(TAG, "Downloaded new medical record: " + remoteRecord.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading medical records", e);
        }
    }
    
    /**
     * Get last sync time for a collection type
     * Returns the most recent successful sync timestamp for the given entity type
     */
    private long getLastSyncTime(SyncStatusDao syncDao, String userId, String entityType) {
        try {
            Long lastSyncTime = syncDao.getLastSyncTimeForType(userId, entityType);
            return lastSyncTime != null ? lastSyncTime : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting last sync time for type: " + entityType, e);
            return 0;
        }
    }
    
    // ==================== CONFLICT RESOLUTION ====================
    
    /**
     * Resolve conflict between local and remote data using timestamp comparison
     * Strategy: Last Write Wins
     * 
     * @param localData Local version of the data
     * @param remoteData Remote version of the data
     * @param localTimestamp Timestamp of local version (updatedAt)
     * @param remoteTimestamp Timestamp of remote version (updatedAt)
     * @return The newer version based on timestamp comparison
     */
    private <T> T resolveConflictWithTimestamp(T localData, T remoteData, long localTimestamp, long remoteTimestamp) {
        Log.d(TAG, "Resolving conflict - Local timestamp: " + localTimestamp + ", Remote timestamp: " + remoteTimestamp);
        
        // Compare timestamps and keep newer version
        if (localTimestamp > remoteTimestamp) {
            Log.d(TAG, "Local version is newer - keeping local");
            return localData;
        } else {
            Log.d(TAG, "Remote version is newer or equal - keeping remote");
            return remoteData;
        }
    }
    
    /**
     * Resolve conflict between local and remote data
     * Default strategy: Last Write Wins
     * 
     * @deprecated Use resolveConflictWithTimestamp for timestamp-based resolution
     */
    @Deprecated
    private <T> T resolveConflict(T localData, T remoteData, String strategy) {
        switch (strategy) {
            case "last_write_wins":
                // Compare timestamps and keep newer one
                // For now, just return remote (Firestore is source of truth)
                return remoteData;
                
            case "local_wins":
                return localData;
                
            case "remote_wins":
                return remoteData;
                
            case "manual":
                // Mark for manual resolution
                // Store both versions for user to choose
                return null;
                
            default:
                return remoteData;
        }
    }
    
    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "DataSyncWorker stopped");
        executor.shutdownNow();
    }
}
