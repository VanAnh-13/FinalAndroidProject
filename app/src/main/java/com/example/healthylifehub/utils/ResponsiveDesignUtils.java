package com.example.healthylifehub.utils;

import android.content.Context;
import android.view.View;

/**
 * Utility class for responsive design - Stub implementation
 */
public class ResponsiveDesignUtils {
    
    public static boolean isTablet(Context context) {
        return false; // Assume phone
    }
    
    public static int getScreenWidthDp(Context context) {
        return context.getResources().getConfiguration().screenWidthDp;
    }
    
    public static String getScreenSizeCategory(Context context) {
        return "normal";
    }
    
    public static void adjustPadding(View view, int basePadding) {
        // Empty stub
    }
}
