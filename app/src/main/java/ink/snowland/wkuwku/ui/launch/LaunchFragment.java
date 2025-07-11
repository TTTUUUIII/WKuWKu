package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.ui.launch.LaunchViewModel.*;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.net.Uri;
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

import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import ink.snowland.wkuwku.device.HwController;
import ink.snowland.wkuwku.device.VirtualController;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.interfaces.OnEmulatorEventListener;
import ink.snowland.wkuwku.util.BiosProvider;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.NoFilterArrayAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchFragment extends BaseFragment implements OnEmulatorEventListener, View.OnClickListener, BaseActivity.OnKeyEventListener, BaseActivity.OnTouchEventListener {
    private static final String TAG = "PlayFragment";
    private static final int PLAYER_1 = 0;
    private static final int PLAYER_2 = 1;
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
    private final SparseArray<BaseController> mControllerRoutes = new SparseArray<>();
    private final List<BaseController> mControllers = new ArrayList<>();

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
        mRenderer = new GLRenderer(requireContext());
        binding.glSurfaceView.setEGLContextClientVersion(3);
        binding.glSurfaceView.setRenderer(mRenderer);
        binding.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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

    @SuppressLint("ClickableViewAccessibility")
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
        int status = mViewModel.startEmulator(mGame);
        if (status == LaunchViewModel.NO_ERR) {
            attachToEmulator();
            if (mAutoLoadState && !mAutoLoadDisabled) {
                handler.postDelayed(mViewModel::loadStateAtLast, 300);
            }
        } else if (status == ERR_LOAD_FAILED){
            showSnackbar(R.string.load_game_failed, SNACKBAR_LENGTH_SHORT);
        } else if (status == ERR_EMULATOR_NOT_FOUND) {
            showSnackbar(R.string.no_matching_emulator_found, SNACKBAR_LENGTH_SHORT);
        }
    }

    private final Runnable mHideTimerTask = () -> {
        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        binding.topControlMenu.animate()
                .alpha(0.f)
                .setDuration(duration)
                .withEndAction(() -> {
                    binding.topControlMenu.setVisibility(View.GONE);
                });
        binding.controllerRoot.animate()
                .alpha(0.f)
                .setDuration(duration)
                .withEndAction(() -> {
            binding.controllerRoot.setVisibility(View.GONE);
        });
    };

    private void resetHideTimer() {
        handler.removeCallbacks(mHideTimerTask);
        binding.topControlMenu.setVisibility(View.VISIBLE);
        binding.topControlMenu.setAlpha(1.f);
        binding.controllerRoot.setVisibility(View.VISIBLE);
        binding.controllerRoot.setAlpha(1.f);
        handler.postDelayed(mHideTimerTask, 5000);
    }

    private void attachToEmulator() {
        if (mViewModel.getEmulator() == null) return;
        final Emulator emulator = mViewModel.getEmulator();
        mRenderer.setPixelFormat(emulator.getPixelFormat());
        mRenderer.setScreenRotation(emulator.getRotation());
        emulator.setOnEmulatorEventListener(this);
        mAutoLoadDisabled = SettingsManager.getStringSet(BLACKLIST_AUTO_LOAD_STATE).contains(emulator.getTag());
    }

    private int mCurrentWidth;
    private int mCurrentHeight;
    private void adjustScreenSize(int width, int height) {
        if (mCurrentWidth == width && mCurrentHeight == height) return;
        float ratio = (float) width / height;
        mCurrentWidth = width;
        mCurrentHeight = height;
        binding.glSurfaceView.post(() -> {
            ViewGroup.LayoutParams lp = binding.glSurfaceView.getLayoutParams();
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
            binding.glSurfaceView.setLayoutParams(lp);
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
        resetHideTimer();
        parentActivity.addOnTouchEventListener(this);
        mViewModel.resumeEmulator();
    }

    @Override
    public void onPause() {
        super.onPause();
        parentActivity.removeOnKeyEventListener(this);
        parentActivity.removeOnTouchEventListener(this);
        handler.removeCallbacks(mHideTimerTask);
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

    private void selectDefaultController() {
//        BaseController controller;
//        switch (mGame.system) {
//            case "game-gear":
//            case "master-system":
//            case "mega-cd":
//            case "mega-drive":
//            case "sega-pico":
//            case "sg-1000":
//            case "saturn":
//                controller = new SegaController(parentActivity);
//                break;
//            default:
//                controller = new VirtualController(parentActivity);
//        }
        BaseController controller = new VirtualController(parentActivity);
        List<InputDevice> devices = getInputDevices();
        for (InputDevice device : devices) {
            if (device.isVirtual()) continue;
            if ((device.getSources() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
            || (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            || ((device.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                mControllers.add(new HwController(parentActivity, device.getName(), device.getId()));
            }
        }
        mControllers.add(controller);
        binding.controllerRoot.addView(controller.getView());
        mControllerRoutes.put(PLAYER_1, controller);
    }

    private DialogLayoutExitGameBinding mExitLayoutBinding;

    private AlertDialog mExitDialog;

    @SuppressLint("SetTextI18n")
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
            mExitLayoutBinding.player1Dropdown.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString();
                    if (text.contains("@")) {
                        try {
                            int deviceId = Integer.parseInt(text.split("@")[0]);
                            for (BaseController controller : mControllers) {
                                if (controller.getDeviceId() == deviceId) {
                                    mControllerRoutes.put(PLAYER_1, controller);
                                    break;
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            mExitLayoutBinding.player2Dropdown.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString();
                    if (text.contains("@")) {
                        try {
                            int deviceId = Integer.parseInt(text.split("@")[0]);
                            for (BaseController controller : mControllers) {
                                if (controller.getDeviceId() == deviceId) {
                                    mControllerRoutes.put(PLAYER_2, controller);
                                    break;
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }
        if (mExitDialog.isShowing()) return;
        mExitLayoutBinding.saveState.setChecked(SettingsManager.getBoolean(AUTO_SAVE_STATE_CHECKED, true));
        List<String> sources = mControllers.stream().map(it -> it.getDeviceId() + "@" + it.getName()).collect(Collectors.toList());
        mExitLayoutBinding.player1Dropdown.setAdapter(new NoFilterArrayAdapter<>(parentActivity, R.layout.layout_simple_text, sources));
        BaseController primaryController = mControllerRoutes.get(PLAYER_1);
        String primarySource = primaryController.getDeviceId() + "@" + primaryController.getName();
        mExitLayoutBinding.player1Dropdown.setText(primarySource);
        sources = sources.stream().filter(it -> !it.equals(primarySource)).collect(Collectors.toList());
        mExitLayoutBinding.player2Dropdown.setAdapter(new NoFilterArrayAdapter<>(parentActivity, R.layout.layout_simple_text, sources));
        BaseController controller = mControllerRoutes.get(PLAYER_2);
        if (controller != null) {
            mExitLayoutBinding.player2Dropdown.setText(controller.getDeviceId() + "@" + controller.getName());
        } else {
            mExitLayoutBinding.player2Dropdown.setText("N/A");
        }
        mExitDialog.show();
    }

    private void onExit() {
        if (mViewModel.getEmulator() == null) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
            return;
        }
        boolean captureScreen = !FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png").exists();
        if (mExitLayoutBinding.saveState.isChecked()) {
            mViewModel.pauseEmulator();
            mViewModel.saveCurrentSate();
            captureScreen = true;
        }
        if (captureScreen) {
            mRenderer.exportAsPNG(FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png"));
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
            mViewModel.resetEmulator();
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
        int deviceId = event.getDeviceId();
        boolean handled = false;
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
        for (BaseController controller : mControllers) {
            if (controller.getDeviceId() != deviceId) continue;
            handled = controller.onKeyEvent(event);;
        }
        return handled;
    }

    @Override
    public void onPixelFormatChanged(int format) {
        mRenderer.setPixelFormat(format);
    }

    @Override
    public void onRotationChanged(int rotation) {
        mRenderer.setScreenRotation(rotation);
    }

    @Override
    public void onDrawFramebuffer(final byte[] data, int width, int height, int pitch) {
        if (mRenderer.getScreenRotation() == 1 || mRenderer.getScreenRotation() == 3) {
            adjustScreenSize(height, width);
        } else {
            adjustScreenSize(width, height);
        }
        mRenderer.updateFramebuffer(data, width, height, pitch);
        binding.glSurfaceView.requestRender();
    }

    @Override
    public short onGetInputState(int port, int device, int index, int id) {
        BaseController controller = mControllerRoutes.get(port);
        if (controller == null || controller.type != device) return 0;
        return controller.getState(id);
    }

    @Override
    public boolean onRumbleEvent(int port, int effect, int streng) {
        return false;
    }

    @Override
    public void onMessage(@NonNull EmMessageExt message) {
        if (message.type == EmMessageExt.MESSAGE_TARGET_OSD) {
            handler.post(() -> showSnackbar(message.msg, message.duration));
        }
    }

    @Override
    public void onTouchEvent(MotionEvent ev) {
        resetHideTimer();
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        super.onInputDeviceAdded(deviceId);
        InputDevice device = getInputDevice(deviceId);
        if (device.isVirtual()) return;
        if ((device.getSources() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        || (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        || (device.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            mControllers.add(new HwController(requireContext(), device.getName(), device.getId()));
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        super.onInputDeviceRemoved(deviceId);
        for (BaseController controller : mControllers) {
            if (controller.getDeviceId() == deviceId) {
                mControllers.remove(controller);
                break;
            }
        }
    }
}