package com.example.healthylifehub.ui.shared;

import android.content.Context;
import android.util.AttributeSet;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import com.example.healthylifehub.utils.network.NetworkMonitor;
import com.google.android.material.button.MaterialButton;

/**
 * NetworkAwareButton is a custom MaterialButton that automatically handles network connectivity.
 * It disables itself when network is unavailable and updates its text accordingly.
 * 
 * Features:
 * - Automatically observes network connectivity
 * - Disables button when offline
 * - Updates button text to indicate offline state
 * - Restores original state when network is available
 */
public class NetworkAwareButton extends MaterialButton {
    
    private NetworkMonitor networkMonitor;
    private String originalText;
    private String offlineText;
    private boolean isNetworkRequired = true;
    private boolean wasEnabledBeforeNetworkLoss = true;
    private Observer<Boolean> networkObserver;
    
    public NetworkAwareButton(Context context) {
        super(context);
        init(context);
    }
    
    public NetworkAwareButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public NetworkAwareButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        networkMonitor = NetworkMonitor.getInstance(context);
        originalText = getText() != null ? getText().toString() : "";
        offlineText = originalText + " (Offline)";
        
        // Create network observer
        networkObserver = isConnected -> {
            if (isNetworkRequired) {
                updateNetworkState(isConnected);
            }
        };
    }
    
    /**
     * Start observing network connectivity
     * Call this in onResume() or when the button becomes visible
     */
    public void startNetworkObserving(LifecycleOwner lifecycleOwner) {
        if (networkMonitor != null && networkObserver != null) {
            networkMonitor.isConnected().observe(lifecycleOwner, networkObserver);
        }
    }
    
    /**
     * Stop observing network connectivity
     * Call this in onPause() or when the button is no longer visible
     */
    public void stopNetworkObserving(LifecycleOwner lifecycleOwner) {
        if (networkMonitor != null && networkObserver != null) {
            networkMonitor.isConnected().removeObserver(networkObserver);
        }
    }
    
    /**
     * Set whether this button requires network connectivity
     * @param required true if network is required, false otherwise
     */
    public void setNetworkRequired(boolean required) {
        this.isNetworkRequired = required;
        
        if (!required) {
            // Restore original state if network is no longer required
            setEnabled(wasEnabledBeforeNetworkLoss);
            setText(originalText);
        } else {
            // Check current network state
            boolean isConnected = networkMonitor != null && networkMonitor.isCurrentlyConnected();
            updateNetworkState(isConnected);
        }
    }
    
    /**
     * Set custom text to display when offline
     * @param offlineText Text to show when network is unavailable
     */
    public void setOfflineText(String offlineText) {
        this.offlineText = offlineText;
        
        // Update current text if currently offline
        boolean isConnected = networkMonitor != null && networkMonitor.isCurrentlyConnected();
        if (!isConnected && isNetworkRequired) {
            setText(this.offlineText);
        }
    }
    
    /**
     * Override setText to keep track of original text
     */
    @Override
    public void setText(CharSequence text, BufferType type) {
        // Only update originalText if we're not currently in offline mode
        boolean isConnected = networkMonitor == null || networkMonitor.isCurrentlyConnected();
        if (isConnected || !isNetworkRequired) {
            originalText = text != null ? text.toString() : "";
            if (offlineText.equals(originalText + " (Offline)")) {
                offlineText = originalText + " (Offline)";
            }
        }
        
        super.setText(text, type);
    }
    
    /**
     * Override setEnabled to keep track of enabled state before network loss
     */
    @Override
    public void setEnabled(boolean enabled) {
        // Only update wasEnabledBeforeNetworkLoss if network is available
        boolean isConnected = networkMonitor == null || networkMonitor.isCurrentlyConnected();
        if (isConnected || !isNetworkRequired) {
            wasEnabledBeforeNetworkLoss = enabled;
        }
        
        super.setEnabled(enabled);
    }
    
    /**
     * Update button state based on network connectivity
     */
    private void updateNetworkState(boolean isConnected) {
        if (isConnected) {
            // Network is available - restore original state
            setEnabled(wasEnabledBeforeNetworkLoss);
            super.setText(originalText, BufferType.NORMAL);
        } else {
            // Network is unavailable - disable and update text
            wasEnabledBeforeNetworkLoss = isEnabled();
            setEnabled(false);
            super.setText(offlineText, BufferType.NORMAL);
        }
    }
    
    /**
     * Check if button is currently in offline mode
     * @return true if offline mode is active
     */
    public boolean isInOfflineMode() {
        return isNetworkRequired && 
               (networkMonitor == null || !networkMonitor.isCurrentlyConnected());
    }
    
    /**
     * Get the original text (before offline modification)
     * @return Original button text
     */
    public String getOriginalText() {
        return originalText;
    }
    
    /**
     * Get the offline text
     * @return Offline button text
     */
    public String getOfflineText() {
        return offlineText;
    }
    
    /**
     * Check if network is required for this button
     * @return true if network is required
     */
    public boolean isNetworkRequired() {
        return isNetworkRequired;
    }
}