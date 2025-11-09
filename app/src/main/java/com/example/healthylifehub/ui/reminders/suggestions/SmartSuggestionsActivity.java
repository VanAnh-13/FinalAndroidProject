package com.example.healthylifehub.ui.reminders.suggestions;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthylifehub.R;
import com.example.healthylifehub.data.model.SmartSuggestion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Smart Suggestions Activity
 * Displays AI-generated suggestions for reminder optimization
 */
public class SmartSuggestionsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private LinearLayout llSuggestionsContainer, llEmptyState;
    private MaterialButton btnSkipOptimize, btnApplyOptimize;
    private MaterialButton btnSkipWater, btnCreateWater;
    private MaterialButton btnSkipMerge, btnMerge;
    
    private SmartSuggestionsViewModel viewModel;
    private List<SmartSuggestion> currentSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_suggestions);

        initViews();
        initViewModel();
        setupListeners();
        loadSuggestions();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        llSuggestionsContainer = findViewById(R.id.ll_suggestions_container);
        llEmptyState = findViewById(R.id.ll_empty_state);
        
        btnSkipOptimize = findViewById(R.id.btn_skip_optimize);
        btnApplyOptimize = findViewById(R.id.btn_apply_optimize);
        
        btnSkipWater = findViewById(R.id.btn_skip_water);
        btnCreateWater = findViewById(R.id.btn_create_water);
        
        btnSkipMerge = findViewById(R.id.btn_skip_merge);
        btnMerge = findViewById(R.id.btn_merge);
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SmartSuggestionsViewModel.class);
        
        // Observe suggestions
        viewModel.getSuggestions().observe(this, suggestions -> {
            currentSuggestions = suggestions;
            if (suggestions != null && !suggestions.isEmpty()) {
                showSuggestions();
                updateSuggestionsUI(suggestions);
            } else {
                showEmptyState();
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            // TODO: Show/hide loading indicator
        });
        
        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe has data
        viewModel.getHasData().observe(this, hasData -> {
            if (hasData != null && !hasData) {
                showEmptyState();
            }
        });
    }
    
    private void loadSuggestions() {
        viewModel.loadSuggestions();
    }
    
    private void updateSuggestionsUI(List<SmartSuggestion> suggestions) {
        // Update UI based on suggestion types
        // This is a simplified version - in production you'd dynamically create views
        
        for (SmartSuggestion suggestion : suggestions) {
            switch (suggestion.getType()) {
                case "optimize_time":
                    // Show optimize time suggestion
                    break;
                case "create_reminder":
                    // Show create reminder suggestion
                    break;
                case "merge_reminders":
                    // Show merge suggestion
                    break;
                case "change_frequency":
                    // Show frequency change suggestion
                    break;
            }
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // Optimize Reminder
        btnSkipOptimize.setOnClickListener(v -> {
            SmartSuggestion suggestion = findSuggestionByType("optimize_time");
            if (suggestion != null) {
                viewModel.dismissSuggestion(suggestion);
                showSnackbar("Đã bỏ qua gợi ý");
            }
        });

        btnApplyOptimize.setOnClickListener(v -> {
            SmartSuggestion suggestion = findSuggestionByType("optimize_time");
            if (suggestion != null) {
                viewModel.applySuggestion(suggestion);
                showSnackbar("Đã áp dụng thay đổi");
            }
        });

        // Water Reminder
        btnSkipWater.setOnClickListener(v -> {
            SmartSuggestion suggestion = findSuggestionByType("create_reminder");
            if (suggestion != null) {
                viewModel.dismissSuggestion(suggestion);
                showSnackbar("Đã bỏ qua gợi ý");
            }
        });

        btnCreateWater.setOnClickListener(v -> {
            SmartSuggestion suggestion = findSuggestionByType("create_reminder");
            if (suggestion != null) {
                viewModel.applySuggestion(suggestion);
                showSnackbar("Đã tạo lời nhắc uống nước");
            }
        });

        // Merge Reminders
        btnSkipMerge.setOnClickListener(v -> {
            SmartSuggestion suggestion = findSuggestionByType("merge_reminders");
            if (suggestion != null) {
                viewModel.dismissSuggestion(suggestion);
                showSnackbar("Đã bỏ qua gợi ý");
            }
        });

        btnMerge.setOnClickListener(v -> {
            SmartSuggestion suggestion = findSuggestionByType("merge_reminders");
            if (suggestion != null) {
                viewModel.applySuggestion(suggestion);
                showSnackbar("Đã gộp lời nhắc");
            }
        });
    }
    
    private SmartSuggestion findSuggestionByType(String type) {
        if (currentSuggestions == null) return null;
        
        for (SmartSuggestion suggestion : currentSuggestions) {
            if (type.equals(suggestion.getType())) {
                return suggestion;
            }
        }
        return null;
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        );
        snackbar.setAction("Hoàn tác", v -> {
            Toast.makeText(this, "Đã hoàn tác", Toast.LENGTH_SHORT).show();
        });
        snackbar.show();
    }

    private void showEmptyState() {
        llSuggestionsContainer.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    private void showSuggestions() {
        llSuggestionsContainer.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
    }
}
