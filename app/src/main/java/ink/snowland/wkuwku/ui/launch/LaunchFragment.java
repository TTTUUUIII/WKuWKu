package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.util.FileManager.*;
import static ink.snowland.wkuwku.interfaces.IEmulator.*;
import static ink.snowland.wkuwku.common.Errors.*;
import static ink.snowland.wkuwku.ui.launch.LaunchViewModel.MAX_COUNT_OF_SNAPSHOT;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Pair;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;

import org.wkuwku.util.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.Hotkey;
import ink.snowland.wkuwku.common.ActionListener;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.common.Callable;
import ink.snowland.wkuwku.common.Controller;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.databinding.FragmentLaunchBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.device.ExternalController;
import ink.snowland.wkuwku.device.SegaController;
import ink.snowland.wkuwku.device.VirtualController;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.interfaces.OnEmulatorV2EventListener;
import ink.snowland.wkuwku.util.DownloadManager;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.FileUtils;
import ink.snowland.wkuwku.util.Logger;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.NoFilterArrayAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchFragment extends BaseFragment implements View.OnClickListener, BaseActivity.OnTouchEventListener, OnEmulatorV2EventListener, AudioManager.OnAudioFocusChangeListener {

    //    private static final int TYPE_KEEP                  = 0;
    private static final int TYPE_FULLSCREEN = 1;
    private static final int TYPE_KEEP_FULLSCREEN = 2;

    private static final int PLAYER_1 = 0;
    private static final int PLAYER_2 = 1;
    private static final String HOTKEY_QUICK_SAVE = "hotkey_quick_save";
    private static final String HOTKEY_QUICK_LOAD = "hotkey_quick_load";
    private static final String HOTKEY_SCREENSHOT = "hotkey_screenshot";
    private static final String HOTKEY_RESET = "hotkey_reset";
    private static final String VIRTUAL_CONTROLLER_LAYOUT = "input_virtual_controller_layout";
    private static final String KEEP_SCREEN_ON = "app_keep_screen_on";
    private static final String BLACKLIST_AUTO_LOAD_STATE = "app_blacklist_auto_load_state";
    private static final String AUTO_SAVE_STATE_CHECKED = "app_auto_save_state_checked";
    private static final String PLAYER_1_CONTROLLER_DESCRIPTOR = "player_1_controller_descriptor";
    private static final String PLAYER_2_CONTROLLER_DESCRIPTOR = "player_2_controller_descriptor";
    private FragmentLaunchBinding binding;
    private LaunchViewModel mViewModel;
    private Game mGame;
    private final int mVideoRatioType = NumberUtils
            .parseInt(SettingsManager.getString("app_video_ratio", "0"),
                    0);
    private boolean mKeepScreenOn;
    private boolean mAutoLoadState;
    private boolean mAutoLoadDisabled;
    private final Logger mLogger = new Logger("App", "LaunchFragment");
    private final SparseArray<Controller> mControllerRoutes = new SparseArray<>();
    private int mP1ControllerId;
    private int mP2ControllerId;
    private final List<Controller> mControllerSources = new ArrayList<>();
    private AudioManager mAudioManager;
    private AudioFocusRequest mAudioRequest;
    private BottomSheetBehavior<?> mBottomSheetBehavior;

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
        if (mGame != null) {
            mViewModel.selectEmulator(mGame);
        }
        mAutoLoadState = arguments.getBoolean(ARG_AUTO_LOAD_STATE, false);
        mP1ControllerId = Controller.VIRTUAL_CONTROLLER_DEVICE_ID;
        mP2ControllerId = Controller.INVALID_CONTROLLER_DEVICE_ID;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLaunchBinding.inflate(getLayoutInflater());
        attachToEmulator();
        binding.pendingIndicator.setDataModel(mViewModel);
        if (mVideoRatioType == TYPE_FULLSCREEN) {
            binding.surfaceView.fullScreen();
        } else if (mVideoRatioType == TYPE_KEEP_FULLSCREEN) {
            binding.getRoot().setBackgroundColor(0xFF000000);
        }
        binding.saveState.setChecked(SettingsManager.getBoolean(AUTO_SAVE_STATE_CHECKED, true));
        parentActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.standardSheet);
        bindEvent();
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentActivity.setActionBarVisibility(false);
        attachToController();
        if (savedInstanceState == null) {
            IEmulator emulator = mViewModel.getEmulator();
            if (emulator != null) {
                Optional<EmSystem> system = emulator.getSupportedSystems()
                        .stream()
                        .filter(it -> it.tag.equals(mGame.system))
                        .findFirst();
                if (system.isPresent()) {
                    List<Pair<String, File>> items = system.get().biosFiles.stream()
                            .map(it -> new Pair<>(it.url, getFile(SYSTEM_DIRECTORY, it.name)))
                            .filter(it -> !it.second.exists())
                            .collect(Collectors.toList());
                    if (items.isEmpty()) {
                        startEmulator();
                    } else {
                        mViewModel.setPendingIndicator(true, getString(R.string.downloading_files));
                        DownloadManager.download(items, new ActionListener() {
                            @Override
                            public void onSuccess() {
                                startEmulator();
                                mViewModel.setPendingIndicator(false);
                            }

                            @Override
                            public void onFailure(Throwable e) {
                                e.printStackTrace(System.err);
                                if (e instanceof SocketTimeoutException) {
                                    showSnackbar(R.string.network_timeout, Snackbar.LENGTH_LONG);
                                } else {
                                    showSnackbar(R.string.network_error, Snackbar.LENGTH_LONG);
                                }
                                mViewModel.setPendingIndicator(false);
                            }
                        });
                    }
                }
            } else {
                showSnackbar(R.string.no_matching_emulator_found, Snackbar.LENGTH_LONG);
            }
        } else {
            mVideoWidth = savedInstanceState.getInt("video_width");
            mVideoHeight = savedInstanceState.getInt("video_height");
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                adjustScreenSize(mVideoWidth, mVideoHeight);
            }
        }
        ArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(requireContext(), R.layout.layout_simple_text, new ArrayList<>());
        List<String> sources = mControllerSources.stream()
                .map(it -> String.format(Locale.US, "%d@%s", it.getDeviceId(), it.getName()))
                .collect(Collectors.toList());
        adapter.addAll(sources);
        binding.p1ControllerSelector.setAdapter(adapter);
        adapter = new NoFilterArrayAdapter<>(requireContext(), R.layout.layout_simple_text, new ArrayList<>());
        sources = mControllerSources.stream()
                .filter(it -> !it.isVirtual())
                .map(it -> String.format(Locale.US, "%d@%s", it.getDeviceId(), it.getName()))
                .collect(Collectors.toList());
        adapter.addAll(sources);
        binding.p2ControllerSelector.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("video_width", mVideoWidth);
        outState.putInt("video_height", mVideoHeight);
    }

    private void startEmulator() {
        int status = mViewModel.startEmulator();
        if (status == NO_ERR) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mAudioManager = (AudioManager) parentActivity.getSystemService(Context.AUDIO_SERVICE);
                mAudioRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(this)
                        .setAcceptsDelayedFocusGain(true)
                        .build();
                if (mAudioManager.requestAudioFocus(mAudioRequest) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                    mLogger.e("Request audio focus failed!");
                }
            }
            parentActivity.setPerformanceModeEnable(SettingsManager.getBoolean(SettingsManager.PERFORMANCE_MODE));
            if (mAutoLoadState && !mAutoLoadDisabled) {
                handler.postDelayed(() -> loadStateAt(MAX_COUNT_OF_SNAPSHOT - 1, false), 300);
            }
        } else {
            showSnackbar(R.string.load_game_failed, Snackbar.LENGTH_LONG);
        }
    }

    private final Runnable mHideTimerTask = () -> {
        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        binding.topControlMenu.animate()
                .alpha(0.f)
                .setDuration(duration)
                .withEndAction(() -> binding.topControlMenu.setVisibility(View.GONE));
        binding.controllerRoot.animate()
                .alpha(0.f)
                .setDuration(duration)
                .withEndAction(() -> binding.controllerRoot.setVisibility(View.GONE));
    };

    private void resetHideTimer() {
        handler.removeCallbacks(mHideTimerTask);
        binding.topControlMenu.setVisibility(View.VISIBLE);
        binding.topControlMenu.setAlpha(1.f);
        Controller p1 = mControllerRoutes.get(PLAYER_1);
        Controller p2 = mControllerRoutes.get(PLAYER_2);
        if (binding.switchVirtualController.isChecked()
                && (p1.isVirtual() || (p2 != null && p2.isVirtual()))) {
            binding.controllerRoot.setVisibility(View.VISIBLE);
            binding.controllerRoot.setAlpha(1.f);
        }
        handler.postDelayed(mHideTimerTask, 5000);
    }

    private void attachToEmulator() {
        IEmulator emulator = mViewModel.getEmulator();
        if (emulator != null) {
            emulator.setOnEventListener(this);
            binding.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    emulator.attachSurface(holder.getSurface());
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                    emulator.adjustSurface(width, height);
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    emulator.detachSurface();
                }
            });
            mAutoLoadDisabled = SettingsManager.getStringSet(BLACKLIST_AUTO_LOAD_STATE).contains(emulator.getProp(PROP_ALIAS, String.class));
        }
    }

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private void adjustScreenSize(int width, int height) {
        if (mVideoRatioType != TYPE_FULLSCREEN) {
            binding.surfaceView.adjustSurfaceSize(width, height);
        }
        mVideoWidth = width;
        mVideoHeight = height;
    }

    private final OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            requestExit();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        resetHideTimer();
        mViewModel.resumeEmulator();
    }

    @Override
    public void onPause() {
        super.onPause();
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

    private void attachToController() {
        VirtualController virtualController;
        if (SettingsManager.equals(VIRTUAL_CONTROLLER_LAYOUT, "sega")) {
            virtualController = new SegaController(parentActivity);
        } else {
            virtualController = new VirtualController(parentActivity);
        }
        binding.controllerRoot.addView(virtualController.getView());
        mControllerSources.add(virtualController);
        List<InputDevice> devices = getInputDevices();
        String p1descriptor = SettingsManager.getString(PLAYER_1_CONTROLLER_DESCRIPTOR);
        String p2descriptor = SettingsManager.getString(PLAYER_2_CONTROLLER_DESCRIPTOR);
        for (InputDevice device : devices) {
            if (ExternalController.isSupportedDevice(device)) {
                Controller controller = ExternalController.from(device);
                String descriptor = controller.getDescriptor();
                int deviceId = controller.getDeviceId();
                if (descriptor.equals(p1descriptor)) {
                    mP1ControllerId = deviceId;
                } else if (descriptor.equals(p2descriptor)) {
                    mP2ControllerId = deviceId;
                }
                mControllerSources.add(controller);
            }
        }
        updateControllerRoutes();
    }

    private void requestExit() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED
         || mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            Controller controller = mControllerRoutes.get(PLAYER_1);
            binding.p1ControllerSelector.setText(String.format(Locale.US, "%d@%s", controller.getDeviceId(), controller.getName()));
            controller = mControllerRoutes.get(PLAYER_2);
            if (controller != null) {
                binding.p2ControllerSelector.setText(String.format(Locale.US, "%d@%s", controller.getDeviceId(), controller.getName()));
            } else {
                binding.p2ControllerSelector.setText(R.string.none);
            }
            mViewModel.pauseEmulator();
            mShouldResumeEmulator = true;
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void bindEvent() {
        binding.pendingIndicator.setLifecycleOwner(this);
        binding.buttonSavestate.setOnClickListener(this);
        binding.buttonScreenshot.setOnClickListener(this);
        binding.buttonLoadState1.setOnClickListener(this);
        binding.buttonLoadState2.setOnClickListener(this);
        binding.buttonLoadState3.setOnClickListener(this);
        binding.buttonLoadState4.setOnClickListener(this);
        binding.buttonLoadLastState.setOnClickListener(this);
        binding.exit.setOnClickListener(this);
        binding.reset.setOnClickListener(this);
        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if ((newState == BottomSheetBehavior.STATE_HIDDEN
                        || newState == BottomSheetBehavior.STATE_COLLAPSED)) {
                    if (mShouldResumeEmulator) {
                        mViewModel.resumeEmulator();
                        mShouldResumeEmulator = false;
                    }
                    checkControllerRoutes();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                /*Do nothing*/
            }
        });
    }

    private boolean mShouldResumeEmulator = false;

    private boolean mShouldExit = false;
    private void onExit() {
        if (!mViewModel.isPlaying()) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
            return;
        }
        Controller controller = mControllerRoutes.get(PLAYER_1);
        SettingsManager.putString(PLAYER_1_CONTROLLER_DESCRIPTOR, controller.getDescriptor());
        controller = mControllerRoutes.get(PLAYER_2);
        if (controller != null) {
            SettingsManager.putString(PLAYER_2_CONTROLLER_DESCRIPTOR, controller.getDescriptor());
        } else {
            SettingsManager.remove(PLAYER_2_CONTROLLER_DESCRIPTOR);
        }
        File screenshot = FileManager.getFile(FileManager.IMAGE_DIRECTORY, mGame.id + ".png");
        boolean captureScreen = !screenshot.exists();
        if (binding.saveState.isChecked()) {
            if (!mAutoLoadDisabled) {
                saveCurrentState(false);
            }
            captureScreen = true;
        }
        if (captureScreen) {
            captureScreen(screenshot.getPath(), error -> {
                if (mShouldExit) {
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                } else {
                    mShouldExit = true;
                }
            });
        }
        SettingsManager.putBoolean(AUTO_SAVE_STATE_CHECKED, binding.saveState.isChecked());
        mViewModel.stopEmulator();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mAudioManager != null) {
            mAudioManager.abandonAudioFocusRequest(mAudioRequest);
        }
        mGame.lastPlayedTime = System.currentTimeMillis();
        parentActivity.setPerformanceModeEnable(false);
        AppDatabase.db.gameInfoDao().update(mGame)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> error.printStackTrace(System.err))
                .doFinally(() -> {
                    if (mShouldExit) {
                        NavController navController = NavHostFragment.findNavController(this);
                        navController.popBackStack();
                    } else {
                        mShouldExit = true;
                    }
                })
                .subscribe();
    }

    public static final String ARG_GAME = "game";
    public static final String ARG_AUTO_LOAD_STATE = "auto_load_state";

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.reset) {
            mViewModel.resetEmulator();
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (viewId == R.id.exit) {
            mShouldResumeEmulator = false;
            onExit();
        } else {
            Controller controller = mControllerSources.get(0 /*VirtualController*/);
            if (controller instanceof VirtualController) {
                VirtualController vc = (VirtualController) controller;
                vc.vibrator();
            }
            if (viewId == R.id.button_savestate) {
                saveCurrentState(true);
            } else if (viewId == R.id.button_screenshot) {
                takeScreenshot();
            } else {
                int index = -1;
                if (viewId == R.id.button_load_last_state) {
                    index = LaunchViewModel.MAX_COUNT_OF_SNAPSHOT - 1;
                } else if (viewId == R.id.button_load_state4) {
                    index = 3;
                } else if (viewId == R.id.button_load_state3) {
                    index = 2;
                } else if (viewId == R.id.button_load_state2) {
                    index = 1;
                } else if (viewId == R.id.button_load_state1) {
                    index = 0;
                }
                if (index != -1) {
                    loadStateAt(index, true);
                }
            }
        }
    }

    private void takeScreenshot() {
        File tmp = new File(FileManager.getCacheDirectory(), "tmp.png");
        captureScreen(tmp.getAbsolutePath(), err -> {
            assert err != null;
            if (err == NO_ERR) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, mGame.title + "@" + System.currentTimeMillis() + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + getString(R.string.app_name));
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                ContentResolver contentResolver = requireContext().getContentResolver();
                Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return;
                try (FileInputStream from = new FileInputStream(tmp);
                     OutputStream to = contentResolver.openOutputStream(uri)) {
                    if (to == null) return;
                    FileUtils.copy(from, to);
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    contentResolver.update(uri, values, null, null);
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
                FileUtils.delete(tmp);
                showSnackbar(R.string.screenshot_saved, Snackbar.LENGTH_SHORT);
            } else if (err == ERR_NOT_SUPPORTED){
                showSnackbar(R.string.feat_not_supported, Snackbar.LENGTH_LONG);
            } else {
                showSnackbar(R.string.operation_failed, Snackbar.LENGTH_LONG);
            }
        });
    }

    @Override
    public boolean onKeyEvent(@NonNull KeyEvent event) {
        int deviceId = event.getDeviceId();
        InputDevice device = event.getDevice();
        if (device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC && !device.isVirtual()) {
            IEmulator emulator = mViewModel.getEmulator();
            if (emulator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (device.isExternal()) {
                        return emulator.dispatchKeyEvent(event);
                    }
                } else {
                    return emulator.dispatchKeyEvent(event);
                }
            }
        } else {
            for (Controller controller : mControllerSources) {
                if (controller.getDeviceId() == deviceId) {
                    return controller.dispatchKeyEvent(event);
                }
            }
        }
        return super.onKeyEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
        int deviceId = event.getDeviceId();
        boolean handled = false;
        for (Controller controller : mControllerSources) {
            if (controller.getDeviceId() != deviceId) continue;
            handled = controller.dispatchGenericMotionEvent(event);
        }
        return handled;
    }

    @Override
    public boolean onHotkeyEvent(@NonNull Hotkey hotkey) {
        boolean handled = true;
        switch (hotkey.key) {
            case HOTKEY_QUICK_SAVE:
                saveCurrentState(true);
                break;
            case HOTKEY_QUICK_LOAD:
                loadStateAt(MAX_COUNT_OF_SNAPSHOT - 1, true);
                break;
            case HOTKEY_SCREENSHOT:
                takeScreenshot();
                break;
            case HOTKEY_RESET:
                mViewModel.resetEmulator();
                break;
            default:
                handled = false;
        }
        if (handled) {
            return true;
        }
        return super.onHotkeyEvent(hotkey);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onInputDeviceAdded(int deviceId) {
        super.onInputDeviceAdded(deviceId);
        InputDevice device = getInputDevice(deviceId);
        if (ExternalController.isSupportedDevice(device)) {
            Controller controller = ExternalController.from(device);
            String descriptor = controller.getDescriptor();
            String p1descriptor = SettingsManager.getString(PLAYER_1_CONTROLLER_DESCRIPTOR);
            String p2descriptor = SettingsManager.getString(PLAYER_2_CONTROLLER_DESCRIPTOR);
            mControllerSources.add(controller);
            ((ArrayAdapter<String>)binding.p1ControllerSelector.getAdapter())
                    .add(String.format(Locale.US, "%d@%s", controller.getDeviceId(), controller.getName()));
            ((ArrayAdapter<String>)binding.p2ControllerSelector.getAdapter())
                    .add(String.format(Locale.US, "%d@%s", controller.getDeviceId(), controller.getName()));
            if (descriptor.equals(p1descriptor)) {
                mP1ControllerId = deviceId;
                updateControllerRoutes();
            } else if (descriptor.equals(p2descriptor)) {
                mP2ControllerId = deviceId;
                updateControllerRoutes();
            }
        }
    }

    @Override
    public void onTouchEvent(MotionEvent ev) {
        resetHideTimer();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onInputDeviceRemoved(int deviceId) {
        super.onInputDeviceRemoved(deviceId);
        Optional<Controller> controller = mControllerSources.stream()
                .filter(it -> it.getDeviceId() == deviceId)
                .findFirst();
        if (controller.isPresent()) {
            Controller it = controller.get();
            Controller p1 = mControllerRoutes.get(PLAYER_1);
            Controller p2 = mControllerRoutes.get(PLAYER_2);
            mControllerSources.remove(it);
            ((ArrayAdapter<String>)binding.p1ControllerSelector.getAdapter())
                    .remove(String.format(Locale.US, "%d@%s", it.getDeviceId(), it.getName()));
            ((ArrayAdapter<String>)binding.p2ControllerSelector.getAdapter())
                    .remove(String.format(Locale.US, "%d@%s", it.getDeviceId(), it.getName()));
            if (p1 == it) {
                mP1ControllerId = Controller.VIRTUAL_CONTROLLER_DEVICE_ID;
                updateControllerRoutes();
            } else if (p2 == it) {
                mP2ControllerId = Controller.INVALID_CONTROLLER_DEVICE_ID;
                updateControllerRoutes();
            }
        }
    }

    private void updateControllerRoutes() {
        for (Controller controller : mControllerSources) {
            if (controller.getDeviceId() == mP1ControllerId) {
                mControllerRoutes.put(PLAYER_1, controller);
            } else if (controller.getDeviceId() == mP2ControllerId) {
                mControllerRoutes.put(PLAYER_2, controller);
            }
        }
        if (mControllerRoutes.get(PLAYER_1) == null) {
            mControllerRoutes.put(PLAYER_1, mControllerSources.get(0) /*VirtualController*/);
        }
        if (mControllerRoutes.get(PLAYER_1) == mControllerRoutes.get(PLAYER_2)) {
            mControllerRoutes.put(PLAYER_2, null);
            binding.p2ControllerSelector.setText(R.string.none);
        }
    }

    private void checkControllerRoutes() {
        boolean update = false;
        String summary = binding.p1ControllerSelector.getText().toString();
        if (summary.contains("@")) {
            try {
                int deviceId = NumberUtils.parseInt(summary.split("@")[0], Controller.INVALID_CONTROLLER_DEVICE_ID);
                if (deviceId != Controller.INVALID_CONTROLLER_DEVICE_ID
                        && mP1ControllerId != deviceId) {
                    mP1ControllerId = deviceId;
                    update = true;
                }
            } catch (IndexOutOfBoundsException ignored) {}
        }

        if (summary.contains("@")) {
            try {
                int deviceId = NumberUtils.parseInt(summary.split("@")[0], Controller.INVALID_CONTROLLER_DEVICE_ID);
                if (deviceId != Controller.INVALID_CONTROLLER_DEVICE_ID
                        && mP2ControllerId != deviceId) {
                    mP2ControllerId = deviceId;
                    update = true;
                }
            } catch (IndexOutOfBoundsException ignored) {}
        }

        if (update) {
            updateControllerRoutes();
        }
    }

    @Override
    public void onVideoSizeChanged(int vw, int vh, int rotation) {
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            adjustScreenSize(vh, vw);
        } else {
            adjustScreenSize(vw, vh);
        }
    }

    @Override
    public int onPollInputState(int port, int device, int index, int id) {
        Controller controller = mControllerRoutes.get(port);
        if (controller != null && controller.isTypes(device)) {
            return controller.getState(device, index, id);
        } else if (device == RETRO_DEVICE_POINTER) {
            return binding.surfaceView.getTouchState(id);
        }
        return 0;
    }

    @Override
    public void onMessage(@NonNull EmMessageExt message) {
        if (message.type == EmMessageExt.MESSAGE_TARGET_OSD) {
            handler.post(() -> showSnackbar(message.msg, message.duration));
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        /*Do Nothing*/
    }

    private void captureScreen(@NonNull String path, @NonNull Callable<Integer> listener) {
        int error = mViewModel.captureScreen(path);
        if (error == NO_ERR) {
            listener.call(NO_ERR);
        } else if (error == ERR_NOT_SUPPORTED) {
            binding.surfaceView.asyncCopyPixels(bitmap -> {
                if (bitmap != null) {
                    try (FileOutputStream out = new FileOutputStream(path)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        listener.call(NO_ERR);
                    } catch (IOException e) {
                        listener.call(ERR);
                    }
                    bitmap.recycle();
                } else {
                    listener.call(ERR);
                }
            });
        } else {
            listener.call(ERR);
        }
    }

    private void loadStateAt(int index, boolean ui) {
        int error = mViewModel.loadStateAt(index);
        if (ui && error == ERR_NOT_SUPPORTED) {
            showSnackbar(R.string.feat_not_supported);
        }
    }

    private void saveCurrentState(boolean ui) {
        int error = mViewModel.saveCurrentSate();
        if (ui) {
            if (error == NO_ERR) {
                showSnackbar(getString(R.string.fmt_state_saved, mViewModel.getSnapshotsCount()), Snackbar.LENGTH_SHORT);
            } else if (error == ERR_NOT_SUPPORTED) {
                showSnackbar(R.string.feat_not_supported);
            }
        }
    }
}