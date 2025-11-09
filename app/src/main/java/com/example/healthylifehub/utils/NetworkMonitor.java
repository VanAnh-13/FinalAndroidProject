package com.example.healthylifehub.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Network Monitor
 * Monitors network connectivity changes
 * Triggers data sync when network becomes available
 */
public class NetworkMonitor {
    
    private static final String TAG = "NetworkMonitor";
    private static NetworkMonitor instance;
    
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private final MutableLiveData<NetworkType> networkType = new MutableLiveData<>(NetworkType.NONE);
    
    private ConnectivityManager.NetworkCallback networkCallback;
    
    public enum NetworkType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN
    }
    
    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        checkInitialConnection();
    }
    
    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Register network callback to listen for connectivity changes
     */
    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network available: " + network);
                isConnected.postValue(true);
                updateNetworkType(network);
            }
            
            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost: " + network);
                isConnected.postValue(false);
                networkType.postValue(NetworkType.NONE);
            }
            
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                Log.d(TAG, "Network capabilities changed");
                updateNetworkType(network);
            }
        };
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }
    
    /**
     * Check initial connection state
     */
    private void checkInitialConnection() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                isConnected.postValue(true);
                updateNetworkType(activeNetwork);
                return;
            }
        }
        isConnected.postValue(false);
        networkType.postValue(NetworkType.NONE);
    }
    
    /**
     * Update network type based on capabilities
     */
    private void updateNetworkType(Network network) {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            networkType.postValue(NetworkType.NONE);
            return;
        }
        
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            networkType.postValue(NetworkType.WIFI);
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            networkType.postValue(NetworkType.CELLULAR);
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            networkType.postValue(NetworkType.ETHERNET);
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            networkType.postValue(NetworkType.VPN);
        } else {
            networkType.postValue(NetworkType.NONE);
        }
    }
    
    /**
     * Get connection status as LiveData
     */
    public LiveData<Boolean> isConnected() {
        return isConnected;
    }
    
    /**
     * Get network type as LiveData
     */
    public LiveData<NetworkType> getNetworkType() {
        return networkType;
    }
    
    /**
     * Check if currently connected (synchronous)
     */
    public boolean isCurrentlyConnected() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && 
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
    
    /**
     * Check if on WiFi (for large downloads)
     */
    public boolean isOnWiFi() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
    
    /**
     * Check if on metered connection (cellular)
     */
    public boolean isMeteredConnection() {
        return !connectivityManager.isActiveNetworkMetered();
    }
    
    /**
     * Unregister network callback (call in onDestroy)
     */
    public void unregister() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }
}
