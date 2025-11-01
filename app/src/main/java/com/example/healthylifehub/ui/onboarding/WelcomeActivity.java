package com.example.healthylifehub.ui.onboarding;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthylifehub.MainActivity;
import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.OnboardingItem;
import com.example.healthylifehub.databinding.ActivityWelcomeBinding;
import com.example.healthylifehub.ui.auth.LoginActivity;
import com.example.healthylifehub.ui.auth.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends BaseActivity<ActivityWelcomeBinding> {
    private OnboardingAdapter adapter;
    private List<OnboardingItem> onboardingItems;
    private int currentPage = 0;

    public WelcomeActivity() {
        super(ActivityWelcomeBinding::inflate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserLoggedIn()) {
            navigateToDashboard();
            return;
        }
    }

    @Override
    public void initData() {
        setupOnboardingItems();
        setupViewPager();
        setupIndicators();
    }

    @Override
    public void bindData() {
        // Initial data binding (if needed)
    }

    @Override
    public void setOnClick() {
        setupButtons();
    }

    private void setupOnboardingItems() {
        onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_health_tracking,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
        ));
        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_reminder,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
        ));
        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_medical_records,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
        ));
    }

    private void setupViewPager() {
        adapter = new OnboardingAdapter(onboardingItems);
        getBinding().viewPagerOnboarding.setAdapter(adapter);

        getBinding().viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updateIndicators(position);
            }
        });
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingItems.size()];
        int spacing = getResources().getDimensionPixelSize(R.dimen.indicator_spacing);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(spacing, 0, spacing, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(getDrawable(R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(params);
            getBinding().indicatorContainer.addView(indicators[i]);
        }

        if (indicators.length > 0) {
            indicators[0].setImageDrawable(getDrawable(R.drawable.indicator_active));
        }
    }

    private void updateIndicators(int position) {
        int childCount = getBinding().indicatorContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) getBinding().indicatorContainer.getChildAt(i);
            if (i == position) {
                imageView.setImageDrawable(getDrawable(R.drawable.indicator_active));
            } else {
                imageView.setImageDrawable(getDrawable(R.drawable.indicator_inactive));
            }
        }
    }

    private void setupButtons() {
        getBinding().btnRegister.setOnClickListener(v -> {
            navigateToRegister();
        });

        getBinding().btnLogin.setOnClickListener(v -> {
            navigateToLogin();
        });

        getBinding().btnSkip.setOnClickListener(v -> {
            showExitDialog();
        });
    }

    private boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_dialog_title)
                .setMessage(R.string.exit_dialog_message)
                .setPositiveButton(R.string.exit_button, (dialog, which) -> {
                    finishAffinity();
                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
