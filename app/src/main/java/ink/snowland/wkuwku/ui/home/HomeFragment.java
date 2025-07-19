package ink.snowland.wkuwku.ui.home;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import ink.snowland.wkuwku.ui.coreopt.CoreOptionsFragment;
import ink.snowland.wkuwku.ui.game.GamesFragment;
import ink.snowland.wkuwku.ui.history.HistoryFragment;

public class HomeFragment extends BaseFragment implements NavigationBarView.OnItemSelectedListener {

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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.viewPager.setAdapter(new PagerSlideAdapter());
        binding.viewPager.registerOnPageChangeCallback(mPageChangedCallback);
        binding.bottomNavView.setOnItemSelectedListener(this);
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