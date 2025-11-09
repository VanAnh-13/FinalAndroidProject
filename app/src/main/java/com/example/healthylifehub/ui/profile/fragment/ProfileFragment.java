package com.example.healthylifehub.ui.profile.fragment;

import android.content.Intent;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentProfileBinding;
import com.example.healthylifehub.ui.auth.LoginActivity;
import com.example.healthylifehub.ui.profile.settings.SettingsActivity;
import com.example.healthylifehub.ui.profile.edit.EditProfileActivity;
import com.example.healthylifehub.ui.profile.reports.ExportReportsActivity;
import com.example.healthylifehub.ui.notifications.center.NotificationsCenterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileViewModel viewModel;

    public ProfileFragment() {
        super(FragmentProfileBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        // Force create profile structure if it doesn't exist
        // This is a one-time fix for existing users
        forceCreateProfileIfNeeded();
    }
    
    /**
     * Force create profile structure for existing users
     * This ensures old users get the new profile structure
     */
    private void forceCreateProfileIfNeeded() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            // Check if document exists
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Document doesn't exist, create it
                        createUserDocument(userId, currentUser);
                    } else {
                        // Check if profile field exists
                        Object profile = documentSnapshot.get("profile");
                        if (profile == null) {
                            // Profile field doesn't exist, update document
                            updateUserDocumentWithProfile(userId, currentUser);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProfileFragment", "Error checking document", e);
                });
        }
    }
    
    private void createUserDocument(String userId, FirebaseUser user) {
        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("userId", userId);
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoURL", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        userData.put("role", "user");
        userData.put("createdAt", com.google.firebase.Timestamp.now());
        userData.put("updatedAt", com.google.firebase.Timestamp.now());
        
        // Create profile nested object
        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
        profile.put("dateOfBirth", null);
        profile.put("gender", "");
        profile.put("height", 0);
        profile.put("weight", 0);
        profile.put("bloodType", "");
        profile.put("medicalHistory", "");
        
        userData.put("profile", profile);
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener(aVoid -> 
                android.util.Log.d("ProfileFragment", "User document created successfully"))
            .addOnFailureListener(e -> 
                android.util.Log.e("ProfileFragment", "Error creating user document", e));
    }
    
    private void updateUserDocumentWithProfile(String userId, FirebaseUser user) {
        // Create profile nested object
        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
        profile.put("dateOfBirth", null);
        profile.put("gender", "");
        profile.put("height", 0);
        profile.put("weight", 0);
        profile.put("bloodType", "");
        profile.put("medicalHistory", "");
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("profile", profile);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> 
                android.util.Log.d("ProfileFragment", "Profile field added successfully"))
            .addOnFailureListener(e -> 
                android.util.Log.e("ProfileFragment", "Error updating document", e));
    }

    @Override
    public void bindData() {
        // Load user data from Firebase
        loadUserProfile();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload user profile when returning to this fragment
        // This ensures data is refreshed after editing profile
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Reload current user to get latest data
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && getBinding() != null) {
                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (refreshedUser != null) {
                        // Load user name
                        String displayName = refreshedUser.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) {
                            getBinding().tvProfileName.setText(displayName);
                        }

                        // Load user email
                        String email = refreshedUser.getEmail();
                        if (email != null && !email.isEmpty()) {
                            getBinding().tvProfileEmail.setText(email);
                        }

                        // Load profile avatar
                        if (refreshedUser.getPhotoUrl() != null) {
                            Glide.with(this)
                                    .load(refreshedUser.getPhotoUrl())
                                    .circleCrop()
                                    .into(getBinding().ivProfileAvatar);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void observeData() {
        // Observe profile data from ViewModel
        viewModel.getHeight().observe(getViewLifecycleOwner(), height -> {
            getBinding().tvHeight.setText(height);
        });

        viewModel.getWeight().observe(getViewLifecycleOwner(), weight -> {
            getBinding().tvWeight.setText(weight);
        });

        viewModel.getBmi().observe(getViewLifecycleOwner(), bmi -> {
            getBinding().tvBmiValue.setText(bmi);
        });

        viewModel.getBloodType().observe(getViewLifecycleOwner(), bloodType -> {
            getBinding().tvBloodType.setText(bloodType);
        });

        viewModel.getBirthDate().observe(getViewLifecycleOwner(), birthDate -> {
            getBinding().tvBirthDate.setText(birthDate);
        });

        viewModel.getGender().observe(getViewLifecycleOwner(), gender -> {
            getBinding().tvGender.setText(gender);
        });

        viewModel.getMedicalHistory().observe(getViewLifecycleOwner(), history -> {
            getBinding().tvMedicalHistory.setText(history);
        });

        viewModel.getMetricsTracked().observe(getViewLifecycleOwner(), count -> {
            getBinding().tvMetricsTracked.setText(String.valueOf(count));
        });

        viewModel.getRemindersCreated().observe(getViewLifecycleOwner(), count -> {
            getBinding().tvRemindersCreated.setText(String.valueOf(count));
        });

        viewModel.getRecordsEntered().observe(getViewLifecycleOwner(), count -> {
            getBinding().tvRecordsEntered.setText(String.valueOf(count));
        });
    }

    @Override
    public void setOnClick() {
        // Menu button - open drawer
        getBinding().ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.healthylifehub.utils.navigation.DrawerController) {
                ((com.example.healthylifehub.utils.navigation.DrawerController) getActivity()).openDrawer();
            }
        });

        // Settings button
        getBinding().ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        });
        
        // DEBUG: Long press on avatar to force create profile
        getBinding().ivProfileAvatar.setOnLongClickListener(v -> {
            forceCreateProfileIfNeeded();
            Toast.makeText(getContext(), "Đang tạo profile structure...", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Edit profile button
        getBinding().btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        // Edit medical history button
        getBinding().btnEditMedicalHistory.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.example.healthylifehub.ui.profile.history.MedicalHistoryActivity.class);
            startActivity(intent);
        });

        // Export reports action
        getBinding().actionExportReports.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ExportReportsActivity.class);
            startActivity(intent);
        });

        // Manage notifications action
        getBinding().actionManageNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotificationsCenterActivity.class);
            startActivity(intent);
        });

        // Privacy & security action
        getBinding().actionPrivacy.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Privacy & Security coming soon", Toast.LENGTH_SHORT).show();
        });

        // Help action
        getBinding().actionHelp.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Help coming soon", Toast.LENGTH_SHORT).show();
        });

        // Logout action
        getBinding().actionLogout.setOnClickListener(v -> {
            handleLogout();
        });
    }

    private void handleLogout() {
        if (getActivity() != null) {
            com.example.healthylifehub.utils.auth.AuthManager.logout(getActivity());
        }
    }
}
