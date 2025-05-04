package ink.snowland.wkuwku.ui.core;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.databinding.FragmentCoreBinding;

public class CoreFragment extends Fragment {

    private FragmentCoreBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreBinding.inflate(inflater);
        return binding.getRoot();
    }
}