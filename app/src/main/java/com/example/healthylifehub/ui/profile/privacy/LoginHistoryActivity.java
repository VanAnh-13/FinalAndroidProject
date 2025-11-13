package com.example.healthylifehub.ui.profile.privacy;

import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityLoginHistoryBinding;

/**
 * LoginHistoryActivity - Display user's login history
 */
public class LoginHistoryActivity extends BaseActivity<ActivityLoginHistoryBinding> {
    
    private LoginHistoryViewModel viewModel;
    private LoginHistoryAdapter adapter;
    
    public LoginHistoryActivity() {
        super(ActivityLoginHistoryBinding::inflate);
    }
    
    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(LoginHistoryViewModel.class);
        
        // Setup RecyclerView
        adapter = new LoginHistoryAdapter();
        getBinding().recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getBinding().recyclerView.setAdapter(adapter);
        
        // Load login history
        viewModel.loadLoginHistory();
    }
    
    @Override
    public void bindData() {
        // Setup toolbar
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử đăng nhập");
        }
        
        // Observe data
        viewModel.getLoginHistory().observe(this, history -> {
            if (history != null && !history.isEmpty()) {
                adapter.setLoginHistory(history);
                getBinding().emptyState.setVisibility(View.GONE);
                getBinding().recyclerView.setVisibility(View.VISIBLE);
            } else {
                getBinding().emptyState.setVisibility(View.VISIBLE);
                getBinding().recyclerView.setVisibility(View.GONE);
            }
        });
        
        viewModel.getLoading().observe(this, isLoading -> {
            getBinding().progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
    
    @Override
    public void setOnClick() {
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
    }
}