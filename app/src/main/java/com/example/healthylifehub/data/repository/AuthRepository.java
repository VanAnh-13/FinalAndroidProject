package com.example.healthylifehub.data.repository;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.BuildConfig;
import com.example.healthylifehub.base.BaseRepository;
import com.example.healthylifehub.base.DataState;
import com.example.healthylifehub.data.model.User;
import com.example.healthylifehub.data.model.UserProfile;
import com.example.healthylifehub.utils.AsyncErrorLogger;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AuthRepository extends BaseRepository {
    private static final String TAG = "AuthRepository";
    private static final String USERS_COLLECTION = "users";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final GoogleSignInClient googleSignInClient;
    private final CompositeDisposable compositeDisposable;
    private final Context context;

    public AuthRepository(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.compositeDisposable = new CompositeDisposable();

        String webClientId = getWebClientIdFromGoogleServices();
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        this.googleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    /**
     * Extract Web Client ID from BuildConfig
     * The Web Client ID is configured in build.gradle.kts and extracted from google-services.json
     * This is more secure than hardcoding it directly in the code
     */
    private String getWebClientIdFromGoogleServices() {
        return BuildConfig.GOOGLE_WEB_CLIENT_ID;
    }

    public LiveData<DataState<User>> loginWithEmail(String email, String password) {
        MutableLiveData<DataState<User>> result = new MutableLiveData<>();
        result.setValue(DataState.loading());

        compositeDisposable.add(
                Observable.fromCallable(() -> {
                    // Step 1: Authenticate with Firebase
                    Task<AuthResult> task = firebaseAuth.signInWithEmailAndPassword(email, password);
                    Tasks.await(task);
                    
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            return firebaseUser;
                        }
                    }
                    throw new Exception("Login failed - no user returned");
                })
                .flatMap(firebaseUser -> {
                    // Step 2: Update last login time
                    return Observable.fromCallable(() -> {
                        updateLastLoginSync(firebaseUser.getUid());
                        return new User(
                                firebaseUser.getUid(),
                                firebaseUser.getEmail(),
                                firebaseUser.getDisplayName(),
                                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                        );
                    });
                })
                .retryWhen(errors -> errors.zipWith(
                        Observable.range(1, MAX_RETRY_ATTEMPTS),
                        (error, retryCount) -> {
                            Log.w(TAG, "Login attempt " + retryCount + " failed: " + error.getMessage());
                            if (retryCount >= MAX_RETRY_ATTEMPTS) {
                                throw new RuntimeException("Max retry attempts reached", error);
                            }
                            long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount - 1);
                            Log.d(TAG, "Retrying after " + backoffTime + "ms");
                            return retryCount;
                        }
                ).flatMap(retryCount -> {
                    long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount - 1);
                    return Observable.timer(backoffTime, TimeUnit.MILLISECONDS);
                }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        user -> {
                            Log.d(TAG, "Login successful for user: " + user.getEmail());
                            result.setValue(DataState.success(user));
                        },
                        error -> {
                            String errorMessage = error.getMessage() != null ? 
                                    error.getMessage() : "Login failed";
                            Log.e(TAG, "Login failed", error);
                            result.setValue(DataState.error(errorMessage));
                        }
                )
        );

        return result;
    }

    public LiveData<DataState<User>> registerWithEmail(String name, String email, String password) {
        MutableLiveData<DataState<User>> result = new MutableLiveData<>();
        result.setValue(DataState.loading());

        compositeDisposable.add(
                Observable.fromCallable(() -> {
                    // Step 1: Create Firebase Auth account
                    Log.d(TAG, "Step 1: Creating Firebase Auth account");
                    Task<AuthResult> task = firebaseAuth.createUserWithEmailAndPassword(email, password);
                    Tasks.await(task);
                    
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            return firebaseUser;
                        }
                    }
                    throw new Exception("Registration failed - no user returned");
                })
                .flatMap(firebaseUser -> {
                    // Step 2: Create user document in Firestore
                    Log.d(TAG, "Step 2: Creating Firestore document");
                    return Observable.fromCallable(() -> {
                        User user = new User(
                                firebaseUser.getUid(),
                                firebaseUser.getEmail(),
                                name,
                                null
                        );
                        saveUserToFirestoreSync(user);
                        return firebaseUser;
                    });
                })
                .flatMap(firebaseUser -> {
                    // Step 3: Send email verification
                    Log.d(TAG, "Step 3: Sending verification email");
                    return Observable.fromCallable(() -> {
                        Task<Void> emailTask = firebaseUser.sendEmailVerification();
                        Tasks.await(emailTask);
                        
                        return new User(
                                firebaseUser.getUid(),
                                firebaseUser.getEmail(),
                                name,
                                null
                        );
                    });
                })
                .retryWhen(errors -> errors.zipWith(
                        Observable.range(1, MAX_RETRY_ATTEMPTS),
                        (error, retryCount) -> {
                            Log.w(TAG, "Registration attempt " + retryCount + " failed: " + error.getMessage());
                            if (retryCount >= MAX_RETRY_ATTEMPTS) {
                                throw new RuntimeException("Max retry attempts reached", error);
                            }
                            long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount - 1);
                            Log.d(TAG, "Retrying after " + backoffTime + "ms");
                            return retryCount;
                        }
                ).flatMap(retryCount -> {
                    long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(2, retryCount - 1);
                    return Observable.timer(backoffTime, TimeUnit.MILLISECONDS);
                }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        user -> {
                            Log.d(TAG, "Registration successful. Email verification sent to: " + user.getEmail());
                            result.setValue(DataState.success(user));
                        },
                        error -> {
                            String errorMessage = error.getMessage() != null ? 
                                    error.getMessage() : "Registration failed";
                            Log.e(TAG, "Registration failed", error);
                            result.setValue(DataState.error(errorMessage));
                        }
                )
        );

        return result;
    }

    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    public LiveData<DataState<User>> loginWithGoogle(Intent data) {
        MutableLiveData<DataState<User>> result = new MutableLiveData<>();
        result.setValue(DataState.loading());

        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);

            if (account != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    User user = new User(
                                            firebaseUser.getUid(),
                                            firebaseUser.getEmail(),
                                            firebaseUser.getDisplayName(),
                                            firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                                    );
                                    saveUserToFirestore(user);
                                    result.setValue(DataState.success(user));
                                }
                            } else {
                                String errorMessage = authTask.getException() != null ?
                                        authTask.getException().getMessage() : "Google sign-in failed";
                                result.setValue(DataState.error(errorMessage));
                                Log.e(TAG, "Google sign-in failed", authTask.getException());
                            }
                        });
            }
        } catch (ApiException e) {
            result.setValue(DataState.error("Google sign-in failed: " + e.getMessage()));
            Log.e(TAG, "Google sign-in error", e);
        }

        return result;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public LiveData<DataState<Void>> signOut() {
        MutableLiveData<DataState<Void>> result = new MutableLiveData<>();
        result.setValue(DataState.loading());

        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                result.setValue(DataState.success(null));
            } else {
                Log.w(TAG, "Google sign-out failed, but Firebase sign-out was successful.", task.getException());
                result.setValue(DataState.success(null));
            }
        });
        return result;
    }

    public LiveData<DataState<Void>> sendPasswordResetEmail(String email) {
        MutableLiveData<DataState<Void>> result = new MutableLiveData<>();
        result.setValue(DataState.loading());

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result.setValue(DataState.success(null));
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to send password reset email";
                        result.setValue(DataState.error(errorMessage));
                    }
                });
        return result;
    }

    private void saveUserToFirestore(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoURL", user.getPhotoUrl());
        userData.put("role", "user");
        userData.put("createdAt", com.google.firebase.Timestamp.now());
        userData.put("updatedAt", com.google.firebase.Timestamp.now());
        
        // Create empty profile nested object according to Project_Summary.md structure
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
        profile.put("dateOfBirth", null);
        profile.put("gender", "");
        profile.put("height", 0);
        profile.put("weight", 0);
        profile.put("bloodType", "");
        profile.put("medicalHistory", "");
        
        userData.put("profile", profile);

        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore with profile structure"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    /**
     * Synchronous version for RxJava chain
     */
    private void saveUserToFirestoreSync(User user) throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoURL", user.getPhotoUrl());
        userData.put("role", "user");
        userData.put("createdAt", com.google.firebase.Timestamp.now());
        userData.put("updatedAt", com.google.firebase.Timestamp.now());
        
        // Create empty profile nested object according to Project_Summary.md structure
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
        profile.put("dateOfBirth", null);
        profile.put("gender", "");
        profile.put("height", 0);
        profile.put("weight", 0);
        profile.put("bloodType", "");
        profile.put("medicalHistory", "");
        
        userData.put("profile", profile);

        Task<Void> task = firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userData);
        Tasks.await(task);
        
        if (!task.isSuccessful()) {
            throw new Exception("Failed to save user to Firestore");
        }
        Log.d(TAG, "User saved to Firestore with profile structure");
    }

    private void updateLastLogin(String uid) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("lastLogin", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Last login updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating last login", e));
    }

    /**
     * Synchronous version for RxJava chain
     */
    private void updateLastLoginSync(String uid) throws Exception {
        Task<Void> task = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("lastLogin", System.currentTimeMillis());
        Tasks.await(task);
        
        if (!task.isSuccessful()) {
            throw new Exception("Failed to update last login");
        }
        Log.d(TAG, "Last login updated");
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
    }

    // ========== CompletableFuture-based Async Methods ==========
    
    /**
     * Task 3.1: Login with email using CompletableFuture
     * Uses CompletableFuture.supplyAsync() with getIoExecutor()
     * Wraps Firebase signInWithEmailAndPassword with Tasks.await()
     * Converts FirebaseUser to User model on background thread
     * 
     * @param email User's email address
     * @param password User's password
     * @return CompletableFuture<User> containing the authenticated user
     */
    public CompletableFuture<User> loginWithEmailAsync(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Authenticate with Firebase on background thread
                Task<AuthResult> task = firebaseAuth.signInWithEmailAndPassword(email, password);
                Tasks.await(task);
                
                if (!task.isSuccessful() || task.getResult() == null) {
                    throw new Exception("Login failed - authentication unsuccessful");
                }
                
                FirebaseUser firebaseUser = task.getResult().getUser();
                if (firebaseUser == null) {
                    throw new Exception("Login failed - no user returned");
                }
                
                // Step 2: Update last login time
                updateLastLoginSync(firebaseUser.getUid());
                
                // Step 3: Convert FirebaseUser to User model on background thread
                User user = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getEmail(),
                    firebaseUser.getDisplayName(),
                    firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                );
                
                Log.d(TAG, "Login successful for user: " + user.getEmail());
                return user;
                
            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                throw new CompletionException(e);
            }
        }, getIoExecutor())
        .exceptionally(throwable -> {
            // Log error with user context
            AsyncErrorLogger.context()
                .put("email", email)
                .put("operation", "email_login")
                .log("login_with_email", throwable);
            throw new CompletionException(throwable);
        });
    }
    
    /**
     * Task 3.2: Register with email using RxJava Observable chain
     * Creates Observable chain: createUser → createFirestoreDoc → sendVerification
     * Uses Schedulers.io() for background operations
     * Uses AndroidSchedulers.mainThread() for result delivery
     * 
     * @param email User's email address
     * @param password User's password
     * @param profile User's profile information
     * @return Observable<User> that emits the registered user
     */
    public io.reactivex.rxjava3.core.Observable<User> registerWithEmailAsync(
            String email, String password, UserProfile profile) {
        
        return io.reactivex.rxjava3.core.Observable.fromCallable(() -> {
            // Step 1: Create Firebase Auth account
            Log.d(TAG, "Step 1: Creating Firebase Auth account");
            Task<AuthResult> task = firebaseAuth.createUserWithEmailAndPassword(email, password);
            Tasks.await(task);
            
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new Exception("Registration failed - account creation unsuccessful");
            }
            
            FirebaseUser firebaseUser = task.getResult().getUser();
            if (firebaseUser == null) {
                throw new Exception("Registration failed - no user returned");
            }
            
            return firebaseUser;
        })
        .flatMap(firebaseUser -> {
            // Step 2: Create Firestore user document
            Log.d(TAG, "Step 2: Creating Firestore document with profile");
            return io.reactivex.rxjava3.core.Observable.fromCallable(() -> {
                saveUserWithProfileToFirestoreSync(firebaseUser, profile);
                return firebaseUser;
            });
        })
        .flatMap(firebaseUser -> {
            // Step 3: Send email verification
            Log.d(TAG, "Step 3: Sending verification email");
            return io.reactivex.rxjava3.core.Observable.fromCallable(() -> {
                Task<Void> emailTask = firebaseUser.sendEmailVerification();
                Tasks.await(emailTask);
                
                if (!emailTask.isSuccessful()) {
                    Log.w(TAG, "Email verification send failed, but continuing", emailTask.getException());
                }
                
                // Return User model
                User user = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getEmail(),
                    profile.getFullName(),
                    null
                );
                
                Log.d(TAG, "Registration successful. Email verification sent to: " + user.getEmail());
                return user;
            });
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Task 3.3: Login with Google using CompletableFuture
     * Processes Google Sign-In result on background thread
     * Handles token exchange asynchronously
     * 
     * @param data Intent containing Google Sign-In result
     * @return CompletableFuture<User> containing the authenticated user
     */
    public CompletableFuture<User> loginWithGoogleAsync(Intent data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Process Google Sign-In result on background thread
                Log.d(TAG, "Processing Google Sign-In result");
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                
                if (account == null || account.getIdToken() == null) {
                    throw new Exception("Google Sign-In failed - no account or token");
                }
                
                // Step 2: Handle token exchange asynchronously
                Log.d(TAG, "Exchanging Google token for Firebase credential");
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                
                Task<AuthResult> authTask = firebaseAuth.signInWithCredential(credential);
                Tasks.await(authTask);
                
                if (!authTask.isSuccessful() || authTask.getResult() == null) {
                    throw new Exception("Firebase authentication with Google credential failed");
                }
                
                FirebaseUser firebaseUser = authTask.getResult().getUser();
                if (firebaseUser == null) {
                    throw new Exception("Google Sign-In failed - no Firebase user returned");
                }
                
                // Step 3: Save user to Firestore and convert to User model
                User user = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getEmail(),
                    firebaseUser.getDisplayName(),
                    firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                );
                
                saveUserToFirestoreSync(user);
                updateLastLoginSync(firebaseUser.getUid());
                
                Log.d(TAG, "Google Sign-In successful for user: " + user.getEmail());
                return user;
                
            } catch (Exception e) {
                Log.e(TAG, "Google Sign-In failed", e);
                throw new CompletionException(e);
            }
        }, getIoExecutor())
        .exceptionally(throwable -> {
            // Log error with operation context
            AsyncErrorLogger.context()
                .put("operation", "google_login")
                .log("login_with_google", throwable);
            throw new CompletionException(throwable);
        });
    }
    
    /**
     * Helper method: Save user with profile to Firestore synchronously
     * Used in RxJava registration chain
     */
    private void saveUserWithProfileToFirestoreSync(FirebaseUser firebaseUser, UserProfile profile) throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", firebaseUser.getUid());
        userData.put("email", firebaseUser.getEmail());
        userData.put("displayName", profile.getFullName());
        userData.put("photoURL", null);
        userData.put("role", "user");
        userData.put("createdAt", com.google.firebase.Timestamp.now());
        userData.put("updatedAt", com.google.firebase.Timestamp.now());
        
        // Create profile nested object with provided profile data
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("fullName", profile.getFullName() != null ? profile.getFullName() : "");
        profileMap.put("dateOfBirth", profile.getDateOfBirth());
        profileMap.put("gender", profile.getGender() != null ? profile.getGender() : "");
        profileMap.put("height", profile.getHeight());
        profileMap.put("weight", profile.getWeight());
        profileMap.put("bloodType", profile.getBloodType() != null ? profile.getBloodType() : "");
        profileMap.put("medicalHistory", profile.getMedicalHistory() != null ? profile.getMedicalHistory() : "");
        
        userData.put("profile", profileMap);

        Task<Void> task = firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.getUid())
                .set(userData);
        Tasks.await(task);
        
        if (!task.isSuccessful()) {
            throw new Exception("Failed to save user with profile to Firestore");
        }
        Log.d(TAG, "User saved to Firestore with complete profile structure");
    }
}
