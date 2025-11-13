package com.example.healthylifehub.ui.profile.privacy;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.healthylifehub.base.BaseViewModel;
import java.util.List;
import java.util.ArrayList;

/**
 * ViewModel for LoginHistoryActivity
 * Temporary implementation - will be enhanced with real data
 */
public class LoginHistoryViewModel extends BaseViewModel {
    
    private final MutableLiveData<List<LoginHistoryItem>> loginHistory = new MutableLiveData<>();
    
    public LoginHistoryViewModel(@NonNull Application application) {
        super(application);
    }
    
    public void loadLoginHistory() {
        showLoading();
        
        // Simulate loading - replace with real Firebase query
        new android.os.Handler().postDelayed(() -> {
            // Create sample data for now
            List<LoginHistoryItem> sampleData = new ArrayList<>();
            sampleData.add(new LoginHistoryItem("Hôm nay", "14:30", "Android", "192.168.1.1"));
            sampleData.add(new LoginHistoryItem("Hôm qua", "09:15", "Android", "192.168.1.1"));
            sampleData.add(new LoginHistoryItem("2 ngày trước", "20:45", "Web", "192.168.1.5"));
            
            loginHistory.setValue(sampleData);
            hideLoading();
        }, 1000);
    }
    
    public LiveData<List<LoginHistoryItem>> getLoginHistory() {
        return loginHistory;
    }
    
    /**
     * Simple data class for login history items
     */
    public static class LoginHistoryItem {
        private String date;
        private String time;
        private String device;
        private String location;
        
        public LoginHistoryItem(String date, String time, String device, String location) {
            this.date = date;
            this.time = time;
            this.device = device;
            this.location = location;
        }
        
        // Getters
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getDevice() { return device; }
        public String getLocation() { return location; }
    }
}