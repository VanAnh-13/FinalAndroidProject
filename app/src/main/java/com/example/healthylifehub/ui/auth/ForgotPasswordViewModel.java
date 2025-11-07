package com.example.healthylifehub.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.data.repository.AuthRepository;

/**
 * ForgotPasswordViewModel - Handles password reset logic
 * Implements UC-HLH-01 security requirements
 * Follows MVVM architecture by delegating to AuthRepository
 */
public class ForgotPasswordViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> emailTrigger = new MutableLiveData<>();

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application.getApplicationContext());
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    /**
     * Get reset password state as LiveData
     * Uses Transformations.switchMap to react to email trigger changes
     */
    public LiveData<DataState<Void>> getResetPasswordState() {
        return Transformations.switchMap(emailTrigger, email -> {
            if (email == null || email.isEmpty()) {
                MutableLiveData<DataState<Void>> emptyState = new MutableLiveData<>();
                emptyState.setValue(DataState.error("Email không được để trống"));
                return emptyState;
            }
            return authRepository.sendPasswordResetEmail(email);
        });
    }

    /**
     * Send password reset email
     * Validates email and triggers repository call
     * @param email User's email address
     */
    public void sendPasswordResetEmail(String email) {
        // Clear previous errors
        emailError.setValue(null);
        
        // Validate email
        if (email == null || email.trim().isEmpty()) {
            emailError.setValue("Vui lòng nhập email");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.setValue("Email không hợp lệ");
            return;
        }
        
        // Trigger repository call through LiveData transformation
        emailTrigger.setValue(email);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up repository resources
        if (authRepository != null) {
            authRepository.cleanup();
        }
    }
}
