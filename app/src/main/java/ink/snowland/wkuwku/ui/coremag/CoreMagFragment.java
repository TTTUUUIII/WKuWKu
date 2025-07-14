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
import ink.snowland.wkuwku.databinding.ItemCoreMetadataBinding;

public class CoreMagFragment extends BaseFragment {
    private FragmentCoreMagBinding binding;
    private CoreMagViewModel mViewModel;
    private final CoreMetadataAdapter mAdapter = new CoreMetadataAdapter();
    private final List<CoreManifest.CoreElement> mCoreList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CoreMagViewModel.class);
        parentActivity.setActionbarTitle(R.string.core_manage);
        mViewModel.getManifest().observe(this, manifest -> {
            mCoreList.addAll(manifest.cores);
            mAdapter.notifyItemRangeInserted(0, mCoreList.size());
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

    private class CoreMetadataHolder extends RecyclerView.ViewHolder {

        private final ItemCoreMetadataBinding itemBinding;

        public CoreMetadataHolder(@NonNull ItemCoreMetadataBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(CoreManifest.CoreElement data) {
            itemBinding.system.setText(data.alias);
        }
    }

    private class CoreMetadataAdapter extends RecyclerView.Adapter<CoreMetadataHolder> {

        @NonNull
        @Override
        public CoreMetadataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCoreMetadataBinding itemBinding = ItemCoreMetadataBinding.inflate(getLayoutInflater(), parent, false);
            return new CoreMetadataHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull CoreMetadataHolder holder, int position) {
            holder.bind(mCoreList.get(position));
        }

        @Override
        public int getItemCount() {
            return mCoreList.size();
        }
    }
}