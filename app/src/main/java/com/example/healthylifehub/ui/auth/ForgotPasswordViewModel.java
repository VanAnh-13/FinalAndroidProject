package com.example.healthylifehub.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.healthylifehub.base.DataState;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ForgotPasswordViewModel - Handles password reset logic
 * Implements UC-HLH-01 security requirements
 */
public class ForgotPasswordViewModel extends ViewModel {

    private final FirebaseAuth firebaseAuth;
    private final ExecutorService executorService;

    private final MutableLiveData<DataState<Void>> resetPasswordState = new MutableLiveData<>();
    private final MutableLiveData<String> emailError = new MutableLiveData<>();

    public ForgotPasswordViewModel() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<DataState<Void>> getResetPasswordState() {
        return resetPasswordState;
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    /**
     * Send password reset email
     * Uses Firebase Authentication's built-in password reset
     * @param email User's email address
     */
    public void sendPasswordResetEmail(String email) {
        resetPasswordState.setValue(DataState.loading());

        CompletableFuture.supplyAsync(() -> {
            try {
                Task<Void> task = firebaseAuth.sendPasswordResetEmail(email);
                
                // Wait for task to complete
                while (!task.isComplete()) {
                    Thread.sleep(100);
                }

                if (task.isSuccessful()) {
                    return DataState.success(null);
                } else {
                    String errorMessage = task.getException() != null 
                        ? task.getException().getMessage() 
                        : "Không thể gửi email đặt lại mật khẩu";
                    return DataState.<Void>error(errorMessage);
                }
            } catch (Exception e) {
                return DataState.<Void>error(e.getMessage());
            }
        }, executorService).thenAccept(state -> {
            resetPasswordState.postValue((DataState<Void>) state);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
