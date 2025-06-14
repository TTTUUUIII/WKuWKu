package ink.snowland.wkuwku.ui.macro;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentMacroBinding;
import ink.snowland.wkuwku.databinding.ItemMacroBinding;
import ink.snowland.wkuwku.db.entity.MacroScript;
import ink.snowland.wkuwku.widget.MacroEditDialog;

public class MacroFragment extends BaseFragment implements View.OnClickListener {
    private MacroViewModel mViewModel;
    private FragmentMacroBinding binding;
    private MacroEditDialog mMacroEditDialog;
    private ViewAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int submitDelayedMillis = savedInstanceState == null ? 300 : 0;
        mViewModel = new ViewModelProvider(this).get(MacroViewModel.class);
        mAdapter = new ViewAdapter();
        mViewModel.getAll()
                .observe(this, data -> submitDelayed(data, mAdapter, submitDelayedMillis));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMacroBinding.inflate(getLayoutInflater());
        binding.fab.setOnClickListener(this);
        parentActivity.setActionbarTitle(R.string.key_macro_manage);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
        DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(decoration);
        return binding.getRoot();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.fab) {
            showEditDialog(null);
        }
    }

    private void showEditDialog(@Nullable MacroScript base) {
        if (mMacroEditDialog == null)
            mMacroEditDialog = new MacroEditDialog(parentActivity);
        if (base == null) {
            mMacroEditDialog.show(mViewModel::add);
        } else {
            mMacroEditDialog.show(mViewModel::update, base);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMacroBinding itemBinding;

        public ViewHolder(@NonNull ItemMacroBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(@NonNull MacroScript script) {
            itemBinding.setScript(script);
            itemBinding.buttonDelete.setOnClickListener(v -> {
                mViewModel.delete(script);
            });
            itemBinding.buttonEdit.setOnClickListener(v -> {
                showEditDialog(script);
            });
        }
    }

    private class ViewAdapter extends ListAdapter<MacroScript, ViewHolder> {

        protected ViewAdapter() {
            super(new DiffUtil.ItemCallback<MacroScript>() {
                @Override
                public boolean areItemsTheSame(@NonNull MacroScript oldItem, @NonNull MacroScript newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull MacroScript oldItem, @NonNull MacroScript newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemMacroBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position));
        }
    }
}