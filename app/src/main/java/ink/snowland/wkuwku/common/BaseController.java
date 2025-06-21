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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ink.snowland.wkuwku.util.SettingsManager;

public abstract class BaseController {
    private static final String VIBRATION_FEEDBACK = "app_input_vibration_feedback";
    protected Vibrator vibrator;
    public static final int KEY_DOWN    = 1;
    public static final int KEY_UP      = 0;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    public final int type;
    private final LayoutInflater mLayoutInflater;

    public BaseController(@NonNull Context context, int type) {
        this.type = type;
        if (SettingsManager.getBoolean(VIBRATION_FEEDBACK, true)) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!vibrator.hasVibrator()) {
                vibrator = null;
            }
        }
        mLayoutInflater = LayoutInflater.from(context);
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
        return false;
    }

    protected LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }

    @Nullable
    public abstract View getView();


    public abstract short getState(int id);
    public abstract void setState(int id, int v);
}
