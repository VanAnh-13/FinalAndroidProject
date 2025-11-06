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
    }

    @Override
    public void bindData() {
        // Load user data from Firebase
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Load user name
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                getBinding().tvProfileName.setText(displayName);
            }

            // Load user email
            String email = currentUser.getEmail();
            if (email != null && !email.isEmpty()) {
                getBinding().tvProfileEmail.setText(email);
            }

            // Load profile avatar
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(getBinding().ivProfileAvatar);
            }
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

        // Edit profile button
        getBinding().btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        // Edit medical history button
        getBinding().btnEditMedicalHistory.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit medical history coming soon", Toast.LENGTH_SHORT).show();
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
