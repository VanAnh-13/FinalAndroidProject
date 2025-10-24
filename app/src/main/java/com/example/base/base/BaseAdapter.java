package com.example.base.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * BaseAdapter is an abstract base class for RecyclerView adapters in the Base application.
 * This class provides a standardized approach to adapter creation using view binding,
 * reducing boilerplate code and ensuring consistent data handling across adapters.
 * 
 * @param <T> The data type that this adapter handles
 * @param <VB> The ViewBinding type associated with the adapter's item layout
 */
public abstract class BaseAdapter<T, VB extends ViewBinding> extends RecyclerView.Adapter<BaseViewHolder<VB>> {

    /**
     * Functional interface for view binding inflation in adapters.
     * This interface defines a contract for inflating view binding objects
     * for RecyclerView items, promoting type safety and reducing casting.
     * 
     * @param <VB> The ViewBinding type to be inflated
     */
    public interface BindingInflater<VB extends ViewBinding> {
        /**
         * Inflates the view binding for a RecyclerView item.
         * 
         * @param inflater The LayoutInflater used to inflate the binding
         * @param parent The parent ViewGroup
         * @param attachToParent Whether to attach the inflated view to the parent
         * @return The inflated ViewBinding instance
         */
        VB inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent);
    }

    /** The binding inflater instance used to create view bindings for items */
    private final BindingInflater<VB> bindingInflater;
    
    /** The list of data items managed by this adapter */
    private final List<T> dataList;

    /**
     * Constructor for BaseAdapter with initial data list.
     * Initializes the adapter with the specified binding inflater and data list.
     * 
     * @param bindingInflater The binding inflater used to create view bindings
     * @param dataList The initial list of data items (can be null)
     */
    protected BaseAdapter(BindingInflater<VB> bindingInflater, List<T> dataList) {
        this.bindingInflater = bindingInflater;
        this.dataList = dataList != null ? dataList : new ArrayList<>();
    }

    /**
     * Constructor for BaseAdapter with empty data list.
     * Initializes the adapter with the specified binding inflater and an empty data list.
     * 
     * @param bindingInflater The binding inflater used to create view bindings
     */
    protected BaseAdapter(BindingInflater<VB> bindingInflater) {
        this(bindingInflater, new ArrayList<>());
    }

    /**
     * Creates a new ViewHolder instance for the RecyclerView.
     * This method inflates the view binding and wraps it in a BaseViewHolder.
     * 
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new BaseViewHolder that holds a View of the given view type
     */
    @NonNull
    @Override
    public BaseViewHolder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VB binding = bindingInflater.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BaseViewHolder<>(binding);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * 
     * @return The total number of items in this adapter
     */
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     * This method calls the abstract bindData and onItemClick methods
     * to handle data binding and click events.
     * 
     * @param holder The ViewHolder which should be updated
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<VB> holder, int position) {
        bindData(holder.getBinding(), dataList.get(position), position);
        onItemClick(holder.getBinding(), dataList.get(position), position);
    }

    /**
     * Abstract method for binding data to the view binding.
     * Subclasses must implement this method to populate UI elements
     * with data from the specified item.
     * 
     * @param binding The view binding for the item
     * @param item The data item to bind
     * @param position The position of the item in the adapter
     */
    public abstract void bindData(VB binding, T item, int position);

    /**
     * Abstract method for handling item click events.
     * Subclasses must implement this method to define click behavior
     * for items in the RecyclerView.
     * 
     * @param binding The view binding for the clicked item
     * @param item The data item that was clicked
     * @param position The position of the clicked item in the adapter
     */
    public abstract void onItemClick(VB binding, T item, int position);

    /**
     * Updates the data item at the specified position.
     * This method replaces the existing item and notifies the adapter
     * of the change to trigger a UI update.
     * 
     * @param position The position of the item to update
     * @param data The new data item
     */
    public void setData(int position, T data) {
        if (position >= dataList.size()) {
            return;
        }
        dataList.set(position, data);
        notifyItemChanged(position);
    }

    /**
     * Removes the data item at the specified position.
     * This method removes the item from the data list and notifies
     * the adapter of the removal to trigger a UI update.
     * 
     * @param position The position of the item to remove
     */
    public void removeData(int position) {
        if (position >= dataList.size()) {
            return;
        }
        dataList.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Gets the current data list managed by this adapter.
     * This method provides access to the underlying data list
     * for external manipulation or querying.
     * 
     * @return The list of data items
     */
    public List<T> getDataList() {
        return dataList;
    }
}