package com.example.healthylifehub.data.source.network;

/**
 * APIService interface defines the contract for all API operations in the Base application.
 * This interface uses Retrofit annotations to define HTTP endpoints and request methods,
 * providing a clean and type-safe way to interact with REST APIs.
 * 
 * The interface pattern allows for easy testing through mocking and provides
 * a clear contract for all network operations in the application.
 */
public interface APIService {
    
    // TODO: Define API endpoints using Retrofit annotations
    // Example:
    // @GET("users")
    // Call<List<User>> getUsers();
    // 
    // @POST("auth/login")
    // Call<AuthResponse> login(@Body LoginRequest request);
    // 
    // @GET("users/{id}")
    // Call<User> getUserById(@Path("id") int userId);
    // 
    // @PUT("users/{id}")
    // Call<User> updateUser(@Path("id") int userId, @Body User user);
    // 
    // @DELETE("users/{id}")
    // Call<Void> deleteUser(@Path("id") int userId);
}