package com.example.healthylifehub.ui.reminders.suggestions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.SmartSuggestion;
import com.example.healthylifehub.data.model.UserBehavior;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.services.SmartReminderAI;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Smart Suggestions screen
 * Manages AI-generated suggestions and user interactions
 */
public class SmartSuggestionsViewModel extends AndroidViewModel {
    
    private final RemindersRepository repository;
    private final MutableLiveData<List<SmartSuggestion>> suggestions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<UserBehavior> userBehavior = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasData = new MutableLiveData<>(false);
    
    public SmartSuggestionsViewModel(@NonNull Application application) {
        super(application);
        repository = new RemindersRepository();
    }
    
    /**
     * Load and generate smart suggestions
     */
    public void loadSuggestions() {
        isLoading.setValue(true);
        
        // Observe reminders from repository
        repository.loadReminders().observeForever(reminders -> {
            if (reminders != null && !reminders.isEmpty()) {
                // Analyze behavior
                UserBehavior behavior = SmartReminderAI.analyzeUserBehavior(reminders);
                userBehavior.setValue(behavior);
                
                // Generate suggestions
                List<Reminder> activeReminders = filterActiveReminders(reminders);
                List<SmartSuggestion> generatedSuggestions = SmartReminderAI.generateSmartSuggestions(
                    activeReminders, 
                    behavior
                );
                
                suggestions.setValue(generatedSuggestions);
                hasData.setValue(!generatedSuggestions.isEmpty());
                isLoading.setValue(false);
            } else {
                suggestions.setValue(new ArrayList<>());
                hasData.setValue(false);
                isLoading.setValue(false);
            }
        });
    }
    
    /**
     * Apply a suggestion
     * @param suggestion Suggestion to apply
     */
    public void applySuggestion(SmartSuggestion suggestion) {
        isLoading.setValue(true);
        
        switch (suggestion.getType()) {
            case "optimize_time":
                applyTimeOptimization(suggestion);
                break;
            case "create_reminder":
                applyCreateReminder(suggestion);
                break;
            case "merge_reminders":
                applyMergeReminders(suggestion);
                break;
            case "change_frequency":
                applyFrequencyChange(suggestion);
                break;
        }
    }
    
    /**
     * Dismiss a suggestion
     * @param suggestion Suggestion to dismiss
     */
    public void dismissSuggestion(SmartSuggestion suggestion) {
        suggestion.setStatus("dismissed");
        suggestion.setDismissedAt(System.currentTimeMillis());
        
        // Remove from current list
        List<SmartSuggestion> currentSuggestions = suggestions.getValue();
        if (currentSuggestions != null) {
            currentSuggestions.remove(suggestion);
            suggestions.setValue(new ArrayList<>(currentSuggestions));
            hasData.setValue(!currentSuggestions.isEmpty());
        }
        
        // TODO: Save dismissed status to database
    }
    
    // ==================== APPLY SUGGESTION METHODS ====================
    
    private void applyTimeOptimization(SmartSuggestion suggestion) {
        String reminderId = suggestion.getReminderId();
        Long newTime = suggestion.getSuggestedTime();
        
        if (reminderId == null || newTime == null) {
            errorMessage.setValue("Lỗi: Thiếu thông tin");
            isLoading.setValue(false);
            return;
        }
        
        repository.getReminderById(reminderId).thenAccept(reminder -> {
            if (reminder != null) {
                reminder.setReminderTime(newTime);
                
                repository.updateReminder(reminder).thenAccept(success -> {
                    isLoading.postValue(false);
                    if (success) {
                        markSuggestionAsApplied(suggestion);
                    } else {
                        errorMessage.postValue("Lỗi khi cập nhật lời nhắc");
                    }
                });
            } else {
                isLoading.postValue(false);
                errorMessage.postValue("Không tìm thấy lời nhắc");
            }
        });
    }
    
    private void applyCreateReminder(SmartSuggestion suggestion) {
        Reminder newReminder = new Reminder();
        newReminder.setTitle(suggestion.getSuggestedTitle());
        newReminder.setDescription("Tạo tự động từ gợi ý thông minh");
        newReminder.setReminderTime(System.currentTimeMillis() + 3600000); // 1 hour from now
        newReminder.setFrequency(suggestion.getSuggestedFrequency());
        newReminder.setActive(true);
        
        repository.createReminder(newReminder).thenAccept(reminderId -> {
            isLoading.postValue(false);
            if (reminderId != null) {
                markSuggestionAsApplied(suggestion);
            } else {
                errorMessage.postValue("Lỗi khi tạo lời nhắc");
            }
        });
    }
    
    private void applyMergeReminders(SmartSuggestion suggestion) {
        // TODO: Implement merge logic
        // This would involve:
        // 1. Get both reminders
        // 2. Create new merged reminder
        // 3. Delete old reminders
        
        isLoading.setValue(false);
        errorMessage.setValue("Chức năng gộp lời nhắc đang phát triển");
    }
    
    private void applyFrequencyChange(SmartSuggestion suggestion) {
        String reminderId = suggestion.getReminderId();
        String newFrequency = suggestion.getSuggestedFrequency();
        
        if (reminderId == null || newFrequency == null) {
            errorMessage.setValue("Lỗi: Thiếu thông tin");
            isLoading.setValue(false);
            return;
        }
        
        repository.getReminderById(reminderId).thenAccept(reminder -> {
            if (reminder != null) {
                reminder.setFrequency(newFrequency);
                
                repository.updateReminder(reminder).thenAccept(success -> {
                    isLoading.postValue(false);
                    if (success) {
                        markSuggestionAsApplied(suggestion);
                    } else {
                        errorMessage.postValue("Lỗi khi cập nhật tần suất");
                    }
                });
            } else {
                isLoading.postValue(false);
                errorMessage.postValue("Không tìm thấy lời nhắc");
            }
        });
    }
    
    private void markSuggestionAsApplied(SmartSuggestion suggestion) {
        suggestion.setStatus("applied");
        suggestion.setAppliedAt(System.currentTimeMillis());
        
        // Remove from current list
        List<SmartSuggestion> currentSuggestions = suggestions.getValue();
        if (currentSuggestions != null) {
            currentSuggestions.remove(suggestion);
            suggestions.postValue(new ArrayList<>(currentSuggestions));
            hasData.postValue(!currentSuggestions.isEmpty());
        }
        
        // TODO: Save applied status to database
    }
    
    // ==================== HELPER METHODS ====================
    
    private List<Reminder> filterActiveReminders(List<Reminder> reminders) {
        List<Reminder> active = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (reminder.isActive()) {
                active.add(reminder);
            }
        }
        return active;
    }
    
    // ==================== GETTERS ====================
    
    public LiveData<List<SmartSuggestion>> getSuggestions() {
        return suggestions;
    }
    
    public LiveData<UserBehavior> getUserBehavior() {
        return userBehavior;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getHasData() {
        return hasData;
    }
}
