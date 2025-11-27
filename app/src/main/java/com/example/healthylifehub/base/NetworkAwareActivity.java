package com.example.healthylifehub.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.lifecycle.Observer;
import androidx.viewbinding.ViewBinding;
import com.example.healthylifehub.R;
import com.example.healthylifehub.ui.shared.NetworkStatusIndicator;
import com.example.healthylifehub.utils.network.NetworkMonitor;
import java.util.ArrayList;
import java.util.List;

/**
 * NetworkAwareActivity extends BaseActivity with network connectivity awareness.
 * This class provides automatic network monitoring and UI updates based on connectivity state.
 * 
 * Features:
 * - Automatic network monitoring
 * - Network status callbacks for subclasses
 * - Built-in network status indicator support
 * - Network-dependent UI element management
 * - Automatic disable/enable of network-dependent views
 * 
 * @param <VB> The ViewBinding type for this activity
 */
public abstract class NetworkAwareActivity<VB extends ViewBinding> extends BaseActivity<VB> {
    
    // Network monitoring
    protected NetworkMonitor networkMonitor;
    protected NetworkStatusIndicator networkStatusIndicator;
    
    private Observer<Boolean> connectivityObserver;
    private Observer<NetworkMonitor.NetworkType> networkTypeObserver;
    
    private boolean isNetworkAvailable = true;
    private NetworkMonitor.NetworkType currentNetworkType = NetworkMonitor.NetworkType.NONE;
    
    // List of views that should be disabled when network is unavailable
    private List<View> networkDependentViews = new ArrayList<>();

    /**
     * Constructor that takes a binding inflater function.
     * 
     * @param bindingInflater A function that takes a LayoutInflater and returns the ViewBinding
     */
    public NetworkAwareActivity(BindingInflater<VB> bindingInflater) {
        super(bindingInflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize network monitoring before calling super
        networkMonitor = NetworkMonitor.getInstance(this);
        setupNetworkObservers();
        
        super.onCreate(savedInstanceState);
        
        // Network status indicator is optional - we use SimpleNetworkManager instead
        networkStatusIndicator = null;
        if (networkStatusIndicator != null) {
            setupNetworkStatusIndicator();
        }
        
        // Initialize network-dependent views after UI is set up
        initNetworkDependentViews();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startNetworkObserving();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopNetworkObserving();
    }
    
    // ==================== NETWORK METHODS ====================
    
    /**
     * Setup network observers
     */
    private void setupNetworkObservers() {
        connectivityObserver = isConnected -> {
            isNetworkAvailable = isConnected;
            onNetworkConnectivityChanged(isConnected);
            updateNetworkDependentViews();
        };
        
        networkTypeObserver = networkType -> {
            currentNetworkType = networkType;
            onNetworkTypeChanged(networkType);

        };
    }
    
    /**
     * Setup network status indicator
     */
    private void setupNetworkStatusIndicator() {
        if (networkStatusIndicator != null) {
            // Configure the indicator based on activity preferences
            networkStatusIndicator.setAutoHideWhenConnected(shouldAutoHideNetworkIndicator());
            
            // Set custom messages if needed
            String[] messages = getNetworkStatusMessages();
            if (messages != null && messages.length >= 3) {
                networkStatusIndicator.setMessages(messages[0], messages[1], messages[2]);
            }
        }
    }
    
    /**
     * Start observing network changes
     */
    private void startNetworkObserving() {
        if (networkMonitor != null && connectivityObserver != null && networkTypeObserver != null) {
            networkMonitor.isConnected().observe(this, connectivityObserver);
            networkMonitor.getNetworkType().observe(this, networkTypeObserver);
        }
        
        if (networkStatusIndicator != null) {
            networkStatusIndicator.startObserving(this);
        }
    }
    
    /**
     * Stop observing network changes
     */
    private void stopNetworkObserving() {
        if (networkMonitor != null && connectivityObserver != null && networkTypeObserver != null) {
            networkMonitor.isConnected().removeObserver(connectivityObserver);
            networkMonitor.getNetworkType().removeObserver(networkTypeObserver);
        }
        
        if (networkStatusIndicator != null) {
            networkStatusIndicator.stopObserving(this);
        }
    }
    
    /**
     * Update network-dependent views based on connectivity
     */
    private void updateNetworkDependentViews() {
        for (View view : networkDependentViews) {
            if (view != null) {
                view.setEnabled(isNetworkAvailable);
                view.setAlpha(isNetworkAvailable ? 1.0f : 0.5f);
            }
        }
    }
    
    /**
     * Add a view to the list of network-dependent views
     * These views will be automatically disabled when network is unavailable
     */
    protected void addNetworkDependentView(View view) {
        if (view != null && !networkDependentViews.contains(view)) {
            networkDependentViews.add(view);
            // Update immediately
            view.setEnabled(isNetworkAvailable);
            view.setAlpha(isNetworkAvailable ? 1.0f : 0.5f);
        }
    }
    
    /**
     * Remove a view from the list of network-dependent views
     */
    protected void removeNetworkDependentView(View view) {
        networkDependentViews.remove(view);
    }
    
    /**
     * Clear all network-dependent views
     */
    protected void clearNetworkDependentViews() {
        networkDependentViews.clear();
    }
    
    /**
     * Check if network is currently available
     */
    protected boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }
    
    /**
     * Get current network type
     */
    protected NetworkMonitor.NetworkType getCurrentNetworkType() {
        return currentNetworkType;
    }
    
    /**
     * Check if currently on WiFi
     */
    protected boolean isOnWiFi() {
        return currentNetworkType == NetworkMonitor.NetworkType.WIFI;
    }
    
    /**
     * Check if on metered connection
     */
    protected boolean isOnMeteredConnection() {
        return networkMonitor != null && networkMonitor.isMeteredConnection();
    }
    
    /**
     * Show network error message
     */
    protected void showNetworkError(String message) {
        String errorMessage = message != null ? message : getString(R.string.error_network);
        Toast.makeText(this, "⚠️ " + errorMessage, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show network restored message
     */
    protected void showNetworkRestored() {
        Toast.makeText(this, getString(R.string.toast_network_connected), Toast.LENGTH_SHORT).show();
    }
    
    // ==================== NETWORK CALLBACK METHODS ====================
    
    /**
     * Called when network connectivity changes
     * Override this method to handle connectivity changes
     */
    protected void onNetworkConnectivityChanged(boolean isConnected) {
        if (isConnected) {
            onNetworkConnected();
        } else {
            onNetworkDisconnected();
        }
    }
    
    /**
     * Called when network becomes available
     * Override this method to handle network connection
     */
    protected void onNetworkConnected() {
        // Default implementation - show restored message
        showNetworkRestored();
    }
    
    /**
     * Called when network becomes unavailable
     * Override this method to handle network disconnection
     */
    protected void onNetworkDisconnected() {
        // Default implementation - show error message
        showNetworkError("Mất kết nối mạng. Một số tính năng có thể không hoạt động.");
    }
    
    /**
     * Called when network type changes (WiFi, Cellular, etc.)
     * Override this method to handle network type changes
     */
    protected void onNetworkTypeChanged(NetworkMonitor.NetworkType networkType) {
        // Default implementation - do nothing
    }
    
    /**
     * Initialize network-dependent views
     * Override this method to add views that should be disabled when offline
     */
    protected void initNetworkDependentViews() {
        // Default implementation - do nothing
        // Subclasses should override this to add their network-dependent views
    }
    
    /**
     * Determine if network status indicator should auto-hide when connected
     * Override this method to customize behavior
     */
    protected boolean shouldAutoHideNetworkIndicator() {
        return true; // Default: auto-hide when connected
    }
    
    /**
     * Get custom network status messages
     * Override this method to provide custom messages
     * @return Array of [connected, offline, connecting] messages, or null for defaults
     */
    protected String[] getNetworkStatusMessages() {
        return null; // Default: use built-in messages
    }
}