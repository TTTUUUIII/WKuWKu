package ink.snowland.wkuwku.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.widget.CheckLatestVersionWorker;
import ink.snowland.wkuwku.databinding.ActivityMainBinding;
import ink.snowland.wkuwku.util.SettingsManager;

public class MainActivity extends BaseActivity {
    private static final String CHECK_UPDATE_WORK = "check_update";
    private static final String NEW_VERSION_NOTIFICATION = "app_new_version_notification";
    private NavController mNavController;
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private InstallApkReceiver mInstallApkReceiver;
    private ActivityResultLauncher<Intent> mRequestInstallPackageLauncher;
    private Uri mNewApkUri;
    private MainViewModel mViewModel;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
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
        if (!mViewModel.newVersionChecked && SettingsManager.getBoolean(NEW_VERSION_NOTIFICATION, true)) {
            mViewModel.newVersionChecked = true;
            checkUpdate();
            mInstallApkReceiver = new InstallApkReceiver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mInstallApkReceiver, new IntentFilter(CheckLatestVersionWorker.ACTION_UPDATE_APK), Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(mInstallApkReceiver, new IntentFilter(CheckLatestVersionWorker.ACTION_UPDATE_APK));
            }
            mRequestInstallPackageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (getPackageManager().canRequestPackageInstalls()) {
                    installPackage(mNewApkUri);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_about) {
            showAboutDialog();
        } else {
            final int navResId;
            if (itemId == R.id.action_macro) {
                navResId = R.id.macro_fragment;
            } else if (itemId == R.id.action_trash) {
                navResId = R.id.trash_fragment;
            } else if (itemId == R.id.action_extension) {
                navResId = R.id.plug_fragment;
            } else {
                navResId = 0;
            }
            binding.drawerLayout.closeDrawers();
            if (navResId != 0 && isNavigateAble(navResId)) {
                postDelayed(() -> {
                    mNavController.navigate(navResId);
                }, 200);
            }
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInstallApkReceiver != null)
            unregisterReceiver(mInstallApkReceiver);
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
                        new OneTimeWorkRequest.Builder(CheckLatestVersionWorker.class)
                                .build()
                )
                .enqueue();
    }

    private void showUpdateApkDialog(@NonNull String path, @NonNull String version) {
        mNewApkUri = FileProvider.getUriForFile(this, "ink.snowland.wkuwku.provider", new File(path));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_version_found)
                .setMessage(getString(R.string.fmt_update_version, version))
                .setIcon(R.mipmap.ic_launcher_round)
                .setPositiveButton(R.string.updated,(dialog, which) -> {
                    boolean request = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!getPackageManager().canRequestPackageInstalls()) {
                            showRequestInstallPackageDialog();
                            request = false;
                        }
                    }

                    if (request) {
                        installPackage(mNewApkUri);
                    }
                })
                .setNegativeButton(R.string.ignore_for_now, null)
                .setCancelable(false)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showRequestInstallPackageDialog() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle(R.string.permission_denied)
                .setMessage(R.string.summary_request_install_package_permssion)
                .setPositiveButton(R.string.grant, (dialog, which) -> {
                    mRequestInstallPackageLauncher.launch(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void setDrawerLockedMode(int mode) {
        try {
            binding.drawerLayout.setDrawerLockMode(mode);
        } catch (NullPointerException ignored) {}
    }

    private class InstallApkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(CheckLatestVersionWorker.ACTION_UPDATE_APK)) {
                String apkPath = intent.getStringExtra(CheckLatestVersionWorker.EXTRA_APK_PATH);
                String apkVersion = intent.getStringExtra(CheckLatestVersionWorker.EXTRA_APK_VERSION);
                if (apkPath == null || apkVersion == null) return;
                showUpdateApkDialog(apkPath, apkVersion);
            }
        }
    }
}