package ink.snowland.wkuwku.common;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

    public int getDeviceId() {
        return 0;
    }

    public abstract String getName();

    public boolean onKeyEvent(KeyEvent event) {
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
