package com.example.base.base;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

/**
 * BaseViewHolder is a generic ViewHolder implementation for RecyclerView items in the Base application.
 * This class provides a standardized approach to ViewHolder creation using view binding,
 * eliminating the need for findViewById calls and ensuring type safety.
 * 
 * The ViewHolder pattern is essential for efficient RecyclerView performance,
 * as it caches view references to avoid repeated findViewById operations during scrolling.
 * 
 * @param <VB> The ViewBinding type associated with the RecyclerView item layout
 */
public class BaseViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
    
    /** The view binding instance that provides access to all views in the item layout */
    private final VB binding;

    /**
     * Constructor for BaseViewHolder.
     * Initializes the ViewHolder with the provided view binding and sets up
     * the item view using the binding's root view.
     * 
     * @param binding The ViewBinding instance for this ViewHolder's item layout
     */
    public BaseViewHolder(VB binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    /**
     * Gets the view binding instance associated with this ViewHolder.
     * This method provides access to all views within the item layout
     * through the type-safe view binding object.
     * 
     * @return The ViewBinding instance for this ViewHolder
     */
    public VB getBinding() {
        return binding;
    }
}