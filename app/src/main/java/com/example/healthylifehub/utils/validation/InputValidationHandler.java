package com.example.healthylifehub.utils.validation;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import com.example.healthylifehub.R;
import com.example.healthylifehub.utils.ui.UserFeedbackManager;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Comprehensive input validation handler with clear user feedback
 * Provides real-time validation and user-friendly error messages
 */
public class InputValidationHandler {
    
    // Email pattern for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    );
    
    // Phone pattern for validation
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{10,13}$"
    );
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private boolean isValid;
        private List<String> errors;
        
        public ValidationResult() {
            this.isValid = true;
            this.errors = new ArrayList<>();
        }
        
        public void addError(String error) {
            this.isValid = false;
            this.errors.add(error);
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
    }
    
    /**
     * Validate required text field
     */
    public static boolean validateRequired(Context context, TextInputLayout textInputLayout, 
                                         String fieldName) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return false;
        }
        
        String value = textInputLayout.getEditText().getText().toString().trim();
        
        if (TextUtils.isEmpty(value)) {
            textInputLayout.setError(context.getString(R.string.error_required_field, fieldName));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Validate email field
     */
    public static boolean validateEmail(Context context, TextInputLayout textInputLayout) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return false;
        }
        
        String email = textInputLayout.getEditText().getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            textInputLayout.setError(context.getString(R.string.error_empty_email));
            return false;
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            textInputLayout.setError(context.getString(R.string.error_invalid_email_format));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Validate password field
     */
    public static boolean validatePassword(Context context, TextInputLayout textInputLayout) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return false;
        }
        
        String password = textInputLayout.getEditText().getText().toString();
        
        if (TextUtils.isEmpty(password)) {
            textInputLayout.setError(context.getString(R.string.error_empty_password));
            return false;
        }
        
        if (password.length() < 6) {
            textInputLayout.setError(context.getString(R.string.error_short_password));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Validate numeric range
     */
    public static boolean validateNumericRange(Context context, TextInputLayout textInputLayout, 
                                             double min, double max, String fieldName) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return false;
        }
        
        String value = textInputLayout.getEditText().getText().toString().trim();
        
        if (TextUtils.isEmpty(value)) {
            textInputLayout.setError(context.getString(R.string.error_required_field, fieldName));
            return false;
        }
        
        try {
            double numValue = Double.parseDouble(value);
            if (numValue < min || numValue > max) {
                textInputLayout.setError(context.getString(R.string.error_range_field, fieldName, String.valueOf(min), String.valueOf(max)));
                return false;
            }
        } catch (NumberFormatException e) {
            textInputLayout.setError(context.getString(R.string.error_invalid_number));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Validate positive number
     */
    public static boolean validatePositiveNumber(Context context, TextInputLayout textInputLayout, 
                                               String fieldName) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return false;
        }
        
        String value = textInputLayout.getEditText().getText().toString().trim();
        
        if (TextUtils.isEmpty(value)) {
            textInputLayout.setError(context.getString(R.string.error_required_field, fieldName));
            return false;
        }
        
        try {
            double numValue = Double.parseDouble(value);
            if (numValue <= 0) {
                textInputLayout.setError(context.getString(R.string.error_positive_field, fieldName));
                return false;
            }
        } catch (NumberFormatException e) {
            textInputLayout.setError(context.getString(R.string.error_invalid_number));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Validate text length
     */
    public static boolean validateTextLength(Context context, TextInputLayout textInputLayout, 
                                           int minLength, int maxLength, String fieldName) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return false;
        }
        
        String value = textInputLayout.getEditText().getText().toString().trim();
        
        if (value.length() < minLength) {
            textInputLayout.setError(context.getString(R.string.error_min_length_field, fieldName, minLength));
            return false;
        }
        
        if (value.length() > maxLength) {
            textInputLayout.setError(context.getString(R.string.error_max_length_field, fieldName, maxLength));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Validate blood pressure values
     */
    public static ValidationResult validateBloodPressure(Context context, 
                                                       TextInputLayout systolicLayout, 
                                                       TextInputLayout diastolicLayout) {
        ValidationResult result = new ValidationResult();
        
        // Validate systolic
        if (!validateNumericRange(context, systolicLayout, 70, 250, context.getString(R.string.field_systolic))) {
            result.addError(context.getString(R.string.error_invalid_field, context.getString(R.string.field_systolic)));
        }
        
        // Validate diastolic
        if (!validateNumericRange(context, diastolicLayout, 40, 150, context.getString(R.string.field_diastolic))) {
            result.addError(context.getString(R.string.error_invalid_field, context.getString(R.string.field_diastolic)));
        }
        
        // Check if systolic > diastolic
        if (result.isValid() && systolicLayout.getEditText() != null && 
            diastolicLayout.getEditText() != null) {
            try {
                double systolic = Double.parseDouble(systolicLayout.getEditText().getText().toString());
                double diastolic = Double.parseDouble(diastolicLayout.getEditText().getText().toString());
                
                if (systolic <= diastolic) {
                    systolicLayout.setError(context.getString(R.string.error_systolic_greater));
                    result.addError(context.getString(R.string.error_systolic_greater));
                }
            } catch (NumberFormatException e) {
                // Already handled by individual validations
            }
        }
        
        return result;
    }
    
    /**
     * Validate heart rate
     */
    public static boolean validateHeartRate(Context context, TextInputLayout textInputLayout) {
        return validateNumericRange(context, textInputLayout, 30, 220, context.getString(R.string.field_heart_rate));
    }
    
    /**
     * Validate blood sugar
     */
    public static boolean validateBloodSugar(Context context, TextInputLayout textInputLayout) {
        return validateNumericRange(context, textInputLayout, 20, 600, context.getString(R.string.field_blood_sugar));
    }
    
    /**
     * Validate weight
     */
    public static boolean validateWeight(Context context, TextInputLayout textInputLayout) {
        return validateNumericRange(context, textInputLayout, 10, 500, context.getString(R.string.field_weight));
    }
    
    /**
     * Validate height
     */
    public static boolean validateHeight(Context context, TextInputLayout textInputLayout) {
        return validateNumericRange(context, textInputLayout, 50, 300, context.getString(R.string.field_height));
    }
    
    /**
     * Validate reminder title
     */
    public static boolean validateReminderTitle(Context context, TextInputLayout textInputLayout) {
        if (!validateRequired(context, textInputLayout, context.getString(R.string.field_title))) {
            return false;
        }
        
        return validateTextLength(context, textInputLayout, 2, 100, context.getString(R.string.field_title));
    }
    
    /**
     * Validate reminder description
     */
    public static boolean validateReminderDescription(Context context, TextInputLayout textInputLayout) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return true; // Description is optional
        }
        
        String value = textInputLayout.getEditText().getText().toString().trim();
        if (!TextUtils.isEmpty(value) && value.length() > 500) {
            textInputLayout.setError(context.getString(R.string.error_description_length));
            return false;
        }
        
        textInputLayout.setError(null);
        return true;
    }
    
    /**
     * Show validation summary for multiple errors
     */
    public static void showValidationSummary(Context context, ValidationResult result) {
        if (!result.isValid() && !result.getErrors().isEmpty()) {
            StringBuilder message = new StringBuilder("Vui lòng sửa các lỗi sau:\n");
            for (int i = 0; i < result.getErrors().size(); i++) {
                message.append("• ").append(result.getErrors().get(i));
                if (i < result.getErrors().size() - 1) {
                    message.append("\n");
                }
            }
            UserFeedbackManager.showError(context, message.toString());
        }
    }
    
    /**
     * Clear all validation errors
     */
    public static void clearValidationErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setError(null);
            }
        }
    }
    
    /**
     * Set up real-time validation for EditText
     */
    public static void setupRealTimeValidation(TextInputLayout textInputLayout, 
                                             ValidationCallback callback) {
        if (textInputLayout == null || textInputLayout.getEditText() == null) {
            return;
        }
        
        textInputLayout.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (callback != null) {
                    callback.validate();
                }
            }
        });
    }
    
    /**
     * Validation callback interface
     */
    public interface ValidationCallback {
        void validate();
    }
}
