package ink.snowland.wkuwku.ui.coreopt;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import android.os.SystemClock;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.BaseTextWatcher;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.databinding.FragmentCoreoptBinding;
import ink.snowland.wkuwku.databinding.ItemCoreEnumOptionBinding;
import ink.snowland.wkuwku.databinding.ItemCoreOptionBinding;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.NoFilterArrayAdapter;

public class CoreOptionsFragment extends BaseFragment {

    private static final String SELECTED_CORE = "app_selected_core";
    private FragmentCoreoptBinding binding;
    private IEmulator mEmulator;
    private final ViewAdapter mAdapter = new ViewAdapter();
    private List<EmOption> mCurrentOptions;
    private CoreOptionsViewModel mViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreoptBinding.inflate(inflater, container, false);
        mViewModel = new ViewModelProvider(this).get(CoreOptionsViewModel.class);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.pendingIndicator.setDataModel(mViewModel);
        binding.pendingIndicator.setLifecycleOwner(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Collection<IEmulator> emulators = EmulatorManager.getEmulators();
        String[] tags = emulators.stream().map(it -> (String) it.getProp(IEmulator.PROP_ALIAS))
                .toArray(String[]::new);
        binding.coreSelector.setAdapter(new NoFilterArrayAdapter<>(requireActivity(), R.layout.layout_simple_text, tags));
        mViewModel.setPendingIndicator(true, R.string.loading);
        runAtTime(() -> {
            binding.coreSelector.addTextChangedListener((BaseTextWatcher) (alias, start, before, count) -> onEmulatorChanged(alias.toString()));
            binding.coreSelector.setText(SettingsManager.getString(SELECTED_CORE, "fceumm"));
        }, SystemClock.uptimeMillis() + 400);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentEmulatorOptions();
    }

    private void onEmulatorChanged(@NonNull String alias) {
        IEmulator emulator = EmulatorManager.getEmulator(alias);
        if (emulator != null && mEmulator != emulator) {
            saveCurrentEmulatorOptions();
            mEmulator = emulator;
            SettingsManager.putString(SELECTED_CORE, (String) mEmulator.getProp(IEmulator.PROP_ALIAS));
            mCurrentOptions = mViewModel.getEmulatorOptions(mEmulator);
            if (mCurrentOptions == null) {
                List<EmOption> options = mEmulator.getOptions().stream()
                        .sorted()
                        .collect(Collectors.toList());
                for (EmOption option : options) {
                    String val = SettingsManager.getString(option.key);
                    if (val.isEmpty()) continue;
                    option.val = val;
                }
                mCurrentOptions = options;
                mViewModel.putEmulatorOptions(mEmulator, options);
            }
            mAdapter.submitList(mCurrentOptions);
        }
        mViewModel.setPendingIndicator(false);
    }

    private void saveCurrentEmulatorOptions() {
        if (mCurrentOptions == null || mEmulator == null) return;
        for (EmOption option : mCurrentOptions) {
            if (!option.enable) continue;
            SettingsManager.putString(option.key, option.val);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewBinding _binding;

        public ViewHolder(@NonNull ViewBinding itemBinding) {
            super(itemBinding.getRoot());
            _binding = itemBinding;
        }

        public void bind(@NonNull EmOption option) {
            if (_binding instanceof ItemCoreOptionBinding) {
                ItemCoreOptionBinding itemBinding = (ItemCoreOptionBinding) _binding;
                itemBinding.setOption(option);
                switch (option.inputType) {
                    case EmOption.NUMBER:
                        itemBinding.editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        break;
                    case EmOption.NUMBER_DECIMAL:
                        itemBinding.editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        break;
                    case EmOption.NUMBER_SIGNED:
                        itemBinding.editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        break;
                }
            } else {
                ItemCoreEnumOptionBinding itemBinding = (ItemCoreEnumOptionBinding) _binding;
                NoFilterArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(parentActivity, R.layout.layout_simple_text, option.allowVals);
                itemBinding.autoComplete.setAdapter(adapter);
                itemBinding.setOption(option);
            }
        }
    }

    private class ViewAdapter extends ListAdapter<EmOption, ViewHolder> {
        protected ViewAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull EmOption oldItem, @NonNull EmOption newItem) {
                    return oldItem.key.equals(newItem.key);
                }

                @Override
                public boolean areContentsTheSame(@NonNull EmOption oldItem, @NonNull EmOption newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TEXT) {
                return new ViewHolder(ItemCoreOptionBinding.inflate(getLayoutInflater(), parent, false));
            } else {
                return new ViewHolder(ItemCoreEnumOptionBinding.inflate(getLayoutInflater(), parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        @Override
        public int getItemViewType(int position) {
            EmOption option = getItem(position);
            return option.allowVals == null || option.allowVals.length == 0 ? TEXT : ENUM;
        }
    }

    private static final int ENUM = 1;
    private static final int TEXT = 2;
}