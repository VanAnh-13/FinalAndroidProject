package com.example.healthylifehub.ui.profile.privacy;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityPrivacySecurityBinding;
import com.example.healthylifehub.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * PrivacySecurityActivity - Manage user privacy and security settings
 * Follows project pattern: BaseActivity + ViewModel + ViewBinding
 */
public class PrivacySecurityActivity extends BaseActivity<ActivityPrivacySecurityBinding> {
    
    private static final String TAG = "PrivacySecurity";
    private PrivacySecurityViewModel viewModel;
    
    public PrivacySecurityActivity() {
        super(ActivityPrivacySecurityBinding::inflate);
    }
    
    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(PrivacySecurityViewModel.class);
        
        // Load current settings
        viewModel.loadPrivacySettings();
    }
    
    @Override
    public void bindData() {
        // Setup toolbar
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quyền riêng tư & Bảo mật");
        }
        
        // Load current user info
        loadCurrentUserInfo();
        
        // Observe ViewModel
        viewModel.getIsLoading().observe(this, isLoading -> {
            getBinding().progressBar.setVisibility(isLoading ? 
                android.view.View.VISIBLE : android.view.View.GONE);
        });
        
        viewModel.getPrivacySettings().observe(this, settings -> {
            if (settings != null) {
                updateUI(settings);
            }
        });
        
        viewModel.getActionResult().observe(this, result -> {
            if (result != null) {
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void setOnClick() {
        // Toolbar back button
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
        
        // Privacy Settings
        getBinding().switchDataCollection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updateDataCollectionEnabled(isChecked);
        });
        
        getBinding().switchPersonalizedAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updatePersonalizedAdsEnabled(isChecked);
        });
        
        getBinding().switchAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updateAnalyticsEnabled(isChecked);
        });
        
        getBinding().switchLocationServices.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.updateLocationServicesEnabled(isChecked);
        });
        
        // Security Actions
        getBinding().actionChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        
        getBinding().actionTwoFactor.setOnClickListener(v -> {
            Toast.makeText(this, "Two-Factor Authentication coming soon", Toast.LENGTH_SHORT).show();
        });
        
        getBinding().actionLoginHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginHistoryActivity.class);
            startActivity(intent);
        });
        
        getBinding().actionConnectedApps.setOnClickListener(v -> {
            Toast.makeText(this, "Connected Apps management coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Data Management
        getBinding().actionExportData.setOnClickListener(v -> showExportDataDialog());
        
        getBinding().actionDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        
        // Legal
        getBinding().actionPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(this, LegalDocumentActivity.class);
            intent.putExtra("document_type", "privacy_policy");
            startActivity(intent);
        });
        
        getBinding().actionTermsOfService.setOnClickListener(v -> {
            Intent intent = new Intent(this, LegalDocumentActivity.class);
            intent.putExtra("document_type", "terms_of_service");
            startActivity(intent);
        });
    }
    
    /**
     * Load current user information
     */
    private void loadCurrentUserInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            getBinding().tvUserEmail.setText(currentUser.getEmail());
            getBinding().tvUserName.setText(currentUser.getDisplayName());
            
            // Show last sign in time
            long lastSignIn = currentUser.getMetadata().getLastSignInTimestamp();
            if (lastSignIn > 0) {
                String lastSignInText = "Đăng nhập lần cuối: " + 
                    android.text.format.DateUtils.getRelativeTimeSpanString(lastSignIn);
                getBinding().tvLastSignIn.setText(lastSignInText);
            }
        }
    }
    
    /**
     * Update UI with privacy settings
     */
    private void updateUI(PrivacySettings settings) {
        getBinding().switchDataCollection.setChecked(settings.isDataCollectionEnabled());
        getBinding().switchPersonalizedAds.setChecked(settings.isPersonalizedAdsEnabled());
        getBinding().switchAnalytics.setChecked(settings.isAnalyticsEnabled());
        getBinding().switchLocationServices.setChecked(settings.isLocationServicesEnabled());
    }
    
    /**
     * Show change password dialog
     */
    private void showChangePasswordDialog() {
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        
        com.google.android.material.textfield.TextInputEditText etCurrentPassword = 
            dialogView.findViewById(R.id.etCurrentPassword);
        com.google.android.material.textfield.TextInputEditText etNewPassword = 
            dialogView.findViewById(R.id.etNewPassword);
        com.google.android.material.textfield.TextInputEditText etConfirmPassword = 
            dialogView.findViewById(R.id.etConfirmPassword);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Đổi mật khẩu")
            .setView(dialogView)
            .setPositiveButton("Đổi mật khẩu", null) // Will override onclick
            .setNegativeButton("Hủy", (d, which) -> d.dismiss())
            .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String currentPassword = etCurrentPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                
                // Validate inputs
                if (currentPassword.isEmpty()) {
                    etCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
                    return;
                }
                
                if (newPassword.isEmpty()) {
                    etNewPassword.setError("Vui lòng nhập mật khẩu mới");
                    return;
                }
                
                if (newPassword.length() < 6) {
                    etNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
                    return;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    etConfirmPassword.setError("Xác nhận mật khẩu không khớp");
                    return;
                }
                
                // Change password via ViewModel
                viewModel.changePassword(currentPassword, newPassword);
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    /**
     * Show export data confirmation dialog
     */
    private void showExportDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Xuất dữ liệu cá nhân")
            .setMessage("Bạn có muốn xuất toàn bộ dữ liệu cá nhân không?\n\n" +
                       "Bao gồm:\n" +
                       "• Thông tin hồ sơ\n" +
                       "• Chỉ số sức khỏe\n" +
                       "• Nhắc nhở\n" +
                       "• Hồ sơ y tế\n\n" +
                       "File sẽ được gửi qua email trong 24-48h.")
            .setPositiveButton("Xuất dữ liệu", (dialog, which) -> {
                viewModel.requestDataExport();
                dialog.dismiss();
            })
            .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    /**
     * Show delete account confirmation dialog
     */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle("⚠️ Xóa tài khoản")
            .setMessage("CẢNH BÁO: Hành động này không thể hoàn tác!\n\n" +
                       "Xóa tài khoản sẽ:\n" +
                       "• Xóa vĩnh viễn toàn bộ dữ liệu\n" +
                       "• Hủy tất cả nhắc nhở\n" +
                       "• Xóa hồ sơ y tế\n" +
                       "• Không thể khôi phục\n\n" +
                       "Bạn có chắc chắn muốn tiếp tục?")
            .setPositiveButton("XÓA TÀI KHOẢN", (dialog, which) -> {
                // Show second confirmation
                showFinalDeleteConfirmation();
                dialog.dismiss();
            })
            .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    /**
     * Show final delete confirmation with password
     */
    private void showFinalDeleteConfirmation() {
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_delete_account, null);
        
        com.google.android.material.textfield.TextInputEditText etPassword = 
            dialogView.findViewById(R.id.etPassword);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("⚠️ Xác nhận cuối cùng")
            .setView(dialogView)
            .setPositiveButton("XÓA VĨNH VIỄN", null)
            .setNegativeButton("Hủy", (d, which) -> d.dismiss())
            .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            // Make positive button red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                getResources().getColor(R.color.error_red, getTheme()));
            
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = etPassword.getText().toString().trim();
                
                if (password.isEmpty()) {
                    etPassword.setError("Vui lòng nhập mật khẩu để xác nhận");
                    return;
                }
                
                // Delete account via ViewModel
                viewModel.deleteAccount(password);
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
}