package com.example.healthylifehub.utils.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class ApplicationContextProvider {
    
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    
    public static void init(Application application) {
        context = application.getApplicationContext();
    }
    
    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("Application context not initialized. Call init() in Application.onCreate()");
        }
        return context;
    }
}

