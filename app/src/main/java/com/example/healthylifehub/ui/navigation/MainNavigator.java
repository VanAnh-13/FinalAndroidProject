package com.example.healthylifehub.ui.navigation;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthylifehub.R;
import com.example.healthylifehub.MainActivity;
import com.example.healthylifehub.ui.dashboard.DashboardFragment;
import com.example.healthylifehub.ui.metrics.list.MetricsFragment;
import com.example.healthylifehub.ui.profile.fragment.ProfileFragment;
import com.example.healthylifehub.ui.profile.settings.SettingsActivity;
import com.example.healthylifehub.ui.records.fragment.RecordsFragment;
import com.example.healthylifehub.ui.reminders.fragment.RemindersFragment;
import com.example.healthylifehub.utils.auth.AuthManager;
import com.example.healthylifehub.utils.navigation.FragmentSwitcher;

/**
 * Navigator class implementing Command Pattern to handle navigation actions.
 * Centralizes navigation logic for bottom navigation and drawer menu.
 */
public final class MainNavigator {

    /**
     * Result object returned after handling a navigation command.
     * Uses Builder pattern for flexible outcomes.
     */
    public static final class Outcome {
        public final boolean handled;
        public final @Nullable Fragment newCurrentFragment;
        public final @Nullable Integer bottomNavItemId;

        private Outcome(boolean handled, @Nullable Fragment newCurrentFragment, @Nullable Integer bottomNavItemId) {
            this.handled = handled;
            this.newCurrentFragment = newCurrentFragment;
            this.bottomNavItemId = bottomNavItemId;
        }

        public static Outcome switched(Fragment fragment, int bottomNavItemId) {
            return new Outcome(true, fragment, bottomNavItemId);
        }

        public static Outcome handled() {
            return new Outcome(true, null, null);
        }

        public static Outcome ignored() {
            return new Outcome(false, null, null);
        }
    }

    private final MainActivity activity;
    private final FragmentSwitcher switcher;
    private final DashboardFragment dashboardFragment;
    private final MetricsFragment metricsFragment;
    private final RemindersFragment remindersFragment;
    private final RecordsFragment recordsFragment;
    private final ProfileFragment profileFragment;

    public MainNavigator(
            MainActivity activity,
            FragmentSwitcher switcher,
            DashboardFragment dashboardFragment,
            MetricsFragment metricsFragment,
            RemindersFragment remindersFragment,
            RecordsFragment recordsFragment,
            ProfileFragment profileFragment
    ) {
        this.activity = activity;
        this.switcher = switcher;
        this.dashboardFragment = dashboardFragment;
        this.metricsFragment = metricsFragment;
        this.remindersFragment = remindersFragment;
        this.recordsFragment = recordsFragment;
        this.profileFragment = profileFragment;
    }

    /**
     * Maps bottom navigation item IDs to fragments.
     *
     * @param itemId the selected bottom navigation item ID
     * @return the corresponding fragment, or null if not found
     */
    public @Nullable Fragment getFragmentForBottomItem(int itemId) {
        switch (itemId) {
            case R.id.nav_dashboard:
                return dashboardFragment;
            case R.id.nav_metrics:
                return metricsFragment;
            case R.id.nav_reminders:
                return remindersFragment;
            case R.id.nav_records:
                return recordsFragment;
            case R.id.nav_profile:
                return profileFragment;
            default:
                return null;
        }
    }

    /**
     * Handles drawer menu item selection using Command Pattern.
     *
     * @param itemId          the selected drawer menu item ID
     * @param currentFragment the currently displayed fragment
     * @return Outcome describing the result (fragment switch, activity launch, etc.)
     */
    public Outcome onDrawerItemSelected(int itemId, Fragment currentFragment) {
        switch (itemId) {
            case R.id.nav_drawer_dashboard:
                if (currentFragment != dashboardFragment) switcher.switchFragment(currentFragment, dashboardFragment);
                return Outcome.switched(dashboardFragment, R.id.nav_dashboard);

            case R.id.nav_drawer_metrics:
                if (currentFragment != metricsFragment) switcher.switchFragment(currentFragment, metricsFragment);
                return Outcome.switched(metricsFragment, R.id.nav_metrics);

            case R.id.nav_drawer_reminders:
                if (currentFragment != remindersFragment) switcher.switchFragment(currentFragment, remindersFragment);
                return Outcome.switched(remindersFragment, R.id.nav_reminders);

            case R.id.nav_drawer_records:
                if (currentFragment != recordsFragment) switcher.switchFragment(currentFragment, recordsFragment);
                return Outcome.switched(recordsFragment, R.id.nav_records);

            case R.id.nav_drawer_quick_actions:
                activity.startActivity(new Intent(activity, com.example.healthylifehub.ui.actions.QuickActionsActivity.class));
                return Outcome.handled();

            case R.id.nav_drawer_profile:
                if (currentFragment != profileFragment) switcher.switchFragment(currentFragment, profileFragment);
                return Outcome.switched(profileFragment, R.id.nav_profile);

            case R.id.nav_drawer_settings:
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                return Outcome.handled();

            case R.id.nav_drawer_help:
                Toast.makeText(activity, activity.getString(R.string.toast_help_support_coming_soon), Toast.LENGTH_SHORT).show();
                return Outcome.handled();

            case R.id.nav_drawer_logout:
                AuthManager.logout(activity);
                return Outcome.handled();

            default:
                return Outcome.ignored();
        }
    }
}



