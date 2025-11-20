package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserProfile - Contains user profile information for registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private double height;
    private double weight;
    private String bloodType;
    private String medicalHistory;
    private String email;
    private double bmi;
    
    /**
     * Simple constructor for basic registration
     */
    public UserProfile(String fullName) {
        this.fullName = fullName;
        this.dateOfBirth = null;
        this.gender = "";
        this.height = 0;
        this.weight = 0;
        this.bloodType = "";
        this.medicalHistory = "";
        this.email = "";
        this.bmi = 0;
    }
}
