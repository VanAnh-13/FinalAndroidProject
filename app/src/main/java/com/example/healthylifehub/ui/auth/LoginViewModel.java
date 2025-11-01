package com.example.healthylifehub.ui.auth;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.data.model.User;
import com.example.healthylifehub.data.repository.AuthRepository;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LoginViewModel extends BaseViewModel {
    private final AuthRepository authRepository;
    private final CompositeDisposable compositeDisposable;
    private final MutableLiveData<DataState<User>> loginState = new MutableLiveData<>();
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.compositeDisposable = new CompositeDisposable();
    }

    public LiveData<DataState<User>> getLoginState() {
        return loginState;
    }

    public LiveData<String> getEmailError() {
        return emailError;
    }

    public LiveData<String> getPasswordError() {
        return passwordError;
    }

    public void login(String email, String password) {
        if (!validateInput(email, password)) {
            return;
        }

        // Observe the LiveData from repository
        authRepository.loginWithEmail(email, password).observeForever(state -> {
            loginState.setValue(state);
        });
    }

    public Intent getGoogleSignInIntent() {
        return authRepository.getGoogleSignInIntent();
    }

    public void handleGoogleSignInResult(Intent data) {
        // Observe the LiveData from repository
        authRepository.loginWithGoogle(data).observeForever(state -> {
            loginState.setValue(state);
        });
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailError.setValue("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.setValue("Email không hợp lệ");
            isValid = false;
        } else {
            emailError.setValue(null);
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordError.setValue("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            passwordError.setValue("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        } else {
            passwordError.setValue(null);
        }

        return isValid;
    }

    public void clearErrors() {
        emailError.setValue(null);
        passwordError.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up RxJava disposables
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        if (authRepository != null) {
            authRepository.cleanup();
        }
    }
}
