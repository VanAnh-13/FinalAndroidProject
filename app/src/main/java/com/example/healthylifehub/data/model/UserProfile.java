package com.example.healthylifehub.data.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserProfile {
    private String userId;
    private String fullName;
    private String email;
    private String birthDate;
    private String gender;
    private String height;
    private String weight;
    private String bloodType;
    private String medicalHistory;
    private String bmi;
}
