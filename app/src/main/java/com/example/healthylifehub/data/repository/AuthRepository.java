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

    public void signOut() {
        firebaseAuth.signOut();
        googleSignInClient.signOut();
    }

    private void saveUserToFirestore(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoUrl", user.getPhotoUrl());
        userData.put("createdAt", user.getCreatedAt());
        userData.put("lastLogin", user.getLastLogin());

        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    /**
     * Synchronous version for RxJava chain
     */
    private void saveUserToFirestoreSync(User user) throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("photoUrl", user.getPhotoUrl());
        userData.put("createdAt", user.getCreatedAt());
        userData.put("lastLogin", user.getLastLogin());

        Task<Void> task = firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userData);
        Tasks.await(task);
        
        if (!task.isSuccessful()) {
            throw new Exception("Failed to save user to Firestore");
        }
        Log.d(TAG, "User saved to Firestore");
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
}
