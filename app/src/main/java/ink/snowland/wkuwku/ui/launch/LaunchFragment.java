package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.ui.launch.LaunchViewModel.*;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.outlook.wn123o.retrosystem.RetroSystem;

import java.io.IOException;
import java.io.OutputStream;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.BaseController;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.databinding.DialogLayoutExitGameBinding;
import ink.snowland.wkuwku.databinding.FragmentLaunchBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.device.GLRenderer;
import ink.snowland.wkuwku.device.SegaController;
import ink.snowland.wkuwku.device.StandardController;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.interfaces.OnEmulatorEventListener;
import ink.snowland.wkuwku.util.BiosProvider;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchFragment extends BaseFragment implements RetroSystem.OnEventListener, View.OnClickListener, BaseActivity.OnKeyEventListener, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "PlayFragment";
    private static final int PLAYER_1 = 0;
    private static final int PLAYER_2 = 0;
    private static final int SNACKBAR_LENGTH_SHORT = 500;
    private static final String KEEP_SCREEN_ON = "app_keep_screen_on";
    private static final String VIDEO_RATIO = "app_video_ratio";
    private static final String BLACKLIST_AUTO_LOAD_STATE = "app_blacklist_auto_load_state";
    private static final String AUTO_SAVE_STATE_CHECKED = "app_auto_save_state_checked";
    private FragmentLaunchBinding binding;
    private GLRenderer mRenderer;
    private LaunchViewModel mViewModel;
    private Game mGame;
    private Snackbar mSnackbar;
    private boolean mKeepScreenOn;
    private boolean mAutoLoadState;
    private boolean mAutoLoadDisabled;
    private final SparseArray<BaseController> mControllers = new SparseArray<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKeepScreenOn = SettingsManager.getBoolean(KEEP_SCREEN_ON, true);
        parentActivity.setStatusBarVisibility(false);
        parentActivity.setDrawerLockedMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        if (mKeepScreenOn) {
            parentActivity.setKeepScreenOn(true);
        }
        mViewModel = new ViewModelProvider(this).get(LaunchViewModel.class);
        Bundle arguments = getArguments();
        assert arguments != null;
        mGame = arguments.getParcelable(ARG_GAME);
        mAutoLoadState = arguments.getBoolean(ARG_AUTO_LOAD_STATE, false);
        RetroSystem.setOnEventListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest fq = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .build();
            AudioManager am = (AudioManager) parentActivity.getSystemService(Context.AUDIO_SERVICE);
            RetroSystem.setAudioVolume(am.requestAudioFocus(fq) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? 1.0f : 0.0f);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLaunchBinding.inflate(getLayoutInflater());
        mRenderer = new GLRenderer(requireContext());
        binding.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                RetroSystem.attachSurface(holder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                RetroSystem.adjustSurface(width, height);
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                RetroSystem.detachSurface();
            }
        });
        binding.pendingIndicator.setDataModel(mViewModel);
        binding.pendingIndicator.setLifecycleOwner(this);
        mSnackbar = Snackbar.make(binding.snackbarContainer, "", Snackbar.LENGTH_SHORT);
        mSnackbar.setAction(R.string.close, snackbar -> mSnackbar.dismiss());
        mSnackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        parentActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        binding.buttonSavestate.setOnClickListener(this);
        binding.buttonScreenshot.setOnClickListener(this);
        binding.buttonLoadState1.setOnClickListener(this);
        binding.buttonLoadState2.setOnClickListener(this);
        binding.buttonLoadState3.setOnClickListener(this);
        binding.buttonLoadState4.setOnClickListener(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentActivity.setActionBarVisibility(false);
        selectDefaultController();
        if (savedInstanceState == null) {
            mViewModel.setPendingIndicator(true, getString(R.string.downloading_files));
            Disposable disposable = BiosProvider.downloadBiosForGame(mGame, FileManager.getFileDirectory(FileManager.SYSTEM_DIRECTORY))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(error -> {
                        error.printStackTrace(System.err);
                        Toast.makeText(parentActivity, R.string.load_bios_failed, Toast.LENGTH_SHORT).show();
                    })
                    .doFinally(() -> {
                        mViewModel.setPendingIndicator(false);
                    })
                    .subscribe(this::launch, error -> {/*Ignored*/});
        } else {
            attachToEmulator();
        }
    }

    private void launch() {
        RetroSystem.use(mGame.coreAlias);
        boolean noError = RetroSystem.start(mGame.filepath);
//        if (status == LaunchViewModel.NO_ERR) {
//            attachToEmulator();
//            if (mAutoLoadState && !mAutoLoadDisabled) {
//                handler.postDelayed(mViewModel::loadStateAtLast, 300);
//            }
//        } else if (status == ERR_LOAD_FAILED){
//            showSnackbar(R.string.load_game_failed, SNACKBAR_LENGTH_SHORT);
//        } else if (status == ERR_EMULATOR_NOT_FOUND) {
//            showSnackbar(R.string.no_matching_emulator_found, SNACKBAR_LENGTH_SHORT);
//        }
    }

    private void attachToEmulator() {
//        mAutoLoadDisabled = SettingsManager.getStringSet(BLACKLIST_AUTO_LOAD_STATE).contains(emulator.getTag());
    }
    private void adjustScreenSize(int width, int height) {
        float ratio = (float) width / height;
        binding.surfaceView.post(() -> {
            ViewGroup.LayoutParams lp = binding.surfaceView.getLayoutParams();
            if ("full screen".equals(SettingsManager.getString(VIDEO_RATIO))) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                binding.getRoot().setFitsSystemWindows(false);
            } else {
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (landscape) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.width = (int) (displayMetrics.heightPixels * ratio);
                } else {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = (int) (displayMetrics.widthPixels / ratio);
                }
                binding.getRoot().setFitsSystemWindows(true);
            }
            binding.getRoot().requestApplyInsets();
            binding.surfaceView.setLayoutParams(lp);
        });
    }

    private final OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            showExitGameDialog();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        parentActivity.addOnKeyEventListener(this);
        RetroSystem.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        parentActivity.removeOnKeyEventListener(this);
        RetroSystem.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        parentActivity.setStatusBarVisibility(true);
        parentActivity.setActionBarVisibility(true);
        parentActivity.setDrawerLockedMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (mKeepScreenOn) {
            parentActivity.setKeepScreenOn(false);
        }
    }

    private void selectDefaultController() {
        BaseController controller;
        switch (mGame.coreAlias) {
            case "game-gear":
            case "master-system":
            case "mega-cd":
            case "mega-drive":
            case "sega-pico":
            case "sg-1000":
            case "saturn":
                controller = new SegaController(parentActivity);
                break;
            default:
                controller = new StandardController(parentActivity);
        }
        if (controller.getView() != null) {
            binding.controllerRoot.addView(controller.getView());
        }
        mControllers.put(PLAYER_1, controller);
    }

    private DialogLayoutExitGameBinding mExitLayoutBinding;

    private AlertDialog mExitDialog;

    private void showExitGameDialog() {
        if (mExitDialog == null) {
            mExitLayoutBinding = DialogLayoutExitGameBinding.inflate(getLayoutInflater());
            mExitLayoutBinding.reset.setOnClickListener(this);
            mExitLayoutBinding.exit.setOnClickListener(this);
            mExitDialog = new MaterialAlertDialogBuilder(requireActivity())
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setTitle(R.string.options)
                    .setView(mExitLayoutBinding.getRoot())
                    .create();
        }
        mExitLayoutBinding.saveState.setChecked(SettingsManager.getBoolean(AUTO_SAVE_STATE_CHECKED, true));
        if (mExitDialog.isShowing()) return;
        mExitDialog.show();
    }

    private void onExit() {
//        if (mViewModel.getEmulator() == null) {
//            NavController navController = NavHostFragment.findNavController(this);
//            navController.popBackStack();
//            return;
//        }
//        boolean captureScreen = !FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png").exists();
        if (mExitLayoutBinding.saveState.isChecked()) {
            mViewModel.saveCurrentSate();
//            captureScreen = true;
        }
//        if (captureScreen) {
//            mRenderer.exportAsPNG(FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png"));
//        }
        SettingsManager.putBoolean(AUTO_SAVE_STATE_CHECKED, mExitLayoutBinding.saveState.isChecked());
        RetroSystem.stop();
        mGame.lastPlayedTime = System.currentTimeMillis();
        Disposable disposable = AppDatabase.db.gameInfoDao().update(mGame)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    error.printStackTrace(System.err);
                })
                .doFinally(() -> {
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                })
                .subscribe();
    }

    public static final String ARG_GAME = "game";
    public static final String ARG_AUTO_LOAD_STATE = "auto_load_state";

    @MainThread
    private void showSnackbar(@NonNull String msg, int duration) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            handler.post(() -> {
                mSnackbar.setText(msg);
                mSnackbar.setDuration(duration);
                mSnackbar.show();
            });
        } else {
            mSnackbar.setText(msg);
            mSnackbar.setDuration(duration);
            mSnackbar.show();
        }

    }

    @MainThread
    private void showSnackbar(@StringRes int resId, int duration) {
        showSnackbar(getString(resId), duration);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.reset) {
            RetroSystem.reset();
            mExitDialog.dismiss();
        } else if (viewId == R.id.exit) {
            onExit();
            mExitDialog.dismiss();
        } else {
            mControllers.get(PLAYER_1).vibrator();
            if (viewId == R.id.button_savestate && mViewModel.saveCurrentSate()) {
                showSnackbar(getString(R.string.fmt_state_saved, mViewModel.getSnapshotsCount()), SNACKBAR_LENGTH_SHORT);
            } else if (viewId == R.id.button_load_state4) {
                mViewModel.loadStateAt(3);
            } else if (viewId == R.id.button_load_state3) {
                mViewModel.loadStateAt(2);
            } else if (viewId == R.id.button_load_state2) {
                mViewModel.loadStateAt(1);
            } else if (viewId == R.id.button_load_state1) {
                mViewModel.loadStateAt(0);
            } else if (viewId == R.id.button_screenshot) {
                takeScreenshot();
            }
        }
    }

    private void takeScreenshot() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, mGame.title + "@" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + getString(R.string.app_name));
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return;
        try (OutputStream fos = contentResolver.openOutputStream(uri)){
            if (fos == null) return;
            mRenderer.exportAsPNG(fos);
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            contentResolver.update(uri, values, null, null);
            showSnackbar(R.string.screenshot_saved, SNACKBAR_LENGTH_SHORT);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private int mL1ButtonState = KeyEvent.ACTION_UP;
    private int mR1ButtonState = KeyEvent.ACTION_UP;
    @Override
    public boolean onKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1 && mL1ButtonState != event.getAction()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mViewModel.saveCurrentSate();
                showSnackbar(getString(R.string.fmt_state_saved, mViewModel.getSnapshotsCount()), SNACKBAR_LENGTH_SHORT);
            }
            mL1ButtonState = event.getAction();
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1 && mR1ButtonState != event.getAction()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mViewModel.loadStateAtLast();
            }
            mR1ButtonState = event.getAction();
        }
        return dispatchKeyEvent(event);
    }

    private boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        int size = mControllers.size();
        boolean handled = false;
        for (int i = 0; i < size; ++i) {
            if (mControllers.valueAt(i).onKeyEvent(event)) {
                handled = true;
                break;
            }
        }
        return handled;
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        adjustScreenSize(width, height);
    }

    @Override
    public int onInputCallback(int port, int device, int index, int id) {
        BaseController controller = mControllers.get(port);
        if (controller == null || controller.type != device) return 0;
        return controller.getState(id);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        RetroSystem.setAudioVolume(focusChange == AudioManager.AUDIOFOCUS_GAIN ? 1.0f : 0.0f);
    }
}