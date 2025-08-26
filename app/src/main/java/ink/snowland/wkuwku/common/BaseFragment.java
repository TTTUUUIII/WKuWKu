package ink.snowland.wkuwku.common;

import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.Hotkey;
import ink.snowland.wkuwku.util.SettingsManager;

public class BaseFragment extends Fragment implements InputManager.InputDeviceListener, BaseActivity.OnInputEventListener, BaseActivity.OnTouchEventListener {
    protected BaseActivity parentActivity;
    protected Handler handler;
    private NavOptions mNavAnimOptions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNavAnimOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build();
        parentActivity = (BaseActivity) requireActivity();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onStart() {
        super.onStart();
        parentActivity.addOnInputEventListener(this);
        parentActivity.addInputDeviceListener(this);
        parentActivity.addOnTouchEventListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        parentActivity.removeOnInputEventListener(this);
        parentActivity.removeInputDeviceListener(this);
        parentActivity.removeOnTouchEventListener(this);
    }

    @Override
    public boolean onHotkeyEvent(@NonNull Hotkey hotkey) {
        return false;
    }

    protected void runAtDelayed(@NonNull Runnable r, long delayMillis) {
        handler.postDelayed(r, delayMillis);
    }

    protected void runAtTime(@NonNull Runnable r, long updateMillis) {
        handler.postAtTime(r, updateMillis);
    }

    protected void run(@NonNull Runnable r) {
        handler.post(r);
    }

    protected void showSnackbar(@StringRes int resId, int duration) {
        parentActivity.showSnackbar(resId, duration);
    }

    protected void showSnackbar(@StringRes int resId) {
        parentActivity.showSnackbar(resId, Snackbar.LENGTH_SHORT);
    }

    protected void showSnackbar(@NonNull String msg, int duration) {
        parentActivity.showSnackbar(msg, duration);
    }

    protected List<InputDevice> getInputDevices() {
        return parentActivity.getInputDevices();
    }

    protected InputDevice getInputDevice(int deviceId) {
        return parentActivity.getInputDevice(deviceId);
    }

    protected NavOptions getNavAnimOptions() {
        if (SettingsManager.getBoolean(SettingsManager.NAVIGATION_ANIMATION, true)) {
            return mNavAnimOptions;
        } else {
            return null;
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {

    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {

    }

    @Override
    public void onInputDeviceChanged(int deviceId) {

    }

    @Override
    public boolean onKeyEvent(@NonNull KeyEvent event) {
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }
}
