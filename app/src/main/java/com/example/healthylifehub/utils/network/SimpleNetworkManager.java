package com.example.healthylifehub.utils.network;

import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import com.example.healthylifehub.ui.shared.NetworkDialog;

/**
 * SimpleNetworkManager - Quản lý network đơn giản
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
            if (isConnected) handleNetworkConnected();
            else handleNetworkDisconnected();
        };
        networkTypeObserver = networkType -> {};
    }
    
    public void startMonitoring(LifecycleOwner lifecycleOwner) {
        networkMonitor.isConnected().observe(lifecycleOwner, connectivityObserver);
        networkMonitor.getNetworkType().observe(lifecycleOwner, networkTypeObserver);
    }
    
    public void stopMonitoring(LifecycleOwner lifecycleOwner) {
        networkMonitor.isConnected().removeObserver(connectivityObserver);
        networkMonitor.getNetworkType().removeObserver(networkTypeObserver);
    }
    
    private void handleNetworkConnected() {
        if (!isFirstConnection) {
            networkDialog.showOnline(getNetworkTypeString());
            triggerFirestoreSync();
        }
        isFirstConnection = false;
    }
    
    private void handleNetworkDisconnected() {
        networkDialog.showOffline();
        isFirstConnection = false;
    }
    
    private String getNetworkTypeString() {
        NetworkMonitor.NetworkType type = networkMonitor.getNetworkType().getValue();
        if (type == null) return "mạng";
        switch (type) {
            case WIFI: return "WiFi";
            case CELLULAR: return "mạng di động";
            case ETHERNET: return "Ethernet";
            case VPN: return "VPN";
            default: return "mạng";
        }
    }
    
    private void triggerFirestoreSync() {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork();
        } catch (Exception e) {
            android.util.Log.e("SimpleNetworkManager", "Error enabling Firestore network", e);
        }
    }
    
    public void disableFirestoreNetwork() {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork();
        } catch (Exception e) {
            android.util.Log.e("SimpleNetworkManager", "Error disabling Firestore network", e);
        }
    }
    
    public void showError(String message) { networkDialog.showError(message); }
    public void showSuccess(String message) { networkDialog.showSuccess(message); }
    public void showConnecting() { networkDialog.showConnecting(); }
    public void hideDialog() { networkDialog.dismiss(); }
    public boolean isNetworkAvailable() { return networkMonitor.isCurrentlyConnected(); }
    public boolean isOnWiFi() { return networkMonitor.isOnWiFi(); }
    public void destroy() { if (networkDialog != null) networkDialog.destroy(); }
}
