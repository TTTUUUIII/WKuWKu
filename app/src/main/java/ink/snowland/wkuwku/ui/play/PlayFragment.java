package ink.snowland.wkuwku.ui.play;

import static ink.snowland.wkuwku.interfaces.Emulator.*;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.BaseController;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.databinding.FragmentPlayBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.device.AudioDevice;
import ink.snowland.wkuwku.device.NESController;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.device.GLVideoDevice;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlayFragment extends BaseFragment {
    private static final String TAG = "PlayFragment";
    private static final String AUTO_RESTORE_LAST_STATE = "app_emulator_restore_last_state";
    private static final String AUTO_MARK_BROKEN_WHEN_START_GAME_FAILED = "app_mark_broken_when_start_game_failed";
    private FragmentPlayBinding binding;
    private Emulator mEmulator;
    private GLVideoDevice mVideoDevice;
    private BaseController mController;
    private AudioDevice mAudioDevice;
    private PlayViewModel mViewModel;
    private Game mGame;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity.setStatusBarVisibility(false);
        parentActivity.setActionBarVisibility(false);
        parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mViewModel = new ViewModelProvider(this).get(PlayViewModel.class);
        mAudioDevice = new AudioDevice();
        mVideoDevice = new GLVideoDevice(requireContext()) {
            @Override
            public void refresh(byte[] data, int width, int height, int pitch) {
                super.refresh(data, width, height, pitch);
                binding.glSurfaceView.requestRender();
            }
        };
        Bundle arguments = getArguments();
        if (arguments != null) {
            mGame = arguments.getParcelable(ARG_GAME);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            startGame();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPlayBinding.inflate(getLayoutInflater());
        binding.glSurfaceView.setEGLContextClientVersion(3);
        binding.glSurfaceView.setRenderer(mVideoDevice.getRenderer());
        binding.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        parentActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        return binding.getRoot();
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
        parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        parentActivity.setStatusBarVisibility(true);
        parentActivity.setActionBarVisibility(true);
        if (mEmulator != null) {
            mEmulator.suspend();
        }
    }



    private void startGame() {
        boolean success = false;
        if (mGame != null && mGame.state == Game.STATE_VALID) {
            mEmulator = getEmulatorForGame(mGame);
            if (mEmulator != null) {
                applyOptions();
                prepareController();
                mEmulator.attachDevice(AUDIO_DEVICE, mAudioDevice);
                mEmulator.attachDevice(VIDEO_DEVICE, mVideoDevice);
                mEmulator.attachDevice(INPUT_DEVICE, mController);
                mEmulator.setSystemDirectory(Objects.requireNonNull(parentActivity.getExternalCacheDir()));
                if (mEmulator.run(new File(mGame.filepath))) {
                    if (SettingsManager.getBoolean(AUTO_RESTORE_LAST_STATE)) {
                        loadCurrentState(true);
                    }
                    success = true;
                }
            }
        }
        if (!success) {
            if (mGame != null && mGame.state != Game.STATE_BROKEN) {
                if (SettingsManager.getBoolean(AUTO_MARK_BROKEN_WHEN_START_GAME_FAILED)) {
                    mGame.state = Game.STATE_BROKEN;
                    Disposable disposable = mViewModel.update(mGame)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe();
                }
            }
            Toast.makeText(parentActivity.getApplicationContext(), R.string.load_game_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareController() {
        assert mGame != null;
        if (mGame.system.toLowerCase(Locale.ROOT).equals("nes")) {
            mController = new NESController(0, parentActivity);
            binding.getRoot().addView(mController.getView());
        } else {
            /*Not supported yet.*/
            Log.w(TAG, "No controller for system \"" + mGame.system + "\"");
        }
    }

    private void saveCurrentState(boolean auto) {
        if (mEmulator == null) return;
        if (mGame.filepath.endsWith("fds")) return;
        final String ext = auto ? ".ast" : ".st";
        mEmulator.save(SAVE_STATE, FileManager.getFile(FileManager.STATE_DIRECTORY, mGame.md5 + ext));
    }

    private void loadCurrentState(boolean auto) {
        if (mEmulator == null) return;
        final String ext = auto ? ".ast" : ".st";
        File file = FileManager.getFile(FileManager.STATE_DIRECTORY, mGame.md5 + ext);
        if (file.exists()) {
            mEmulator.load(LOAD_STATE, file);
        }
    }

    private void applyOptions() {
        assert mEmulator != null;
        Collection<EmOption> options = mEmulator.getOptions();
        for (EmOption option : options) {
            if (!option.supported) continue;
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
                .doOnComplete(() -> {
                    if (SettingsManager.getBoolean(AUTO_RESTORE_LAST_STATE)) {
                        saveCurrentState(true);
                    }
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                })
                .subscribe();
    }

    public static final String ARG_GAME = "game";

    private static Emulator getEmulatorForGame(@NonNull Game game) {
        String system = game.system.toLowerCase(Locale.ROOT);
        String tag = SettingsManager.getString(String.format(Locale.ROOT, "app_%s_core", system));
        if (tag.isEmpty()) {
            if (system.equals("nes")) {
                tag = "fceumm";
            }
        }
        return EmulatorManager.getEmulator(tag);
    }
}