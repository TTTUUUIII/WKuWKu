package ink.snowland.wkuwku.ui.home;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationBarView;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentHomeBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.ui.coreopt.CoreOptionsFragment;
import ink.snowland.wkuwku.ui.game.GamesFragment;
import ink.snowland.wkuwku.ui.history.HistoryFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeFragment extends BaseFragment implements NavigationBarView.OnItemSelectedListener {

    private static boolean isFirstLaunch = true;

    private FragmentHomeBinding binding;
    private final BaseFragment[] mPages = new BaseFragment[] {
            new GamesFragment(),
            new HistoryFragment(),
            new CoreOptionsFragment()
    };

    private final ViewPager2.OnPageChangeCallback mPageChangedCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            final BaseFragment fragment = mPages[position];
            parentActivity.setActionbarTitle(fragment.getTitleRes());
            int itemId = binding.bottomNavView.getSelectedItemId();
            if (position != getDestinationIndex(itemId)) {
                binding.bottomNavView.setSelectedItemId(getItemId(position));
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(getLayoutInflater());
        binding.viewPager.setAdapter(new PagerSlideAdapter());
        binding.viewPager.registerOnPageChangeCallback(mPageChangedCallback);
        binding.bottomNavView.setOnItemSelectedListener(this);
        if (isFirstLaunch) {
            isFirstLaunch = false;
            Disposable ignored = AppDatabase.db.gameInfoDao()
                    .isExistsHistory()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(exists -> {
                        if (exists) {
                            binding.viewPager.setCurrentItem(1, false);
                        }
                    }, error -> {/*ignored*/});
        }
        return binding.getRoot();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        int position = getDestinationIndex(itemId);
        if (position != binding.viewPager.getCurrentItem()) {
            binding.viewPager.setCurrentItem(position);
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.viewPager.unregisterOnPageChangeCallback(mPageChangedCallback);
    }

    private int getDestinationIndex(@IdRes int resId) {
        if (resId == R.id.game_fragment) {
            return 0;
        } else if (resId == R.id.history_fragment) {
            return 1;
        } else if (resId == R.id.core_fragment) {
            return 2;
        } else {
            throw new RuntimeException();
        }
    }

    private int getItemId(int position) {
        if (position == 0) {
            return R.id.game_fragment;
        } else if (position == 1) {
            return R.id.history_fragment;
        } else if (position == 2) {
            return R.id.core_fragment;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    private class PagerSlideAdapter extends FragmentStateAdapter {

        public PagerSlideAdapter() {
            super(parentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mPages[position];
        }

        @Override
        public int getItemCount() {
            return mPages.length;
        }
    }
}