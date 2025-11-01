package com.example.healthylifehub.data.model;

import androidx.annotation.DrawableRes;

public class OnboardingItem {
    private final int iconResId;
    private final String title;
    private final String description;

    public OnboardingItem(@DrawableRes int iconResId, String title, String description) {
        this.iconResId = iconResId;
        this.title = title;
        this.description = description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
