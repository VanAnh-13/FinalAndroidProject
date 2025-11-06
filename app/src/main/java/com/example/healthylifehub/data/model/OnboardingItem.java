package com.example.healthylifehub.data.model;

import androidx.annotation.DrawableRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingItem {
    private @DrawableRes int iconResId;
    private String title;
    private String description;
}
