package com.example.healthylifehub.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.MainActivity;
import com.bumptech.glide.Glide;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.databinding.ActivityLoginBinding;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends BaseActivity<ActivityLoginBinding> {
    private static final String PREFS_NAME = "HealthyLifePrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_EMAIL = "email";

    private LoginViewModel viewModel;
    private SharedPreferences sharedPreferences;

    public LoginActivity() {
        super(ActivityLoginBinding::inflate);
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    viewModel.handleGoogleSignInResult(result.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        loadSavedCredentials();
    }

    @Override
    public void bindData() {
        // Load Google icon on main thread
        loadGoogleIcon();
        // Observe ViewModel after UI is ready
        observeViewModel();
    }

    private void loadGoogleIcon() {
        Glide.with(this)
                .load("https://www.google.com/images/branding/googleg/1x/googleg_standard_color_128dp.png")
                .into(getBinding().ivGoogleIcon);
    }

    @Override
    public void setOnClick() {
        // Setup all click listeners
        setupUI();
    }

    private void setupUI() {
        // Back button
        getBinding().ivBack.setOnClickListener(v -> handleBack());

        // Login button
        getBinding().btnLogin.setOnClickListener(v -> handleLogin());

        // Google Sign-In button
        getBinding().btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());

        // Forgot password
        getBinding().tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Sign up link
        getBinding().tvSignUp.setOnClickListener(v -> handleSignUp());

        // Clear errors when user starts typing
        getBinding().etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().tilEmail.setError(null);
            }
        });

        getBinding().etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().tilPassword.setError(null);
            }
        });
    }

    private void observeViewModel() {
        // Observe login state
        viewModel.getLoginState().observe(this, state -> {
            if (state.getStatus() == DataState.Status.LOADING) {
                showLoading(true);
            } else if (state.getStatus() == DataState.Status.SUCCESS) {
                showLoading(false);
                String email = getBinding().etEmail.getText().toString().trim();
                com.example.healthylifehub.utils.security.SecurityUtils.resetLoginAttempts(this, email);
                saveCredentials();
                navigateToDashboard();
            } else if (state.getStatus() == DataState.Status.ERROR) {
                showLoading(false);
                String email = getBinding().etEmail.getText().toString().trim();
                com.example.healthylifehub.utils.security.SecurityUtils.recordFailedLoginAttempt(this, email);
                
                int remainingAttempts = com.example.healthylifehub.utils.security.SecurityUtils.getRemainingAttempts(this, email);
                String errorMessage = state.getMessage();
                if (remainingAttempts > 0 && remainingAttempts <= 3) {
                    errorMessage += "\nCòn " + remainingAttempts + " lần thử";
                }
                showError(errorMessage);
            }
        });

        // Observe email error
        viewModel.getEmailError().observe(this, error -> {
            getBinding().tilEmail.setError(error);
        });

        // Observe password error
        viewModel.getPasswordError().observe(this, error -> {
            getBinding().tilPassword.setError(error);
        });
    }

    private void handleLogin() {
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();
        
        // Sanitize email input
        email = com.example.healthylifehub.utils.security.SecurityUtils.sanitizeEmail(email);
        
        // Check if account is locked
        if (com.example.healthylifehub.utils.security.SecurityUtils.isAccountLocked(this, email)) {
            int remainingMinutes = com.example.healthylifehub.utils.security.SecurityUtils.getRemainingLockoutMinutes(this, email);
            String message = getString(R.string.account_locked, remainingMinutes);
            showError(message);
            return;
        }
        
        // Attempt login
        viewModel.login(email, password);
    }

    private void handleGoogleSignIn() {
        Intent signInIntent = viewModel.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    private void handleSignUp() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void handleBack() {
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showLoading(boolean isLoading) {
        getBinding().btnLogin.setEnabled(!isLoading);
        getBinding().btnGoogleSignIn.setEnabled(!isLoading);
        getBinding().progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        getBinding().btnLogin.setText(isLoading ? "" : getString(R.string.login_button));
    }

    private void showError(String message) {
        Snackbar.make(getBinding().getRoot(), message != null ? message : getString(R.string.error_login_failed),
                Snackbar.LENGTH_LONG).show();
    }

    private void loadSavedCredentials() {
        boolean rememberLogin = sharedPreferences.getBoolean(KEY_REMEMBER_LOGIN, false);
        getBinding().cbRememberLogin.setChecked(rememberLogin);

        if (rememberLogin) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            getBinding().etEmail.setText(savedEmail);
        }
    }

    private void saveCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean rememberLogin = getBinding().cbRememberLogin.isChecked();
        
        editor.putBoolean(KEY_REMEMBER_LOGIN, rememberLogin);
        if (rememberLogin) {
            editor.putString(KEY_EMAIL, getBinding().etEmail.getText().toString().trim());
        } else {
            editor.remove(KEY_EMAIL);
        }
        editor.apply();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
