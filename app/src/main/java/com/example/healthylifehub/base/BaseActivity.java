package com.example.healthylifehub.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

/**
 * BaseActivity is an abstract base class for all activities in the Base application.
 * This class provides a standardized approach to activity creation using view binding,
 * ensuring consistent lifecycle management and reducing boilerplate code across activities.
 * 
 * @param <VB> The ViewBinding type associated with the activity's layout
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    /**
     * Functional interface for view binding inflation.
     * This interface defines a contract for inflating view binding objects
     * using a LayoutInflater, promoting type safety and reducing casting.
     * 
     * @param <VB> The ViewBinding type to be inflated
     */
    public interface BindingInflater<VB extends ViewBinding> {
        /**
         * Inflates the view binding using the provided LayoutInflater.
         * 
         * @param inflater The LayoutInflater used to inflate the binding
         * @return The inflated ViewBinding instance
         */
        VB inflate(LayoutInflater inflater);
    }

    /** The binding inflater instance used to create the view binding */
    private final BindingInflater<VB> bindingInflater;
    
    /** The view binding instance for this activity */
    private VB _binding;

    /**
     * Constructor for BaseActivity.
     * Initializes the activity with the specified binding inflater.
     * 
     * @param bindingInflater The binding inflater used to create view binding
     */
    protected BaseActivity(BindingInflater<VB> bindingInflater) {
        this.bindingInflater = bindingInflater;
    }

    /**
     * Gets the current view binding instance.
     * Provides access to the activity's view binding for UI manipulation.
     * 
     * @return The current ViewBinding instance
     */
    protected VB getBinding() {
        return _binding;
    }

    /**
     * Called when the activity is starting.
     * This method handles the standard activity creation process including
     * view binding inflation, content view setting, and lifecycle method calls.
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *                          previously being shut down then this Bundle contains the data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inflate the view binding using the provided inflater
        _binding = bindingInflater.inflate(getLayoutInflater());
        
        // Set the content view to the root of the binding
        setContentView(_binding.getRoot());

        // Execute the activity lifecycle methods in order
        initData();
        bindData();
        setOnClick();
    }

    /**
     * Abstract method for initializing activity data.
     * Subclasses must implement this method to set up initial data,
     * configure variables, and prepare the activity state.
     */
    public abstract void initData();

    /**
     * Abstract method for binding data to UI components.
     * Subclasses must implement this method to connect data sources
     * to UI elements and populate views with content.
     */
    public abstract void bindData();

    /**
     * Abstract method for setting up user interaction handlers.
     * Subclasses must implement this method to configure click listeners,
     * touch handlers, and other user interaction callbacks.
     */
    public abstract void setOnClick();
}