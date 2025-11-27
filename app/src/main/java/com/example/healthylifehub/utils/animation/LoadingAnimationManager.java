package com.example.healthylifehub.utils.animation;

import android.view.View;

/**
 * Manager for loading animations
 */
public class LoadingAnimationManager {
    
    public static void showLoading(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }
    
    public static void hideLoading(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }
}
