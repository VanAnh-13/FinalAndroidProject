package com.example.healthylifehub.base;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

/**
 * BaseFragment is an abstract base class for all fragments in the Base application.
 * This class provides a standardized approach to fragment creation using view binding,
 * ensuring consistent lifecycle management, loading state handling, and reducing
 * boilerplate code across fragments.
 * 
 * @param <VB> The ViewBinding type associated with the fragment's layout
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    /**
     * Functional interface for view binding inflation in fragments.
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
    
    /** The view binding instance for this fragment */
    private VB _binding;
    
    /** Dialog instance used to display loading state to the user */
    private Dialog loadingDialog;

    /**
     * Constructor for BaseFragment.
     * Initializes the fragment with the specified binding inflater.
     * 
     * @param bindingInflater The binding inflater used to create view binding
     */
    protected BaseFragment(BindingInflater<VB> bindingInflater) {
        this.bindingInflater = bindingInflater;
    }

    /**
     * Gets the current view binding instance.
     * Provides access to the fragment's view binding for UI manipulation.
     * 
     * @return The current ViewBinding instance
     */
    protected VB getBinding() {
        return _binding;
    }

    /**
     * Abstract method to get the associated ViewModel.
     * Subclasses must implement this method to provide the specific
     * ViewModel instance used by the fragment.
     * 
     * @return The BaseViewModel instance associated with this fragment
     */
    protected abstract BaseViewModel getViewModel();

    /**
     * Gets or creates the loading dialog for this fragment.
     * This method lazily initializes the loading dialog when needed
     * and ensures it's properly associated with the fragment's context.
     * 
     * @return The loading Dialog instance, or null if context is unavailable
     */
    protected Dialog getLoadingDialog() {
        if (loadingDialog == null && getContext() != null) {
            loadingDialog = new Dialog(getContext());
            loadingDialog.setContentView(new ProgressBar(getContext()));
            loadingDialog.setCancelable(false);
            loadingDialog.setTitle("Loading...");
        }
        return loadingDialog;
    }

    /**
     * Called when the fragment is starting.
     * This method handles the fragment creation process and calls
     * the initData method for data initialization.
     * 
     * @param savedInstanceState If the fragment is being re-constructed from
     *                          a previous saved state, this is the state
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * This method inflates the view binding and returns the root view.
     * 
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * @return The View for the fragment's UI, or null
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = bindingInflater.inflate(inflater);
        return _binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned.
     * This method sets up the fragment's UI, observes ViewModel data,
     * and configures user interactions.
     * 
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe loading state from ViewModel and manage loading dialog
        getViewModel().getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                Dialog dialog = getLoadingDialog();
                if (dialog != null && !dialog.isShowing()) {
                    dialog.show();
                }
            } else {
                Dialog dialog = getLoadingDialog();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });

        // Execute fragment lifecycle methods in order
        bindData();
        observeData();
        setOnClick();
    }

    /**
     * Abstract method for initializing fragment data.
     * Subclasses must implement this method to set up initial data,
     * configure variables, and prepare the fragment state.
     */
    public abstract void initData();

    /**
     * Abstract method for binding data to UI components.
     * Subclasses must implement this method to connect data sources
     * to UI elements and populate views with content.
     */
    public abstract void bindData();

    /**
     * Abstract method for observing data changes.
     * Subclasses must implement this method to set up observers
     * for LiveData or other observable data sources.
     */
    public abstract void observeData();

    /**
     * Abstract method for setting up user interaction handlers.
     * Subclasses must implement this method to configure click listeners,
     * touch handlers, and other user interaction callbacks.
     */
    public abstract void setOnClick();
}