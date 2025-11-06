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

        // Validate required fields
        if (fullName.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_SHORT).show();
            getBinding().etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_email, Toast.LENGTH_SHORT).show();
            getBinding().etEmail.requestFocus();
            return;
        }

        // Update ViewModel
        if (!height.isEmpty()) {
            viewModel.updateHeight(height + " cm");
        }
        if (!weight.isEmpty()) {
            viewModel.updateWeight(weight + " kg");
        }
        if (!bloodType.isEmpty()) {
            viewModel.updateBloodType(bloodType);
        }
        if (!birthDate.isEmpty()) {
            viewModel.updateBirthDate(birthDate);
        }
        viewModel.updateGender(gender);
        viewModel.updateMedicalHistory(medicalHistory);

        // Calculate and update BMI if both height and weight are provided
        if (!height.isEmpty() && !weight.isEmpty()) {
            try {
                double heightInMeters = Double.parseDouble(height) / 100.0;
                double weightInKg = Double.parseDouble(weight);
                double bmi = weightInKg / (heightInMeters * heightInMeters);
                viewModel.updateBmi(String.format(Locale.getDefault(), "%.1f", bmi));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // TODO: Update Firebase user profile with name and avatar
        // This would require Firebase Storage for avatar upload

        Toast.makeText(this, R.string.profile_updated_successfully, Toast.LENGTH_SHORT).show();

        // Return to previous screen
        finish();
    }
}
