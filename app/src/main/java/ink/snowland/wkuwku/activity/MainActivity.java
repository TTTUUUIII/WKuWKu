package ink.snowland.wkuwku.activity;

import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.CheckUpdateWorker;
import ink.snowland.wkuwku.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {
    private static final String CHECK_UPDATE_WORK = "check_update";
    private NavController mNavController;
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setSupportActionBar(binding.toolBar);
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, R.string.start, R.string.close);
        binding.drawerLayout.addDrawerListener(mActionBarDrawerToggle);
        binding.navigationView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
        mActionBarDrawerToggle.syncState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            View heroImageView = binding.navigationView.getHeaderView(0).findViewById(R.id.hero_image_view);
            heroImageView.setRenderEffect(RenderEffect.createBlurEffect(18, 18, Shader.TileMode.MIRROR));
        }
        assert fragment != null;
        mNavController = fragment.getNavController();
        mNavController.addOnDestinationChangedListener((navController, navDestination, bundle) -> {
            mActionBarDrawerToggle.setDrawerIndicatorEnabled(navController.getPreviousBackStackEntry() == null);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_macro) {
            if (isNavigateAble(R.id.macro_fragment))
                mNavController.navigate(R.id.macro_fragment/*, null, navAnimOptions*/);
        } else if (itemId == R.id.action_about) {
            showAboutDialog();
        } else if (itemId == R.id.action_trash) {
            if (isNavigateAble(R.id.trash_fragment)) {
                mNavController.navigate(R.id.trash_fragment/*, null, navAnimOptions*/);
            }
        }
        binding.drawerLayout.closeDrawers();
        return true;
    }

    /*private final NavOptions navAnimOptions = new NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build();*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            if (isNavigateAble(R.id.settings_fragment)) {
                mNavController.navigate(R.id.settings_fragment/*, null, navAnimOptions*/);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return mNavController.navigateUp() || super.onSupportNavigateUp();
    }

    private boolean isNavigateAble(@IdRes int id) {
        NavDestination destination = mNavController.getCurrentDestination();
        return destination != null && destination.getId() != id;
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_wkuwku)
                .setIcon(R.mipmap.ic_launcher_round)
                .setMessage(getString(R.string.fmt_about_wkuwku, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME))
                .show();
    }

    private void checkUpdate() {
        WorkManager manager = WorkManager.getInstance(getApplication());
        manager.beginUniqueWork(CHECK_UPDATE_WORK,
                        ExistingWorkPolicy.REPLACE,
                        new OneTimeWorkRequest.Builder(CheckUpdateWorker.class)
                                .build()
                )
                .enqueue();
    }

    @Override
    public void setDrawerLockedMode(int mode) {
        try {
            binding.drawerLayout.setDrawerLockMode(mode);
        } catch (NullPointerException ignored) {}
    }
}