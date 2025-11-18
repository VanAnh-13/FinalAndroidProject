package com.example.healthylifehub.ui.shared;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import com.example.healthylifehub.R;
import com.example.healthylifehub.utils.NetworkMonitor;
import com.google.android.material.card.MaterialCardView;

/**
 * NetworkStatusIndicator is a custom view that displays network connectivity status.
 * It shows different states: Connected (WiFi/Cellular), Offline, and Connecting.
 * 
 * Features:
 * - Real-time network status updates
 * - Different colors and icons for different states
 * - Automatic show/hide based on connectivity
 * - Customizable messages and appearance
 */
public class NetworkStatusIndicator extends MaterialCardView {
    
    private NetworkMonitor networkMonitor;
    private TextView statusText;
    private View statusIndicator;
    private LinearLayout containerLayout;
    
    private Observer<Boolean> connectivityObserver;
    private Observer<NetworkMonitor.NetworkType> networkTypeObserver;
    
    private boolean autoHideWhenConnected = true;
    private String connectedMessage = "✅ Đã kết nối";
    private String offlineMessage = "⚠️ Không có mạng";
    private String connectingMessage = "🔄 Đang kết nối...";
    
    public NetworkStatusIndicator(Context context) {
        super(context);
        init(context);
    }
    
    public NetworkStatusIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public NetworkStatusIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        networkMonitor = NetworkMonitor.getInstance(context);
        
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_network_status_indicator, this, true);
        
        // Find views
        statusText = findViewById(R.id.tv_status);
        statusIndicator = findViewById(R.id.view_status_indicator);
        containerLayout = findViewById(R.id.ll_container);
        
        // Set initial state
        updateNetworkStatus(false, NetworkMonitor.NetworkType.NONE);
        
        // Create observers
        connectivityObserver = isConnected -> {
            NetworkMonitor.NetworkType currentType = networkMonitor.getNetworkType().getValue();
            if (currentType == null) currentType = NetworkMonitor.NetworkType.NONE;
            updateNetworkStatus(isConnected, currentType);
        };
        
        networkTypeObserver = networkType -> {
            Boolean isConnected = networkMonitor.isConnected().getValue();
            if (isConnected == null) isConnected = false;
            updateNetworkStatus(isConnected, networkType);
        };
    }
    
    /**
     * Start observing network status
     * Call this in onResume() or when the view becomes visible
     */
    public void startObserving(LifecycleOwner lifecycleOwner) {
        if (networkMonitor != null) {
            networkMonitor.isConnected().observe(lifecycleOwner, connectivityObserver);
            networkMonitor.getNetworkType().observe(lifecycleOwner, networkTypeObserver);
        }
    }
    
    /**
     * Stop observing network status
     * Call this in onPause() or when the view is no longer visible
     */
    public void stopObserving(LifecycleOwner lifecycleOwner) {
        if (networkMonitor != null) {
            networkMonitor.isConnected().removeObserver(connectivityObserver);
            networkMonitor.getNetworkType().removeObserver(networkTypeObserver);
        }
    }
    
    /**
     * Update network status display
     */
    private void updateNetworkStatus(boolean isConnected, NetworkMonitor.NetworkType networkType) {
        if (isConnected) {
            showConnectedState(networkType);
        } else {
            showOfflineState();
        }
    }
    
    /**
     * Show connected state with network type
     */
    private void showConnectedState(NetworkMonitor.NetworkType networkType) {
        String message;
        int indicatorColor;
        
        switch (networkType) {
            case WIFI:
                message = "✅ WiFi kết nối";
                indicatorColor = ContextCompat.getColor(getContext(), android.R.color.holo_green_dark);
                break;
            case CELLULAR:
                message = "📱 Mạng di động";
                indicatorColor = ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark);
                break;
            case ETHERNET:
                message = "🔌 Ethernet";
                indicatorColor = ContextCompat.getColor(getContext(), android.R.color.holo_green_dark);
                break;
            case VPN:
                message = "🔒 VPN kết nối";
                indicatorColor = ContextCompat.getColor(getContext(), android.R.color.holo_purple);
                break;
            default:
                message = connectedMessage;
                indicatorColor = ContextCompat.getColor(getContext(), android.R.color.holo_green_dark);
                break;
        }
        
        statusText.setText(message);
        statusIndicator.setBackgroundColor(indicatorColor);
        setCardBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
        
        // Auto-hide if enabled
        if (autoHideWhenConnected) {
            // Show briefly then hide
            setVisibility(View.VISIBLE);
            postDelayed(() -> setVisibility(View.GONE), 2000);
        } else {
            setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Show offline state
     */
    private void showOfflineState() {
        statusText.setText(offlineMessage);
        statusIndicator.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
        setCardBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));
        setVisibility(View.VISIBLE);
    }
    
    /**
     * Show connecting state (can be called manually)
     */
    public void showConnectingState() {
        statusText.setText(connectingMessage);
        statusIndicator.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_dark));
        setCardBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_light));
        setVisibility(View.VISIBLE);
    }
    
    /**
     * Set whether to auto-hide when connected
     */
    public void setAutoHideWhenConnected(boolean autoHide) {
        this.autoHideWhenConnected = autoHide;
    }
    
    /**
     * Set custom messages
     */
    public void setMessages(String connected, String offline, String connecting) {
        this.connectedMessage = connected;
        this.offlineMessage = offline;
        this.connectingMessage = connecting;
    }
    
    /**
     * Force show the indicator
     */
    public void show() {
        setVisibility(View.VISIBLE);
    }
    
    /**
     * Force hide the indicator
     */
    public void hide() {
        setVisibility(View.GONE);
    }
    
    /**
     * Check if currently showing offline state
     */
    public boolean isShowingOffline() {
        return getVisibility() == View.VISIBLE && 
               statusText.getText().toString().equals(offlineMessage);
    }
    
    /**
     * Check if currently showing connected state
     */
    public boolean isShowingConnected() {
        return getVisibility() == View.VISIBLE && 
               !statusText.getText().toString().equals(offlineMessage) &&
               !statusText.getText().toString().equals(connectingMessage);
    }
}