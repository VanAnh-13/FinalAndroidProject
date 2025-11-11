package com.example.healthylifehub.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.healthylifehub.MainActivity;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.databinding.ActivityRegisterBinding;
import com.google.android.material.snackbar.Snackbar;

public class RegisterActivity extends BaseActivity<ActivityRegisterBinding> {
    private RegisterViewModel viewModel;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    viewModel.handleGoogleSignInResult(result.getData());
                }
            });

    public RegisterActivity() {
        super(ActivityRegisterBinding::inflate);
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
    }

    @Override
    public void bindData() {
        // Load Google icon on main thread
        loadGoogleIcon();
        // Observe ViewModel after UI is ready
        observeViewModel();
    }

    @Override
    public void setOnClick() {
        setupUI();
    }

    private void setupUI() {
        getBinding().ivBack.setOnClickListener(v -> handleBack());

        getBinding().btnRegister.setOnClickListener(v -> handleRegister());

        getBinding().btnGoogleSignUp.setOnClickListener(v -> handleGoogleSignUp());

        getBinding().tvLogin.setOnClickListener(v -> handleLogin());

        getBinding().tvTermsLink.setOnClickListener(v -> handleTermsClick());

        getBinding().cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setTermsAccepted(isChecked);
            updateRegisterButtonState();
        });

        getBinding().etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().tilName.setError(null);
            }
        });

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

        getBinding().etConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().tilConfirmPassword.setError(null);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getRegisterState().observe(this, state -> {
            if (state.getStatus() == DataState.Status.LOADING) {
                showLoading(true);
            } else if (state.getStatus() == DataState.Status.SUCCESS) {
                showLoading(false);
                markOnboardingComplete();
                navigateToDashboard();
            } else if (state.getStatus() == DataState.Status.ERROR) {
                showLoading(false);
                showError(state.getMessage());
            }
        });

        viewModel.getNameError().observe(this, error -> {
            getBinding().tilName.setError(error);
        });

        viewModel.getEmailError().observe(this, error -> {
            getBinding().tilEmail.setError(error);
        });

        viewModel.getPasswordError().observe(this, error -> {
            getBinding().tilPassword.setError(error);
        });

        viewModel.getConfirmPasswordError().observe(this, error -> {
            getBinding().tilConfirmPassword.setError(error);
        });

        viewModel.getTermsAccepted().observe(this, accepted -> {
            updateRegisterButtonState();
        });
    }

    private void loadGoogleIcon() {
        Glide.with(this)
                .load("https://www.google.com/images/branding/googleg/1x/googleg_standard_color_128dp.png")
                .into(getBinding().ivGoogleIcon);
    }

    private void handleRegister() {
        String name = getBinding().etName.getText().toString().trim();
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();
        String confirmPassword = getBinding().etConfirmPassword.getText().toString().trim();
        boolean termsAccepted = getBinding().cbTerms.isChecked();

        if (!termsAccepted) {
            Toast.makeText(this, R.string.error_terms_not_accepted, Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.register(name, email, password, confirmPassword, termsAccepted);
    }

    private void handleGoogleSignUp() {
        Intent signInIntent = viewModel.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleLogin() {
        finish();
    }

    private void handleBack() {
        finish();
    }

    private void handleTermsClick() {
        Toast.makeText(this, "Điều khoản dịch vụ - Coming soon", Toast.LENGTH_SHORT).show();
    }

    private void updateRegisterButtonState() {
        Boolean termsAccepted = viewModel.getTermsAccepted().getValue();
        boolean hasInput = !getBinding().etName.getText().toString().isEmpty()
                && !getBinding().etEmail.getText().toString().isEmpty()
                && !getBinding().etPassword.getText().toString().isEmpty()
                && !getBinding().etConfirmPassword.getText().toString().isEmpty();

        boolean shouldEnable = termsAccepted != null && termsAccepted && hasInput;
        getBinding().btnRegister.setEnabled(shouldEnable);
        getBinding().btnRegister.setAlpha(shouldEnable ? 1.0f : 0.5f);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showLoading(boolean isLoading) {
        getBinding().btnRegister.setEnabled(!isLoading);
        getBinding().btnGoogleSignUp.setEnabled(!isLoading);
        getBinding().progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        getBinding().btnRegister.setText(isLoading ? "" : getString(R.string.register_button));
    }

    private void showError(String message) {
        Snackbar.make(getBinding().getRoot(), message != null ? message : getString(R.string.error_register_failed),
                Snackbar.LENGTH_LONG).show();
    }

    private void markOnboardingComplete() {
        // No longer needed - authentication state is the indicator
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
