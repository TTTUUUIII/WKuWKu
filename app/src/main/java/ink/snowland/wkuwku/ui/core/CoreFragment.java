package ink.snowland.wkuwku.ui.core;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentCoreBinding;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.widget.NoFilterArrayAdapter;

public class CoreFragment extends BaseFragment {

    private FragmentCoreBinding binding;
    private Emulator mEmulator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreBinding.inflate(inflater);
        mParentActivity.setActionbarSubTitle(R.string.core_options);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Collection<Emulator> emulators = EmulatorManager.getEmulators();
        String[] tags = emulators.stream().map(Emulator::getTag)
                .toArray(String[]::new);
        binding.coreSelector.setAdapter(new NoFilterArrayAdapter<String>(requireActivity(), R.layout.layout_simple_text, tags));
        binding.coreSelector.addTextChangedListener(new CoreChangedCallback());
    }

    private void onReloadOptions() {
        assert mEmulator != null;
    }

    private class CoreChangedCallback implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence newCoreTag, int start, int before, int count) {
            Emulator emulator = EmulatorManager.getEmulatorByTag(newCoreTag.toString());
            if (mEmulator != emulator) {
                mEmulator = emulator;
                if (mEmulator == null) return;
                onReloadOptions();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}