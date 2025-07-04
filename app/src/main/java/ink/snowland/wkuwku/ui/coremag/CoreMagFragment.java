package ink.snowland.wkuwku.ui.coremag;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentCoreMagBinding;

public class CoreMagFragment extends BaseFragment {
    private FragmentCoreMagBinding binding;
    private CoreMagViewModel mViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CoreMagViewModel.class);
        parentActivity.setActionbarTitle(R.string.core_manage);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreMagBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}