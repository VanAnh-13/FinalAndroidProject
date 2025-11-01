package com.example.healthylifehub.ui.profile;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.base.BaseViewModel;

public class ProfileViewModel extends BaseViewModel {

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

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        loadProfileData();
    }

    private void loadProfileData() {
        // Load default sample data
        // In a real app, this would come from a database or API
        height.setValue("175 cm");
        weight.setValue("70 kg");
        bmi.setValue("22.9");
        bloodType.setValue("O+");
        birthDate.setValue("15/08/1990");
        gender.setValue("Nam");
        medicalHistory.setValue("Dị ứng phấn hoa, Tiền sử đau dạ dày.");
        metricsTracked.setValue(5);
        remindersCreated.setValue(12);
        recordsEntered.setValue(27);
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
}
