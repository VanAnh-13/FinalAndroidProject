package com.example.healthylifehub;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMainBinding;
import com.example.healthylifehub.ui.dashboard.DashboardFragment;
import com.example.healthylifehub.ui.metrics.list.MetricsFragment;
import com.example.healthylifehub.ui.navigation.MainNavigator;
import com.example.healthylifehub.ui.profile.fragment.ProfileFragment;
import com.example.healthylifehub.ui.records.fragment.RecordsFragment;
import com.example.healthylifehub.ui.reminders.fragment.RemindersFragment;
import com.example.healthylifehub.utils.navigation.FragmentSwitcher;
import com.example.healthylifehub.utils.PermissionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Main activity that serves as the container for bottom navigation and drawer.
 * Delegates navigation logic to {@link MainNavigator} (Command Pattern).
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> implements com.example.healthylifehub.utils.navigation.DrawerController {

    private static final String DEFAULT_USER_NAME = "Jessica Smith";
    private static final String DEFAULT_USER_EMAIL = "jessica.smith@example.com";
    private static final int HEADER_VIEW_INDEX = 0;

    private DashboardFragment dashboardFragment;
    private MetricsFragment metricsFragment;
    private RemindersFragment remindersFragment;
    private RecordsFragment recordsFragment;
    private ProfileFragment profileFragment;

    private Fragment currentFragment;
    private MainNavigator mainNavigator;

    public MainActivity() {
        super(ActivityMainBinding::inflate);
    }

    @Override
    public void initData() {
        initializeFragments();
        addFragmentsToContainer();
        setupNavigator();
    }

    private void initializeFragments() {
        dashboardFragment = new DashboardFragment();
        metricsFragment = new MetricsFragment();
        remindersFragment = new RemindersFragment();
        recordsFragment = new RecordsFragment();
        profileFragment = new ProfileFragment();
    }

    private void addFragmentsToContainer() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, dashboardFragment, "dashboard");
        transaction.add(R.id.fragment_container, metricsFragment, "metrics").hide(metricsFragment);
        transaction.add(R.id.fragment_container, remindersFragment, "reminders").hide(remindersFragment);
        transaction.add(R.id.fragment_container, recordsFragment, "records").hide(recordsFragment);
        transaction.add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment);
        transaction.commit();

        currentFragment = dashboardFragment;
    }

    private void setupNavigator() {
        FragmentSwitcher switcher = this::switchFragment;
        mainNavigator = new MainNavigator(
            this,
            switcher,
            dashboardFragment,
            metricsFragment,
            remindersFragment,
            recordsFragment,
            profileFragment
        );
    }
    
    private void setupNavigationDrawer() {
        View headerView = getBinding().navigationView.getHeaderView(HEADER_VIEW_INDEX);
        ImageView avatarImageView = headerView.findViewById(R.id.nav_header_avatar);
        TextView nameTextView = headerView.findViewById(R.id.nav_header_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_header_email);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            setUserName(nameTextView, currentUser);
            setUserEmail(emailTextView, currentUser);
            loadUserAvatar(avatarImageView, currentUser);
        }
    }

    private void setUserName(TextView nameTextView, FirebaseUser user) {
        String displayName = user.getDisplayName();
        
        if (displayName != null && !displayName.isEmpty()) {
            nameTextView.setText(displayName);
        } else {
            // Load from Firestore if displayName is null
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("displayName");
                            nameTextView.setText(name != null ? name : DEFAULT_USER_NAME);
                        } else {
                            nameTextView.setText(DEFAULT_USER_NAME);
                        }
                    })
                    .addOnFailureListener(e -> {
                        nameTextView.setText(DEFAULT_USER_NAME);
                    });
        }
    }

    private void setUserEmail(TextView emailTextView, FirebaseUser user) {
        String email = user.getEmail();
        emailTextView.setText(email != null ? email : DEFAULT_USER_EMAIL);
    }

    private void loadUserAvatar(ImageView avatarImageView, FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                .load(user.getPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(avatarImageView);
        } else {
            avatarImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public void bindData() {
    }

    @Override
    public void setOnClick() {
        setupBottomNavigation();
        setupDrawerNavigation();
        setupNavigationDrawer();
        
        // Request notification permission for Android 13+
        requestNotificationPermissionIfNeeded();
    }
    
    /**
     * Request POST_NOTIFICATIONS permission for Android 13+
     */
    private void requestNotificationPermissionIfNeeded() {
        if (!PermissionManager.isNotificationPermissionGranted(this)) {
            // Check if should show rationale
            if (PermissionManager.shouldShowNotificationPermissionRationale(this)) {
                showNotificationPermissionRationale();
            } else {
                // Request permission directly
                PermissionManager.requestNotificationPermission(this);
            }
        }
    }
    
    /**
     * Show rationale dialog for notification permission
     */
    private void showNotificationPermissionRationale() {
        new android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_permission_title))
            .setMessage(getString(R.string.notification_permission_message))
            .setPositiveButton(getString(R.string.allow), (dialog, which) -> {
                PermissionManager.requestNotificationPermission(this);
                dialog.dismiss();
            })
            .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                dialog.dismiss();
                // Show info that some features won't work
                android.widget.Toast.makeText(this, 
                    getString(R.string.some_features_disabled), 
                    android.widget.Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        PermissionManager.handlePermissionResult(requestCode, permissions, grantResults, 
            new PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    android.widget.Toast.makeText(MainActivity.this, 
                        getString(R.string.notification_enabled_success), 
                        android.widget.Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onPermissionDenied() {
                    // Show settings guidance
                    showPermissionDeniedGuidance();
                }
            });
    }
    
    /**
     * Show guidance when permission is denied
     */
    private void showPermissionDeniedGuidance() {
        new android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_disabled_title))
            .setMessage(getString(R.string.notification_disabled_message))
            .setPositiveButton(getString(R.string.understood), (dialog, which) -> dialog.dismiss())
            .setNeutralButton(getString(R.string.open_settings), (dialog, which) -> {
                // Open app settings
                android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                android.net.Uri uri = android.net.Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                dialog.dismiss();
            })
            .show();
    }

    private void setupBottomNavigation() {
        getBinding().bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = mainNavigator.getFragmentForBottomItem(item.getItemId());
            if (selectedFragment != null && selectedFragment != currentFragment) {
                switchFragment(currentFragment, selectedFragment);
                currentFragment = selectedFragment;
                return true;
            }
            return false;
        });
        getBinding().bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    private void setupDrawerNavigation() {
        getBinding().navigationView.setNavigationItemSelectedListener(item -> {
            MainNavigator.Outcome outcome = mainNavigator.onDrawerItemSelected(item.getItemId(), currentFragment);
            handleNavigationOutcome(outcome);
            getBinding().drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void handleNavigationOutcome(MainNavigator.Outcome outcome) {
        if (outcome == null || !outcome.handled) {
            return;
        }
        if (outcome.newCurrentFragment != null) {
            currentFragment = outcome.newCurrentFragment;
        }
        if (outcome.bottomNavItemId != 0) {
            getBinding().bottomNavigation.setSelectedItemId(outcome.bottomNavItemId);
        }
    }

    private void switchFragment(Fragment from, Fragment to) {
        getSupportFragmentManager().beginTransaction()
            .hide(from)
            .show(to)
            .commit();
    }

    public void openDrawer() {
        getBinding().drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (getBinding().drawerLayout.isDrawerOpen(GravityCompat.START)) {
            getBinding().drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}
