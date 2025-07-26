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
import androidx.recyclerview.widget.ListAdapter;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.Hotkey;

public class BaseFragment extends Fragment implements InputManager.InputDeviceListener, BaseActivity.OnKeyEventListener, BaseActivity.OnTouchEventListener {
    protected BaseActivity parentActivity;
    protected Handler handler;
    protected NavOptions navAnimOptions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navAnimOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.zoom_in_right)
                .setPopEnterAnim(R.anim.zoom_in_left)
                .build();
        parentActivity = (BaseActivity) requireActivity();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onStart() {
        super.onStart();
        parentActivity.addOnKeyEventListener(this);
        parentActivity.addInputDeviceListener(this);
        parentActivity.addOnTouchEventListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        parentActivity.removeOnKeyEventListener(this);
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

    protected void showSnackbar(@NonNull String msg, int duration) {
        parentActivity.showSnackbar(msg, duration);
    }

    protected  <T> void submitDelayed(@Nullable List<T> data, ListAdapter<T, ?> adapter, int delayed) {
        runAtDelayed(() -> adapter.submitList(data), delayed);
    }

    protected List<InputDevice> getInputDevices() {
        return parentActivity.getInputDevices();
    }

    protected InputDevice getInputDevice(int deviceId) {
        return parentActivity.getInputDevice(deviceId);
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

    public @StringRes int getTitleRes() {
        return R.string.app_name;
    }

    @Override
    public boolean onKeyEvent(@NonNull KeyEvent event) {
        return false;
    }

    @Override
    public void onTouchEvent(MotionEvent ev) {

    }
}
