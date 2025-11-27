package com.example.healthylifehub.ui.auth;

import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.databinding.ActivityForgotPasswordBinding;
import com.example.healthylifehub.utils.security.SecurityUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * ForgotPasswordActivity - Implements password reset functionality
 * Security features:
 * - Email validation
 * - Rate limiting
 * - Secure token generation
 * - User feedback without revealing account existence
 */
public class ForgotPasswordActivity extends BaseActivity<ActivityForgotPasswordBinding> {

    private ForgotPasswordViewModel viewModel;

    public ForgotPasswordActivity() {
        super(ActivityForgotPasswordBinding::inflate);
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);
        observeViewModel();
    }

    @Override
    public void bindData() {
        // Initial data binding if needed
    }

    @Override
    public void setOnClick() {
        // Back button
        getBinding().ivBack.setOnClickListener(v -> finish());

        // Send reset link button
        getBinding().btnSendResetLink.setOnClickListener(v -> handleSendResetLink());

        // Back to login link
        getBinding().tvBackToLogin.setOnClickListener(v -> finish());

        // Clear error when user starts typing
        getBinding().etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getBinding().tilEmail.setError(null);
            }
        });
    }

    private void observeViewModel() {
        // Observe reset password state
        viewModel.getResetPasswordState().observe(this, state -> {
            if (state.getStatus() == DataState.Status.LOADING) {
                showLoading(true);
            } else if (state.getStatus() == DataState.Status.SUCCESS) {
                showLoading(false);
                showSuccessDialog();
            } else if (state.getStatus() == DataState.Status.ERROR) {
                showLoading(false);
                showError(state.getMessage());
            }
        });

        // Observe email error
        viewModel.getEmailError().observe(this, error -> {
            getBinding().tilEmail.setError(error);
        });
    }

    private void handleSendResetLink() {
        String email = getBinding().etEmail.getText().toString().trim();
        
        // Sanitize email input
        email = SecurityUtils.sanitizeEmail(email);
        
        // Validate email
        if (email.isEmpty()) {
            getBinding().tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            getBinding().tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        
        // Send reset link
        viewModel.sendPasswordResetEmail(email);
    }

    private void showLoading(boolean isLoading) {
        getBinding().btnSendResetLink.setEnabled(!isLoading);
        getBinding().progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        getBinding().btnSendResetLink.setText(isLoading ? "" : getString(R.string.send_reset_link));
    }

    private void showError(String message) {
        Snackbar.make(getBinding().getRoot(), 
            message != null ? message : getString(R.string.error_send_reset_link),
            Snackbar.LENGTH_LONG).show();
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.check_email)
            .setMessage(R.string.reset_link_sent)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                dialog.dismiss();
                finish();
            })
            .setCancelable(false)
            .show();
    }
}
