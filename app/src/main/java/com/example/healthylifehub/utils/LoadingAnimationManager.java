package com.example.healthylifehub.utils;

import android.view.View;

/**
 * Manager for loading animations - Stub implementation
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
