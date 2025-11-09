package com.example.healthylifehub.ui.profile.fragment;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.data.model.UserProfile;
import com.example.healthylifehub.data.repository.UserRepository;

import java.util.concurrent.CompletableFuture;

public class ProfileViewModel extends BaseViewModel {

    private final UserRepository userRepository;
    
    private final MutableLiveData<String> height = new MutableLiveData<>();
    private final MutableLiveData<String> weight = new MutableLiveData<>();
    private final MutableLiveData<String> bmi = new MutableLiveData<>();
    private final MutableLiveData<String> bloodType = new MutableLiveData<>();
    private final MutableLiveData<String> birthDate = new MutableLiveData<>();
    private final MutableLiveData<String> gender = new MutableLiveData<>();
    private final MutableLiveData<String> medicalHistory = new MutableLiveData<>();
    private final MutableLiveData<Integer> metricsTracked = new MutableLiveData<>();
    private final MutableLiveData<Integer> remindersCreated = new MutableLiveData<>();
    private final MutableLiveData<Integer> recordsEntered = new MutableLiveData<>();
    
    // LiveData for save operation result
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
        loadProfileData();
    }

    private void loadProfileData() {
        // Load real data from Firestore
        userRepository.loadUserProfile().observeForever(userData -> {
            if (userData != null) {
                // Extract profile nested object
                Object profileObj = userData.get("profile");
                if (profileObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> profile = (java.util.Map<String, Object>) profileObj;
                    
                    // Load height
                    Object heightObj = profile.get("height");
                    if (heightObj instanceof Number) {
                        double heightValue = ((Number) heightObj).doubleValue();
                        if (heightValue > 0) {
                            height.setValue(heightValue + " cm");
                        } else {
                            height.setValue("");
                        }
                    }
                    
                    // Load weight
                    Object weightObj = profile.get("weight");
                    if (weightObj instanceof Number) {
                        double weightValue = ((Number) weightObj).doubleValue();
                        if (weightValue > 0) {
                            weight.setValue(weightValue + " kg");
                        } else {
                            weight.setValue("");
                        }
                    }
                    
                    // Calculate and set BMI only if both height and weight are valid
                    if (heightObj instanceof Number && weightObj instanceof Number) {
                        double h = ((Number) heightObj).doubleValue();
                        double w = ((Number) weightObj).doubleValue();
                        
                        // Validate ranges before calculating BMI
                        if (h >= 50 && h <= 300 && w >= 10 && w <= 500) {
                            double heightInMeters = h / 100.0; // convert cm to meters
                            double bmiValue = w / (heightInMeters * heightInMeters);
                            bmi.setValue(String.format(java.util.Locale.getDefault(), "%.1f", bmiValue));
                        } else {
                            bmi.setValue("");
                        }
                    } else {
                        bmi.setValue("");
                    }
                    
                    // Load blood type
                    Object bloodTypeObj = profile.get("bloodType");
                    if (bloodTypeObj instanceof String) {
                        bloodType.setValue((String) bloodTypeObj);
                    }
                    
                    // Load birth date - convert from Timestamp to String
                    Object birthDateObj = profile.get("dateOfBirth");
                    if (birthDateObj instanceof com.google.firebase.Timestamp) {
                        com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) birthDateObj;
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                        birthDate.setValue(sdf.format(timestamp.toDate()));
                    } else if (birthDateObj instanceof String) {
                        birthDate.setValue((String) birthDateObj);
                    }
                    
                    // Load gender - convert from English to Vietnamese
                    Object genderObj = profile.get("gender");
                    if (genderObj instanceof String) {
                        String genderStr = (String) genderObj;
                        if (genderStr.equalsIgnoreCase("male")) {
                            gender.setValue("Nam");
                        } else if (genderStr.equalsIgnoreCase("female")) {
                            gender.setValue("Nữ");
                        } else {
                            gender.setValue("Khác");
                        }
                    }
                    
                    // Load medical history
                    Object historyObj = profile.get("medicalHistory");
                    if (historyObj instanceof String) {
                        medicalHistory.setValue((String) historyObj);
                    }
                }
                
                // Set default values for statistics (these would come from counting subcollections)
                // TODO: Implement actual counting from Firestore subcollections
                metricsTracked.setValue(0);
                remindersCreated.setValue(0);
                recordsEntered.setValue(0);
            } else {
                // Set default empty values if no data exists
                height.setValue("");
                weight.setValue("");
                bmi.setValue("");
                bloodType.setValue("");
                birthDate.setValue("");
                gender.setValue("");
                medicalHistory.setValue("");
                metricsTracked.setValue(0);
                remindersCreated.setValue(0);
                recordsEntered.setValue(0);
            }
        });
    }

    // Getters for LiveData
    public LiveData<String> getHeight() {
        return height;
    }

    public LiveData<String> getWeight() {
        return weight;
    }

    public LiveData<String> getBmi() {
        return bmi;
    }

    public LiveData<String> getBloodType() {
        return bloodType;
    }

    public LiveData<String> getBirthDate() {
        return birthDate;
    }

    public LiveData<String> getGender() {
        return gender;
    }

    public LiveData<String> getMedicalHistory() {
        return medicalHistory;
    }

    public LiveData<Integer> getMetricsTracked() {
        return metricsTracked;
    }

    public LiveData<Integer> getRemindersCreated() {
        return remindersCreated;
    }

    public LiveData<Integer> getRecordsEntered() {
        return recordsEntered;
    }

    // Methods to update profile data (for future use)
    public void updateHeight(String newHeight) {
        height.setValue(newHeight);
    }

    public void updateWeight(String newWeight) {
        weight.setValue(newWeight);
    }

    public void updateBmi(String newBmi) {
        bmi.setValue(newBmi);
    }

    public void updateBloodType(String newBloodType) {
        bloodType.setValue(newBloodType);
    }

    public void updateBirthDate(String newBirthDate) {
        birthDate.setValue(newBirthDate);
    }

    public void updateGender(String newGender) {
        gender.setValue(newGender);
    }

    public void updateMedicalHistory(String newHistory) {
        medicalHistory.setValue(newHistory);
    }
    
    public LiveData<Boolean> getSaveResult() {
        return saveResult;
    }
    
    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }
    
    /**
     * Save profile data to Firestore
     * Uses CompletableFuture for async operation as per Project_Summary.md UC-03
     * 
     * @param fullName User's full name
     * @param email User's email
     * @param birthDate Birth date string (dd/MM/yyyy)
     * @param gender Gender (Nam/Nữ/Khác)
     * @param height Height with unit (e.g., "175 cm")
     * @param weight Weight with unit (e.g., "70 kg")
     * @param bloodType Blood type (e.g., "O+")
     * @param medicalHistory Medical history text
     * @param bmi BMI value
     */
    public void saveProfile(String fullName, String email, String birthDate, String gender,
                           String height, String weight, String bloodType, 
                           String medicalHistory, String bmi) {
        
        isSaving.setValue(true);
        
        // Create UserProfile object
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName(fullName);
        userProfile.setEmail(email);
        userProfile.setBirthDate(birthDate);
        userProfile.setGender(gender);
        userProfile.setHeight(height);
        userProfile.setWeight(weight);
        userProfile.setBloodType(bloodType);
        userProfile.setMedicalHistory(medicalHistory);
        userProfile.setBmi(bmi);
        
        // Save to Firestore using CompletableFuture (async)
        userRepository.updateUserProfile(userProfile)
            .thenAccept(success -> {
                // Update on main thread
                isSaving.postValue(false);
                saveResult.postValue(success);
                
                // Update local LiveData if successful
                if (success) {
                    this.height.postValue(height);
                    this.weight.postValue(weight);
                    this.bmi.postValue(bmi);
                    this.bloodType.postValue(bloodType);
                    this.birthDate.postValue(birthDate);
                    this.gender.postValue(gender);
                    this.medicalHistory.postValue(medicalHistory);
                }
            })
            .exceptionally(throwable -> {
                // Handle error
                isSaving.postValue(false);
                saveResult.postValue(false);
                throwable.printStackTrace();
                return null;
            });
    }
    
    /**
     * Save profile with auth update (name, email, photo)
     */
    public void saveProfileWithAuth(String fullName, String email, String photoUrl) {
        isSaving.setValue(true);
        
        userRepository.updateUserProfileWithAuth(fullName, email, photoUrl)
            .thenAccept(success -> {
                isSaving.postValue(false);
                saveResult.postValue(success);
            })
            .exceptionally(throwable -> {
                isSaving.postValue(false);
                saveResult.postValue(false);
                throwable.printStackTrace();
                return null;
            });
    }
}
