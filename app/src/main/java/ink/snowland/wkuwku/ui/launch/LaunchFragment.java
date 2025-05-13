package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.interfaces.Emulator.*;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import ink.snowland.wkuwku.device.AudioDevice;
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

public class LaunchFragment extends BaseFragment implements OnEmulatorEventListener {
    private static final String TAG = "PlayFragment";
    private static final String AUTO_RESTORE_LAST_STATE = "app_emulator_restore_last_state";
    private static final String AUTO_MARK_BROKEN_WHEN_START_GAME_FAILED = "app_mark_broken_when_start_game_failed";
    private FragmentLaunchBinding binding;
    private Emulator mEmulator;
    private GLVideoDevice mVideoDevice;
    private BaseController mController;
    private AudioDevice mAudioDevice;
    private LaunchViewModel mViewModel;
    private Game mGame;
    private Snackbar mSnackbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity.setStatusBarVisibility(false);
        parentActivity.setActionBarVisibility(false);
        parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mViewModel = new ViewModelProvider(this).get(LaunchViewModel.class);
        mAudioDevice = new AudioDevice();
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
//        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mSnackbar.getView().getLayoutParams();
//        layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, getResources().getDisplayMetrics());
        selectController();
        parentActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            selectEmulator();
            if (mEmulator == null) {
                Toast.makeText(parentActivity, R.string.no_matching_emulator_found, Toast.LENGTH_SHORT).show();
                return;
            }
            mViewModel.setPendingIndicator(true, getString(R.string.fmt_downloading, "bios"));
            Disposable disposable = BiosProvider.downloadBiosForGame(mGame, FileManager.getCacheDirectory())
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
        parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        parentActivity.setStatusBarVisibility(true);
        parentActivity.setActionBarVisibility(true);
        if (mEmulator != null) {
            mEmulator.suspend();
        }
    }

    private void launch() {
        boolean success = false;
        if (mGame.state == Game.STATE_VALID && mEmulator != null) {
            applyOptions();
            mEmulator.setEmulatorEventListener(this);
            mEmulator.attachDevice(AUDIO_DEVICE, mAudioDevice);
            mEmulator.attachDevice(VIDEO_DEVICE, mVideoDevice);
            mEmulator.attachDevice(INPUT_DEVICE, mController);
            mEmulator.setSystemDirectory(SYSTEM_DIR, FileManager.getCacheDirectory());
            mEmulator.setSystemDirectory(SAVE_DIR, FileManager.getFileDirectory(mEmulator.getTag()));
            if (mEmulator.run(mGame.filepath, mGame.system)) {
                if (SettingsManager.getBoolean(AUTO_RESTORE_LAST_STATE)) {
                    loadCurrentState(true);
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
    }

    private void saveCurrentState(boolean auto) {
        if (mEmulator == null) return;
        if (mGame.system.equals("famicom")) return;
        final String prefix = mEmulator.getTag();
        final String ext = auto ? ".ast" : ".st";
        mEmulator.save(SAVE_STATE, FileManager.getFile(FileManager.STATE_DIRECTORY, prefix + "@" + mGame.md5 + ext));
    }

    private void loadCurrentState(boolean auto) {
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
    private void selectEmulator() {
        String tag = SettingsManager.getString(String.format(Locale.ROOT, "app_%s_core", mGame.system));
        if (tag.isEmpty()) {
            mEmulator = EmulatorManager.getDefaultEmulator(mGame.system);
        } else {
            mEmulator = EmulatorManager.getEmulator(tag);
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
}