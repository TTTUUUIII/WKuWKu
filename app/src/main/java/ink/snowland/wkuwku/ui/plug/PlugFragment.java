package ink.snowland.wkuwku.ui.plug;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;


import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentPlugBinding;
import ink.snowland.wkuwku.databinding.ItemPlugBinding;
import ink.snowland.wkuwku.databinding.LayoutPlugAvailableBinding;
import ink.snowland.wkuwku.databinding.LayoutPlugInstalledBinding;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import ink.snowland.wkuwku.util.PlugManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlugFragment extends BaseFragment implements TabLayout.OnTabSelectedListener {
    private static final int INSTALLED_SCREEN = 0;
    private static final int AVAILABLE_SCREEN = 1;
    private FragmentPlugBinding binding;

    private PlugViewModel mViewModel;
    private final PagerAdapter mPagerAdapter = new PagerAdapter();
    private final PlugViewAdapter<PlugManifestExt> mInstalledPlugAdapter = new PlugViewAdapter<>() {
        @Override
        public void submitList(@Nullable List<PlugManifestExt> list) {
            super.submitList(list);
            mPlugInstalledBinding.emptyListIndicator.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        }
    };
    //    private final PlugViewAdapter mAvailablePlugAdapter = new PlugViewAdapter();
    private Disposable mDisposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PlugViewModel.class);
        mDisposable = mViewModel.getInstalledPlug()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mInstalledPlugAdapter::submitList, error -> {
                    error.printStackTrace(System.err);
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlugBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tabLayout.addOnTabSelectedListener(this);
        binding.viewPager.setAdapter(mPagerAdapter);
        binding.viewPager.setUserInputEnabled(false);
        parentActivity.setActionbarTitle(R.string.extension_manage);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        binding.viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    private LayoutPlugInstalledBinding mPlugInstalledBinding;
    private LayoutPlugAvailableBinding mPlugAvailableBinding;

    private final class PagerViewHolder extends RecyclerView.ViewHolder {

        public PagerViewHolder(int viewType) {
            super(viewType == INSTALLED_SCREEN ? mPlugInstalledBinding.getRoot() : mPlugAvailableBinding.getRoot());
        }

        public void bind(int viewType) {
            if (viewType == INSTALLED_SCREEN) {
                mPlugInstalledBinding.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
                mPlugInstalledBinding.recyclerView.setAdapter(mInstalledPlugAdapter);
            } else {
//                LayoutPlugAvailableBinding _bind = (LayoutPlugAvailableBinding) itemBinding;
//                _bind.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
//                _bind.recyclerView.setAdapter(mAvailablePlugAdapter);
//                _bind.emptyListIndicator.setVisibility(mAvailablePlugAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    private final class PagerAdapter extends RecyclerView.Adapter<PagerViewHolder> {

        @NonNull
        @Override
        public PagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == INSTALLED_SCREEN) {
                mPlugInstalledBinding = LayoutPlugInstalledBinding.inflate(getLayoutInflater(), parent, false);
            } else {
                mPlugAvailableBinding = LayoutPlugAvailableBinding.inflate(getLayoutInflater(), parent, false);
            }
            return new PagerViewHolder(viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull PagerViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return binding.tabLayout.getTabCount();
        }
    }

    private void showDeletePlugDialog(@NonNull PlugManifestExt manifest) {
        new MaterialAlertDialogBuilder(parentActivity)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.emergency)
                .setMessage(getString(R.string.fmt_delete_plug, manifest.origin.name))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    PlugManager.uninstall(manifest.origin, null);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private final class PlugViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlugBinding itemBinding;

        public PlugViewHolder(@NonNull ItemPlugBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(Object o) {
            if (o instanceof PlugManifestExt) {
                PlugManifestExt manifest = (PlugManifestExt) o;
                Drawable icon = PlugManager.getPlugIcon(manifest.origin);
                itemBinding.setManifest(manifest.origin);
                if (icon != null) {
                    Glide.with(parentActivity)
                            .load(icon)
                            .into(itemBinding.plugIcon);
                }
                itemBinding.control.setText(manifest.enabled ? R.string.disable : R.string.enable);
                itemBinding.control.setOnClickListener(v -> {
                    manifest.enabled = !manifest.enabled;
                    mViewModel.update(manifest)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete(() -> {
                                itemBinding.control.setText(manifest.enabled ? R.string.disable : R.string.enable);
                                if (manifest.enabled && !PlugManager.isInstalled(manifest.origin)) {
                                    PlugManager.install(manifest.origin, null);
                                }
                            })
                            .subscribe();
                });
                itemBinding.uninstall.setOnClickListener(v -> {
                    showDeletePlugDialog(manifest);
                });
            }
        }
    }

    private class PlugViewAdapter<T> extends ListAdapter<T, PlugViewHolder> {


        private PlugViewAdapter() {
            super(new DiffUtil.ItemCallback<T>() {
                @Override
                public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                    return oldItem == newItem;
                }

                @SuppressLint("DiffUtilEquals")
                @Override
                public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @NonNull
        @Override
        public PlugViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PlugViewHolder(ItemPlugBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PlugViewHolder holder, int position) {
            holder.bind(getItem(position));
        }
    }
}