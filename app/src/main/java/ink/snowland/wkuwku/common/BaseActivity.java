package ink.snowland.wkuwku.common;

import android.content.Intent;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.ArraySet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.activity.QRScannerActivity;
import ink.snowland.wkuwku.bean.Hotkey;
import ink.snowland.wkuwku.util.HotkeysManager;
import ink.snowland.wkuwku.util.SettingsManager;

public abstract class BaseActivity extends AppCompatActivity implements OnApplyWindowInsetsListener, SettingsManager.OnSettingChangedListener, InputManager.InputDeviceListener {
    private static final String APP_THEME = "app_theme";
    private ActivityResultLauncher<Intent> mQRCodeScannerLauncher;
    private ActivityResultLauncher<String[]> mOpenDocumentLauncher;
    private OnResultCallback<Uri> mOpenDocumentCallback;
    private OnResultCallback<String> mOnQRScanResultCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    protected NavOptions navAnimOptions = null;
    private final List<OnKeyEventListener> mKeyListeners = new ArrayList<>();
    private final List<OnTouchEventListener> mTouchEventListener = new ArrayList<>();
    private final List<InputManager.InputDeviceListener> mInputDeviceListeners = new ArrayList<>();
    private WindowInsetsCompat mWindowInsets;
    private InputManager mInputManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        updateAppTheme();
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), this);
        mOpenDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (mOpenDocumentCallback != null) {
                mOpenDocumentCallback.onResult(uri);
                mOpenDocumentCallback = null;
            }
        });
        mQRCodeScannerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (mOnQRScanResultCallback != null) {
                if (result.getResultCode() != RESULT_OK) {
                    mOnQRScanResultCallback.onResult(null);
                } else {
                    mOnQRScanResultCallback.onResult(Objects.requireNonNull(result.getData()).getStringExtra(QRScannerActivity.EXTRA_QR_RESULT));
                }
            }
        });
        navAnimOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.zoom_in_right)
                .setPopEnterAnim(R.anim.zoom_in_left)
                .build();
        mInputManager = (InputManager) getSystemService(INPUT_SERVICE);
    }

    private final Set<Integer> mPressDownKeys = new ArraySet<>();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Hotkey hotkey = null;
        if (mPressDownKeys.add(keyCode)) {
            List<Hotkey> hotkeys = HotkeysManager
                    .getHotkeys(false);
            int[] k0 = mPressDownKeys.stream()
                    .sorted()
                    .mapToInt(Integer::intValue)
                    .toArray();
            for (Hotkey it : hotkeys) {
                int[] k1 = it.getKeys();
                if (k1.length == 0 || k1.length > k0.length) continue;
                int index = Arrays.binarySearch(k0, k1[0]);
                if (index < 0 || k0.length - index < k1.length) continue;
                boolean matched = true;
                for (int i = 0; i < k1.length; ++i) {
                    if (k1[i] != k0[index + i]) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    if (hotkey == null) {
                        hotkey = it;
                    } else if (hotkey.getKeys().length < it.getKeys().length) {
                        hotkey = it;
                    }
                }
            }
        }
        boolean handled = false;
        for (OnKeyEventListener listener : mKeyListeners) {
            if (listener.onKeyEvent(event)) {
                handled = true;
            }
            if (hotkey != null && listener.onHotkeyEvent(hotkey)) {
                handled = true;
            }
            if (handled) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mPressDownKeys.clear();
        for (OnKeyEventListener listener : mKeyListeners) {
            if (listener.onKeyEvent(event)) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @NonNull
    @Override
    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
        mWindowInsets = insets;
        return insets;
    }

    @Nullable
    public WindowInsetsCompat getWindowInsets() {
        return mWindowInsets;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mInputManager.registerInputDeviceListener(this, handler);
        SettingsManager.addSettingChangedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mInputManager.unregisterInputDeviceListener(this);
        SettingsManager.removeSettingChangedListener(this);
    }

    public void onSettingChanged(@NonNull String key) {
        if (APP_THEME.equals(key)) {
            updateAppTheme();
        }
    }

    private void updateAppTheme() {
        String theme = SettingsManager.getString(APP_THEME, "system");
        if (theme.equals("dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (theme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public List<InputDevice> getInputDevices() {
        int[] deviceIds = mInputManager.getInputDeviceIds();
        ArrayList<InputDevice> inputDevices = new ArrayList<>();
        for (int deviceId : deviceIds) {
            inputDevices.add(mInputManager.getInputDevice(deviceId));
        }
        return inputDevices;
    }

    public InputDevice getInputDevice(int deviceId) {
        return mInputManager.getInputDevice(deviceId);
    }

    public void addInputDeviceListener(@NonNull InputManager.InputDeviceListener listener) {
        if (mInputDeviceListeners.contains(listener)) return;
        mInputDeviceListeners.add(listener);
    }

    public void removeInputDeviceListener(@NonNull InputManager.InputDeviceListener listener) {
        mInputDeviceListeners.remove(listener);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        for (OnTouchEventListener listener : mTouchEventListener) {
            listener.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        for (InputManager.InputDeviceListener listener : mInputDeviceListeners) {
            listener.onInputDeviceAdded(deviceId);
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        for (InputManager.InputDeviceListener listener : mInputDeviceListeners) {
            listener.onInputDeviceRemoved(deviceId);
        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        for (InputManager.InputDeviceListener listener : mInputDeviceListeners) {
            listener.onInputDeviceChanged(deviceId);
        }
    }

    public void addOnKeyEventListener(OnKeyEventListener listener) {
        if (mKeyListeners.contains(listener)) return;
        mKeyListeners.add(listener);
    }

    public void removeOnKeyEventListener(OnKeyEventListener listener) {
        mKeyListeners.remove(listener);
    }

    public void addOnTouchEventListener(OnTouchEventListener listener) {
        if (mTouchEventListener.contains(listener)) return;
        mTouchEventListener.add(listener);
    }

    public void removeOnTouchEventListener(OnTouchEventListener listener) {
        mTouchEventListener.remove(listener);
    }

    public void setStatusBarVisibility(boolean visibility) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (!visibility) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
        }
    }

    public void setActionBarVisibility(boolean visibility) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        if (visibility) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
    }

    public void setActionbarTitle(@StringRes int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(resId);
        }
    }

    public void clearActionbarSubTitle(@StringRes int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            CharSequence subtitle = actionBar.getSubtitle();
            if (subtitle == null) return;
            if (subtitle.equals(getString(resId))) {
                actionBar.setSubtitle("");
            }
        }
    }

    public void setKeepScreenOn(boolean keep) {
        if (keep) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void setPerformanceModeEnable(boolean enable) {
        if (isPerformanceModeSupported()) {
            getWindow().setSustainedPerformanceMode(enable);
        }
    }

    public boolean isPerformanceModeSupported() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm.isSustainedPerformanceModeSupported();
    }

    public void scanQrCode(OnResultCallback<String> callback) {
        mOnQRScanResultCallback = callback;
        Intent intent = new Intent(getApplicationContext(), QRScannerActivity.class);
        mQRCodeScannerLauncher.launch(intent);
    }

    public void openDocument(@NonNull String type, @NonNull OnResultCallback<Uri> callback) {
        mOpenDocumentLauncher.launch(new String[]{type});
        mOpenDocumentCallback = callback;
    }

    public void setDrawerLockedMode(int mode) {
    }

    public interface OnResultCallback<T> {
        void onResult(T t);
    }

    protected void installPackage(@NonNull Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    protected void postDelayed(Runnable r, long delayMillis) {
        handler.postDelayed(r, delayMillis);
    }

    public interface OnKeyEventListener {
        boolean onKeyEvent(@NonNull KeyEvent event);

        boolean onHotkeyEvent(@NonNull Hotkey hotkey);
    }

    public interface OnTouchEventListener {
        void onTouchEvent(MotionEvent ev);
    }
}
