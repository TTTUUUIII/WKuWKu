package ink.snowland.wkuwku.ui.home;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationBarView;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentHomeBinding;

public class HomeFragment extends BaseFragment implements NavigationBarView.OnItemSelectedListener {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    private NavController mNavController;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavHostFragment fragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assert fragment != null;
        mNavController = fragment.getNavController();
        binding.bottomNavView.setOnItemSelectedListener(this);
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int destId = item.getItemId();
        NavDestination currentDestination = mNavController.getCurrentDestination();
        if (currentDestination == null) return false;
        if (!mNavController.popBackStack(destId, false)) {
            int startIndex = getDestinationIndex(currentDestination.getId());
            int destIndex = getDestinationIndex(destId);
            NavOptions navOptions;
            if (startIndex < destIndex) {
                navOptions = new NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_right)
                        .setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_left)
                        .setPopExitAnim(R.anim.slide_out_right)
                        .build();
            } else {
                navOptions = new NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_left)
                        .setExitAnim(R.anim.slide_out_right)
                        .setPopEnterAnim(R.anim.slide_in_right)
                        .setPopExitAnim(R.anim.slide_out_left)
                        .build();
            }
            mNavController.navigate(destId, null, navOptions);
        }
        return true;
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
}