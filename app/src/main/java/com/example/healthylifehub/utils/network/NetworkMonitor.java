package com.example.healthylifehub.utils.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.healthylifehub.sync.SyncManager;

/**
 * Network Monitor - Monitors network connectivity changes
 */
public class NetworkMonitor {
    
    private static final String TAG = "NetworkMonitor";
    private static NetworkMonitor instance;
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private final MutableLiveData<NetworkType> networkType = new MutableLiveData<>(NetworkType.NONE);
    
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean wasDisconnected = false;
    
    public enum NetworkType { NONE, WIFI, CELLULAR, ETHERNET, VPN }
    
    private NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        checkInitialConnection();
    }
    
    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }
    
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
                if (wasDisconnected) {
                    triggerSyncOnConnectivityRestore();
                    wasDisconnected = false;
                }
            }
            
            @Override
            public void onLost(@NonNull Network network) {
                isConnected.postValue(false);
                networkType.postValue(NetworkType.NONE);
                wasDisconnected = true;
            }
            
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                updateNetworkType(network);
            }
        };
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }
    
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
    
    private void updateNetworkType(Network network) {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) { networkType.postValue(NetworkType.NONE); return; }
        
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) networkType.postValue(NetworkType.WIFI);
        else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) networkType.postValue(NetworkType.CELLULAR);
        else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) networkType.postValue(NetworkType.ETHERNET);
        else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) networkType.postValue(NetworkType.VPN);
        else networkType.postValue(NetworkType.NONE);
    }
    
    public LiveData<Boolean> isConnected() { return isConnected; }
    public LiveData<NetworkType> getNetworkType() { return networkType; }
    
    public boolean isCurrentlyConnected() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && 
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
    
    public boolean isOnWiFi() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
    
    public boolean isMeteredConnection() { return connectivityManager.isActiveNetworkMetered(); }
    
    private void triggerSyncOnConnectivityRestore() {
        try {
            SyncManager syncManager = new SyncManager(context);
            syncManager.triggerImmediateSync();
        } catch (Exception e) {
            Log.e(TAG, "Error triggering sync on connectivity restore", e);
        }
    }
    
    public void unregister() {
        if (networkCallback != null) {
            try { connectivityManager.unregisterNetworkCallback(networkCallback); } 
            catch (Exception e) { Log.e(TAG, "Error unregistering network callback", e); }
        }
    }
}
