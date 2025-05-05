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

import java.util.ArrayList;
import java.util.Collection;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.databinding.FragmentCoreBinding;
import ink.snowland.wkuwku.databinding.ItemCoreEnumOptionBinding;
import ink.snowland.wkuwku.databinding.ItemCoreOptionBinding;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.widget.NoFilterArrayAdapter;

public class CoreFragment extends BaseFragment {

    private FragmentCoreBinding binding;
    private Emulator mEmulator;
    private final ViewAdapter mAdapter = new ViewAdapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreBinding.inflate(inflater);
        mParentActivity.setActionbarSubTitle(R.string.core_options);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
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
        mAdapter.submitList(new ArrayList<>(mEmulator.getOptions()));
    }

    private class CoreChangedCallback implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence newCoreTag, int start, int before, int count) {
            Emulator emulator = EmulatorManager.getEmulator(newCoreTag.toString());
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

    private class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewBinding _binding;
        public ViewHolder(@NonNull ViewBinding itemBinding) {
            super(itemBinding.getRoot());
            this._binding = itemBinding;
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