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

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentRemindersBinding;
import com.example.healthylifehub.ui.reminders.RemindersViewModel;
import com.example.healthylifehub.ui.reminders.fragment.adapter.RemindersAdapter;
import com.example.healthylifehub.ui.reminders.add_edit.AddEditReminderActivity;
import com.example.healthylifehub.ui.reminders.detail.ReminderDetailActivity;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.data.repository.SuggestionsRepository;
import com.example.healthylifehub.data.model.SmartSuggestion;
import com.example.healthylifehub.ui.shared.AppSharedViewModel;
import android.widget.Toast;

public class RemindersFragment extends BaseFragment<FragmentRemindersBinding> {

    private RemindersViewModel viewModel;
    private AppSharedViewModel appSharedViewModel;
    private RemindersAdapter adapter;
    private RemindersRepository remindersRepository;
    private SuggestionsRepository suggestionsRepository;
    private SmartSuggestion currentSuggestion;

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
        observeData();  // ✅ GỌI OBSERVE DATA ĐỂ LOAD DỮ LIỆU TỪ FIREBASE
        setOnClick();
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);
        // Get shared ViewModel from Activity scope for app-wide data synchronization
        appSharedViewModel = new ViewModelProvider(requireActivity()).get(AppSharedViewModel.class);
        remindersRepository = new RemindersRepository();
        suggestionsRepository = new SuggestionsRepository();
        
        adapter = new RemindersAdapter(reminder -> {
            Intent intent = new Intent(requireContext(), ReminderDetailActivity.class);
            intent.putExtra("reminderId", reminder.getReminderId());
            startActivity(intent);
        });
        
        adapter.setToggleStatusListener((reminderId, isActive) -> {
            remindersRepository.toggleReminderStatus(reminderId, isActive)
                .thenAccept(success -> {
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            String message = isActive ? getString(R.string.marked_complete) : getString(R.string.skipped_next_scheduled);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.update_error), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
        });
        
        // Xử lý nút "Hoàn thành" và "Bỏ qua" - dùng shared ViewModel để sync
        adapter.setActionListener(new RemindersAdapter.OnReminderActionListener() {
            @Override
            public void onReminderCompleted(com.example.healthylifehub.data.model.Reminder reminder) {
                // Dùng shared ViewModel để cập nhật tất cả observers
                appSharedViewModel.completeReminder(reminder);
                Toast.makeText(requireContext(), getString(R.string.toast_reminder_completed), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReminderSkipped(com.example.healthylifehub.data.model.Reminder reminder) {
                // Bỏ qua nhắc nhở
                Toast.makeText(requireContext(), getString(R.string.skipped_next_scheduled), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onReminderDeleted(com.example.healthylifehub.data.model.Reminder reminder) {
                // Xóa nhắc nhở - dùng shared ViewModel để sync
                appSharedViewModel.deleteReminder(reminder);
                Toast.makeText(requireContext(), getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void bindData() {
        getBinding().rvReminders.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().rvReminders.setAdapter(adapter);
    }

    @Override
    public void observeData() {
        // Observe shared reminders for real-time sync
        appSharedViewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.setReminders(reminders);
        });
        
        suggestionsRepository.loadPendingSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            if (suggestions != null && !suggestions.isEmpty()) {
                currentSuggestion = suggestions.get(0);
                showSuggestion(currentSuggestion);
            } else {
                hideSuggestion();
            }
        });
    }
    
    private void showSuggestion(SmartSuggestion suggestion) {
        getBinding().cardSmartSuggestion.setVisibility(View.VISIBLE);
        getBinding().tvSuggestionText.setText(suggestion.getTitle() + "\n" + suggestion.getDescription());
    }
    
    private void hideSuggestion() {
        getBinding().cardSmartSuggestion.setVisibility(View.GONE);
    }

    @Override
    public void setOnClick() {
        getBinding().ivAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditReminderActivity.class);
            startActivity(intent);
        });
        
        getBinding().btnSuggestionCreate.setOnClickListener(v -> {
            if (currentSuggestion != null) {
                Intent intent = new Intent(requireContext(), AddEditReminderActivity.class);
                intent.putExtra("suggestionId", currentSuggestion.getSuggestionId());
                intent.putExtra("suggestedTitle", currentSuggestion.getSuggestedTitle());
                intent.putExtra("suggestedTime", currentSuggestion.getSuggestedTime());
                intent.putExtra("suggestedFrequency", currentSuggestion.getSuggestedFrequency());
                startActivity(intent);
            } else {
                Intent intent = new Intent(requireContext(), AddEditReminderActivity.class);
                startActivity(intent);
            }
        });
        
        getBinding().btnSuggestionLater.setOnClickListener(v -> {
            hideSuggestion();
            Toast.makeText(requireContext(), getString(R.string.later), Toast.LENGTH_SHORT).show();
        });
    }
}
