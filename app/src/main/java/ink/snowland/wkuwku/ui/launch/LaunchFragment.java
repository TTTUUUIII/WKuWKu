package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.interfaces.Emulator.*;
import static ink.snowland.wkuwku.ui.launch.LaunchViewModel.*;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.BaseController;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.databinding.DialogLayoutExitGameBinding;
import ink.snowland.wkuwku.databinding.FragmentLaunchBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.device.SegaController;
import ink.snowland.wkuwku.device.StandardController;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.device.GLVideoDevice;
import ink.snowland.wkuwku.util.BiosProvider;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchFragment extends BaseFragment implements OnEmulatorEventListener, View.OnClickListener {
    private static final String TAG = "PlayFragment";
    private static final String KEEP_SCREEN_ON = "app_keep_screen_on";
    private static final String VIDEO_RATIO = "app_video_ratio";
    private static final String BLACKLIST_AUTO_LOAD_STATE = "app_blacklist_auto_load_state";
    private static final String AUTO_SAVE_STATE_CHECKED = "app_auto_save_state_checked";
    private FragmentLaunchBinding binding;
    private GLVideoDevice mVideoDevice;
    private BaseController mController;
    private LaunchViewModel mViewModel;
    private Game mGame;
    private Snackbar mSnackbar;
    private boolean mKeepScreenOn;
    private boolean mAutoLoadState;
    private boolean mAutoLoadDisabled;

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
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLaunchBinding.inflate(getLayoutInflater());
        final String ratio = SettingsManager.getString(VIDEO_RATIO);
        binding.startReserved.setVisibility("full screen".equals(ratio) ? View.GONE : View.VISIBLE);
        binding.endReserved.setVisibility(binding.startReserved.getVisibility());
        mVideoDevice = new GLVideoDevice(requireContext()) {
            @Override
            public void refresh(byte[] data, int width, int height, int pitch) {
                super.refresh(data, width, height, pitch);
                binding.glSurfaceView.requestRender();
            }
        };
        mVideoDevice.setVideoRatio("keep aspect ratio".equals(ratio) ? GLVideoDevice.KEEP_ORIGIN : GLVideoDevice.COVERED);
        binding.glSurfaceView.setEGLContextClientVersion(3);
        binding.glSurfaceView.setRenderer(mVideoDevice.getRenderer());
        binding.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        binding.pendingIndicator.setDataModel(mViewModel);
        binding.pendingIndicator.setLifecycleOwner(this);
        mSnackbar = Snackbar.make(binding.snackbarContainer, "", Snackbar.LENGTH_SHORT);
        mSnackbar.setAction(R.string.close, snackbar -> mSnackbar.dismiss());
        mSnackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        parentActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        binding.buttonSave.setOnClickListener(this);
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
        selectController();
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
        int status = mViewModel.startEmulator(mGame);
        if (status == LaunchViewModel.NO_ERR) {
            attachToEmulator();
            if (mAutoLoadState && !mAutoLoadDisabled) {
                handler.postDelayed(mViewModel::loadStateAtLast, 300);
            }
        } else if (status == ERR_LOAD_FAILED){
            showSnackbar(R.string.load_game_failed, 300);
        } else if (status == ERR_EMULATOR_NOT_FOUND) {
            showSnackbar(R.string.no_matching_emulator_found, 300);
        }
    }

    private void attachToEmulator() {
        if (mViewModel.getEmulator() == null) return;
        final Emulator emulator = mViewModel.getEmulator();
        emulator.attachDevice(VIDEO_DEVICE, mVideoDevice);
        emulator.attachDevice(INPUT_DEVICE, mController);
        emulator.setEmulatorEventListener(this);
        mAutoLoadDisabled = SettingsManager.getStringSet(BLACKLIST_AUTO_LOAD_STATE).contains(emulator.getTag());
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
        mViewModel.resumeEmulator();
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.pauseEmulator();
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

    private void selectController() {
        switch (mGame.system) {
            case "game-gear":
            case "master-system":
            case "mega-cd":
            case "mega-drive":
            case "sega-pico":
            case "sg-1000":
            case "saturn":
                mController = new SegaController(0, parentActivity);
                break;
            default:
                mController = new StandardController(0, parentActivity);
        }
        binding.controllerRoot.addView(mController.getView());
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
        if (mViewModel.getEmulator() == null) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
            return;
        }
        if (mExitLayoutBinding.saveState.isChecked()) {
            mViewModel.pauseEmulator();
            mVideoDevice.exportAsPNG(FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png"));
            mViewModel.saveCurrentSate();
        }
        SettingsManager.putBoolean(AUTO_SAVE_STATE_CHECKED, mExitLayoutBinding.saveState.isChecked());
        mViewModel.stopEmulator();
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

    @Override
    public void onShowMessage(@NonNull EmMessageExt msg) {
        if (msg.type == EmMessageExt.MESSAGE_TARGET_OSD) {
            handler.post(() -> showSnackbar(msg.msg, msg.duration));
        }
    }

    @MainThread
    private void showSnackbar(@NonNull String msg, int duration) {
        mSnackbar.setText(msg);
        mSnackbar.setDuration(duration);
        mSnackbar.show();
    }

    @MainThread
    private void showSnackbar(@StringRes int resId, int duration) {
        showSnackbar(getString(resId), duration);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.reset) {
            mViewModel.resetEmulator();
            mExitDialog.dismiss();
        } else if (viewId == R.id.exit) {
            onExit();
            mExitDialog.dismiss();
        } else {
            mController.vibrator();
            if (viewId == R.id.button_save && mViewModel.saveCurrentSate()) {
                showSnackbar(getString(R.string.fmt_state_saved, mViewModel.getSnapshotsCount()), 300);
            } else if (viewId == R.id.button_load_state4) {
                mViewModel.loadStateAt(3);
            } else if (viewId == R.id.button_load_state3) {
                mViewModel.loadStateAt(2);
            } else if (viewId == R.id.button_load_state2) {
                mViewModel.loadStateAt(1);
            } else if (viewId == R.id.button_load_state1) {
                mViewModel.loadStateAt(0);
            }
        }
    }
}