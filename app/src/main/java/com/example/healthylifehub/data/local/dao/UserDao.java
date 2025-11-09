package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.healthylifehub.data.model.User;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * DAO for User operations
 * Dùng trực tiếp User model (không cần Entity riêng)
 * Supports both LiveData and RxJava3
 */
@Dao
public interface UserDao {
    
    /**
     * Insert or replace user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);
    
    /**
     * Update user
     */
    @Update
    void updateUser(User user);
    
    /**
     * Get user by ID (LiveData for reactive updates)
     */
    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    LiveData<User> getUserLiveData(String userId);
    
    /**
     * Get user by ID (synchronous)
     */
    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    User getUser(String userId);
    
    /**
     * Get all users
     */
    @Query("SELECT * FROM users")
    LiveData<List<User>> getAllUsers();
    
    /**
     * Delete user
     */
    @Query("DELETE FROM users WHERE uid = :userId")
    void deleteUser(String userId);
    
    /**
     * Get users that need sync
     */
    @Query("SELECT * FROM users WHERE needsSync = 1")
    List<User> getUsersNeedingSync();
    
    /**
     * Mark user as synced
     */
    @Query("UPDATE users SET needsSync = 0, lastSyncedAt = :syncTime WHERE uid = :userId")
    void markAsSynced(String userId, long syncTime);
    
    // ==================== RxJava3 Methods ====================
    
    /**
     * Insert user (RxJava)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertUserRx(User user);
    
    /**
     * Update user (RxJava)
     */
    @Update
    Completable updateUserRx(User user);
    
    /**
     * Get user by ID (RxJava - Flowable for reactive updates)
     */
    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    Flowable<User> getUserFlowable(String userId);
    
    /**
     * Get user by ID (RxJava - Maybe for one-time fetch)
     */
    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    Maybe<User> getUserMaybe(String userId);
    
    /**
     * Get all users (RxJava)
     */
    @Query("SELECT * FROM users")
    Flowable<List<User>> getAllUsersRx();
    
    /**
     * Delete user (RxJava)
     */
    @Query("DELETE FROM users WHERE uid = :userId")
    Completable deleteUserRx(String userId);
    
    /**
     * Get users needing sync (RxJava)
     */
    @Query("SELECT * FROM users WHERE needsSync = 1")
    Single<List<User>> getUsersNeedingSyncRx();
}
