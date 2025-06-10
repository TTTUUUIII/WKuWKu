package ink.snowland.wkuwku.ui.plug;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.PlugRes;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentPlugBinding;
import ink.snowland.wkuwku.databinding.ItemPlugManifestBinding;
import ink.snowland.wkuwku.databinding.ItemPlugResBinding;
import ink.snowland.wkuwku.databinding.LayoutPlugAvailableBinding;
import ink.snowland.wkuwku.databinding.LayoutPlugInstalledBinding;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.PlugManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
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
            if (mPlugInstalledBinding != null) {
                mPlugInstalledBinding.emptyListIndicator.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    };
    private final PlugViewAdapter<PlugRes> mAvailablePlugAdapter = new PlugViewAdapter<>() {
        @Override
        public void submitList(@Nullable List<PlugRes> list) {
            super.submitList(list);
            if (mPlugAvailableBinding != null) {
                mPlugAvailableBinding.emptyListIndicator.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    };

    private boolean mAvailablePlugListLoaded = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PlugViewModel.class);
        Disposable disposable = mViewModel.getAvailablePlugInfos()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    mAvailablePlugListLoaded = true;
                    mViewModel.setPendingIndicator(false);
                })
                .subscribe(mAvailablePlugAdapter::submitList, error -> {
                    error.printStackTrace(System.err);
                });
        mViewModel.getAll().observe(this, mInstalledPlugAdapter::submitList);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlugBinding.inflate(inflater, container, false);
        binding.loadingIndicator.setLifecycleOwner(this);
        binding.loadingIndicator.setDataModel(mViewModel);
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
        int screen = tab.getPosition();
        binding.viewPager.setCurrentItem(screen);
        if (screen == AVAILABLE_SCREEN && !mAvailablePlugListLoaded) {
            mViewModel.setPendingIndicator(true, R.string.loading);
        }
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
                DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
                mPlugInstalledBinding.recyclerView.addItemDecoration(decoration);
                mPlugInstalledBinding.emptyListIndicator.setVisibility(mInstalledPlugAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                mPlugInstalledBinding.installFromStorage.setOnClickListener(v -> {
                    parentActivity.openDocument("application/vnd.android.package-archive", plugUri -> {
                        if (plugUri == null) return;
                        File temp = new File(FileManager.getCacheDirectory(), ".plug.apk");
                        mViewModel.setPendingIndicator(true, R.string.copying_files);
                        try (InputStream fis = parentActivity.getContentResolver().openInputStream(plugUri)) {
                            if (fis == null) return;
                            FileManager.copy(fis, temp);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                            mViewModel.setPendingIndicator(false);
                            FileManager.delete(temp);
                        }
                        if (temp.exists()) {
                            mViewModel.setPendingMessage(R.string.installing);
                            PlugManager.install(temp, new PlugManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    FileManager.delete(temp);
                                    mViewModel.setPendingIndicator(false);
                                }

                                @Override
                                public void onFailure(Throwable e) {
                                    e.printStackTrace(System.err);
                                    Toast.makeText(parentActivity, R.string.install_failed, Toast.LENGTH_SHORT).show();
                                    FileManager.delete(temp);
                                    mViewModel.setPendingIndicator(false);
                                }
                            });
                        }
                    });
                });
            } else {
                mPlugAvailableBinding.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
                mPlugAvailableBinding.recyclerView.setAdapter(mAvailablePlugAdapter);
                DividerItemDecoration decoration = new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL);
                mPlugAvailableBinding.recyclerView.addItemDecoration(decoration);
                if (mAvailablePlugListLoaded) {
                    mPlugAvailableBinding.emptyListIndicator.setVisibility(mAvailablePlugAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            }
        }
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
        private final ViewBinding itemBinding;

        public PlugViewHolder(@NonNull ViewBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(Object o) {
            if (itemBinding instanceof ItemPlugManifestBinding && o instanceof PlugManifestExt) {
                PlugManifestExt manifest = (PlugManifestExt) o;
                Drawable icon = PlugManager.getPlugIcon(manifest.origin);
                ItemPlugManifestBinding _binding = (ItemPlugManifestBinding) itemBinding;
                _binding.setManifest(manifest.origin);
                if (icon != null) {
                    Glide.with(parentActivity)
                            .load(icon)
                            .into(_binding.plugIcon);
                }
                _binding.control.setText(manifest.enabled ? R.string.disable : R.string.enable);
                _binding.control.setOnClickListener(v -> {
                    manifest.enabled = !manifest.enabled;
                    mViewModel.update(manifest)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete(() -> {
                                _binding.control.setText(manifest.enabled ? R.string.disable : R.string.enable);
                                if (manifest.enabled && !PlugManager.isInstalled(manifest.origin)) {
                                    PlugManager.install(manifest.origin, null);
                                }
                            })
                            .subscribe();
                });
                _binding.uninstall.setOnClickListener(v -> {
                    showDeletePlugDialog(manifest);
                });
            } else if (itemBinding instanceof ItemPlugResBinding && o instanceof PlugRes) {
                PlugRes res = (PlugRes) o;
                ItemPlugResBinding _binding = (ItemPlugResBinding) itemBinding;
                _binding.setPlugRes(res);
                if (res.iconUrl != null) {
                    Glide.with(parentActivity)
                            .load(res.iconUrl)
                            .error(R.drawable.ic_extension)
                            .into(_binding.plugIcon);
                }
                final PlugManifestExt plug = mViewModel.findInstalledPlug(res.packageName);
                if (plug != null) {
                    _binding.installButton.setEnabled(plug.origin.getVersionCode() < res.versionCode);
                    _binding.installButton.setTag(_binding.installButton.isEnabled() ? "upgrade" : "installed");
                    _binding.installButton.setText(_binding.installButton.isEnabled() ? R.string.upgrade : R.string.installed);
                }
                _binding.installButton.setOnClickListener(v -> {
                    if ("upgrade".equals(v.getTag())) return;
                    _binding.installButton.setText(R.string.connecting);
                    _binding.installButton.setEnabled(false);
                    Disposable disposable = Single.create((SingleOnSubscribe<File>) emitter -> {
                                final File temp = new File(FileManager.getCacheDirectory(), ".plug.apk");
                                URL url = new URL(res.url);
                                FileManager.copy(url, temp, (progress, max) -> handler.post(() -> _binding.installButton.setText(getString(R.string.fmt_downloading, (float) progress / max * 100))));
                                emitter.onSuccess(temp);
                            }).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess(file -> {
                                _binding.installButton.setText(getString(R.string.installing));
                                PlugManager.install(file, new PlugManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        _binding.installButton.setText(R.string.installed);
                                        FileManager.delete(file);
                                    }

                                    @Override
                                    public void onFailure(Throwable e) {
                                        _binding.installButton.setText(R.string.install);
                                        _binding.installButton.setEnabled(true);
                                        e.printStackTrace(System.err);
                                        Toast.makeText(parentActivity, R.string.install_failed, Toast.LENGTH_SHORT).show();
                                        FileManager.delete(file);
                                    }
                                });
                            })
                            .subscribe((file, error) -> {
                                if (error != null) {
                                    _binding.installButton.setText(R.string.install);
                                    _binding.installButton.setEnabled(true);
                                    Toast.makeText(parentActivity, R.string.install_failed, Toast.LENGTH_SHORT).show();
                                    error.printStackTrace(System.err);
                                }
                            });
                });
            }
        }
    }

    private class PlugViewAdapter<T> extends ListAdapter<T, PlugViewHolder> {


        private PlugViewAdapter() {
            super(new DiffUtil.ItemCallback<T>() {
                @Override
                public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                    return oldItem.equals(newItem);
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
            if (viewType == INSTALLED_SCREEN) {
                return new PlugViewHolder(ItemPlugManifestBinding.inflate(getLayoutInflater(), parent, false));
            } else {
                return new PlugViewHolder(ItemPlugResBinding.inflate(getLayoutInflater(), parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull PlugViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof PlugManifestExt ? INSTALLED_SCREEN : AVAILABLE_SCREEN;
        }
    }
}