package com.example.healthylifehub.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Base repository for Firebase operations.
 * Provides common Firebase instances and user information.
 */
public abstract class FirebaseRepository {
    
    protected final FirebaseFirestore db;
    protected final FirebaseAuth auth;
    
    public FirebaseRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Get current user ID
     * @return User ID or null if not logged in
     */
    protected String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Check if user is logged in
     * @return true if user is logged in
     */
    protected boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}

