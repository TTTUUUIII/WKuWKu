package ink.snowland.wkuwku.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowInsetsCompat;
//import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

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
import ink.snowland.wkuwku.view.EmojiWorkshopView;
import ink.snowland.wkuwku.databinding.ActivityMainBinding;
import ink.snowland.wkuwku.util.SettingsManager;

public class MainActivity extends BaseActivity {
    public static final int REQUEST_UNKNOWN         = 0;
    public static final int REQUEST_NAVIGATE        = 1;
    public static final int REQUEST_INSTALL_PACKAGE = 2;
    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_NAVIGATE_RES_ID = "extra_navigate_res_id";
    public static final String EXTRA_PACKAGE_FILE_PATH = "extra_package_file_path";
    private static final String EMOJI_WORKSHOP_SOURCE = "app_emoji_workshop_source";
    private static final String EMOJI_WORKSHOP_EMOJI_SIZE = "app_emoji_workshop_emoji_size";
    private static final String DISTANCE_BETWEEN_EMOJIS = "app_distance_between_emojis";
    private NavController mNavController;
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private ActivityResultLauncher<Intent> mRequestInstallPackageLauncher;
    private Uri mApkUri;
//    private MainViewModel mViewModel;
    private final EmojiWorkshopView.Options mEmojiWorkshopOptions = new EmojiWorkshopView.Options(SettingsManager.getString(EMOJI_WORKSHOP_SOURCE, "\uD83D\uDC22☺️⭐️"), SettingsManager.getInt(DISTANCE_BETWEEN_EMOJIS, 40), SettingsManager.getInt(EMOJI_WORKSHOP_EMOJI_SIZE, 40));

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.emojiWorkshopView.setOptions(mEmojiWorkshopOptions);
//        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
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
        mRequestInstallPackageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getPackageManager().canRequestPackageInstalls()) {
                installPackage(mApkUri);
            }
        });
        checkRuntimePermissions();
        checkIntent(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        checkIntent(intent);
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

    private void checkIntent(@Nullable Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent == null) return;
        int requestId = intent.getIntExtra(EXTRA_REQUEST_ID, REQUEST_UNKNOWN);
        switch (requestId) {
            case REQUEST_NAVIGATE:
                int fragmentId = intent.getIntExtra(EXTRA_NAVIGATE_RES_ID, 0);
                if (fragmentId == 0) return;
                mNavController.navigate(fragmentId, null, navAnimOptions);
                break;
            case REQUEST_INSTALL_PACKAGE:
                String path = intent.getStringExtra(EXTRA_PACKAGE_FILE_PATH);
                if (path == null) return;
                File file = new File(path);
                if (!file.exists() || !file.getName().endsWith(".apk")) return;
                mApkUri = FileProvider.getUriForFile(this, "ink.snowland.wkuwku.provider", new File(path));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!getPackageManager().canRequestPackageInstalls()) {
                        showRequestInstallPackageDialog();
                        return;
                    }
                }
                installPackage(mApkUri);
            default:
        }
    }
}