package com.example.healthylifehub.utils.security;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Permission management utility for smart reminder system
 * Handles notification and alarm scheduling permissions
 */
public class PermissionManager {
    
    private static final String TAG = "PermissionManager";
    
    // Permission request codes
    public static final int REQUEST_CODE_NOTIFICATION = 1001;
    
    /**
     * Callback interface for permission results
     */
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }
    
    /**
     * Check if notification permission is granted
     * 
     * @param context Application context
     * @return true if notification permission is granted
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            return notificationManager.areNotificationsEnabled();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking notification permission", e);
            return false;
        }
    }
    
    /**
     * Check if exact alarm permission is granted (Android 12+)
     * 
     * @param context Application context
     * @return true if exact alarm permission is granted
     */
    public static boolean isExactAlarmPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                return alarmManager != null && alarmManager.canScheduleExactAlarms();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking exact alarm permission", e);
                return false;
            }
        }
        return true; // Always available on older versions
    }
    
    /**
     * Check if all required permissions for smart reminders are granted
     * 
     * @param context Application context
     * @return true if all permissions are granted
     */
    public static boolean areAllPermissionsGranted(Context context) {
        return isNotificationPermissionGranted(context) && isExactAlarmPermissionGranted(context);
    }
    
    /**
     * Check if should show notification permission rationale
     * 
     * @param activity Activity context
     * @return true if should show rationale
     */
    public static boolean shouldShowNotificationPermissionRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
        }
        return false;
    }
    
    /**
     * Request notification permission
     * 
     * @param activity Activity context
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                REQUEST_CODE_NOTIFICATION);
        }
    }
    
    /**
     * Handle permission request result
     * 
     * @param requestCode Request code
     * @param permissions Permissions array
     * @param grantResults Grant results array
     * @param callback Callback for result
     */
    public static void handlePermissionResult(int requestCode, String[] permissions, 
                                            int[] grantResults, PermissionCallback callback) {
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Notification permission granted");
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            } else {
                Log.w(TAG, "❌ Notification permission denied");
                if (callback != null) {
                    callback.onPermissionDenied();
                }
            }
        }
    }

    /**
     * Log current permission status for debugging
     * 
     * @param context Application context
     */
    public static void logPermissionStatus(Context context) {
        Log.d(TAG, "📊 Permission Status:");
        Log.d(TAG, "  - Notifications: " + isNotificationPermissionGranted(context));
        Log.d(TAG, "  - Exact Alarms: " + isExactAlarmPermissionGranted(context));
        Log.d(TAG, "  - All Required: " + areAllPermissionsGranted(context));
    }
}
