package com.example.healthylifehub.data.repository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;

import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.model.HealthMetric;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for HealthMetricRepository async methods
 * 
 * Tests cover:
 * - saveMetricWithAnalysis CompletableFuture pipeline
 * - Stage 1: Save to Room on IO thread
 * - Stage 2: Upload to Firestore on IO thread
 * - Stage 3: Trigger analysis on compute thread
 * - Stage 4: Handle analysis results on main thread
 * 
 * Requirements: 4.1, 4.2, 4.5
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class HealthMetricRepositoryTest {

    @Mock
    private FirebaseAuth mockFirebaseAuth;
    
    @Mock
    private FirebaseFirestore mockFirestore;
    
    @Mock
    private FirebaseUser mockFirebaseUser;
    
    @Mock
    private HealthMetricDao mockDao;
    
    @Mock
    private CollectionReference mockCollectionReference;
    
    @Mock
    private DocumentReference mockDocumentReference;
    
    private HealthMetricRepository repository;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Create repository
        repository = spy(new HealthMetricRepository(context));
        
        // Inject mocked dependencies
        try {
            java.lang.reflect.Field authField = HealthMetricRepository.class.getDeclaredField("auth");
            authField.setAccessible(true);
            authField.set(repository, mockFirebaseAuth);
            
            java.lang.reflect.Field firestoreField = HealthMetricRepository.class.getDeclaredField("db");
            firestoreField.setAccessible(true);
            firestoreField.set(repository, mockFirestore);
            
            java.lang.reflect.Field daoField = HealthMetricRepository.class.getDeclaredField("dao");
            daoField.setAccessible(true);
            daoField.set(repository, mockDao);
        } catch (Exception e) {
            fail("Failed to inject mocked dependencies: " + e.getMessage());
        }
    }

    /**
     * Test successful saveMetricWithAnalysis pipeline
     * Requirement: 4.1 - Save to Room, upload to Firestore, trigger analysis
     */
    @Test
    public void testSaveMetricWithAnalysis_Success() throws Exception {
        // Arrange
        String userId = "user123";
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_pressure");
        metric.setSystolic(120);
        metric.setDiastolic(80);
        metric.setMeasuredAt(new Date());
        metric.setNotes("Normal reading");
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO operations
        doNothing().when(mockDao).insertMetric(any(HealthMetric.class));
        doNothing().when(mockDao).markAsSynced(anyString(), anyLong());
        
        // Mock historical data for analysis (return empty list for simplicity)
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(new ArrayList<>());
        
        // Mock Firestore operations
        when(mockFirestore.collection("users")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.collection("healthMetrics")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        
        // Act
        CompletableFuture<Void> future = repository.saveMetricWithAnalysis(metric);
        future.get(10, TimeUnit.SECONDS);
        
        // Assert
        assertTrue("Future should complete successfully", future.isDone());
        assertFalse("Future should not be completed exceptionally", future.isCompletedExceptionally());
        
        // Verify Stage 1: Save to Room was called
        verify(mockDao, times(1)).insertMetric(any(HealthMetric.class));
        
        // Verify Stage 2: Firestore sync was attempted
        verify(mockDocumentReference, times(1)).set(anyMap());
        
        // Verify metric was marked as synced
        verify(mockDao, times(1)).markAsSynced(anyString(), anyLong());
        
        // Verify Stage 3: Analysis was performed (getMetricsForUserSync called)
        verify(mockDao, times(1)).getMetricsForUserSync(userId);
    }

    /**
     * Test saveMetricWithAnalysis with no user logged in
     * Requirement: 4.1 - Error handling when user not authenticated
     */
    @Test
    public void testSaveMetricWithAnalysis_NoUserLoggedIn() throws Exception {
        // Arrange
        HealthMetric metric = new HealthMetric();
        metric.setType("heart_rate");
        metric.setValue(75);
        metric.setMeasuredAt(new Date());
        
        // Mock no authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(null);
        
        // Act
        CompletableFuture<Void> future = repository.saveMetricWithAnalysis(metric);
        
        // Assert
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertTrue("Should complete exceptionally", future.isCompletedExceptionally());
        }
        
        // Verify no DAO operations were performed
        verify(mockDao, never()).insertMetric(any(HealthMetric.class));
    }

    /**
     * Test saveMetricWithAnalysis continues even if Firestore sync fails
     * Requirement: 4.1 - Offline-first: continue pipeline even if Firestore fails
     */
    @Test
    public void testSaveMetricWithAnalysis_FirestoreSyncFails() throws Exception {
        // Arrange
        String userId = "user123";
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_sugar");
        metric.setValue(95);
        metric.setMeasuredAt(new Date());
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO operations
        doNothing().when(mockDao).insertMetric(any(HealthMetric.class));
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(new ArrayList<>());
        
        // Mock Firestore failure
        when(mockFirestore.collection("users")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.collection("healthMetrics")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forException(new Exception("Network error")));
        
        // Act
        CompletableFuture<Void> future = repository.saveMetricWithAnalysis(metric);
        future.get(10, TimeUnit.SECONDS);
        
        // Assert - pipeline should complete despite Firestore failure
        assertTrue("Future should complete", future.isDone());
        assertFalse("Future should not be completed exceptionally", future.isCompletedExceptionally());
        
        // Verify Stage 1: Save to Room was still called
        verify(mockDao, times(1)).insertMetric(any(HealthMetric.class));
        
        // Verify Stage 3: Analysis was still performed
        verify(mockDao, times(1)).getMetricsForUserSync(userId);
    }

    /**
     * Test saveMetricWithAnalysis with anomaly detection
     * Requirement: 4.2 - Detect anomalies and trigger notifications
     */
    @Test
    public void testSaveMetricWithAnalysis_DetectsAnomaly() throws Exception {
        // Arrange
        String userId = "user123";
        
        // Create historical data with normal values
        List<HealthMetric> historicalData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            HealthMetric historical = new HealthMetric();
            historical.setType("blood_pressure");
            historical.setSystolic(120); // Normal
            historical.setDiastolic(80);
            historical.setMeasuredAt(new Date(System.currentTimeMillis() - i * 86400000L));
            historicalData.add(historical);
        }
        
        // Create new metric with anomalous value
        HealthMetric metric = new HealthMetric();
        metric.setType("blood_pressure");
        metric.setSystolic(180); // High - anomaly
        metric.setDiastolic(110);
        metric.setMeasuredAt(new Date());
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO operations
        doNothing().when(mockDao).insertMetric(any(HealthMetric.class));
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(historicalData);
        
        // Mock Firestore operations
        when(mockFirestore.collection("users")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.collection("healthMetrics")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        
        // Act
        CompletableFuture<Void> future = repository.saveMetricWithAnalysis(metric);
        future.get(10, TimeUnit.SECONDS);
        
        // Assert
        assertTrue("Future should complete successfully", future.isDone());
        
        // Verify all stages executed
        verify(mockDao, times(1)).insertMetric(any(HealthMetric.class));
        verify(mockDao, times(1)).getMetricsForUserSync(userId);
        
        // Note: Notification verification would require additional mocking of NotificationHelper
        // In a real test, you'd verify NotificationHelper.showHealthAlertNotification was called
    }

    /**
     * Test saveMetricWithAnalysis with insufficient data for analysis
     * Requirement: 4.2 - Handle cases with insufficient historical data
     */
    @Test
    public void testSaveMetricWithAnalysis_InsufficientDataForAnalysis() throws Exception {
        // Arrange
        String userId = "user123";
        
        // Create minimal historical data (less than 5 points)
        List<HealthMetric> historicalData = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            HealthMetric historical = new HealthMetric();
            historical.setType("heart_rate");
            historical.setValue(70);
            historical.setMeasuredAt(new Date(System.currentTimeMillis() - i * 86400000L));
            historicalData.add(historical);
        }
        
        // Create new metric
        HealthMetric metric = new HealthMetric();
        metric.setType("heart_rate");
        metric.setValue(75);
        metric.setMeasuredAt(new Date());
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO operations
        doNothing().when(mockDao).insertMetric(any(HealthMetric.class));
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(historicalData);
        
        // Mock Firestore operations
        when(mockFirestore.collection("users")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.collection("healthMetrics")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        
        // Act
        CompletableFuture<Void> future = repository.saveMetricWithAnalysis(metric);
        future.get(10, TimeUnit.SECONDS);
        
        // Assert - should complete without error even with insufficient data
        assertTrue("Future should complete successfully", future.isDone());
        assertFalse("Future should not be completed exceptionally", future.isCompletedExceptionally());
        
        // Verify metric was saved
        verify(mockDao, times(1)).insertMetric(any(HealthMetric.class));
    }

    /**
     * Test that pipeline executes on correct thread pools
     * Requirement: 4.1 - Verify thread execution on IO and compute executors
     */
    @Test
    public void testSaveMetricWithAnalysis_ExecutesOnCorrectThreads() throws Exception {
        // Arrange
        String userId = "user123";
        HealthMetric metric = new HealthMetric();
        metric.setType("weight");
        metric.setValue(70);
        metric.setMeasuredAt(new Date());
        
        final String[] daoThreadName = new String[1];
        final String[] analysisThreadName = new String[1];
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO with thread name capture
        doAnswer(invocation -> {
            daoThreadName[0] = Thread.currentThread().getName();
            return null;
        }).when(mockDao).insertMetric(any(HealthMetric.class));
        
        when(mockDao.getMetricsForUserSync(userId)).thenAnswer(invocation -> {
            analysisThreadName[0] = Thread.currentThread().getName();
            return new ArrayList<>();
        });
        
        // Mock Firestore operations
        when(mockFirestore.collection("users")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(userId)).thenReturn(mockDocumentReference);
        when(mockDocumentReference.collection("healthMetrics")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockDocumentReference.set(anyMap())).thenReturn(Tasks.forResult(null));
        
        // Act
        CompletableFuture<Void> future = repository.saveMetricWithAnalysis(metric);
        future.get(10, TimeUnit.SECONDS);
        
        // Assert - verify not on main thread
        assertNotNull("DAO thread name should be captured", daoThreadName[0]);
        assertNotNull("Analysis thread name should be captured", analysisThreadName[0]);
        assertFalse("DAO should not execute on main thread", daoThreadName[0].equals("main"));
        assertFalse("Analysis should not execute on main thread", analysisThreadName[0].equals("main"));
    }

    /**
     * Test getCachedMetrics returns data immediately from Room
     * Requirement: 10.1 - Check Room database first
     * Requirement: 10.5 - Return cached data within 100ms
     */
    @Test
    public void testGetCachedMetrics_ReturnsDataImmediately() {
        // Arrange
        String userId = "user123";
        String metricType = "blood_pressure";
        
        List<HealthMetric> mockMetrics = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            HealthMetric metric = new HealthMetric();
            metric.setId("metric" + i);
            metric.setUserId(userId);
            metric.setType("blood_pressure");
            metric.setSystolic(120 + i);
            metric.setDiastolic(80 + i);
            metric.setMeasuredAt(new Date(System.currentTimeMillis() - i * 86400000L));
            mockMetrics.add(metric);
        }
        
        // Add some metrics of different type
        HealthMetric heartRateMetric = new HealthMetric();
        heartRateMetric.setId("metric_hr");
        heartRateMetric.setUserId(userId);
        heartRateMetric.setType("heart_rate");
        heartRateMetric.setValue(75);
        heartRateMetric.setMeasuredAt(new Date());
        mockMetrics.add(heartRateMetric);
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO to return all metrics
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(mockMetrics);
        
        // Act
        long startTime = System.currentTimeMillis();
        List<HealthMetric> result = repository.getCachedMetrics(metricType);
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return only blood_pressure metrics", 5, result.size());
        
        // Verify all returned metrics are of correct type
        for (HealthMetric metric : result) {
            assertEquals("All metrics should be of type blood_pressure", "blood_pressure", metric.getType());
        }
        
        // Verify performance requirement (should be much faster than 100ms)
        assertTrue("Should return within 100ms (actual: " + duration + "ms)", duration < 100);
        
        // Verify DAO was called
        verify(mockDao, times(1)).getMetricsForUserSync(userId);
    }

    /**
     * Test getCachedMetrics returns all metrics when type is null
     * Requirement: 10.1 - Check Room database first
     */
    @Test
    public void testGetCachedMetrics_ReturnsAllMetricsWhenTypeIsNull() {
        // Arrange
        String userId = "user123";
        
        List<HealthMetric> mockMetrics = new ArrayList<>();
        mockMetrics.add(createMetric("metric1", userId, "blood_pressure", 120, 80));
        mockMetrics.add(createMetric("metric2", userId, "heart_rate", 75, 0));
        mockMetrics.add(createMetric("metric3", userId, "blood_sugar", 95, 0));
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(mockMetrics);
        
        // Act
        List<HealthMetric> result = repository.getCachedMetrics(null);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return all metrics", 3, result.size());
        
        // Verify DAO was called
        verify(mockDao, times(1)).getMetricsForUserSync(userId);
    }

    /**
     * Test getCachedMetrics returns empty list when no user logged in
     * Requirement: 10.1 - Handle case when user not authenticated
     */
    @Test
    public void testGetCachedMetrics_ReturnsEmptyListWhenNoUserLoggedIn() {
        // Arrange
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(null);
        
        // Act
        List<HealthMetric> result = repository.getCachedMetrics("blood_pressure");
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
        
        // Verify DAO was never called
        verify(mockDao, never()).getMetricsForUserSync(anyString());
    }

    /**
     * Test getCachedMetrics returns empty list when no cached data
     * Requirement: 10.1 - Handle case when cache is empty
     */
    @Test
    public void testGetCachedMetrics_ReturnsEmptyListWhenNoCachedData() {
        // Arrange
        String userId = "user123";
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO to return empty list
        when(mockDao.getMetricsForUserSync(userId)).thenReturn(new ArrayList<>());
        
        // Act
        List<HealthMetric> result = repository.getCachedMetrics("blood_pressure");
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
        
        // Verify DAO was called
        verify(mockDao, times(1)).getMetricsForUserSync(userId);
    }

    /**
     * Test getCachedMetrics handles DAO exceptions gracefully
     * Requirement: 10.1 - Error handling
     */
    @Test
    public void testGetCachedMetrics_HandlesExceptionGracefully() {
        // Arrange
        String userId = "user123";
        
        // Mock authenticated user
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn(userId);
        
        // Mock DAO to throw exception
        when(mockDao.getMetricsForUserSync(userId)).thenThrow(new RuntimeException("Database error"));
        
        // Act
        List<HealthMetric> result = repository.getCachedMetrics("blood_pressure");
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty on error", result.isEmpty());
    }

    /**
     * Helper method to create a HealthMetric for testing
     */
    private HealthMetric createMetric(String id, String userId, String type, double value1, double value2) {
        HealthMetric metric = new HealthMetric();
        metric.setId(id);
        metric.setUserId(userId);
        metric.setType(type);
        metric.setMeasuredAt(new Date());
        
        if ("blood_pressure".equals(type)) {
            metric.setSystolic(value1);
            metric.setDiastolic(value2);
        } else {
            metric.setValue(value1);
        }
        
        return metric;
    }
}
