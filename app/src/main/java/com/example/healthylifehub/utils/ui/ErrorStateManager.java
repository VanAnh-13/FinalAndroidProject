package com.example.healthylifehub.utils.ui;

import android.view.View;

/**
 * Manager for error states - Stub implementation
 */
public class ErrorStateManager {
    
    public enum ErrorType {
        NETWORK_ERROR,
        SERVER_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }
    
    public static void showError(View container, String message) {
        // Empty stub
    }
    
    public static void showError(View container, String message, ErrorType errorType, Runnable retryAction) {
        // Empty stub
    }
    
    public static void hideError(View container) {
        // Empty stub
    }
}
