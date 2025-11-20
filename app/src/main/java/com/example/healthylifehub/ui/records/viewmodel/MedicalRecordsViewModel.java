package com.example.healthylifehub.ui.records.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.repository.MedicalRecordsRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * ViewModel for Medical Records with RxJava-based debounced search
 * 
 * Features:
 * - Debounced search (300ms delay to avoid excessive queries)
 * - Distinct until changed (avoids duplicate queries)
 * - Switch map (cancels previous search when new query arrives)
 * - Background thread execution for search
 * - Main thread result delivery
 * 
 * Requirements: 8.1, 8.2, 8.3
 */
public class MedicalRecordsViewModel extends BaseViewModel {
    
    private static final String TAG = "MedicalRecordsViewModel";
    private static final int DEBOUNCE_TIMEOUT_MS = 300;
    
    private final MedicalRecordsRepository repository;
    private final PublishSubject<String> searchQuerySubject;
    private final MutableLiveData<List<MedicalRecord>> searchResults;
    private final CompositeDisposable disposables;
    
    public MedicalRecordsViewModel(Application application) {
        super(application);
        this.repository = new MedicalRecordsRepository(application);
        this.searchQuerySubject = PublishSubject.create();
        this.searchResults = new MutableLiveData<>();
        this.disposables = new CompositeDisposable();
        
        setupSearch();
    }
    
    /**
     * Setup the RxJava search pipeline with debouncing and cancellation
     * 
     * Pipeline stages:
     * 1. debounce(300ms) - Wait 300ms after last input before processing
     * 2. distinctUntilChanged() - Skip if query is same as previous
     * 3. switchMap() - Cancel previous search when new query arrives
     * 4. subscribeOn(Schedulers.io()) - Execute search on background thread
     * 5. observeOn(AndroidSchedulers.mainThread()) - Deliver results on main thread
     * 
     * Requirements: 8.1, 8.2, 8.3
     */
    private void setupSearch() {
        disposables.add(
            searchQuerySubject
                .debounce(DEBOUNCE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .switchMap(query -> 
                    Observable.fromCallable(() -> performSearch(query))
                        .subscribeOn(Schedulers.io())
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    results -> {
                        searchResults.setValue(results);
                        Log.d(TAG, "Search completed: " + results.size() + " results");
                    },
                    error -> {
                        Log.e(TAG, "Search error", error);
                        searchResults.setValue(Collections.emptyList());
                    }
                )
        );
    }
    
    /**
     * Submit a search query
     * The query will be debounced and processed asynchronously
     * 
     * @param query Search query string
     */
    public void search(String query) {
        if (query == null) {
            query = "";
        }
        searchQuerySubject.onNext(query);
    }
    
    /**
     * Get search results LiveData for observation
     * 
     * @return LiveData containing search results
     */
    public LiveData<List<MedicalRecord>> getSearchResults() {
        return searchResults;
    }
    
    /**
     * Get all medical records (non-search)
     * 
     * @return LiveData containing all medical records
     */
    public LiveData<List<MedicalRecord>> getAllRecords() {
        return repository.getAllRecords();
    }
    
    /**
     * Perform the actual search operation
     * This method is called on a background thread by the RxJava pipeline
     * 
     * Requirements: 8.2, 8.4
     * 
     * @param query Search query string
     * @return List of matching medical records
     */
    private List<MedicalRecord> performSearch(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return Collections.emptyList();
            }
            
            long startTime = System.currentTimeMillis();
            List<MedicalRecord> results = repository.searchRecords(query);
            long duration = System.currentTimeMillis() - startTime;
            
            Log.d(TAG, "Search completed in " + duration + "ms for query: " + query);
            
            return results;
        } catch (Exception e) {
            Log.e(TAG, "Error performing search", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Clean up resources when ViewModel is destroyed
     * Clears all RxJava disposables to prevent memory leaks
     * 
     * Requirements: 8.1
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        Log.d(TAG, "Disposables cleared");
    }
}
