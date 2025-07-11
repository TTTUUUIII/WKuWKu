package ink.snowland.wkuwku.common;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.recyclerview.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ink.snowland.wkuwku.R;

public class BaseFragment extends Fragment implements InputManager.InputDeviceListener{
    protected BaseActivity parentActivity;
    protected Handler handler;
    protected NavOptions navAnimOptions;
    private InputManager mInputManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navAnimOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.zoom_in_right)
                .setPopEnterAnim(R.anim.zoom_in_left)
                .build();
        parentActivity = (BaseActivity) requireActivity();
        handler = new Handler(Looper.getMainLooper());
        mInputManager = (InputManager) requireContext().getSystemService(Context.INPUT_SERVICE);
    }

    @Override
    public void onStart() {
        super.onStart();
        mInputManager.registerInputDeviceListener(this, handler);
    }

    @Override
    public void onStop() {
        super.onStop();
        mInputManager.unregisterInputDeviceListener(this);
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

    protected  <T> void submitDelayed(@Nullable List<T> data, ListAdapter<T, ?> adapter, int delayed) {
        runAtDelayed(() -> adapter.submitList(data), delayed);
    }

    protected List<InputDevice> getInputDevices() {
        int[] deviceIds = mInputManager.getInputDeviceIds();
        ArrayList<InputDevice> inputDevices = new ArrayList<>();
        for (int deviceId : deviceIds) {
            inputDevices.add(mInputManager.getInputDevice(deviceId));
        }
        return inputDevices;
    }

    protected InputDevice getInputDevice(int deviceId) {
        return mInputManager.getInputDevice(deviceId);
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
}
