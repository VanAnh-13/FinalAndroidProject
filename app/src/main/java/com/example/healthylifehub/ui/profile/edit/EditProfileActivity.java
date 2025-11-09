package com.example.healthylifehub.ui.profile.edit;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.ActivityEditProfileBinding;
import com.example.healthylifehub.ui.profile.fragment.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends BaseActivity<ActivityEditProfileBinding> {

    private ProfileViewModel viewModel;
    private Calendar selectedDate = Calendar.getInstance();
    private Uri selectedAvatarUri;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public EditProfileActivity() {
        super(ActivityEditProfileBinding::inflate);
    }

    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedAvatarUri = uri;
                        loadAvatar(uri);
                    }
                });

        // Setup blood type dropdown
        setupBloodTypeDropdown();

        // Load current user data
        loadCurrentUserData();
    }

    private void setupBloodTypeDropdown() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                bloodTypes
        );
        ((AutoCompleteTextView) getBinding().etBloodType).setAdapter(adapter);
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Load name
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                getBinding().etFullName.setText(displayName);
            }

            // Load email
            String email = currentUser.getEmail();
            if (email != null && !email.isEmpty()) {
                getBinding().etEmail.setText(email);
            }

            // Load avatar
            if (currentUser.getPhotoUrl() != null) {
                loadAvatar(currentUser.getPhotoUrl());
            }
        }
    }

    private void loadAvatar(Uri uri) {
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(getBinding().ivAvatar);
    }

    @Override
    public void bindData() {
        // Observe and bind ViewModel data
        viewModel.getHeight().observe(this, height -> {
            if (height != null && !height.isEmpty()) {
                String numericValue = height.replaceAll("[^0-9.]", "");
                getBinding().etHeight.setText(numericValue);
            }
        });

        viewModel.getWeight().observe(this, weight -> {
            if (weight != null && !weight.isEmpty()) {
                String numericValue = weight.replaceAll("[^0-9.]", "");
                getBinding().etWeight.setText(numericValue);
            }
        });

        viewModel.getBloodType().observe(this, bloodType -> {
            if (bloodType != null && !bloodType.isEmpty()) {
                getBinding().etBloodType.setText(bloodType, false);
            }
        });

        viewModel.getBirthDate().observe(this, birthDate -> {
            if (birthDate != null && !birthDate.isEmpty()) {
                getBinding().etBirthDate.setText(birthDate);
            }
        });

        viewModel.getGender().observe(this, gender -> {
            if (gender != null && !gender.isEmpty()) {
                if (gender.equalsIgnoreCase("Nam") || gender.equalsIgnoreCase("Male")) {
                    getBinding().rbMale.setChecked(true);
                } else if (gender.equalsIgnoreCase("Nữ") || gender.equalsIgnoreCase("Female")) {
                    getBinding().rbFemale.setChecked(true);
                } else {
                    getBinding().rbOther.setChecked(true);
                }
            }
        });

        viewModel.getMedicalHistory().observe(this, history -> {
            if (history != null && !history.isEmpty()) {
                getBinding().etMedicalHistory.setText(history);
            }
        });
        
        // Observe save result
        viewModel.getSaveResult().observe(this, success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(this, R.string.profile_updated_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Lưu thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Observe loading state
        viewModel.getIsSaving().observe(this, isSaving -> {
            if (isSaving != null) {
                // Disable/enable save button based on loading state
                getBinding().tvSave.setEnabled(!isSaving);
                getBinding().tvSave.setAlpha(isSaving ? 0.5f : 1.0f);
                
                // You can also show a progress bar if you have one in the layout
                // getBinding().progressBar.setVisibility(isSaving ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void observeData() {
        // Additional data observation if needed
    }

    @Override
    public void setOnClick() {
        // Cancel button
        getBinding().tvCancel.setOnClickListener(v -> {
            finish();
        });

        // Save button
        getBinding().tvSave.setOnClickListener(v -> {
            saveProfileData();
        });

        // Edit avatar button
        getBinding().fabEditAvatar.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Birth date picker
        getBinding().etBirthDate.setOnClickListener(v -> {
            showDatePicker();
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    getBinding().etBirthDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveProfileData() {
        // Get all input values
        String fullName = getBinding().etFullName.getText().toString().trim();
        String email = getBinding().etEmail.getText().toString().trim();
        String birthDate = getBinding().etBirthDate.getText().toString().trim();
        String height = getBinding().etHeight.getText().toString().trim();
        String weight = getBinding().etWeight.getText().toString().trim();
        String bloodType = getBinding().etBloodType.getText().toString().trim();
        String medicalHistory = getBinding().etMedicalHistory.getText().toString().trim();

        // Get selected gender
        String gender;
        if (getBinding().rbMale.isChecked()) {
            gender = getString(R.string.male);
        } else if (getBinding().rbFemale.isChecked()) {
            gender = getString(R.string.female);
        } else {
            gender = getString(R.string.other);
        }

        // ========== VALIDATION ==========
        
        // 1. Validate full name
        if (fullName.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_SHORT).show();
            getBinding().etFullName.requestFocus();
            return;
        }
        
        if (fullName.length() < 2) {
            Toast.makeText(this, R.string.error_name_too_short, Toast.LENGTH_SHORT).show();
            getBinding().etFullName.requestFocus();
            return;
        }
        
        if (fullName.length() > 100) {
            Toast.makeText(this, R.string.error_name_too_long, Toast.LENGTH_SHORT).show();
            getBinding().etFullName.requestFocus();
            return;
        }

        // 2. Validate email
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_email, Toast.LENGTH_SHORT).show();
            getBinding().etEmail.requestFocus();
            return;
        }
        
        if (!isValidEmail(email)) {
            Toast.makeText(this, R.string.error_invalid_email_format, Toast.LENGTH_SHORT).show();
            getBinding().etEmail.requestFocus();
            return;
        }

        // 3. Validate birth date (if provided)
        if (!birthDate.isEmpty()) {
            if (!isValidBirthDate(birthDate)) {
                Toast.makeText(this, R.string.error_future_birth_date, Toast.LENGTH_SHORT).show();
                getBinding().etBirthDate.requestFocus();
                return;
            }
            
            int age = calculateAge(birthDate);
            if (age < 1 || age > 150) {
                Toast.makeText(this, R.string.error_invalid_age, Toast.LENGTH_SHORT).show();
                getBinding().etBirthDate.requestFocus();
                return;
            }
        }

        // 4. Validate height (if provided)
        if (!height.isEmpty()) {
            try {
                double heightValue = Double.parseDouble(height);
                if (heightValue < 50 || heightValue > 300) {
                    Toast.makeText(this, R.string.error_invalid_height, Toast.LENGTH_SHORT).show();
                    getBinding().etHeight.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_height, Toast.LENGTH_SHORT).show();
                getBinding().etHeight.requestFocus();
                return;
            }
        }

        // 5. Validate weight (if provided)
        if (!weight.isEmpty()) {
            try {
                double weightValue = Double.parseDouble(weight);
                if (weightValue < 10 || weightValue > 500) {
                    Toast.makeText(this, R.string.error_invalid_weight, Toast.LENGTH_SHORT).show();
                    getBinding().etWeight.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_weight, Toast.LENGTH_SHORT).show();
                getBinding().etWeight.requestFocus();
                return;
            }
        }

        // ========== END VALIDATION ==========

        // Prepare data with units
        String heightWithUnit = height.isEmpty() ? "" : height + " cm";
        String weightWithUnit = weight.isEmpty() ? "" : weight + " kg";
        
        // Calculate BMI if both height and weight are provided
        String bmi = "";
        if (!height.isEmpty() && !weight.isEmpty()) {
            try {
                double heightInMeters = Double.parseDouble(height) / 100.0;
                double weightInKg = Double.parseDouble(weight);
                double bmiValue = weightInKg / (heightInMeters * heightInMeters);
                bmi = String.format(Locale.getDefault(), "%.1f", bmiValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // Save to Firestore via ViewModel (async operation)
        // This will trigger the observers in bindData() when complete
        viewModel.saveProfile(
            fullName,
            email,
            birthDate,
            gender,
            heightWithUnit,
            weightWithUnit,
            bloodType,
            medicalHistory,
            bmi
        );
        
        // Also update Firebase Auth profile with name and email
        // Photo URL will be handled separately when avatar upload is implemented
        viewModel.saveProfileWithAuth(fullName, email, null);
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }
    
    /**
     * Validate birth date is not in the future
     */
    private boolean isValidBirthDate(String birthDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);
            java.util.Date date = sdf.parse(birthDate);
            
            if (date == null) {
                return false;
            }
            
            // Check if date is not in the future
            return !date.after(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculate age from birth date
     */
    private int calculateAge(String birthDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            java.util.Date date = sdf.parse(birthDate);
            
            if (date == null) {
                return 0;
            }
            
            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.setTime(date);
            
            Calendar today = Calendar.getInstance();
            
            int age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
            
            // Adjust if birthday hasn't occurred this year yet
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            
            return age;
        } catch (Exception e) {
            return 0;
        }
    }
}
