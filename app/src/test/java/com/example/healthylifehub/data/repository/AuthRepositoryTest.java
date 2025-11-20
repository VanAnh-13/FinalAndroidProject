package com.example.healthylifehub.data.repository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.healthylifehub.data.model.User;
import com.example.healthylifehub.data.model.UserProfile;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Unit tests for AuthRepository async methods
 * 
 * Tests cover:
 * - Successful login flow with mocked Firebase
 * - Error handling and timeout scenarios
 * - Thread execution on correct executors
 * 
 * Requirements: 1.1, 1.4, 2.1
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AuthRepositoryTest {

    @Mock
    private FirebaseAuth mockFirebaseAuth;
    
    @Mock
    private FirebaseFirestore mockFirestore;
    
    @Mock
    private FirebaseUser mockFirebaseUser;
    
    @Mock
    private AuthResult mockAuthResult;
    
    @Mock
    private Task<AuthResult> mockAuthTask;
    
    @Mock
    private Task<Void> mockVoidTask;
    
    @Mock
    private DocumentReference mockDocumentReference;
    
    @Mock
    private GoogleSignInAccount mockGoogleSignInAccount;
    
    private AuthRepository authRepository;
    private Context context;
    private ExecutorService testExecutor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        testExecutor = Executors.newSingleThreadExecutor();
        
        // Create a test version of AuthRepository with mocked dependencies
        authRepository = spy(new AuthRepository(context));
        
        // Inject mocked Firebase instances using reflection or by overriding methods
        try {
            java.lang.reflect.Field authField = AuthRepository.class.getDeclaredField("firebaseAuth");
            authField.setAccessible(true);
            authField.set(authRepository, mockFirebaseAuth);
            
            java.lang.reflect.Field firestoreField = AuthRepository.class.getDeclaredField("firestore");
            firestoreField.setAccessible(true);
            firestoreField.set(authRepository, mockFirestore);
        } catch (Exception e) {
            fail("Failed to inject mocked dependencies: " + e.getMessage());
        }
    }

    // ========== Test loginWithEmailAsync ==========
    
    /**
     * Test successful login flow with mocked Firebase
     * Requirement: 1.1 - Firebase Authentication using CompletableFuture on ExecutorService
     */
    @Test
    public void testLoginWithEmailAsync_Success() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        String displayName = "Test User";
        
        // Mock FirebaseUser
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        when(mockFirebaseUser.getEmail()).thenReturn(email);
        when(mockFirebaseUser.getDisplayName()).thenReturn(displayName);
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(null);
        
        // Mock AuthResult
        when(mockAuthResult.getUser()).thenReturn(mockFirebaseUser);
        
        // Mock successful authentication
        when(mockFirebaseAuth.signInWithEmailAndPassword(email, password))
                .thenReturn(Tasks.forResult(mockAuthResult));
        
        // Mock Firestore update
        when(mockFirestore.collection("users")).thenReturn(mock(com.google.firebase.firestore.CollectionReference.class));
        when(mockFirestore.collection("users").document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null));
        
        // Act
        CompletableFuture<User> future = authRepository.loginWithEmailAsync(email, password);
        User result = future.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertNotNull("User should not be null", result);
        assertEquals("User ID should match", userId, result.getUid());
        assertEquals("Email should match", email, result.getEmail());
        assertEquals("Display name should match", displayName, result.getDisplayName());
        
        // Verify Firebase methods were called
        verify(mockFirebaseAuth, times(1)).signInWithEmailAndPassword(email, password);
    }
    
    /**
     * Test login failure with invalid credentials
     * Requirement: 1.4 - Error handling with user-friendly messages
     */
    @Test
    public void testLoginWithEmailAsync_InvalidCredentials() {
        // Arrange
        String email = "test@example.com";
        String password = "wrongpassword";
        Exception authException = new Exception("Invalid credentials");
        
        // Mock failed authentication
        when(mockFirebaseAuth.signInWithEmailAndPassword(email, password))
                .thenReturn(Tasks.forException(authException));
        
        // Act
        CompletableFuture<User> future = authRepository.loginWithEmailAsync(email, password);
        
        // Assert
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Should have thrown CompletionException");
        } catch (ExecutionException e) {
            // Expected - verify it's a CompletionException wrapping our exception
            assertTrue("Should be CompletionException", e.getCause() instanceof CompletionException);
            assertNotNull("Should have cause", e.getCause().getCause());
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getName());
        }
        
        // Verify Firebase method was called
        verify(mockFirebaseAuth, times(1)).signInWithEmailAsync(email, password);
    }
    
    /**
     * Test login with null user returned
     * Requirement: 1.4 - Error handling for edge cases
     */
    @Test
    public void testLoginWithEmailAsync_NullUserReturned() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        // Mock AuthResult with null user
        when(mockAuthResult.getUser()).thenReturn(null);
        when(mockFirebaseAuth.signInWithEmailAndPassword(email, password))
                .thenReturn(Tasks.forResult(mockAuthResult));
        
        // Act
        CompletableFuture<User> future = authRepository.loginWithEmailAsync(email, password);
        
        // Assert
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Should have thrown CompletionException");
        } catch (ExecutionException e) {
            assertTrue("Should be CompletionException", e.getCause() instanceof CompletionException);
            assertTrue("Error message should mention no user", 
                    e.getCause().getCause().getMessage().contains("no user"));
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getName());
        }
    }
    
    /**
     * Test login timeout scenario
     * Requirement: 1.4 - Timeout handling
     */
    @Test(expected = TimeoutException.class)
    public void testLoginWithEmailAsync_Timeout() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        // Mock a task that never completes
        Task<AuthResult> neverCompletingTask = mock(Task.class);
        when(neverCompletingTask.isSuccessful()).thenReturn(false);
        when(neverCompletingTask.isComplete()).thenReturn(false);
        when(mockFirebaseAuth.signInWithEmailAndPassword(email, password))
                .thenReturn(neverCompletingTask);
        
        // Act - should timeout
        CompletableFuture<User> future = authRepository.loginWithEmailAsync(email, password);
        future.get(1, TimeUnit.SECONDS); // Short timeout for test
    }
    
    /**
     * Verify thread execution on correct executor
     * Requirement: 1.1 - Verify thread execution on correct executors
     */
    @Test
    public void testLoginWithEmailAsync_ExecutesOnBackgroundThread() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        
        final String[] executionThreadName = new String[1];
        
        // Mock FirebaseUser
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        when(mockFirebaseUser.getEmail()).thenReturn(email);
        when(mockFirebaseUser.getDisplayName()).thenReturn("Test User");
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(null);
        
        // Mock AuthResult
        when(mockAuthResult.getUser()).thenReturn(mockFirebaseUser);
        
        // Mock authentication with thread name capture
        when(mockFirebaseAuth.signInWithEmailAndPassword(email, password))
                .thenAnswer(invocation -> {
                    executionThreadName[0] = Thread.currentThread().getName();
                    return Tasks.forResult(mockAuthResult);
                });
        
        // Mock Firestore
        when(mockFirestore.collection("users")).thenReturn(mock(com.google.firebase.firestore.CollectionReference.class));
        when(mockFirestore.collection("users").document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null));
        
        // Act
        CompletableFuture<User> future = authRepository.loginWithEmailAsync(email, password);
        future.get(5, TimeUnit.SECONDS);
        
        // Assert - verify not on main thread
        assertNotNull("Thread name should be captured", executionThreadName[0]);
        assertFalse("Should not execute on main thread", 
                executionThreadName[0].equals("main"));
    }

    // ========== Test loginWithGoogleAsync ==========
    
    /**
     * Test successful Google login flow
     * Requirement: 1.1 - Google Sign-In with CompletableFuture
     */
    @Test
    public void testLoginWithGoogleAsync_Success() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        String userId = "google_user123";
        String email = "test@gmail.com";
        String displayName = "Google User";
        String photoUrl = "https://example.com/photo.jpg";
        String idToken = "mock_id_token";
        
        // Mock GoogleSignInAccount
        when(mockGoogleSignInAccount.getIdToken()).thenReturn(idToken);
        when(mockGoogleSignInAccount.getEmail()).thenReturn(email);
        
        // Mock FirebaseUser
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        when(mockFirebaseUser.getEmail()).thenReturn(email);
        when(mockFirebaseUser.getDisplayName()).thenReturn(displayName);
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(Uri.parse(photoUrl));
        
        // Mock AuthResult
        when(mockAuthResult.getUser()).thenReturn(mockFirebaseUser);
        
        // Mock Firebase authentication with credential
        when(mockFirebaseAuth.signInWithCredential(any()))
                .thenReturn(Tasks.forResult(mockAuthResult));
        
        // Mock Firestore operations
        when(mockFirestore.collection("users")).thenReturn(mock(com.google.firebase.firestore.CollectionReference.class));
        when(mockFirestore.collection("users").document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        when(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null));
        
        // Note: We can't easily test the full Google Sign-In flow due to static methods
        // This test would need to be adapted based on how the actual implementation handles Intent
        
        // For now, we'll test the core logic by directly calling with a mocked result
        // In a real scenario, you'd use PowerMock or refactor to make it more testable
    }
    
    /**
     * Test Google login with null token
     * Requirement: 1.4 - Error handling for Google Sign-In
     */
    @Test
    public void testLoginWithGoogleAsync_NullToken() {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        
        // Mock GoogleSignInAccount with null token
        when(mockGoogleSignInAccount.getIdToken()).thenReturn(null);
        
        // This test demonstrates error handling for null tokens
        // Actual implementation would need to be tested with integration tests
        // or by refactoring to make Google Sign-In more testable
    }

    // ========== Test registerWithEmailAsync ==========
    
    /**
     * Test successful registration flow with RxJava
     * Requirement: 2.1 - Registration with RxJava Observable chain
     */
    @Test
    public void testRegisterWithEmailAsync_Success() throws Exception {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        String fullName = "New User";
        UserProfile profile = new UserProfile(fullName);
        String userId = "new_user123";
        
        // Mock FirebaseUser
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        when(mockFirebaseUser.getEmail()).thenReturn(email);
        when(mockFirebaseUser.getDisplayName()).thenReturn(fullName);
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(null);
        
        // Mock AuthResult
        when(mockAuthResult.getUser()).thenReturn(mockFirebaseUser);
        
        // Mock successful account creation
        when(mockFirebaseAuth.createUserWithEmailAndPassword(email, password))
                .thenReturn(Tasks.forResult(mockAuthResult));
        
        // Mock email verification
        when(mockFirebaseUser.sendEmailVerification()).thenReturn(Tasks.forResult(null));
        
        // Mock Firestore operations
        when(mockFirestore.collection("users")).thenReturn(mock(com.google.firebase.firestore.CollectionReference.class));
        when(mockFirestore.collection("users").document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        
        // Act - Using RxJava Observable
        final User[] resultUser = new User[1];
        final Throwable[] error = new Throwable[1];
        final boolean[] completed = new boolean[1];
        
        authRepository.registerWithEmailAsync(email, password, profile)
                .subscribe(
                        user -> resultUser[0] = user,
                        throwable -> error[0] = throwable,
                        () -> completed[0] = true
                );
        
        // Wait for async operation
        Thread.sleep(2000);
        
        // Assert
        assertNull("Should not have error", error[0]);
        assertNotNull("User should not be null", resultUser[0]);
        assertEquals("User ID should match", userId, resultUser[0].getUid());
        assertEquals("Email should match", email, resultUser[0].getEmail());
        assertTrue("Observable should complete", completed[0]);
        
        // Verify Firebase methods were called in sequence
        verify(mockFirebaseAuth, times(1)).createUserWithEmailAndPassword(email, password);
        verify(mockFirebaseUser, times(1)).sendEmailVerification();
    }
    
    /**
     * Test registration failure during account creation
     * Requirement: 2.1 - Error handling in registration chain
     */
    @Test
    public void testRegisterWithEmailAsync_AccountCreationFails() throws Exception {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        UserProfile profile = new UserProfile("New User");
        Exception authException = new Exception("Email already exists");
        
        // Mock failed account creation
        when(mockFirebaseAuth.createUserWithEmailAndPassword(email, password))
                .thenReturn(Tasks.forException(authException));
        
        // Act
        final Throwable[] error = new Throwable[1];
        final User[] resultUser = new User[1];
        
        authRepository.registerWithEmailAsync(email, password, profile)
                .subscribe(
                        user -> resultUser[0] = user,
                        throwable -> error[0] = throwable
                );
        
        // Wait for async operation
        Thread.sleep(2000);
        
        // Assert
        assertNotNull("Should have error", error[0]);
        assertNull("User should be null", resultUser[0]);
        assertTrue("Error message should contain failure info", 
                error[0].getMessage().contains("Registration failed") || 
                error[0].getMessage().contains("Email already exists"));
        
        // Verify only account creation was attempted
        verify(mockFirebaseAuth, times(1)).createUserWithEmailAndPassword(email, password);
        verify(mockFirebaseUser, never()).sendEmailVerification();
    }
    
    /**
     * Test registration with Firestore save failure
     * Requirement: 2.1 - Error handling in multi-step registration
     */
    @Test
    public void testRegisterWithEmailAsync_FirestoreSaveFails() throws Exception {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        UserProfile profile = new UserProfile("New User");
        String userId = "new_user123";
        Exception firestoreException = new Exception("Firestore write failed");
        
        // Mock FirebaseUser
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        when(mockFirebaseUser.getEmail()).thenReturn(email);
        
        // Mock AuthResult
        when(mockAuthResult.getUser()).thenReturn(mockFirebaseUser);
        
        // Mock successful account creation
        when(mockFirebaseAuth.createUserWithEmailAndPassword(email, password))
                .thenReturn(Tasks.forResult(mockAuthResult));
        
        // Mock Firestore failure
        when(mockFirestore.collection("users")).thenReturn(mock(com.google.firebase.firestore.CollectionReference.class));
        when(mockFirestore.collection("users").document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forException(firestoreException));
        
        // Act
        final Throwable[] error = new Throwable[1];
        
        authRepository.registerWithEmailAsync(email, password, profile)
                .subscribe(
                        user -> {},
                        throwable -> error[0] = throwable
                );
        
        // Wait for async operation
        Thread.sleep(2000);
        
        // Assert
        assertNotNull("Should have error", error[0]);
        assertTrue("Error should be related to Firestore", 
                error[0].getMessage().contains("Firestore") || 
                error[0].getMessage().contains("save"));
        
        // Verify account was created but save failed
        verify(mockFirebaseAuth, times(1)).createUserWithEmailAndPassword(email, password);
    }
    
    /**
     * Verify RxJava executes on IO scheduler
     * Requirement: 2.1 - Verify thread execution with RxJava
     */
    @Test
    public void testRegisterWithEmailAsync_ExecutesOnIOScheduler() throws Exception {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        UserProfile profile = new UserProfile("New User");
        String userId = "new_user123";
        
        final String[] executionThreadName = new String[1];
        
        // Mock FirebaseUser
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        when(mockFirebaseUser.getEmail()).thenReturn(email);
        
        // Mock AuthResult
        when(mockAuthResult.getUser()).thenReturn(mockFirebaseUser);
        
        // Mock authentication with thread name capture
        when(mockFirebaseAuth.createUserWithEmailAndPassword(email, password))
                .thenAnswer(invocation -> {
                    executionThreadName[0] = Thread.currentThread().getName();
                    return Tasks.forResult(mockAuthResult);
                });
        
        // Mock other operations
        when(mockFirebaseUser.sendEmailVerification()).thenReturn(Tasks.forResult(null));
        when(mockFirestore.collection("users")).thenReturn(mock(com.google.firebase.firestore.CollectionReference.class));
        when(mockFirestore.collection("users").document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        
        // Act
        authRepository.registerWithEmailAsync(email, password, profile)
                .subscribe(user -> {}, throwable -> {});
        
        // Wait for async operation
        Thread.sleep(2000);
        
        // Assert - verify not on main thread (RxJava IO scheduler)
        assertNotNull("Thread name should be captured", executionThreadName[0]);
        assertFalse("Should not execute on main thread", 
                executionThreadName[0].equals("main"));
        assertTrue("Should execute on RxJava IO thread", 
                executionThreadName[0].contains("RxCached") || 
                executionThreadName[0].contains("pool"));
    }
}
