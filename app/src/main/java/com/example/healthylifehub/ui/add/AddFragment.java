package com.example.healthylifehub.ui.add;

import androidx.lifecycle.ViewModelProvider;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentAddBinding;

public class AddFragment extends BaseFragment<FragmentAddBinding> {

    private AddViewModel viewModel;

    public AddFragment() {
        super(FragmentAddBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(AddViewModel.class);
    }

    @Override
    public void bindData() {
    }

    @Override
    public void observeData() {
    }

    @Override
    public void setOnClick() {
    }
}
