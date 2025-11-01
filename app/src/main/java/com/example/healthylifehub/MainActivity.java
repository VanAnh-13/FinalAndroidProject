package com.example.healthylifehub;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityMainBinding;
import com.example.healthylifehub.ui.add.AddFragment;
import com.example.healthylifehub.ui.auth.LoginActivity;
import com.example.healthylifehub.ui.dashboard.DashboardFragment;
import com.example.healthylifehub.ui.metrics.MetricsFragment;
import com.example.healthylifehub.ui.profile.ProfileFragment;
import com.example.healthylifehub.ui.records.RecordsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private DashboardFragment dashboardFragment;
    private MetricsFragment metricsFragment;
    private AddFragment addFragment;
    private RecordsFragment recordsFragment;
    private ProfileFragment profileFragment;
    private Fragment currentFragment;

    public MainActivity() {
        super(ActivityMainBinding::inflate);
    }

    @Override
    public void initData() {
        dashboardFragment = new DashboardFragment();
        metricsFragment = new MetricsFragment();
        addFragment = new AddFragment();
        recordsFragment = new RecordsFragment();
        profileFragment = new ProfileFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, dashboardFragment, "dashboard");
        transaction.add(R.id.fragment_container, metricsFragment, "metrics").hide(metricsFragment);
        transaction.add(R.id.fragment_container, addFragment, "add").hide(addFragment);
        transaction.add(R.id.fragment_container, recordsFragment, "records").hide(recordsFragment);
        transaction.add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment);
        transaction.commit();

        currentFragment = dashboardFragment;
        
        setupNavigationDrawer();
    }
    
    private void setupNavigationDrawer() {
        View headerView = getBinding().navigationView.getHeaderView(0);
        ImageView avatarImageView = headerView.findViewById(R.id.nav_header_avatar);
        TextView nameTextView = headerView.findViewById(R.id.nav_header_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_header_email);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            nameTextView.setText(currentUser.getDisplayName() != null ? 
                currentUser.getDisplayName() : "Jessica Smith");
            emailTextView.setText(currentUser.getEmail() != null ? 
                currentUser.getEmail() : "jessica.smith@example.com");
            
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(avatarImageView);
            }
        }
    }

    @Override
    public void bindData() {
    }

    @Override
    public void setOnClick() {
        getBinding().bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = dashboardFragment;
            } else if (itemId == R.id.nav_metrics) {
                selectedFragment = metricsFragment;
            } else if (itemId == R.id.nav_add) {
                selectedFragment = addFragment;
            } else if (itemId == R.id.nav_records) {
                selectedFragment = recordsFragment;
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = profileFragment;
            }

            if (selectedFragment != null && selectedFragment != currentFragment) {
                switchFragment(currentFragment, selectedFragment);
                currentFragment = selectedFragment;
                return true;
            }
            return false;
        });

        getBinding().bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        
        getBinding().navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_drawer_dashboard) {
                switchToFragmentFromDrawer(dashboardFragment, R.id.nav_dashboard);
            } else if (itemId == R.id.nav_drawer_metrics) {
                switchToFragmentFromDrawer(metricsFragment, R.id.nav_metrics);
            } else if (itemId == R.id.nav_drawer_reminders) {
                Toast.makeText(this, "Reminders feature coming soon", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_drawer_records) {
                switchToFragmentFromDrawer(recordsFragment, R.id.nav_records);
            } else if (itemId == R.id.nav_drawer_profile) {
                switchToFragmentFromDrawer(profileFragment, R.id.nav_profile);
            } else if (itemId == R.id.nav_drawer_settings) {
                Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_drawer_help) {
                Toast.makeText(this, "Help & Support feature coming soon", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_drawer_logout) {
                handleLogout();
            }
            
            getBinding().drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    
    private void switchToFragmentFromDrawer(Fragment fragment, int bottomNavItemId) {
        if (fragment != currentFragment) {
            switchFragment(currentFragment, fragment);
            currentFragment = fragment;
            getBinding().bottomNavigation.setSelectedItemId(bottomNavItemId);
        }
    }
    
    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void switchFragment(Fragment from, Fragment to) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(from);
        transaction.show(to);
        transaction.commit();
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
