package com.example.healthylifehub.utils;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.Reminder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for ProgressCalculator
 * Tests all frequency types, edge cases, and calculation scenarios
 * Requirements: All requirements validation for progress calculation
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ProgressCalculatorTest {
    
    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
    private static final long MILLIS_PER_WEEK = TimeUnit.DAYS.toMillis(7);
    
    private Reminder testReminder;
    private long baseTime;
    
    @Before
    public void setUp() {
        baseTime = System.currentTimeMillis();
        testReminder = new Reminder();
        testReminder.setReminderId("test-reminder-1");
        testReminder.setTitle("Test Reminder");
        testReminder.setDescription("Test Description");
        testReminder.setCreatedAt(baseTime);
        testReminder.setActive(true);
    }
    
    // ========== calculateTotalExpected Tests ==========
    
    @Test
    public void testCalculateTotalExpected_NullReminder_ReturnsZero() {
        int result = ProgressCalculator.calculateTotalExpected(null);
        assertEquals(0, result);
    }
    
    @Test
    public void testCalculateTotalExpected_NoDeadline_ReturnsZero() {
        testReminder.setFrequency("daily");
        testReminder.setDeadline(null);
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(0, result);
    }
    
    @Test
    public void testCalculateTotalExpected_DeadlineBeforeStart_ReturnsZero() {
        testReminder.setFrequency("daily");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime - MILLIS_PER_DAY); // Deadline before start
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(0, result);
    }
    
    @Test
    public void testCalculateTotalExpected_DeadlineEqualsStart_ReturnsZero() {
        testReminder.setFrequency("daily");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime); // Same time
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(0, result);
    }
    
    @Test
    public void testCalculateTotalExpected_OnceFrequency_ReturnsOne() {
        testReminder.setFrequency("once");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (5 * MILLIS_PER_DAY));
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(1, result);
    }
    
    @Test
    public void testCalculateTotalExpected_DailyFrequency_OneDaySpan() {
        testReminder.setFrequency("daily");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + MILLIS_PER_DAY);
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(2, result); // Day 0 and Day 1 (inclusive)
    }
    
    @Test
    public void testCalculateTotalExpected_DailyFrequency_SevenDaySpan() {
        testReminder.setFrequency("daily");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (7 * MILLIS_PER_DAY));
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(8, result); // 8 days inclusive (0-7)
    }
    
    @Test
    public void testCalculateTotalExpected_WeeklyFrequency_OneWeekSpan() {
        testReminder.setFrequency("weekly");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + MILLIS_PER_WEEK);
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(2, result); // Week 0 and Week 1 (inclusive)
    }
    
    @Test
    public void testCalculateTotalExpected_WeeklyFrequency_FourWeekSpan() {
        testReminder.setFrequency("weekly");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (4 * MILLIS_PER_WEEK));
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(5, result); // 5 weeks inclusive (0-4)
    }
    
    @Test
    public void testCalculateTotalExpected_MonthlyFrequency_SameMonth() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(baseTime);
        startCal.set(Calendar.DAY_OF_MONTH, 1); // First day of month
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(baseTime);
        endCal.set(Calendar.DAY_OF_MONTH, 15); // Same month, different day
        
        testReminder.setFrequency("monthly");
        testReminder.setCreatedAt(startCal.getTimeInMillis());
        testReminder.setDeadline(endCal.getTimeInMillis());
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(1, result); // Same month
    }
    
    @Test
    public void testCalculateTotalExpected_MonthlyFrequency_ThreeMonthSpan() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(baseTime);
        startCal.set(Calendar.MONTH, Calendar.JANUARY);
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(baseTime);
        endCal.set(Calendar.MONTH, Calendar.MARCH);
        
        testReminder.setFrequency("monthly");
        testReminder.setCreatedAt(startCal.getTimeInMillis());
        testReminder.setDeadline(endCal.getTimeInMillis());
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(3, result); // January, February, March
    }
    
    @Test
    public void testCalculateTotalExpected_MonthlyFrequency_CrossYear() {
        Calendar startCal = Calendar.getInstance();
        startCal.set(2023, Calendar.NOVEMBER, 1);
        
        Calendar endCal = Calendar.getInstance();
        endCal.set(2024, Calendar.FEBRUARY, 1);
        
        testReminder.setFrequency("monthly");
        testReminder.setCreatedAt(startCal.getTimeInMillis());
        testReminder.setDeadline(endCal.getTimeInMillis());
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(4, result); // Nov 2023, Dec 2023, Jan 2024, Feb 2024
    }
    
    @Test
    public void testCalculateTotalExpected_UnknownFrequency_ReturnsOne() {
        testReminder.setFrequency("unknown");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (5 * MILLIS_PER_DAY));
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(1, result); // Default to once for unknown frequency
    }
    
    @Test
    public void testCalculateTotalExpected_NullFrequency_ReturnsOne() {
        testReminder.setFrequency(null);
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (5 * MILLIS_PER_DAY));
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(1, result);
    }
    
    @Test
    public void testCalculateTotalExpected_CaseInsensitive() {
        testReminder.setFrequency("DAILY");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (2 * MILLIS_PER_DAY));
        
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(3, result); // Should handle uppercase
        
        testReminder.setFrequency("Weekly");
        result = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(1, result); // Should handle mixed case
    }
    
    // ========== calculateProgress Tests ==========
    
    @Test
    public void testCalculateProgress_ZeroTotal_ReturnsZero() {
        float result = ProgressCalculator.calculateProgress(5, 0);
        assertEquals(0f, result, 0.01f);
    }
    
    @Test
    public void testCalculateProgress_NegativeTotal_ReturnsZero() {
        float result = ProgressCalculator.calculateProgress(5, -10);
        assertEquals(0f, result, 0.01f);
    }
    
    @Test
    public void testCalculateProgress_NegativeCompleted_ReturnsZero() {
        float result = ProgressCalculator.calculateProgress(-5, 10);
        assertEquals(0f, result, 0.01f);
    }
    
    @Test
    public void testCalculateProgress_ValidInput_CalculatesCorrectly() {
        float result = ProgressCalculator.calculateProgress(7, 10);
        assertEquals(70f, result, 0.01f);
        
        result = ProgressCalculator.calculateProgress(3, 4);
        assertEquals(75f, result, 0.01f);
        
        result = ProgressCalculator.calculateProgress(1, 3);
        assertEquals(33.33f, result, 0.01f);
    }
    
    @Test
    public void testCalculateProgress_FullCompletion_Returns100() {
        float result = ProgressCalculator.calculateProgress(10, 10);
        assertEquals(100f, result, 0.01f);
    }
    
    @Test
    public void testCalculateProgress_OverCompletion_CappedAt100() {
        float result = ProgressCalculator.calculateProgress(15, 10);
        assertEquals(100f, result, 0.01f);
    }
    
    @Test
    public void testCalculateProgress_ZeroCompleted_ReturnsZero() {
        float result = ProgressCalculator.calculateProgress(0, 10);
        assertEquals(0f, result, 0.01f);
    }
    
    // ========== getProgressColor Tests ==========
    
    @Test
    public void testGetProgressColor_LowProgress_ReturnsRed() {
        int result = ProgressCalculator.getProgressColor(0f);
        assertEquals(R.color.error_red, result);
        
        result = ProgressCalculator.getProgressColor(25f);
        assertEquals(R.color.error_red, result);
        
        result = ProgressCalculator.getProgressColor(49.9f);
        assertEquals(R.color.error_red, result);
    }
    
    @Test
    public void testGetProgressColor_MediumProgress_ReturnsOrange() {
        int result = ProgressCalculator.getProgressColor(50f);
        assertEquals(R.color.warning_orange, result);
        
        result = ProgressCalculator.getProgressColor(65f);
        assertEquals(R.color.warning_orange, result);
        
        result = ProgressCalculator.getProgressColor(79.9f);
        assertEquals(R.color.warning_orange, result);
    }
    
    @Test
    public void testGetProgressColor_HighProgress_ReturnsGreen() {
        int result = ProgressCalculator.getProgressColor(80f);
        assertEquals(R.color.success_green, result);
        
        result = ProgressCalculator.getProgressColor(90f);
        assertEquals(R.color.success_green, result);
        
        result = ProgressCalculator.getProgressColor(100f);
        assertEquals(R.color.success_green, result);
    }
    
    @Test
    public void testGetProgressColor_NegativeProgress_ReturnsRed() {
        int result = ProgressCalculator.getProgressColor(-10f);
        assertEquals(R.color.error_red, result);
    }
    
    @Test
    public void testGetProgressColor_OverHundredProgress_ReturnsGreen() {
        int result = ProgressCalculator.getProgressColor(150f);
        assertEquals(R.color.success_green, result);
    }
    
    // ========== getProgressText Tests ==========
    
    @Test
    public void testGetProgressText_NullReminder_ReturnsEmpty() {
        String result = ProgressCalculator.getProgressText(null);
        assertEquals("", result);
    }
    
    @Test
    public void testGetProgressText_NoDeadline_ReturnsUnlimited() {
        testReminder.setDeadline(null);
        
        String result = ProgressCalculator.getProgressText(testReminder);
        assertEquals("Không giới hạn thời gian", result);
    }
    
    @Test
    public void testGetProgressText_CompletedReminder_ReturnsCompleted() {
        testReminder.setDeadline(baseTime + MILLIS_PER_DAY);
        testReminder.setTotalExpected(10);
        testReminder.setCompletedCount(10);
        
        String result = ProgressCalculator.getProgressText(testReminder);
        assertEquals("Hoàn thành!", result);
    }
    
    @Test
    public void testGetProgressText_ExpiredReminder_ReturnsExpiredFormat() {
        testReminder.setDeadline(baseTime - MILLIS_PER_DAY); // Expired
        testReminder.setTotalExpected(10);
        testReminder.setCompletedCount(7);
        
        String result = ProgressCalculator.getProgressText(testReminder);
        assertEquals("7/10 hoàn thành (70%) - Hết hạn", result);
    }
    
    @Test
    public void testGetProgressText_ActiveReminder_ReturnsNormalFormat() {
        testReminder.setDeadline(baseTime + MILLIS_PER_DAY); // Future deadline
        testReminder.setTotalExpected(10);
        testReminder.setCompletedCount(7);
        
        String result = ProgressCalculator.getProgressText(testReminder);
        assertEquals("7/10 hoàn thành (70%)", result);
    }
    
    // ========== isApproachingDeadline Tests ==========
    
    @Test
    public void testIsApproachingDeadline_NullReminder_ReturnsFalse() {
        boolean result = ProgressCalculator.isApproachingDeadline(null);
        assertFalse(result);
    }
    
    @Test
    public void testIsApproachingDeadline_NoDeadline_ReturnsFalse() {
        testReminder.setDeadline(null);
        
        boolean result = ProgressCalculator.isApproachingDeadline(testReminder);
        assertFalse(result);
    }
    
    @Test
    public void testIsApproachingDeadline_ExpiredDeadline_ReturnsFalse() {
        testReminder.setDeadline(baseTime - MILLIS_PER_DAY);
        
        boolean result = ProgressCalculator.isApproachingDeadline(testReminder);
        assertFalse(result);
    }
    
    @Test
    public void testIsApproachingDeadline_WithinThreeDays_ReturnsTrue() {
        testReminder.setDeadline(baseTime + (2 * MILLIS_PER_DAY)); // 2 days from now
        
        boolean result = ProgressCalculator.isApproachingDeadline(testReminder);
        assertTrue(result);
        
        testReminder.setDeadline(baseTime + (3 * MILLIS_PER_DAY)); // Exactly 3 days
        result = ProgressCalculator.isApproachingDeadline(testReminder);
        assertTrue(result);
    }
    
    @Test
    public void testIsApproachingDeadline_MoreThanThreeDays_ReturnsFalse() {
        testReminder.setDeadline(baseTime + (4 * MILLIS_PER_DAY)); // 4 days from now
        
        boolean result = ProgressCalculator.isApproachingDeadline(testReminder);
        assertFalse(result);
    }
    
    @Test
    public void testIsApproachingDeadline_VeryClose_ReturnsTrue() {
        testReminder.setDeadline(baseTime + (60 * 1000)); // 1 minute from now
        
        boolean result = ProgressCalculator.isApproachingDeadline(testReminder);
        assertTrue(result);
    }
    
    // ========== getDaysUntilDeadline Tests ==========
    
    @Test
    public void testGetDaysUntilDeadline_NullReminder_ReturnsNegativeOne() {
        int result = ProgressCalculator.getDaysUntilDeadline(null);
        assertEquals(-1, result);
    }
    
    @Test
    public void testGetDaysUntilDeadline_NoDeadline_ReturnsNegativeOne() {
        testReminder.setDeadline(null);
        
        int result = ProgressCalculator.getDaysUntilDeadline(testReminder);
        assertEquals(-1, result);
    }
    
    @Test
    public void testGetDaysUntilDeadline_ExpiredDeadline_ReturnsZero() {
        testReminder.setDeadline(baseTime - MILLIS_PER_DAY);
        
        int result = ProgressCalculator.getDaysUntilDeadline(testReminder);
        assertEquals(0, result);
    }
    
    @Test
    public void testGetDaysUntilDeadline_FutureDeadline_ReturnsCorrectDays() {
        testReminder.setDeadline(baseTime + (5 * MILLIS_PER_DAY));
        
        int result = ProgressCalculator.getDaysUntilDeadline(testReminder);
        assertEquals(5, result);
        
        testReminder.setDeadline(baseTime + (1 * MILLIS_PER_DAY));
        result = ProgressCalculator.getDaysUntilDeadline(testReminder);
        assertEquals(1, result);
    }
    
    @Test
    public void testGetDaysUntilDeadline_PartialDay_RoundsDown() {
        // 1.5 days from now should return 1
        testReminder.setDeadline(baseTime + (36 * 60 * 60 * 1000L)); // 36 hours
        
        int result = ProgressCalculator.getDaysUntilDeadline(testReminder);
        assertEquals(1, result);
    }
    
    // ========== Edge Cases and Integration Tests ==========
    
    @Test
    public void testCalculateTotalExpectedByFrequency_DirectMethod() {
        long start = baseTime;
        long end = baseTime + (7 * MILLIS_PER_DAY);
        
        // Test all frequencies directly
        assertEquals(1, ProgressCalculator.calculateTotalExpectedByFrequency(start, end, "once"));
        assertEquals(8, ProgressCalculator.calculateTotalExpectedByFrequency(start, end, "daily"));
        assertEquals(2, ProgressCalculator.calculateTotalExpectedByFrequency(start, end, "weekly"));
        assertEquals(1, ProgressCalculator.calculateTotalExpectedByFrequency(start, end, "unknown"));
        
        // Test null frequency
        assertEquals(0, ProgressCalculator.calculateTotalExpectedByFrequency(start, end, null));
        
        // Test invalid time range
        assertEquals(0, ProgressCalculator.calculateTotalExpectedByFrequency(end, start, "daily"));
    }
    
    @Test
    public void testIntegration_FullReminderLifecycle() {
        // Create a reminder with 10-day span, daily frequency
        testReminder.setFrequency("daily");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + (10 * MILLIS_PER_DAY));
        
        // Calculate total expected
        int totalExpected = ProgressCalculator.calculateTotalExpected(testReminder);
        assertEquals(11, totalExpected); // 11 days inclusive
        
        testReminder.setTotalExpected(totalExpected);
        
        // Test progress at different completion levels
        testReminder.setCompletedCount(0);
        assertEquals(0f, ProgressCalculator.calculateProgress(0, totalExpected), 0.01f);
        assertEquals(R.color.error_red, ProgressCalculator.getProgressColor(0f));
        
        testReminder.setCompletedCount(5);
        float progress = ProgressCalculator.calculateProgress(5, totalExpected);
        assertEquals(45.45f, progress, 0.01f);
        assertEquals(R.color.error_red, ProgressCalculator.getProgressColor(progress));
        
        testReminder.setCompletedCount(8);
        progress = ProgressCalculator.calculateProgress(8, totalExpected);
        assertEquals(72.73f, progress, 0.01f);
        assertEquals(R.color.warning_orange, ProgressCalculator.getProgressColor(progress));
        
        testReminder.setCompletedCount(10);
        progress = ProgressCalculator.calculateProgress(10, totalExpected);
        assertEquals(90.91f, progress, 0.01f);
        assertEquals(R.color.success_green, ProgressCalculator.getProgressColor(progress));
        
        testReminder.setCompletedCount(11);
        progress = ProgressCalculator.calculateProgress(11, totalExpected);
        assertEquals(100f, progress, 0.01f);
        assertEquals(R.color.success_green, ProgressCalculator.getProgressColor(progress));
    }
    
    @Test
    public void testPerformance_LargeTimeSpans() {
        // Test with very large time spans to ensure no performance issues
        long oneYearInMillis = 365 * MILLIS_PER_DAY;
        
        testReminder.setFrequency("daily");
        testReminder.setCreatedAt(baseTime);
        testReminder.setDeadline(baseTime + oneYearInMillis);
        
        long startTime = System.currentTimeMillis();
        int result = ProgressCalculator.calculateTotalExpected(testReminder);
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly (less than 100ms)
        assertTrue("Calculation took too long: " + (endTime - startTime) + "ms", 
                   (endTime - startTime) < 100);
        
        // Should return reasonable result (366 days for leap year consideration)
        assertTrue("Result should be around 365-366: " + result, result >= 365 && result <= 366);
    }
}