package com.example.healthylifehub.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * PermissionManager - Centralized permission handling
 * Follows project pattern: Utils for reusable components
 */
public class PermissionManager {
    
    private static final String TAG = "PermissionManager";
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    /**
     * Check if POST_NOTIFICATIONS permission is granted
     * @param context Application context
     * @return true if granted or not needed (Android < 13)
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        // Pre-Android 13: notifications enabled by default
        return true;
    }
    
    /**
     * Request POST_NOTIFICATIONS permission (Android 13+)
     * @param activity Activity to request permission from
     * @return true if permission already granted, false if request initiated
     */
    public static boolean requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isNotificationPermissionGranted(activity)) {
                Log.d(TAG, "✅ POST_NOTIFICATIONS already granted");
                return true;
            }
            
            Log.d(TAG, "🔔 Requesting POST_NOTIFICATIONS permission");
            ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST_CODE
            );
            return false;
        }
        
        // Pre-Android 13: no permission needed
        Log.d(TAG, "✅ POST_NOTIFICATIONS not required (Android < 13)");
        return true;
    }
    
    /**
     * Check if user should be shown rationale for notification permission
     * @param activity Activity context
     * @return true if rationale should be shown
     */
    public static boolean shouldShowNotificationPermissionRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            );
        }
        return false;
    }
    
    /**
     * Handle permission result for notifications
     * @param requestCode Request code from onRequestPermissionsResult
     * @param permissions Permissions array
     * @param grantResults Grant results array
     * @param callback Callback for permission result
     */
    public static void handlePermissionResult(
            int requestCode,
            String[] permissions,
            int[] grantResults,
            PermissionCallback callback
    ) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ POST_NOTIFICATIONS permission granted");
                callback.onPermissionGranted();
            } else {
                Log.w(TAG, "❌ POST_NOTIFICATIONS permission denied");
                callback.onPermissionDenied();
            }
        }
    }
    
    /**
     * Callback interface for permission results
     */
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }
}