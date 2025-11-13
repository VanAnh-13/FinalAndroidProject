package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
