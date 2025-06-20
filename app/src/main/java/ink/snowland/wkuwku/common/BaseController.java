package ink.snowland.wkuwku.common;

import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_A;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_B;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_DOWN;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_L;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_L2;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_LEFT;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_R;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_R2;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_RIGHT;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_SELECT;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_START;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_UP;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_X;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ID_JOYPAD_Y;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collection;

import ink.snowland.wkuwku.bean.MacroEvent;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.util.SettingsManager;

public abstract class BaseController extends EmInputDevice {
    private static final String VIBRATION_FEEDBACK = "app_input_vibration_feedback";
    protected Vibrator vibrator;
    public static final int KEY_DOWN    = 1;
    public static final int KEY_UP      = 0;
    protected final Handler handler = new Handler(Looper.getMainLooper());

    public BaseController(@NonNull Context context, int port, int device) {
        super(port, device);
        if (SettingsManager.getBoolean(VIBRATION_FEEDBACK, true)) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!vibrator.hasVibrator()) {
                vibrator = null;
            }
        }
    }

    public final void vibrator() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        } else {
            vibrator.vibrate(20);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return onKeyEvent(event);
    }

    protected boolean onKeyEvent(KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (event.getSource() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
                || (event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            int[] keys = null;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_UP};
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_LEFT};
                    break;
                case KeyEvent.KEYCODE_DPAD_UP_LEFT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_LEFT, RETRO_DEVICE_ID_JOYPAD_UP};
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_RIGHT};
                    break;
                case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_RIGHT, RETRO_DEVICE_ID_JOYPAD_UP};
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_DOWN};
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_LEFT, RETRO_DEVICE_ID_JOYPAD_DOWN};
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_RIGHT, RETRO_DEVICE_ID_JOYPAD_DOWN};
                    break;
                case KeyEvent.KEYCODE_BUTTON_A:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_A};
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_B};
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_X};
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_Y};
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_L};
                    break;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_L2};
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_R};
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_R2};
                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_SELECT};
                    break;
                case KeyEvent.KEYCODE_BUTTON_START:
                    keys = new int[]{RETRO_DEVICE_ID_JOYPAD_START};
                    break;
                default:
            }
            if (keys != null) {
                for (int key : keys) {
                    setState(key, event.getAction() == MotionEvent.ACTION_DOWN ? KEY_DOWN : KEY_UP);
                }
                return true;
            }
        }
        return false;
    }

    public abstract View getView();

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
}
