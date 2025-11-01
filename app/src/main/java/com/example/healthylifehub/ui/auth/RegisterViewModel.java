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

public class RegisterViewModel extends BaseViewModel {
    private final AuthRepository authRepository;
    private final CompositeDisposable compositeDisposable;
    private final MutableLiveData<DataState<User>> registerState = new MutableLiveData<>();
    private final MutableLiveData<String> nameError = new MutableLiveData<>();
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> termsAccepted = new MutableLiveData<>(false);

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.compositeDisposable = new CompositeDisposable();
    }

    public LiveData<DataState<User>> getRegisterState() {
        return registerState;
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

    public void register(String name, String email, String password, String confirmPassword, boolean termsAccepted) {
        if (!validateInput(name, email, password, confirmPassword, termsAccepted)) {
            return;
        }

        // Observe the LiveData from repository
        authRepository.registerWithEmail(name, email, password).observeForever(state -> {
            registerState.setValue(state);
        });
    }

    public Intent getGoogleSignInIntent() {
        return authRepository.getGoogleSignInIntent();
    }

    public void handleGoogleSignInResult(Intent data) {
        // Observe the LiveData from repository
        authRepository.loginWithGoogle(data).observeForever(state -> {
            registerState.setValue(state);
        });
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword, boolean termsAccepted) {
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            nameError.setValue("Vui lòng nhập họ tên");
            isValid = false;
        } else {
            nameError.setValue(null);
        }

        if (TextUtils.isEmpty(email)) {
            emailError.setValue("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.setValue("Email không hợp lệ");
            isValid = false;
        } else {
            emailError.setValue(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordError.setValue("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            passwordError.setValue("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        } else {
            passwordError.setValue(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordError.setValue("Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordError.setValue("Mật khẩu không khớp");
            isValid = false;
        } else {
            confirmPasswordError.setValue(null);
        }

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
