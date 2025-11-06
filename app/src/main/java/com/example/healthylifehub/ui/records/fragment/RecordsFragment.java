package com.example.healthylifehub.ui.records.fragment;

import androidx.lifecycle.ViewModelProvider;
import com.example.healthylifehub.base.BaseFragment;
import com.example.healthylifehub.base.BaseViewModel;
import com.example.healthylifehub.databinding.FragmentRecordsBinding;

public class RecordsFragment extends BaseFragment<FragmentRecordsBinding> {

    private RecordsViewModel viewModel;

    public RecordsFragment() {
        super(FragmentRecordsBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(RecordsViewModel.class);
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
            if (getActivity() instanceof com.example.healthylifehub.utils.navigation.DrawerController) {
                ((com.example.healthylifehub.utils.navigation.DrawerController) getActivity()).openDrawer();
            }
        });
    }
}
