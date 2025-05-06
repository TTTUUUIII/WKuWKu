package ink.snowland.wkuwku.ui.core;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.databinding.FragmentCoreBinding;
import ink.snowland.wkuwku.databinding.ItemCoreEnumOptionBinding;
import ink.snowland.wkuwku.databinding.ItemCoreOptionBinding;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.util.RxUtils;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.NoFilterArrayAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CoreFragment extends BaseFragment {

    private static final String SELECTED_CORE = "app_selected_core";
    private FragmentCoreBinding binding;
    private Emulator mEmulator;
    private final ViewAdapter mAdapter = new ViewAdapter();
    private List<EmOption> mCurrentOptions;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreBinding.inflate(inflater);
        mParentActivity.setActionbarSubTitle(R.string.core_options);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.recyclerView.setHasFixedSize(true);
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
        binding.coreSelector.setText(SettingsManager.getString(SELECTED_CORE, tags[0]));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onSaveOptions();
    }

    private void onReloadOptions() {
        assert mEmulator != null;
        SettingsManager.putString(SELECTED_CORE, mEmulator.getTag());
        RxUtils.newSingle((RxUtils.SingleFunction<List<EmOption>>) observer -> {
                    List<EmOption> options = mEmulator.getOptions().stream()
                            .sorted()
                            .collect(Collectors.toList());
                    for (EmOption option : options) {
                        String val = SettingsManager.getString(option.key);
                        if (val.isEmpty()) continue;
                        option.val = val;
                    }
                    observer.onSuccess(options);
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(options -> {
                    mCurrentOptions = options;
                    mAdapter.submitList(mCurrentOptions);
                })
                .subscribe();
    }

    private class CoreChangedCallback implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence newCoreTag, int start, int before, int count) {
            Emulator emulator = EmulatorManager.getEmulator(newCoreTag.toString());
            if (mEmulator != emulator) {
                onSaveOptions();
                mEmulator = emulator;
                if (mEmulator == null) return;
                onReloadOptions();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private void onSaveOptions() {
        if (mCurrentOptions == null || mEmulator == null) return;
        for (EmOption option : mCurrentOptions) {
            if (!option.supported) continue;
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
                ((ItemCoreOptionBinding) _binding).setOption(option);
            } else {
                ItemCoreEnumOptionBinding itemBinding = (ItemCoreEnumOptionBinding) _binding;
                NoFilterArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(mParentActivity, R.layout.layout_simple_text, option.allowVals);
                itemBinding.autoComplete.setAdapter(adapter);
                itemBinding.setOption(option);
            }
        }
    }

    private class ViewAdapter extends ListAdapter<EmOption, ViewHolder> {
        protected ViewAdapter() {
            super(new DiffUtil.ItemCallback<EmOption>() {
                @Override
                public boolean areItemsTheSame(@NonNull EmOption oldItem, @NonNull EmOption newItem) {
                    return oldItem == newItem;
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