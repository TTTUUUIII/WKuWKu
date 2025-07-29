package ink.snowland.wkuwku.activity;
import static ink.snowland.wkuwku.AppConfig.*;
import static ink.snowland.wkuwku.util.ResourceManager.getStringSafe;

import android.Manifest;
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
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.util.BlurTransformation;
import ink.snowland.wkuwku.util.NotificationManager;
import ink.snowland.wkuwku.view.EmojiWorkshopView;
import ink.snowland.wkuwku.widget.CheckLatestVersionWorker;
import ink.snowland.wkuwku.databinding.ActivityMainBinding;
import ink.snowland.wkuwku.util.SettingsManager;

public class MainActivity extends BaseActivity {
    private static final String CHECK_UPDATE_WORK = "check_update";
    private static final String NEW_VERSION_NOTIFICATION = "app_new_version_notification";
    private static final String EMOJI_WORKSHOP_SOURCE = "app_emoji_workshop_source";
    private static final String EMOJI_WORKSHOP_EMOJI_SIZE = "app_emoji_workshop_emoji_size";
    private static final String DISTANCE_BETWEEN_EMOJIS = "app_distance_between_emojis";
    private NavController mNavController;
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private InstallApkReceiver mInstallApkReceiver;
    private ActivityResultLauncher<Intent> mRequestInstallPackageLauncher;
    private Uri mNewApkUri;
    private MainViewModel mViewModel;
    private final EmojiWorkshopView.Options mEmojiWorkshopOptions = new EmojiWorkshopView.Options(SettingsManager.getString(EMOJI_WORKSHOP_SOURCE, "\uD83D\uDC22☺️⭐️"), SettingsManager.getInt(DISTANCE_BETWEEN_EMOJIS, 40), SettingsManager.getInt(EMOJI_WORKSHOP_EMOJI_SIZE, 40));

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.emojiWorkshopView.setOptions(mEmojiWorkshopOptions);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setSupportActionBar(binding.toolBar);
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open, R.string.close);
        binding.drawerLayout.addDrawerListener(mActionBarDrawerToggle);
        binding.navigationView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
        mActionBarDrawerToggle.syncState();
        ImageView heroImageView = binding.navigationView.getHeaderView(0).findViewById(R.id.hero_image_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Glide.with(this)
                    .load(R.drawable.snow_bros_genesis)
                    .into(heroImageView);
            heroImageView.setRenderEffect(RenderEffect.createBlurEffect(18, 18, Shader.TileMode.CLAMP));
        } else {
            Glide.with(this)
                    .load(R.drawable.snow_bros_genesis)
                    .apply(RequestOptions.bitmapTransform(new BlurTransformation(this, 18)))
                    .into(heroImageView);
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getPackageManager().canRequestPackageInstalls()) {
                    installPackage(mNewApkUri);
                }
            });
        }
        checkRuntimePermissions();
        postDelayed(() -> {
            NotificationManager.postNotification(
                    NotificationManager.NOTIFICATION_ERROR_CHANNEL,
                    getStringSafe(R.string.extension_manage),
                    getStringSafe(R.string.fmt_download_failed_network_error, "123"));
        }, 3000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        int resId = intent.getIntExtra(EXTRA_NAVIGATE_RES_ID, 0);
        if (resId != 0) {
            mNavController.navigate(resId, null, navAnimOptions);
        }
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
            if (navResId != 0 && isNavigateAble(navResId)) {
                mNavController.navigate(navResId, null, navAnimOptions);
            }
            binding.drawerLayout.closeDrawers();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            if (isNavigateAble(R.id.settings_fragment)) {
                mNavController.navigate(R.id.settings_fragment, null, navAnimOptions);
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

    @Override
    public void onSettingChanged(@NonNull String key) {
        super.onSettingChanged(key);
        switch (key) {
            case EMOJI_WORKSHOP_EMOJI_SIZE:
            case EMOJI_WORKSHOP_SOURCE:
            case DISTANCE_BETWEEN_EMOJIS:
                mEmojiWorkshopOptions.setFontSize(SettingsManager.getInt(EMOJI_WORKSHOP_EMOJI_SIZE, mEmojiWorkshopOptions.getFontSize()));
                mEmojiWorkshopOptions.setMinDistance(SettingsManager.getInt(DISTANCE_BETWEEN_EMOJIS, mEmojiWorkshopOptions.getMinDistance()));
                mEmojiWorkshopOptions.setSource(SettingsManager.getString(EMOJI_WORKSHOP_SOURCE, mEmojiWorkshopOptions.getSource()));
                binding.emojiWorkshopView.setOptions(mEmojiWorkshopOptions);
                break;
            default:;
        }
    }

    @NonNull
    @Override
    public WindowInsetsCompat onApplyWindowInsets(@NonNull View root, @NonNull WindowInsetsCompat insets) {
        AppBarLayout view = binding.appBarLayout;
        view.setPadding(view.getPaddingLeft(), insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, view.getPaddingRight(), view.getPaddingBottom());
        return super.onApplyWindowInsets(root, insets);
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about)
                .setIcon(R.mipmap.ic_launcher_round)
                .setMessage(getString(R.string.fmt_about, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME))
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
        String githubReleaseUrl = GITHUB + "releases/tag/" + version;
        Spanned spanned = HtmlCompat.fromHtml(getString(R.string.fmt_update_version, version, githubReleaseUrl), HtmlCompat.FROM_HTML_MODE_COMPACT);
        mNewApkUri = FileProvider.getUriForFile(this, "ink.snowland.wkuwku.provider", new File(path));
        AlertDialog updateDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_version_found)
                .setMessage(spanned)
                .setIcon(R.mipmap.ic_launcher_round)
                .setPositiveButton(R.string.updated, (dialog, which) -> {
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
                .create();
        updateDialog.show();
        TextView dialogTextView = (TextView) updateDialog.findViewById(android.R.id.message);
        if (dialogTextView != null) {
            dialogTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
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
        } catch (NullPointerException ignored) {
        }
    }

    private void checkRuntimePermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), 1);
        }
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

    public static final String EXTRA_NAVIGATE_RES_ID = "extra_navigate_res_id";
}