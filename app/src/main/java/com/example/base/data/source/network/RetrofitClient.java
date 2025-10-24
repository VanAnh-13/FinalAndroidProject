package com.example.base.data.source.network;

import com.example.base.utils.constant.APIConstant;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * RetrofitClient is a singleton class that provides a configured Retrofit instance for the Base application.
 * This class implements the Singleton pattern to ensure a single, properly configured Retrofit instance
 * is used throughout the application, reducing memory usage and ensuring consistent configuration.
 * 
 * The class handles the setup of HTTP client configuration, logging, timeouts, and JSON conversion,
 * providing a ready-to-use Retrofit instance for making API calls.
 */
public class RetrofitClient {

    /** Volatile singleton instance to ensure thread-safe lazy initialization */
    private static volatile Retrofit INSTANCE = null;

    /**
     * Gets the singleton Retrofit instance using double-checked locking pattern.
     * This method ensures thread-safe lazy initialization of the Retrofit instance,
     * creating it only when needed and ensuring only one instance exists.
     * 
     * @return The configured Retrofit singleton instance
     */
    public static Retrofit getInstance() {
        if (INSTANCE == null) {
            synchronized (RetrofitClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = retrofitBuilder();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Builds and configures the Retrofit instance with all necessary components.
     * This method sets up the Retrofit builder with base URL, JSON converter,
     * and HTTP client configuration for optimal API communication.
     * 
     * @return A fully configured Retrofit instance
     */
    private static Retrofit retrofitBuilder() {
        return new Retrofit.Builder()
                .baseUrl(APIConstant.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(provideOkHttpClient())
                .build();
    }

    /**
     * Provides a configured OkHttpClient with logging and timeout settings.
     * This method creates an HTTP client with appropriate interceptors for logging,
     * timeout configurations, and other HTTP-level settings required for API communication.
     * 
     * The logging interceptor helps with debugging by logging request and response details,
     * while timeout settings ensure requests don't hang indefinitely.
     * 
     * @return A configured OkHttpClient instance
     */
    private static OkHttpClient provideOkHttpClient() {
        // Create and configure HTTP logging interceptor for debugging
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Build OkHttpClient with timeout and logging configuration
        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        okHttpClient.connectTimeout(APIConstant.TimeOut.CONNECT_TIME_OUT, TimeUnit.SECONDS)
                .addInterceptor(interceptor);
        
        return okHttpClient.build();
    }
}