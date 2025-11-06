package com.example.healthylifehub.ui.reminders.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentRemindersBinding;
import com.example.healthylifehub.ui.reminders.RemindersViewModel;
import com.example.healthylifehub.ui.reminders.fragment.adapter.RemindersAdapter;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;

public class RemindersFragment extends BaseFragment<FragmentRemindersBinding> {

    private RemindersViewModel viewModel;
    private RemindersAdapter adapter;

    public RemindersFragment() {
        super(FragmentRemindersBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        bindData();
        setOnClick();
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);
        adapter = new RemindersAdapter(reminder -> {
            // Navigate to reminder detail
        });
    }

    @Override
    public void bindData() {
        getBinding().rvReminders.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvReminders.setAdapter(adapter);
    }

    @Override
    public void observeData() {
        // Observe reminders data
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.setReminders(reminders);
        });
    }

    @Override
    public void setOnClick() {
        getBinding().ivAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditReminderActivity.class);
            startActivity(intent);
        });
        
        getBinding().btnSuggestionCreate.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditReminderActivity.class);
            startActivity(intent);
        });
    }
}
