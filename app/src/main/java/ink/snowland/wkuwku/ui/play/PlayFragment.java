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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Objects;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.MainActivity;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.databinding.FragmentPlayBinding;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.device.AudioDevice;
import ink.snowland.wkuwku.device.JoyPad;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.device.GLVideoDevice;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlayFragment extends Fragment implements View.OnTouchListener {

    private static final int JOYSTICK_TRIGGER_THRESHOLD = 50;
    private FragmentPlayBinding binding;
    private Emulator mEmulator;
    private MainActivity mParent;
    private GLVideoDevice mVideoDevice;
    private EmInputDevice mInputDevice;
    private AudioDevice mAudioDevice;
    private PlayViewModel mViewModel;
    private Game mGame;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParent = (MainActivity) requireActivity();
        mParent.setStatusBarVisibility(false);
        mParent.setActionBarVisibility(false);
        mParent.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mViewModel = new ViewModelProvider(this).get(PlayViewModel.class);
        mAudioDevice = new AudioDevice();
        mVideoDevice = new GLVideoDevice(requireContext()) {
            @Override
            public void refresh(byte[] data, int width, int height, int pitch) {
                super.refresh(data, width, height, pitch);
                binding.glSurfaceView.requestRender();
            }
        };
        mInputDevice = new JoyPad(0);
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
        mParent.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
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
        mParent.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        mParent.setStatusBarVisibility(true);
        mParent.setActionBarVisibility(true);
        if (mEmulator != null) {
            mEmulator.suspend();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        int viewId = v.getId();
        int id;
        if (viewId == R.id.button_select) {
            id = RETRO_DEVICE_ID_JOYPAD_SELECT;
        } else if (viewId == R.id.button_start) {
            id = RETRO_DEVICE_ID_JOYPAD_START;
        } else if (viewId == R.id.button_a) {
            id = RETRO_DEVICE_ID_JOYPAD_A;
        } else if (viewId == R.id.button_b) {
            id = RETRO_DEVICE_ID_JOYPAD_B;
        } else if (viewId == R.id.button_x) {
            id = RETRO_DEVICE_ID_JOYPAD_X;
        } else if (viewId == R.id.button_y) {
            id = RETRO_DEVICE_ID_JOYPAD_Y;
        } else if (viewId == R.id.button_l) {
            id = RETRO_DEVICE_ID_JOYPAD_L;
        } else if (viewId == R.id.button_l2) {
            id = RETRO_DEVICE_ID_JOYPAD_L2;
        } else if (viewId == R.id.button_r) {
            id = RETRO_DEVICE_ID_JOYPAD_R;
        } else if (viewId == R.id.button_r2) {
            id = RETRO_DEVICE_ID_JOYPAD_R2;
        } else {
            return false;
        }
        int state;
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            state = EmInputDevice.KEY_DOWN;
        } else if (action == MotionEvent.ACTION_UP) {
            state = EmInputDevice.KEY_UP;
        } else {
            return false;
        }
        mInputDevice.setState(id, state);
        return false;
    }

    private void startGame() {
        boolean success = false;
        if (mGame != null) {
            mEmulator = EmulatorManager.getEmulator(mGame.system);
            if (mEmulator != null) {
                mEmulator.attachDevice(AUDIO_DEVICE, mAudioDevice);
                mEmulator.attachDevice(VIDEO_DEVICE, mVideoDevice);
                mEmulator.attachDevice(INPUT_DEVICE, mInputDevice);
                mEmulator.setSystemDirectory(Objects.requireNonNull(mParent.getExternalCacheDir()));
                if (mEmulator.run(new File(mGame.filepath))) {
                    bindEvents();
                    success = true;
                }
            }
        }
        if (!success) {
            Toast.makeText(mParent.getApplicationContext(), R.string.load_game_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private AlertDialog mExitDialog;
    private void showExitGameDialog() {
        if (mExitDialog == null) {
            mExitDialog = new MaterialAlertDialogBuilder(requireActivity())
                    .setIcon(R.drawable.app_icon)
                    .setTitle(R.string.please_select)
                    .setSingleChoiceItems(R.array.play_fragment_exit_items, -1, (DialogInterface.OnClickListener) (dialog, which) -> {
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
        mGame.lastPlayedTime = System.currentTimeMillis();
        Disposable disposable = mViewModel.update(mGame)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    error.printStackTrace(System.err);
                })
                .doOnComplete(() -> {
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                })
                .subscribe();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindEvents() {
        binding.buttonSelect.setOnTouchListener(this);
        binding.buttonStart.setOnTouchListener(this);
        binding.buttonA.setOnTouchListener(this);
        binding.buttonB.setOnTouchListener(this);
        binding.buttonX.setOnTouchListener(this);
        binding.buttonY.setOnTouchListener(this);
        binding.buttonL.setOnTouchListener(this);
        binding.buttonL2.setOnTouchListener(this);
        binding.buttonR.setOnTouchListener(this);
        binding.buttonR2.setOnTouchListener(this);
        binding.joystickView.setOnMoveListener((angle, strength) -> {
            double rad = Math.toRadians(angle);
            double dist = strength / 100.0;
            int xpos = (int) (dist * Math.cos(rad) * 127);
            int ypos = (int) (dist * Math.sin(rad) * 127);
            int left = xpos < -JOYSTICK_TRIGGER_THRESHOLD ? EmInputDevice.KEY_DOWN : EmInputDevice.KEY_UP;
            int right = xpos > JOYSTICK_TRIGGER_THRESHOLD ? EmInputDevice.KEY_DOWN : EmInputDevice.KEY_UP;
            int up = ypos > JOYSTICK_TRIGGER_THRESHOLD ? EmInputDevice.KEY_DOWN : EmInputDevice.KEY_UP;
            int down = ypos < -JOYSTICK_TRIGGER_THRESHOLD ? EmInputDevice.KEY_DOWN : EmInputDevice.KEY_UP;
            mInputDevice.setState(RETRO_DEVICE_ID_JOYPAD_DOWN, down);
            mInputDevice.setState(RETRO_DEVICE_ID_JOYPAD_UP, up);
            mInputDevice.setState(RETRO_DEVICE_ID_JOYPAD_LEFT, left);
            mInputDevice.setState(RETRO_DEVICE_ID_JOYPAD_RIGHT, right);
        });
    }

    public static final String ARG_GAME = "game";
}