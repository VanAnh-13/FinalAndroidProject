package com.example.healthylifehub.ui.metrics.add_edit;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.repository.HealthMetricRepository;
import com.example.healthylifehub.utils.error.ExceptionHandler;

import java.util.Date;

/**
 * ViewModel for Add/Edit Health Metric screen
 * Handles validation and saving logic with network awareness
 */
public class AddEditMetricViewModel extends BaseViewModel {
    
    private final HealthMetricRepository repository;
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canSave = new MutableLiveData<>(true);
    
    public AddEditMetricViewModel(@NonNull Application application) {
        super(application);
        repository = new HealthMetricRepository(application.getApplicationContext());
        
        // Observe network connectivity to enable/disable save functionality
        getNetworkConnectivity().observeForever(isConnected -> {
            canSave.postValue(isConnected);
            if (!isConnected) {
                errorMessage.postValue("Không có kết nối mạng. Dữ liệu sẽ được lưu cục bộ và đồng bộ khi có mạng.");
            } else {
                // Clear network error when connection is restored
                if (errorMessage.getValue() != null && 
                    errorMessage.getValue().contains("Không có kết nối mạng")) {
                    errorMessage.postValue(null);
                }
            }
        });
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
    
    public LiveData<Boolean> getCanSave() {
        return canSave;
    }
    
    /**
     * Load metric by ID for edit mode
     */
    public LiveData<HealthMetric> loadMetricById(String metricId) {
        MutableLiveData<HealthMetric> metricLiveData = new MutableLiveData<>();
        
        // TODO: Implement load from repository
        // For now, return empty
        // repository.getMetricById(metricId).observe(...);
        
        return metricLiveData;
    }
    
    /**
     * Validate and save blood pressure metric
     */
    public void saveBloodPressure(int systolic, int diastolic, Date measuredAt, String notes) {
        // Validate date first
        ValidationResult dateValidation = validateDate(measuredAt);
        if (!dateValidation.isValid) {
            errorMessage.setValue(dateValidation.errorMessage);
            return;
        }
        
        // Validate blood pressure values
        ValidationResult validation = validateBloodPressure(systolic, diastolic);
        if (!validation.isValid) {
            errorMessage.setValue(validation.errorMessage);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_pressure");
        metric.setSystolic(systolic);
        metric.setDiastolic(diastolic);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        saveMetric(metric);
    }
    
    /**
     * Validate and save blood sugar metric
     */
    public void saveBloodSugar(double value, Date measuredAt, String notes) {
        // Validate date first
        ValidationResult dateValidation = validateDate(measuredAt);
        if (!dateValidation.isValid) {
            errorMessage.setValue(dateValidation.errorMessage);
            return;
        }
        
        ValidationResult validation = validateBloodSugar(value);
        if (!validation.isValid) {
            errorMessage.setValue(validation.errorMessage);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_sugar");
        metric.setValue(value);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        saveMetric(metric);
    }
    
    /**
     * Validate and save weight metric
     */
    public void saveWeight(double value, Date measuredAt, String notes) {
        // Validate date first
        ValidationResult dateValidation = validateDate(measuredAt);
        if (!dateValidation.isValid) {
            errorMessage.setValue(dateValidation.errorMessage);
            return;
        }
        
        ValidationResult validation = validateWeight(value);
        if (!validation.isValid) {
            errorMessage.setValue(validation.errorMessage);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("weight");
        metric.setValue(value);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        saveMetric(metric);
    }
    
    /**
     * Validate and save heart rate metric
     */
    public void saveHeartRate(double value, Date measuredAt, String notes) {
        // Validate date first
        ValidationResult dateValidation = validateDate(measuredAt);
        if (!dateValidation.isValid) {
            errorMessage.setValue(dateValidation.errorMessage);
            return;
        }
        
        ValidationResult validation = validateHeartRate(value);
        if (!validation.isValid) {
            errorMessage.setValue(validation.errorMessage);
            return;
        }
        
        HealthMetric metric = new HealthMetric();
        metric.setType("heart_rate");
        metric.setValue(value);
        metric.setMeasuredAt(measuredAt);
        metric.setNotes(notes);
        
        saveMetric(metric);
    }
    
    /**
     * Save metric to repository with network awareness
     */
    private void saveMetric(HealthMetric metric) {
        isSaving.setValue(true);
        
        // Use network-aware task execution
        executeNetworkTask(
            () -> {
                // This will be executed on background thread
                try {
                    boolean success = repository.saveHealthMetric(metric).get();
                    return DataState.success(success);
                } catch (Exception e) {
                    return DataState.error(e.getMessage());
                }
            },
            success -> {
                // On success
                isSaving.postValue(false);
                saveSuccess.postValue(success);
                
                if (!success) {
                    errorMessage.postValue("Lỗi khi lưu chỉ số. Vui lòng thử lại.");
                } else {
                    // Clear any previous error messages on successful save
                    errorMessage.postValue(null);
                }
            },
            error -> {
                // On error - use ExceptionHandler for better error handling
                isSaving.postValue(false);
                saveSuccess.postValue(false);
                
                // Use ExceptionHandler to get user-friendly error message
                ExceptionHandler.ExceptionResult result = ExceptionHandler.handleException(error);
                
                // Set appropriate error message based on exception category
                switch (result.getCategory()) {
                    case NETWORK_ERROR:
                        errorMessage.postValue("Lỗi kết nối mạng. Dữ liệu đã được lưu cục bộ và sẽ đồng bộ khi có mạng.");
                        break;
                    case FIREBASE_FIRESTORE_ERROR:
                        errorMessage.postValue("Lỗi cơ sở dữ liệu: " + result.getUserMessage());
                        break;
                    case VALIDATION_ERROR:
                        errorMessage.postValue("Dữ liệu không hợp lệ: " + result.getUserMessage());
                        break;
                    default:
                        errorMessage.postValue(result.getUserMessage());
                        break;
                }
                
                // Log the technical details for debugging
                android.util.Log.e("AddEditMetricViewModel", "Error saving metric", error);
            },
            "Không có kết nối mạng. Dữ liệu sẽ được lưu cục bộ và đồng bộ khi có mạng.",
            false // Don't show loading here since we have our own isSaving state
        );
    }
    
    // ==================== VALIDATION METHODS ====================
    
    /**
     * Validate date - không được chọn ngày tương lai
     */
    private ValidationResult validateDate(Date measuredAt) {
        if (measuredAt == null) {
            return new ValidationResult(false, "Vui lòng chọn ngày đo");
        }
        
        Date now = new Date();
        if (measuredAt.after(now)) {
            return new ValidationResult(false, "Ngày đo không được là ngày tương lai");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate blood pressure values
     * Normal range: Systolic 90-180 mmHg, Diastolic 60-120 mmHg
     */
    private ValidationResult validateBloodPressure(int systolic, int diastolic) {
        if (systolic < 70 || systolic > 250) {
            return new ValidationResult(false, "Huyết áp tâm thu phải từ 70-250 mmHg");
        }
        
        if (diastolic < 40 || diastolic > 150) {
            return new ValidationResult(false, "Huyết áp tâm trương phải từ 40-150 mmHg");
        }
        
        if (systolic <= diastolic) {
            return new ValidationResult(false, "Huyết áp tâm thu phải lớn hơn tâm trương");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate blood sugar values
     * Normal range: 70-400 mg/dL
     */
    private ValidationResult validateBloodSugar(double value) {
        if (value < 20 || value > 600) {
            return new ValidationResult(false, "Đường huyết phải từ 20-600 mg/dL");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate weight values
     * Normal range: 10-300 kg
     */
    private ValidationResult validateWeight(double value) {
        if (value < 10 || value > 500) {
            return new ValidationResult(false, "Cân nặng phải từ 10-500 kg");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate heart rate values
     * Normal range: 30-220 bpm
     */
    private ValidationResult validateHeartRate(double value) {
        if (value < 30 || value > 250) {
            return new ValidationResult(false, "Nhịp tim phải từ 30-250 bpm");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Helper class for validation results
     */
    private static class ValidationResult {
        boolean isValid;
        String errorMessage;
        
        ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
}
