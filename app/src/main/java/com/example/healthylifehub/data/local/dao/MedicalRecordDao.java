package com.example.healthylifehub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthylifehub.data.model.MedicalRecord;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * DAO for Medical Records
 * Provides CRUD operations for medical records with RxJava support
 */
@Dao
public interface MedicalRecordDao {
    
    /**
     * Insert a new medical record
     */
    @Insert
    void insert(MedicalRecord record);
    
    /**
     * Insert multiple medical records
     */
    @Insert
    void insertAll(List<MedicalRecord> records);
    
    /**
     * Update an existing medical record
     */
    @Update
    void update(MedicalRecord record);
    
    /**
     * Delete a medical record
     */
    @Delete
    void delete(MedicalRecord record);
    
    /**
     * Delete all medical records for a user
     */
    @Query("DELETE FROM medical_records WHERE id IN (SELECT id FROM medical_records WHERE id LIKE :userId || '%')")
    void deleteByUserId(String userId);
    
    /**
     * Delete all medical records
     */
    @Query("DELETE FROM medical_records")
    void deleteAll();
    
    /**
     * Get all medical records for a user (LiveData)
     */
    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    LiveData<List<MedicalRecord>> getAllRecords();
    
    /**
     * Get medical record by ID
     */
    @Query("SELECT * FROM medical_records WHERE id = :recordId")
    LiveData<MedicalRecord> getRecordById(String recordId);
    
    /**
     * Get all medical records (RxJava - Flowable)
     */
    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    Flowable<List<MedicalRecord>> getAllRecordsRx();
    
    /**
     * Get medical record by ID (RxJava - Flowable)
     */
    @Query("SELECT * FROM medical_records WHERE id = :recordId")
    Flowable<MedicalRecord> getRecordByIdRx(String recordId);
    
    /**
     * Insert record (RxJava - Completable)
     */
    @Insert
    Completable insertRx(MedicalRecord record);
    
    /**
     * Update record (RxJava - Completable)
     */
    @Update
    Completable updateRx(MedicalRecord record);
    
    /**
     * Delete record (RxJava - Completable)
     */
    @Delete
    Completable deleteRx(MedicalRecord record);
}
