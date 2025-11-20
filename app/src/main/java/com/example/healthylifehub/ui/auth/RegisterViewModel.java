package com.example.healthylifehub.ui.auth;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.data.model.User;
import com.example.healthylifehub.data.model.UserProfile;
import com.example.healthylifehub.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * RegisterViewModel - Enhanced with RxJava registration flow
 * Task 14: Create RegisterViewModel with RxJava registration flow
 * 
 * This ViewModel manages user registration state and uses RxJava Observable chains
 * for asynchronous registration operations with proper error handling and cancellation support.
 */
public class RegisterViewModel extends BaseViewModel {
    private static final String TAG = "RegisterViewModel";
    
    private final AuthRepository authRepository;
    private final CompositeDisposable compositeDisposable;
    
    // Task 14.1: Add MutableLiveData<DataState<User>> for registration state
    private final MutableLiveData<DataState<User>> registrationState = new MutableLiveData<>();
    
    private final MutableLiveData<String> nameError = new MutableLiveData<>();
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> termsAccepted = new MutableLiveData<>(false);
    
    // Store observers to remove them later (for backward compatibility)
    private androidx.lifecycle.Observer<DataState<User>> registerObserver;
    private androidx.lifecycle.Observer<DataState<User>> googleSignInObserver;

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        // Task 14.1: Add CompositeDisposable for RxJava cleanup
        this.compositeDisposable = new CompositeDisposable();
    }

    public LiveData<DataState<User>> getRegisterState() {
        return registrationState;
    }
    
    public LiveData<DataState<User>> getRegistrationState() {
        return registrationState;
    }

    public LiveData<String> getNameError() {
        return nameError;
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    public LiveData<String> getPasswordError() {
        return passwordError;
    }

    public LiveData<String> getConfirmPasswordError() {
        return confirmPasswordError;
    }

    public LiveData<Boolean> getTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean accepted) {
        termsAccepted.setValue(accepted);
    }

    /**
     * Task 14.2: Implement register method with RxJava Observable chain
     * Calls authRepository.registerWithEmailAsync()
     * Observes Observable chain with loading, success, error states
     * Updates registrationState LiveData
     * 
     * Task 14.3: Includes input validation before registration
     * - Validates email format
     * - Validates password strength (min 6 chars)
     * - Validates required profile fields
     * 
     * @param name User's full name
     * @param email User's email address
     * @param password User's password
     * @param confirmPassword Password confirmation
     * @param termsAccepted Whether user accepted terms
     */
    public void register(String name, String email, String password, String confirmPassword, boolean termsAccepted) {
        // Task 14.3: Add input validation before registration
        if (!validateInput(name, email, password, confirmPassword, termsAccepted)) {
            return;
        }

        // Task 14.2: Call authRepository.registerWithEmailAsync()
        // Set loading state
        registrationState.setValue(DataState.loading());
        
        // Create UserProfile from name
        UserProfile profile = new UserProfile(name);
        
        // Observe Observable chain with loading, success, error states
        compositeDisposable.add(
            authRepository.registerWithEmailAsync(email, password, profile)
                .subscribe(
                    user -> {
                        // Success: Update registrationState LiveData
                        Log.d(TAG, "Registration successful for user: " + user.getEmail());
                        registrationState.setValue(DataState.success(user));
                    },
                    error -> {
                        // Error: Update registrationState LiveData with error
                        String errorMessage;
                        Throwable cause = error.getCause();
                        if (error instanceof FirebaseAuthUserCollisionException || (cause != null && cause instanceof FirebaseAuthUserCollisionException)) {
                            errorMessage = "Email này đã được sử dụng bởi tài khoản khác.";
                        } else {
                            errorMessage = error.getMessage() != null ? 
                                error.getMessage() : "Đăng ký thất bại";
                        }
                        Log.e(TAG, "Registration failed", error);
                        registrationState.setValue(DataState.error(errorMessage));
                    }
                )
        );
    }
    
    /**
     * Legacy method for backward compatibility
     * Delegates to the new register method
     */
    public void register(String name, String email, String password, String confirmPassword) {
        register(name, email, password, confirmPassword, true);
    }

    public Intent getGoogleSignInIntent() {
        return authRepository.getGoogleSignInIntent();
    }

    public void handleGoogleSignInResult(Intent data) {
        // Remove previous observer if exists to prevent memory leaks
        LiveData<DataState<User>> googleLiveData = authRepository.loginWithGoogle(data);
        if (googleSignInObserver != null) {
            googleLiveData.removeObserver(googleSignInObserver);
        }
        
        // Create new observer and observe
        googleSignInObserver = state -> registrationState.setValue(state);
        googleLiveData.observeForever(googleSignInObserver);
    }

    /**
     * Task 14.3: Add input validation before registration
     * - Validate email format
     * - Validate password strength (min 6 chars)
     * - Validate required profile fields
     */
    private boolean validateInput(String name, String email, String password, String confirmPassword, boolean termsAccepted) {
        boolean isValid = true;

        // Validate required profile field: name
        if (TextUtils.isEmpty(name)) {
            nameError.setValue("Vui lòng nhập họ tên");
            isValid = false;
        } else if (name.trim().length() < 2) {
            nameError.setValue("Họ tên phải có ít nhất 2 ký tự");
            isValid = false;
        } else {
            nameError.setValue(null);
        }

        // Validate email format
        if (TextUtils.isEmpty(email)) {
            emailError.setValue("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.setValue("Email không hợp lệ");
            isValid = false;
        } else {
            emailError.setValue(null);
        }

        // Validate password strength (min 6 chars)
        if (TextUtils.isEmpty(password)) {
            passwordError.setValue("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            passwordError.setValue("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        } else {
            passwordError.setValue(null);
        }

        // Validate password confirmation
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordError.setValue("Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordError.setValue("Mật khẩu không khớp");
            isValid = false;
        } else {
            confirmPasswordError.setValue(null);
        }

        // Validate terms acceptance
        if (!termsAccepted) {
            isValid = false;
        }

        return isValid;
    }

    public void clearErrors() {
        nameError.setValue(null);
        emailError.setValue(null);
        passwordError.setValue(null);
        confirmPasswordError.setValue(null);
    }
    
    /**
     * Task 14.4: Add cancel registration functionality
     * Disposes of ongoing Observable chain
     * Resets registration state
     */
    public void cancelRegistration() {
        Log.d(TAG, "Cancelling registration");
        
        // Dispose of ongoing Observable chain
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        
        // Reset registration state
        registrationState.setValue(null);
        
        Log.d(TAG, "Registration cancelled successfully");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        Log.d(TAG, "ViewModel cleared - cleaning up resources");
        
        // Remove LiveData observers to prevent memory leaks (backward compatibility)
        if (registerObserver != null) {
            registerObserver = null;
        }
        if (googleSignInObserver != null) {
            googleSignInObserver = null;
        }
        
        // Task 14.1: Clean up RxJava disposables
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
            Log.d(TAG, "CompositeDisposable cleared");
        }
        
        // Clean up AuthRepository resources
        if (authRepository != null) {
            authRepository.cleanup();
        }
    }
}
