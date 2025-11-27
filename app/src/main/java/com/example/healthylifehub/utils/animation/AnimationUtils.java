package com.example.healthylifehub.utils.animation;

import android.view.View;

/**
 * Utility class for animations
 */
public class AnimationUtils {
    
    public static void fadeIn(View view) {
        view.setVisibility(View.VISIBLE);
    }
    
    public static void fadeOut(View view) {
        view.setVisibility(View.GONE);
    }
    
    public static void slideIn(View view) {
        view.setVisibility(View.VISIBLE);
    }
    
    public static void slideOut(View view) {
        view.setVisibility(View.GONE);
    }
}
