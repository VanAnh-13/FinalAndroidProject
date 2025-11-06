package com.example.healthylifehub.utils.navigation;

import androidx.fragment.app.Fragment;

/**
 * Command Pattern: Encapsulates the action of switching between fragments.
 * Allows navigation logic to be passed as a dependency.
 */
@FunctionalInterface
public interface FragmentSwitcher {
    /**
     * Switches from one fragment to another.
     *
     * @param from the current fragment
     * @param to   the target fragment
     */
    void switchFragment(Fragment from, Fragment to);
}



