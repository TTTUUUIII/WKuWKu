package ink.snowland.wkuwku.ui.home;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationBarView;

import java.security.InvalidParameterException;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentHomeBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.ui.coreopt.CoreOptionsFragment;
import ink.snowland.wkuwku.ui.game.GamesFragment;
import ink.snowland.wkuwku.ui.history.HistoryFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeFragment extends BaseFragment implements NavigationBarView.OnItemSelectedListener, View.OnClickListener {

    private static boolean isFirstLaunch = true;

    private FragmentHomeBinding binding;
    private PagerSlideAdapter mPagerAdapter;
    private final ViewPager2.OnPageChangeCallback mPageChangedCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            parentActivity.setActionbarTitle(mPagerAdapter.getTitleRes(position));
            parentActivity.setSearchEnable(position == 0);
            binding.prev.setEnabled(position != 0);
            binding.next.setEnabled(position != 2);
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
        mPagerAdapter = new PagerSlideAdapter(this);
        binding.viewPager.setAdapter(mPagerAdapter);
        binding.viewPager.registerOnPageChangeCallback(mPageChangedCallback);
        binding.bottomNavView.setOnItemSelectedListener(this);
        if (isFirstLaunch) {
            isFirstLaunch = false;
            AppDatabase.getDefault()
                    .gameInfoDao()
                    .isExistsHistory()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(exists -> {
                        if (exists) {
                            binding.viewPager.setCurrentItem(1, false);
                        }
                    })
                    .onErrorComplete()
                    .subscribe();
        }
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                && configuration.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) {
            binding.bottomNavView.setVisibility(View.GONE);
            binding.next.setVisibility(View.VISIBLE);
            binding.prev.setVisibility(View.VISIBLE);
            binding.next.setOnClickListener(this);
            binding.prev.setOnClickListener(this);
        }
        /*Disable default padding*/
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavView, null);
        return binding.getRoot();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        int position = getDestinationIndex(itemId);
        if (position != binding.viewPager.getCurrentItem()) {
            binding.viewPager.setCurrentItem(position, getNavAnimOptions() != null);
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.viewPager.unregisterOnPageChangeCallback(mPageChangedCallback);
        mPagerAdapter = null;
        binding = null;
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

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        int position = binding.viewPager.getCurrentItem();
        if (viewId == R.id.prev) {
            position -= 1;
        } else {
            position += 1;
        }
        if (position >= 0 && position < 3) {
            binding.viewPager.setCurrentItem(position, getNavAnimOptions() != null);
        }
    }

    private static class PagerSlideAdapter extends FragmentStateAdapter {

        public PagerSlideAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new GamesFragment();
                case 1: return new HistoryFragment();
                case 2: return new CoreOptionsFragment();
                default:
                    throw new InvalidParameterException();
            }
        }

        public int getTitleRes(int position) {
            switch (position) {
                case 0: return R.string.all_games;
                case 1: return R.string.recent_played;
                case 2: return R.string.core_options;
                default:
                    throw new InvalidParameterException();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}