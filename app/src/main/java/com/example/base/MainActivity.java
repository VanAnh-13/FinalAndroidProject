package com.example.base;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.base.base.BaseActivity;
import com.example.base.databinding.ActivityMainBinding;

/**
 * MainActivity class serves as the main entry point for the Base application.
 * This activity extends BaseActivity to inherit common functionality and lifecycle management.
 * It demonstrates the usage of view binding pattern for UI interaction.
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> {

    /**
     * Constructor for MainActivity.
     * Initializes the activity with the appropriate view binding inflater.
     * The method reference ActivityMainBinding::inflate is passed to the parent constructor
     * to handle automatic view binding inflation.
     */
    public MainActivity() {
        super(ActivityMainBinding::inflate);
    }

    /**
     * Initialize data for the activity.
     * This method is called during the activity lifecycle to set up initial data,
     * configure variables, and prepare the activity for user interaction.
     * Override this method to implement specific data initialization logic.
     */
    @Override
    public void initData() {
        // TODO: Initialize activity data, set up variables, configure initial state
    }

    /**
     * Bind data to UI components.
     * This method is responsible for connecting data sources to UI elements,
     * setting up adapters, and populating views with content.
     * Called after initData() in the activity lifecycle.
     */
    @Override
    public void bindData() {
        // TODO: Bind data to views, set up adapters, populate UI components
    }

    /**
     * Set up click listeners and user interaction handlers.
     * This method configures all click listeners, touch handlers, and other
     * user interaction callbacks for the activity's UI components.
     * Called after bindData() in the activity lifecycle.
     */
    @Override
    public void setOnClick() {
        // TODO: Set up click listeners, touch handlers, user interaction callbacks
    }
}