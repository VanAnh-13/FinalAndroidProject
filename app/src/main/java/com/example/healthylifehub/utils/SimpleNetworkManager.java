package com.example.healthylifehub.utils;

import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import com.example.healthylifehub.ui.shared.NetworkDialog;

/**
 * SimpleNetworkManager - Quản lý network đơn giản
 * Tự động hiển thị NetworkDialog khi có thay đổi mạng
 * Tận dụng tối đa Firestore offline capabilities
 */
public class SimpleNetworkManager {
    
    private NetworkMonitor networkMonitor;
    private NetworkDialog networkDialog;
    private Context context;
    private boolean isFirstConnection = true;
    
    private Observer<Boolean> connectivityObserver;
    private Observer<NetworkMonitor.NetworkType> networkTypeObserver;
    
    public SimpleNetworkManager(Context context) {
        this.context = context;
        this.networkMonitor = NetworkMonitor.getInstance(context);
        this.networkDialog = new NetworkDialog(context);
        
        setupObservers();
    }
    
    private void setupObservers() {
        connectivityObserver = isConnected -> {
            if (isConnected) {
                handleNetworkConnected();
            } else {
                handleNetworkDisconnected();
            }
        };
        
        networkTypeObserver = networkType -> {
            // Update network type info if needed
        };
    }
    
    /**
     * Bắt đầu monitor network
     */
    public void startMonitoring(LifecycleOwner lifecycleOwner) {
        networkMonitor.isConnected().observe(lifecycleOwner, connectivityObserver);
        networkMonitor.getNetworkType().observe(lifecycleOwner, networkTypeObserver);
    }
    
    /**
     * Dừng monitor network
     */
    public void stopMonitoring(LifecycleOwner lifecycleOwner) {
        networkMonitor.isConnected().removeObserver(connectivityObserver);
        networkMonitor.getNetworkType().removeObserver(networkTypeObserver);
    }
    
    /**
     * Xử lý khi có mạng
     */
    private void handleNetworkConnected() {
        if (!isFirstConnection) {
            // Chỉ hiển thị thông báo khi mạng được khôi phục
            String networkType = getNetworkTypeString();
            networkDialog.showOnline(networkType);
            
            // Trigger Firestore sync
            triggerFirestoreSync();
        }
        isFirstConnection = false;
    }
    
    /**
     * Xử lý khi mất mạng
     */
    private void handleNetworkDisconnected() {
        networkDialog.showOffline();
        isFirstConnection = false;
    }
    
    /**
     * Lấy tên loại mạng
     */
    private String getNetworkTypeString() {
        NetworkMonitor.NetworkType type = networkMonitor.getNetworkType().getValue();
        if (type == null) return "mạng";
        
        switch (type) {
            case WIFI:
                return "WiFi";
            case CELLULAR:
                return "mạng di động";
            case ETHERNET:
                return "Ethernet";
            case VPN:
                return "VPN";
            default:
                return "mạng";
        }
    }
    
    /**
     * Trigger Firestore sync khi có mạng trở lại
     * Tận dụng Firestore offline capabilities
     */
    private void triggerFirestoreSync() {
        // Firestore tự động sync khi có mạng trở lại
        // Chúng ta chỉ cần enable network cho Firestore
        try {
            // Enable Firestore network
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .enableNetwork()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("SimpleNetworkManager", "✅ Firestore network enabled");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SimpleNetworkManager", "❌ Failed to enable Firestore network", e);
                });
        } catch (Exception e) {
            android.util.Log.e("SimpleNetworkManager", "Error enabling Firestore network", e);
        }
    }
    
    /**
     * Disable Firestore network khi offline
     */
    public void disableFirestoreNetwork() {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .disableNetwork()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("SimpleNetworkManager", "✅ Firestore network disabled");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SimpleNetworkManager", "❌ Failed to disable Firestore network", e);
                });
        } catch (Exception e) {
            android.util.Log.e("SimpleNetworkManager", "Error disabling Firestore network", e);
        }
    }
    
    /**
     * Hiển thị thông báo lỗi custom
     */
    public void showError(String message) {
        networkDialog.showError(message);
    }
    
    /**
     * Hiển thị thông báo thành công custom
     */
    public void showSuccess(String message) {
        networkDialog.showSuccess(message);
    }
    
    /**
     * Hiển thị trạng thái đang kết nối
     */
    public void showConnecting() {
        networkDialog.showConnecting();
    }
    
    /**
     * Ẩn dialog
     */
    public void hideDialog() {
        networkDialog.dismiss();
    }
    
    /**
     * Kiểm tra có mạng không
     */
    public boolean isNetworkAvailable() {
        return networkMonitor.isCurrentlyConnected();
    }
    
    /**
     * Kiểm tra có WiFi không
     */
    public boolean isOnWiFi() {
        return networkMonitor.isOnWiFi();
    }
    
    /**
     * Cleanup
     */
    public void destroy() {
        if (networkDialog != null) {
            networkDialog.destroy();
        }
    }
}