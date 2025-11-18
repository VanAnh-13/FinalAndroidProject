package com.example.healthylifehub.ui.analytics.enhanced;

import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityEnhancedAnalyticsBinding;

/**
 * Activity to host EnhancedAnalyticsFragment for testing
 */
public class EnhancedAnalyticsActivity extends BaseActivity<ActivityEnhancedAnalyticsBinding> {
    
    public EnhancedAnalyticsActivity() {
        super(ActivityEnhancedAnalyticsBinding::inflate);
    }
    
    @Override
    public void initData() {
        // Setup toolbar
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Phân tích nâng cao");
        }
        
        // Add fragment
        getSupportFragmentManager()
            .beginTransaction()
            .replace(getBinding().fragmentContainer.getId(), new EnhancedAnalyticsFragment())
            .commit();
    }
    
    @Override
    public void bindData() {
        // Nothing to bind for container activity
    }
    
    @Override
    public void setOnClick() {
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
    }
}