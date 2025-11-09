package com.example.healthylifehub.ui.metrics.add_edit;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.repository.RxHealthMetricRepository;

import java.util.Date;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * RxJava-based ViewModel for Add/Edit Metric
 * 
 * Benefits:
 * - Automatic disposal on ViewModel clear
 * - Easy error handling with RxJava operators
 * - Thread management with subscribeOn/observeOn
 * - Composable operations (retry, debounce, etc.)
 */
public class RxAddEditMetricViewModel extends AndroidViewModel {
    
    private final RxHealthMetricRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public RxAddEditMetricViewModel(@NonNull Application application) {
        super(application);
        repository = new RxHealthMetricRepository(application.getApplicationContext());
    }
    
    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }
    
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    // ==================== SAVE METHODS ====================
    
    /**
     * Save blood pressure metric (RxJava version)
     */
    public void saveBloodPressure(int systolic, int diastolic, Date measuredAt, String notes) {
        // Validate
        String validationError = validateBloodPressure(systolic, diastolic);
        if (validationError != null) {
            errorMessage.setValue(validationError);
            return;
        }
        
        // Create metric
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_pressure");
        metric.setSystolic(systolic);
        metric.setDiastolic(diastolic);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        // Save with RxJava
        isSaving.setValue(true);
        
        disposables.add(
            repository.saveHealthMetric(metric)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // onComplete
                        isSaving.setValue(false);
                        saveSuccess.setValue(true);
                    },
                    error -> {
                        // onError
                        isSaving.setValue(false);
                        errorMessage.setValue("Lỗi khi lưu: " + error.getMessage());
                    }
                )
        );
    }
    
    /**
     * Save blood sugar metric
     */
    public void saveBloodSugar(double value, Date measuredAt, String notes) {
        String validationError = validateBloodSugar(value);
        if (validationError != null) {
            errorMessage.setValue(validationError);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_sugar");
        metric.setValue(value);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        isSaving.setValue(true);
        
        disposables.add(
            repository.saveHealthMetric(metric)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        isSaving.setValue(false);
                        saveSuccess.setValue(true);
                    },
                    error -> {
                        isSaving.setValue(false);
                        errorMessage.setValue("Lỗi khi lưu: " + error.getMessage());
                    }
                )
        );
    }
    
    /**
     * Save weight metric
     */
    public void saveWeight(double value, Date measuredAt, String notes) {
        String validationError = validateWeight(value);
        if (validationError != null) {
            errorMessage.setValue(validationError);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("weight");
        metric.setValue(value);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        isSaving.setValue(true);
        
        disposables.add(
            repository.saveHealthMetric(metric)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        isSaving.setValue(false);
                        saveSuccess.setValue(true);
                    },
                    error -> {
                        isSaving.setValue(false);
                        errorMessage.setValue("Lỗi khi lưu: " + error.getMessage());
                    }
                )
        );
    }
    
    /**
     * Save heart rate metric
     */
    public void saveHeartRate(double value, Date measuredAt, String notes) {
        String validationError = validateHeartRate(value);
        if (validationError != null) {
            errorMessage.setValue(validationError);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("heart_rate");
        metric.setValue(value);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        isSaving.setValue(true);
        
        disposables.add(
            repository.saveHealthMetric(metric)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        isSaving.setValue(false);
                        saveSuccess.setValue(true);
                    },
                    error -> {
                        isSaving.setValue(false);
                        errorMessage.setValue("Lỗi khi lưu: " + error.getMessage());
                    }
                )
        );
    }
    
    // ==================== VALIDATION ====================
    
    private String validateBloodPressure(int systolic, int diastolic) {
        if (systolic < 70 || systolic > 250) {
            return "Huyết áp tâm thu phải từ 70-250 mmHg";
        }
        if (diastolic < 40 || diastolic > 150) {
            return "Huyết áp tâm trương phải từ 40-150 mmHg";
        }
        if (systolic <= diastolic) {
            return "Huyết áp tâm thu phải lớn hơn tâm trương";
        }
        return null;
    }
    
    private String validateBloodSugar(double value) {
        if (value < 20 || value > 600) {
            return "Đường huyết phải từ 20-600 mg/dL";
        }
        return null;
    }
    
    private String validateWeight(double value) {
        if (value < 10 || value > 500) {
            return "Cân nặng phải từ 10-500 kg";
        }
        return null;
    }
    
    private String validateHeartRate(double value) {
        if (value < 30 || value > 250) {
            return "Nhịp tim phải từ 30-250 bpm";
        }
        return null;
    }
    
    // ==================== LIFECYCLE ====================
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Dispose all subscriptions to prevent memory leaks
        disposables.clear();
    }
}
