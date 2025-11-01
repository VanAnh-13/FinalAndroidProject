package com.example.healthylifehub.utils.constant;

/**
 * APIConstant class contains all API-related constants used throughout the Base application.
 * This class centralizes API configuration values, endpoints, and timeout settings
 * to ensure consistency and easy maintenance of API-related constants.
 * 
 * Using a constants class helps avoid magic strings and numbers throughout the codebase,
 * making the application more maintainable and reducing the risk of typos.
 */
public class APIConstant {
    
    /** The base URL for all API requests in the application */
    public static final String BASE_URL = "";

    /**
     * EndPoint class contains all API endpoint constants.
     * This nested class organizes API endpoints in a structured way,
     * making it easy to find and maintain endpoint URLs.
     */
    public static class EndPoint {
        // TODO: Add API endpoint constants here
        // Example:
        // public static final String LOGIN = "auth/login";
        // public static final String USERS = "users";
        // public static final String PROFILE = "profile";
    }

    /**
     * TimeOut class contains all timeout-related constants for API requests.
     * This nested class centralizes timeout configurations for different
     * types of network operations, ensuring consistent timeout behavior.
     */
    public static class TimeOut {
        /** Connection timeout duration in seconds for API requests */
        public static final long CONNECT_TIME_OUT = 20L;
        
        // TODO: Add additional timeout constants as needed
        // Example:
        // public static final long READ_TIME_OUT = 30L;
        // public static final long WRITE_TIME_OUT = 30L;
    }
}