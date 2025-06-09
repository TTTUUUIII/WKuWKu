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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

public class LaunchFragment extends BaseFragment implements OnEmulatorEventListener, AudioManager.OnAudioFocusChangeListener, View.OnClickListener {
    private static final String TAG = "PlayFragment";
    private static final int MAX_COUNT_OF_SNAPSHOT = 3;
    private static final String AUTO_MARK_BROKEN_WHEN_START_GAME_FAILED = "app_mark_broken_when_start_game_failed";
    private static final String REVERSE_LANDSCAPE = "app_video_reverse_landscape";
    private static final String KEEP_SCREEN_ON = "app_keep_screen_on";
    private static final String VIDEO_RATIO = "app_video_ratio";
    private FragmentLaunchBinding binding;
    private Emulator mEmulator;
    private GLVideoDevice mVideoDevice;
    private BaseController mController;
    private LaunchViewModel mViewModel;
    private Game mGame;
    private Snackbar mSnackbar;
    private AudioFocusRequest mAudioFocusRequest;
    private AudioManager mAudioManager;
    private boolean mKeepScreenOn;
    private boolean mAutoRestoreState;
    private final List<byte[]> mSnapshots = new ArrayList<>();
    private boolean mGameLoaded = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKeepScreenOn = SettingsManager.getBoolean(KEEP_SCREEN_ON, true);
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
        if (mKeepScreenOn) {
            parentActivity.setKeepScreenOn(true);
        }
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
        mAutoRestoreState = arguments.getBoolean(ARG_AUTO_RESTORE_STATE, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLaunchBinding.inflate(getLayoutInflater());
        final String ratio = SettingsManager.getString(VIDEO_RATIO);
        binding.startReserved.setVisibility("full screen".equals(ratio) ? View.GONE : View.VISIBLE);
        binding.endReserved.setVisibility(binding.startReserved.getVisibility());
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
        parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        parentActivity.setStatusBarVisibility(true);
        parentActivity.setActionBarVisibility(true);
        parentActivity.setDrawerLockedMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (mKeepScreenOn) {
            parentActivity.setKeepScreenOn(false);
        }
    }

    private void launch() {
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
                if ("famicom".equals(mGame.system)) {
                    handler.postDelayed(this::onAutoLoadState, 800);
                } else {
                    onAutoLoadState();
                }
                mGameLoaded = true;
            }
        }
        if (!mGameLoaded) {
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
            case "saturn":
                mController = new SegaController(0, parentActivity);
                break;
            default:
                mController = new StandardController(0, parentActivity);
        }
        binding.controllerRoot.addView(mController.getView());
        mController.setMacros(mViewModel.getMacros());
    }

    @SuppressLint("DefaultLocale")
    private void onAutoSaveState() {
        if (mEmulator == null) return;
        final String prefix = mEmulator.getTag();
        for (int i = 0; i < mSnapshots.size(); i++) {
            try (FileOutputStream fos = new FileOutputStream(FileManager.getFile(FileManager.STATE_DIRECTORY, String.format("%s@%s-%02d.st", prefix, mGame.md5, i + 1)))){
                fos.write(mSnapshots.get(0));
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        File stateFile = FileManager.getFile(FileManager.STATE_DIRECTORY, prefix + "@" + mGame.md5 + ".ast");
        if (mAutoRestoreState || !stateFile.exists()) {
            mVideoDevice.exportAsPNG(FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png"));
            if (mGame.system.equals("saturn")) return;
            mEmulator.save(SAVE_STATE, stateFile);
        }
    }

    @SuppressLint("DefaultLocale")
    private void onAutoLoadState() {
        if (mEmulator == null) return;
        final String prefix = mEmulator.getTag();
        for (int i = 0; i < MAX_COUNT_OF_SNAPSHOT; ++i) {
            File statFile = FileManager.getFile(FileManager.STATE_DIRECTORY, String.format("%s@%s-%02d.st", prefix, mGame.md5, i + 1));
            if (statFile.exists() && statFile.length() != 0) {
                try (FileInputStream fis = new FileInputStream(statFile)){
                    byte[] data = new byte[(int) statFile.length()];
                    int readNumInBytes = fis.read(data, 0, data.length);
                    if (readNumInBytes == statFile.length()) {
                        mSnapshots.add(data);
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        if (!mAutoRestoreState) return;
        File file = FileManager.getFile(FileManager.STATE_DIRECTORY, prefix + "@" + mGame.md5 + ".ast");
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
        if (mEmulator == null || !mGameLoaded) {
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
                    onAutoSaveState();
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                })
                .subscribe();
    }

    public static final String ARG_GAME = "game";
    public static final String ARG_AUTO_RESTORE_STATE = "auto_restore";

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

    @MainThread
    private void showSnackbar(@StringRes int resId, int duration) {
        showSnackbar(getString(resId), duration);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (mEmulator == null) return;
        mEmulator.setAudioVolume(focusChange == AudioManager.AUDIOFOCUS_GAIN ? 1.0f : 0.0f);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        mController.vibrator();
        if (viewId == R.id.button_save) {
            if (mEmulator == null) return;
            byte[] snapshot = mEmulator.getSnapshot();
            if (snapshot == null || snapshot.length == 0) return;
            if (mSnapshots.size() == MAX_COUNT_OF_SNAPSHOT)
                mSnapshots.remove(0);
            mSnapshots.add(snapshot);
            showSnackbar(getString(R.string.fmt_state_saved, mSnapshots.size()), 300);
        } else if (viewId == R.id.button_load_state3) {
            if (mSnapshots.isEmpty()) return;
            byte[] snapshot = mSnapshots.get(mSnapshots.size() - 1);
            if (snapshot != null) {
                if (!mEmulator.setSnapshot(snapshot)) {
                    showSnackbar(R.string.load_state_failed, 300);
                }
            }
        } else if (viewId == R.id.button_load_state2) {
            if (mSnapshots.isEmpty()) return;
            final byte[] snapshot;
            if (mSnapshots.size() > 1) {
                snapshot = mSnapshots.get(1);
            } else {
                snapshot = mSnapshots.get(0);
            }
            if (!mEmulator.setSnapshot(snapshot)) {
                showSnackbar(R.string.load_state_failed, 300);
            }
        } else if (viewId == R.id.button_load_state1) {
            if (mSnapshots.isEmpty()) return;
            byte[] snapshot = mSnapshots.get(0);
            if (!mEmulator.setSnapshot(snapshot)) {
                showSnackbar(R.string.load_state_failed, 300);
            }
        }
    }
}