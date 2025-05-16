package ink.snowland.wkuwku.activity;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {
    private NavController mNavController;
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setSupportActionBar(binding.toolBar);
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolBar, R.string.start, R.string.close);
        binding.drawerLayout.addDrawerListener(toggle);
        binding.navigationView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
        toggle.syncState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            View heroImageView = binding.navigationView.getHeaderView(0).findViewById(R.id.hero_image_view);
            heroImageView.setRenderEffect(RenderEffect.createBlurEffect(18, 18, Shader.TileMode.MIRROR));
        }
        assert fragment != null;
        mNavController = fragment.getNavController();
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
                mNavController.navigate(R.id.macro_fragment, null, navAnimOptions);
        } else if (itemId == R.id.action_about) {
            showAboutDialog();
        }
        binding.drawerLayout.closeDrawers();
        return true;
    }

    private final NavOptions navAnimOptions = new NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build();

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            if (isNavigateAble(R.id.settings_fragment)) {
                mNavController.navigate(R.id.settings_fragment, null, navAnimOptions);
            }
        } else if (itemId == R.id.action_trash) {
            if (isNavigateAble(R.id.trash_fragment)) {

                mNavController.navigate(R.id.trash_fragment, null, navAnimOptions);
            }
        }
        return super.onOptionsItemSelected(item);
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
}