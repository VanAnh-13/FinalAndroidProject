package com.example.healthylifehub.utils.animation;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Manager for fragment transitions
 */
public class FragmentTransitionManager {
    
    public enum TransitionType {
        FADE, SLIDE, NONE
    }
    
    public enum SlideDirection {
        LEFT, RIGHT, UP, DOWN
    }
    
    public static void switchFragment(FragmentManager fragmentManager, Fragment from, Fragment to, TransitionType type) {
        fragmentManager.beginTransaction()
            .hide(from)
            .show(to)
            .commit();
    }
    
    public static void switchFragment(FragmentManager fragmentManager, Fragment from, Fragment to, TransitionType type, SlideDirection direction) {
        switchFragment(fragmentManager, from, to, type);
    }
    
    public static TransitionType getTransitionTypeForBottomNavigation(int fromId, int toId) {
        return TransitionType.NONE;
    }
    
    public static SlideDirection getSlideDirectionForBottomNavigation(int fromId, int toId) {
        return SlideDirection.LEFT;
    }
}
