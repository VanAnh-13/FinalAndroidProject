package com.example.healthylifehub.utils;

import android.content.Context;

import com.example.healthylifehub.R;

import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * Comprehensive validation utility for reminder input validation
 * Implements input validation for deadline selection and reminder data
 * Requirements: 2.2, 8.5
 */
public class ReminderValidator {
    
    private static final String TAG = "ReminderValidator";
    
    // Validation constants
    private static final int MIN_TITLE_LENGTH = 1;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MIN_DEADLINE_DAYS_AHEAD = 1;
    private static final int MAX_DEADLINE_YEARS_AHEAD = 5;
    
    // Time validation patterns
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        public boolean isValid;
        public String errorMessage;
        public int errorCode;
        
        public ValidationResult(boolean isValid, String errorMessage, int errorCode) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null, 0);
        }
        
        public static ValidationResult error(String message, int errorCode) {
            return new ValidationResult(false, message, errorCode);
        }
    }
    
    /**
     * Validate reminder title
     * Requirements: 2.2
     * 
     * @param title The reminder title to validate
     * @return ValidationResult with validation status and error details
     */
    public static ValidationResult validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return ValidationResult.error(
                ErrorHandler.getErrorMessage(ErrorHandler.ERROR_EMPTY_TITLE),
                ErrorHandler.ERROR_EMPTY_TITLE
            );
        }
        
        String trimmedTitle = title.trim();
        
        if (trimmedTitle.length() < MIN_TITLE_LENGTH) {
            return ValidationResult.error(
                "Tiêu đề phải có ít nhất " + MIN_TITLE_LENGTH + " ký tự",
                ErrorHandler.ERROR_EMPTY_TITLE
            );
        }
        
        if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            return ValidationResult.error(
                "Tiêu đề không được vượt quá " + MAX_TITLE_LENGTH + " ký tự",
                ErrorHandler.ERROR_EMPTY_TITLE
            );
        }
        
        // Check for potentially harmful content
        if (containsInvalidCharacters(trimmedTitle)) {
            return ValidationResult.error(
                "Tiêu đề chứa ký tự không hợp lệ",
                ErrorHandler.ERROR_EMPTY_TITLE
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate reminder description
     * 
     * @param description The reminder description to validate
     * @return ValidationResult with validation status and error details
     */
    public static ValidationResult validateDescription(String description) {
        if (description == null) {
            return ValidationResult.success(); // Description is optional
        }
        
        String trimmedDescription = description.trim();
        
        if (trimmedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            return ValidationResult.error(
                "Mô tả không được vượt quá " + MAX_DESCRIPTION_LENGTH + " ký tự",
                ErrorHandler.ERROR_INVALID_DEADLINE
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate deadline selection with comprehensive checks
     * Requirements: 2.2
     * 
     * @param deadline The deadline calendar to validate (can be null)
     * @return ValidationResult with validation status and error details
     */
    public static ValidationResult validateDeadline(Calendar deadline) {
        if (deadline == null) {
            return ValidationResult.success(); // Deadline is optional
        }
        
        Calendar now = Calendar.getInstance();
        Calendar minDeadline = Calendar.getInstance();
        minDeadline.add(Calendar.DAY_OF_MONTH, MIN_DEADLINE_DAYS_AHEAD);
        
        Calendar maxDeadline = Calendar.getInstance();
        maxDeadline.add(Calendar.YEAR, MAX_DEADLINE_YEARS_AHEAD);
        
        // Check if deadline is in the past
        if (deadline.before(now) || deadline.equals(now)) {
            return ValidationResult.error(
                ErrorHandler.getErrorMessage(ErrorHandler.ERROR_DEADLINE_IN_PAST),
                ErrorHandler.ERROR_DEADLINE_IN_PAST
            );
        }
        
        // Check if deadline is too soon (less than 1 day ahead)
        if (deadline.before(minDeadline)) {
            return ValidationResult.error(
                "Thời hạn phải ít nhất " + MIN_DEADLINE_DAYS_AHEAD + " ngày từ bây giờ",
                ErrorHandler.ERROR_INVALID_DEADLINE
            );
        }
        
        // Check if deadline is too far in the future
        if (deadline.after(maxDeadline)) {
            return ValidationResult.error(
                "Thời hạn không được vượt quá " + MAX_DEADLINE_YEARS_AHEAD + " năm từ bây giờ",
                ErrorHandler.ERROR_INVALID_DEADLINE
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate reminder time format and value
     * 
     * @param timeString The time string in HH:mm format
     * @return ValidationResult with validation status and error details
     */
    public static ValidationResult validateTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return ValidationResult.error(
                "Vui lòng chọn thời gian nhắc nhở",
                ErrorHandler.ERROR_INVALID_TIME
            );
        }
        
        String trimmedTime = timeString.trim();
        
        // Check format using regex
        if (!TIME_PATTERN.matcher(trimmedTime).matches()) {
            return ValidationResult.error(
                "Định dạng thời gian không hợp lệ. Sử dụng định dạng HH:mm",
                ErrorHandler.ERROR_INVALID_TIME
            );
        }
        
        // Parse and validate time components
        try {
            String[] parts = trimmedTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            if (hour < 0 || hour > 23) {
                return ValidationResult.error(
                    "Giờ phải từ 00 đến 23",
                    ErrorHandler.ERROR_INVALID_TIME
                );
            }
            
            if (minute < 0 || minute > 59) {
                return ValidationResult.error(
                    "Phút phải từ 00 đến 59",
                    ErrorHandler.ERROR_INVALID_TIME
                );
            }
            
        } catch (NumberFormatException e) {
            return ValidationResult.error(
                "Thời gian chứa ký tự không hợp lệ",
                ErrorHandler.ERROR_INVALID_TIME
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate frequency selection
     * 
     * @param frequency The frequency string to validate
     * @return ValidationResult with validation status and error details
     */
    public static ValidationResult validateFrequency(String frequency) {
        if (frequency == null || frequency.trim().isEmpty()) {
            return ValidationResult.error(
                "Vui lòng chọn tần suất nhắc nhở",
                ErrorHandler.ERROR_INVALID_DEADLINE
            );
        }
        
        String[] validFrequencies = {"once", "daily", "weekly", "monthly"};
        boolean isValid = false;
        
        for (String validFreq : validFrequencies) {
            if (validFreq.equals(frequency.trim())) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            return ValidationResult.error(
                "Tần suất nhắc nhở không hợp lệ",
                ErrorHandler.ERROR_INVALID_DEADLINE
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate complete reminder data
     * 
     * @param title Reminder title
     * @param description Reminder description (optional)
     * @param timeString Time in HH:mm format
     * @param frequency Frequency string
     * @param deadline Deadline calendar (optional)
     * @return ValidationResult with validation status and error details
     */
    public static ValidationResult validateCompleteReminder(String title, String description,
                                                          String timeString, String frequency,
                                                          Calendar deadline) {
        // Validate title
        ValidationResult titleResult = validateTitle(title);
        if (!titleResult.isValid) {
            return titleResult;
        }
        
        // Validate description
        ValidationResult descResult = validateDescription(description);
        if (!descResult.isValid) {
            return descResult;
        }
        
        // Validate time
        ValidationResult timeResult = validateTime(timeString);
        if (!timeResult.isValid) {
            return timeResult;
        }
        
        // Validate frequency
        ValidationResult freqResult = validateFrequency(frequency);
        if (!freqResult.isValid) {
            return freqResult;
        }
        
        // Validate deadline
        ValidationResult deadlineResult = validateDeadline(deadline);
        if (!deadlineResult.isValid) {
            return deadlineResult;
        }
        
        // Additional cross-field validation
        if (deadline != null && "once".equals(frequency)) {
            return ValidationResult.error(
                "Nhắc nhở một lần không cần thời hạn",
                ErrorHandler.ERROR_INVALID_DEADLINE
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate deadline warning threshold (3 days)
     * 
     * @param deadline The deadline to check
     * @return true if deadline is within warning threshold
     */
    public static boolean isDeadlineWithinWarningThreshold(Calendar deadline) {
        if (deadline == null) {
            return false;
        }
        
        Calendar now = Calendar.getInstance();
        Calendar warningThreshold = Calendar.getInstance();
        warningThreshold.add(Calendar.DAY_OF_MONTH, 3);
        
        return deadline.after(now) && deadline.before(warningThreshold);
    }
    
    /**
     * Get deadline warning message
     * 
     * @param deadline The deadline to check
     * @return Warning message or null if no warning needed
     */
    public static String getDeadlineWarningMessage(Calendar deadline) {
        if (!isDeadlineWithinWarningThreshold(deadline)) {
            return null;
        }
        
        Calendar now = Calendar.getInstance();
        long daysUntilDeadline = (deadline.getTimeInMillis() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        
        if (daysUntilDeadline <= 0) {
            return "Nhắc nhở này đã hết hạn";
        } else if (daysUntilDeadline == 1) {
            return "Nhắc nhở này sẽ hết hạn vào ngày mai";
        } else {
            return String.format("Nhắc nhở này sẽ hết hạn trong %d ngày", daysUntilDeadline);
        }
    }
    
    /**
     * Check for invalid characters in text input
     */
    private static boolean containsInvalidCharacters(String text) {
        if (text == null) return false;
        
        // Check for control characters and other potentially harmful content
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        
        // Check for SQL injection patterns (basic check)
        String lowerText = text.toLowerCase();
        String[] sqlKeywords = {"drop", "delete", "insert", "update", "select", "union", "script"};
        for (String keyword : sqlKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Sanitize text input by removing invalid characters
     */
    public static String sanitizeText(String text) {
        if (text == null) return null;
        
        // Remove control characters except newline, carriage return, and tab
        StringBuilder sanitized = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (!Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t') {
                sanitized.append(c);
            }
        }
        
        return sanitized.toString().trim();
    }
}