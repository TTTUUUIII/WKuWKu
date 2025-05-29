package ink.snowland.wkuwku.ui.plug;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.tabs.TabLayout;


import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentPlugBinding;
import ink.snowland.wkuwku.databinding.ItemPlugBinding;
import ink.snowland.wkuwku.databinding.LayoutPlugAvailableBinding;
import ink.snowland.wkuwku.databinding.LayoutPlugInstalledBinding;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import ink.snowland.wkuwku.plug.PlugManifest;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlugFragment extends BaseFragment implements TabLayout.OnTabSelectedListener {
    private static final int INSTALLED_SCREEN = 0;
    private static final int AVAILABLE_SCREEN = 1;
    private FragmentPlugBinding binding;

    private PlugViewModel mViewModel;
    private final PagerAdapter mPagerAdapter = new PagerAdapter();
    private final PlugViewAdapter<PlugManifestExt> mInstalledPlugAdapter = new PlugViewAdapter<>();
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

    private final class PagerViewHolder extends RecyclerView.ViewHolder {
        private final ViewBinding itemBinding;

        public PagerViewHolder(@NonNull ViewBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind() {
            if (itemBinding instanceof LayoutPlugInstalledBinding) {
                LayoutPlugInstalledBinding _bind = (LayoutPlugInstalledBinding) itemBinding;
                _bind.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
                _bind.recyclerView.setAdapter(mInstalledPlugAdapter);
                _bind.emptyListIndicator.setVisibility(mInstalledPlugAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            } else {
                LayoutPlugAvailableBinding _bind = (LayoutPlugAvailableBinding) itemBinding;
                _bind.recyclerView.setLayoutManager(new LinearLayoutManager(parentActivity));
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
                return new PagerViewHolder(LayoutPlugInstalledBinding.inflate(getLayoutInflater(), parent, false));
            } else {
                return new PagerViewHolder(LayoutPlugAvailableBinding.inflate(getLayoutInflater(), parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull PagerViewHolder holder, int position) {
            holder.bind();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
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
                itemBinding.plugName.setText(manifest.origin.name);
                itemBinding.plugSummary.setText(manifest.origin.summary);
            }
        }
    }

    private final class PlugViewAdapter<T> extends ListAdapter<T, PlugViewHolder> {


        protected PlugViewAdapter() {
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