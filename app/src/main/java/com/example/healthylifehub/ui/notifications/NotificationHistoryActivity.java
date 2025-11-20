package com.example.healthylifehub.ui.notifications;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthylifehub.R;
import com.google.android.material.tabs.TabLayout;

/**
 * Activity hiển thị lịch sử thông báo
 */
public class NotificationHistoryActivity extends AppCompatActivity {
    
    private NotificationHistoryViewModel viewModel;
    private RecyclerView recyclerView;
    private NotificationHistoryAdapter adapter;
    private TextView emptyView;
    private TabLayout tabLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_history);
        
        setupToolbar();
        initViews();
        setupViewModel();
        setupRecyclerView();
        setupTabs();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử thông báo");
        }
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        emptyView = findViewById(R.id.textEmptyView);
        tabLayout = findViewById(R.id.tabLayout);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotificationHistoryViewModel.class);
        
        // Observe notifications
        viewModel.getNotifications().observe(this, notifications -> {
            if (notifications == null || notifications.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter.submitList(notifications);
            }
        });
    }
    
    private void setupRecyclerView() {
        adapter = new NotificationHistoryAdapter(notification -> {
            // Mark as read when clicked
            viewModel.markAsRead(notification.getNotificationId());
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chưa đọc"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    viewModel.loadAllNotifications();
                } else {
                    viewModel.loadUnreadNotifications();
                }
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
