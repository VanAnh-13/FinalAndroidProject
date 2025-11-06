package com.example.healthylifehub.utils.auth;

import android.app.Activity;
import android.content.Intent;

import com.example.healthylifehub.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Service/Utility class for authentication operations.
 * Centralizes auth logic to avoid duplication (DRY principle).
 */
public final class AuthManager {

    private AuthManager() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Signs out the current user and navigates to the login screen.
     * Clears the activity stack to prevent back navigation.
     *
     * @param activity the activity context
     */
    public static void logout(Activity activity) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}



