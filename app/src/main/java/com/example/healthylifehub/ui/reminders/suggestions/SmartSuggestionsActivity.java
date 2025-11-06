package com.example.healthylifehub.ui.reminders.suggestions;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthylifehub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class SmartSuggestionsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private LinearLayout llSuggestionsContainer, llEmptyState;
    private MaterialButton btnSkipOptimize, btnApplyOptimize;
    private MaterialButton btnSkipWater, btnCreateWater;
    private MaterialButton btnSkipMerge, btnMerge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_suggestions);

        initViews();
        setupListeners();
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

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // Optimize Reminder
        btnSkipOptimize.setOnClickListener(v -> {
            showSnackbar("Đã bỏ qua gợi ý");
            // TODO: Mark suggestion as skipped
        });

        btnApplyOptimize.setOnClickListener(v -> {
            showSnackbar("Đã áp dụng thay đổi");
            // TODO: Apply optimization
        });

        // Water Reminder
        btnSkipWater.setOnClickListener(v -> {
            showSnackbar("Đã bỏ qua gợi ý");
            // TODO: Mark suggestion as skipped
        });

        btnCreateWater.setOnClickListener(v -> {
            showSnackbar("Đã tạo lời nhắc uống nước");
            // TODO: Create water reminder
        });

        // Merge Reminders
        btnSkipMerge.setOnClickListener(v -> {
            showSnackbar("Đã bỏ qua gợi ý");
            // TODO: Mark suggestion as skipped
        });

        btnMerge.setOnClickListener(v -> {
            showSnackbar("Đã gộp lời nhắc");
            // TODO: Merge reminders
        });
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
