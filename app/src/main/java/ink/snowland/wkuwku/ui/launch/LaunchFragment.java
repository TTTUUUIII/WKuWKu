package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.interfaces.Emulator.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.io.File;
import java.util.Collection;
import java.util.Locale;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.BaseController;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.databinding.FragmentLaunchBinding;
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

public class LaunchFragment extends BaseFragment implements OnEmulatorEventListener, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "PlayFragment";
    private static final String AUTO_RESTORE_LAST_STATE = "app_emulator_restore_last_state";
    private static final String AUTO_MARK_BROKEN_WHEN_START_GAME_FAILED = "app_mark_broken_when_start_game_failed";
    private static final String REVERSE_LANDSCAPE = "app_video_reverse_landscape";
    private FragmentLaunchBinding binding;
    private Emulator mEmulator;
    private GLVideoDevice mVideoDevice;
    private BaseController mController;
    private LaunchViewModel mViewModel;
    private Game mGame;
    private Snackbar mSnackbar;
    private AudioFocusRequest mAudioFocusRequest;
    private AudioManager mAudioManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) parentActivity.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .build();
        }
        parentActivity.setStatusBarVisibility(false);
        if (SettingsManager.getBoolean(REVERSE_LANDSCAPE)) {
            parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else {
            parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        parentActivity.setDrawerLockedMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mViewModel = new ViewModelProvider(this).get(LaunchViewModel.class);
        mVideoDevice = new GLVideoDevice(requireContext()) {
            @Override
            public void refresh(byte[] data, int width, int height, int pitch) {
                super.refresh(data, width, height, pitch);
                binding.glSurfaceView.requestRender();
            }
        };
        Bundle arguments = getArguments();
        assert arguments != null;
        mGame = arguments.getParcelable(ARG_GAME);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLaunchBinding.inflate(getLayoutInflater());
        binding.glSurfaceView.setEGLContextClientVersion(3);
        binding.glSurfaceView.setRenderer(mVideoDevice.getRenderer());
        binding.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        binding.pendingIndicator.setDataModel(mViewModel);
        binding.pendingIndicator.setLifecycleOwner(this);
        mSnackbar = Snackbar.make(binding.snackbarContainer, "", Snackbar.LENGTH_SHORT);
        mSnackbar.setAction(R.string.close, snackbar -> mSnackbar.dismiss());
        mSnackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        parentActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            parentActivity.setActionBarVisibility(false);
            selectController();
            selectEmulator();
            if (mEmulator == null) {
                Toast.makeText(parentActivity, R.string.no_matching_emulator_found, Toast.LENGTH_SHORT).show();
                return;
            }
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
        }
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
        if (mEmulator != null) {
            mEmulator.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEmulator != null) {
            mEmulator.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mEmulator != null) {
            mEmulator.suspend();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        parentActivity.setStatusBarVisibility(true);
        parentActivity.setActionBarVisibility(true);
        parentActivity.setDrawerLockedMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private void launch() {
        boolean success = false;
        if (mGame.state == Game.STATE_VALID && mEmulator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mEmulator.setAudioVolume(mAudioManager.requestAudioFocus(mAudioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? 1.0f : 0.0f);
            }
            applyOptions();
            mEmulator.setEmulatorEventListener(this);
            mEmulator.attachDevice(VIDEO_DEVICE, mVideoDevice);
            mEmulator.attachDevice(INPUT_DEVICE, mController);
            mEmulator.setSystemDirectory(SYSTEM_DIR, FileManager.getFileDirectory(FileManager.SYSTEM_DIRECTORY));
            mEmulator.setSystemDirectory(SAVE_DIR, FileManager.getFileDirectory(FileManager.SAVE_DIRECTORY + "/" + mEmulator.getTag()));
            if (mEmulator.run(mGame.filepath, mGame.system)) {
                if (SettingsManager.getBoolean(AUTO_RESTORE_LAST_STATE, false)) {
                    loadState(true);
                }
                success = true;
            }
        }
        if (!success) {
            if (mGame.state != Game.STATE_BROKEN && SettingsManager.getBoolean(AUTO_MARK_BROKEN_WHEN_START_GAME_FAILED)) {
                mGame.state = Game.STATE_BROKEN;
                Disposable disposable = mViewModel.update(mGame)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
            }
            Toast.makeText(parentActivity.getApplicationContext(), R.string.load_game_failed, Toast.LENGTH_SHORT).show();
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
                mController = new SegaController(0, parentActivity);
                break;
            default:
                mController = new StandardController(0, parentActivity);
        }
        binding.controllerRoot.addView(mController.getView());
        mController.setMacros(mViewModel.getMacros());
    }

    private void saveState(boolean auto) {
        if (mEmulator == null) return;
        if (mGame.system.equals("famicom")) return;
        final String prefix = mEmulator.getTag();
        final String ext = auto ? ".ast" : ".st";
        mEmulator.save(SAVE_STATE, FileManager.getFile(FileManager.STATE_DIRECTORY, prefix + "@" + mGame.md5 + ext));
    }

    private void loadState(boolean auto) {
        if (mEmulator == null) return;
        final String prefix = mEmulator.getTag();
        final String ext = auto ? ".ast" : ".st";
        File file = FileManager.getFile(FileManager.STATE_DIRECTORY, prefix + "@" + mGame.md5 + ext);
        if (file.exists()) {
            mEmulator.load(LOAD_STATE, file);
        }
    }

    private void applyOptions() {
        assert mEmulator != null;
        Collection<EmOption> options = mEmulator.getOptions();
        for (EmOption option : options) {
            if (!option.enable) continue;
            String val = SettingsManager.getString(option.key);
            if (val.isEmpty()) continue;
            option.val = val;
            mEmulator.setOption(option);
        }
    }

    private AlertDialog mExitDialog;

    private void showExitGameDialog() {
        if (mExitDialog == null) {
            mExitDialog = new MaterialAlertDialogBuilder(requireActivity())
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setTitle(R.string.options)
                    .setItems(R.array.play_fragment_exit_items, (DialogInterface.OnClickListener) (dialog, which) -> {
                        if (which == 0) {
                            if (mEmulator == null) return;
                            mEmulator.reset();
                        } else if (which == 1) {
                            exit();
                        }
                        mExitDialog.dismiss();
                    })
                    .create();
        }
        if (mExitDialog.isShowing()) return;
        mExitDialog.show();
    }

    private void exit() {
        if (mEmulator == null) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
            return;
        }
        assert mGame != null;
        mGame.lastPlayedTime = System.currentTimeMillis();
        Disposable disposable = mViewModel.update(mGame)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    error.printStackTrace(System.err);
                })
                .doFinally(() -> {
                    if (SettingsManager.getBoolean(AUTO_RESTORE_LAST_STATE, false)) {
                        saveState(true);
                    }
                    mVideoDevice.exportAsPNG(FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png"));
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                })
                .subscribe();
    }

    public static final String ARG_GAME = "game";

    private void selectEmulator() {
        String tag = SettingsManager.getString(String.format(Locale.ROOT, "app_%s_core", mGame.system));
        if (tag.isEmpty()) {
            mEmulator = EmulatorManager.getDefaultEmulator(mGame.system);
        } else {
            mEmulator = EmulatorManager.getEmulator(tag);
            if (mEmulator == null)
                mEmulator = EmulatorManager.getDefaultEmulator(mGame.system);
        }
    }

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

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (mEmulator == null) return;
        mEmulator.setAudioVolume(focusChange == AudioManager.AUDIOFOCUS_GAIN ? 1.0f : 0.0f);
    }
}