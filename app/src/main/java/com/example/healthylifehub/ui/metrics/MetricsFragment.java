package com.example.healthylifehub.ui.metrics;

import androidx.lifecycle.ViewModelProvider;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentMetricsBinding;

public class MetricsFragment extends BaseFragment<FragmentMetricsBinding> {

    private MetricsViewModel viewModel;

    public MetricsFragment() {
        super(FragmentMetricsBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(MetricsViewModel.class);
    }

    @Override
    public void bindData() {
    }

    @Override
    public void observeData() {
    }

    @Override
    public void setOnClick() {
        getBinding().ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.healthylifehub.MainActivity) {
                ((com.example.healthylifehub.MainActivity) getActivity()).openDrawer();
            }
        });
    }
}
