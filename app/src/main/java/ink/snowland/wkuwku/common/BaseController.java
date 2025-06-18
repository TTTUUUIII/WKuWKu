package ink.snowland.wkuwku.common;

import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_DOWN;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_LEFT;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_RIGHT;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_UP;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collection;

import ink.snowland.wkuwku.bean.MacroEvent;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.util.SettingsManager;

public abstract class BaseController extends EmInputDevice implements View.OnKeyListener {
    private static final String VIBRATION_FEEDBACK = "app_input_vibration_feedback";
    protected Vibrator vibrator;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected View view;

    public BaseController(@NonNull Context context, int port, int device) {
        super(port, device);
        if (SettingsManager.getBoolean(VIBRATION_FEEDBACK, true)) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!vibrator.hasVibrator()) {
                vibrator = null;
            }
        }
        view = onCreateView(context);
        view.setOnKeyListener(this);
    }

    public final void vibrator() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        } else {
            vibrator.vibrate(20);
        }
    }

    public abstract @NonNull View onCreateView(@NonNull Context context);

    public View getView() {
        return view;
    }

    protected void postMacroEvent(@NonNull MacroEvent event) {
        handler.postDelayed(() -> {
            for (int key : event.keys) {
                setState(key, KEY_DOWN);
            }
            handler.postDelayed(() -> {
                for (int key : event.keys) {
                    setState(key, KEY_UP);
                }
            }, event.duration);
        }, event.delayed);
    }

    protected void postMacroEvents(@NonNull Collection<MacroEvent> events) {
        events.forEach(this::postMacroEvent);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD && (source & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD) {
            return false;
        }
        int[] keys;
        int action = event.getAction() == KeyEvent.ACTION_DOWN ? KEY_DOWN : KEY_UP;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_DOWN};
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_DOWN, RETRO_DEVICE_ID_JOYPAD_LEFT};
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_DOWN, RETRO_DEVICE_ID_JOYPAD_RIGHT};
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_UP};
                break;
            case KeyEvent.KEYCODE_DPAD_UP_LEFT:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_UP, RETRO_DEVICE_ID_JOYPAD_LEFT};
                break;
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_UP, RETRO_DEVICE_ID_JOYPAD_RIGHT};
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_LEFT};
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                keys = new int[]{RETRO_DEVICE_ID_JOYPAD_RIGHT};
                break;
            default:
                return false;
        }
        for (int key : keys)
            setState(key, action);
        return true;
    }
}
