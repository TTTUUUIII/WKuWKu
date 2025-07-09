package ink.snowland.wkuwku.ui.coremag;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.CoreManifest;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentCoreMagBinding;
import ink.snowland.wkuwku.databinding.ItemCoremagBinding;

public class CoreMagFragment extends BaseFragment {
    private FragmentCoreMagBinding binding;
    private CoreMagViewModel mViewModel;
    private final CoreAdapter mAdapter = new CoreAdapter();
    private final List<CoreManifest.SystemElement> mSystems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CoreMagViewModel.class);
        parentActivity.setActionbarTitle(R.string.core_manage);
        mViewModel.getManifest().observe(this, manifest -> {
            mSystems.clear();
            manifest.manufacturers.forEach(it -> {
                mSystems.addAll(it.systems);
            });
            mAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCoreMagBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
        return binding.getRoot();
    }

    private class CoreHolder extends RecyclerView.ViewHolder {

        private final ItemCoremagBinding itemBinding;
        public CoreHolder(@NonNull ItemCoremagBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(CoreManifest.SystemElement data) {
            itemBinding.system.setText(data.name);
            itemBinding.foldButton.setOnClickListener(v -> {

            });
        }
    }

    private class CoreAdapter extends RecyclerView.Adapter<CoreHolder> {

        @NonNull
        @Override
        public CoreHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCoremagBinding itemBinding = ItemCoremagBinding.inflate(getLayoutInflater(), parent, false);
            return new CoreHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull CoreHolder holder, int position) {
            holder.bind(mSystems.get(position));
        }

        @Override
        public int getItemCount() {
            return mSystems.size();
        }
    }
}