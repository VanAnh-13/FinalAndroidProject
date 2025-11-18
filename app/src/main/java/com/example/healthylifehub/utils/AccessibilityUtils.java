package com.example.healthylifehub.utils;

import android.view.View;

/**
 * Utility class for accessibility - Stub implementation
 */
public class AccessibilityUtils {
    
    public static void setContentDescription(View view, String description) {
        if (view != null) {
            view.setContentDescription(description);
        }
    }
    
    public static void setAccessibilityRole(View view, String role) {
        // Empty stub
    }
    
    public static void setHeading(View view, boolean isHeading) {
        // Empty stub
    }
    
    public static void setFocusOrder(View first, View second) {
        // Empty stub
    }
    
    public static void makeFocusable(View view) {
        if (view != null) {
            view.setFocusable(true);
        }
    }
    
    public static void announceForAccessibility(View view, String message) {
        if (view != null) {
            view.announceForAccessibility(message);
        }
    }
}
