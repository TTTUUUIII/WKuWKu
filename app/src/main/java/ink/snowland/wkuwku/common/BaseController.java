package ink.snowland.wkuwku.common;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.util.SettingsManager;

public abstract class BaseController extends EmInputDevice {
    private static final String VIBRATION_FEEDBACK = "app_input_vibration_feedback";
    protected Vibrator vibrator;
    protected static final Executor executor = Executors.newSingleThreadExecutor();
    public BaseController(@NonNull Context context, int port, int device) {
        super(port, device);
        if (SettingsManager.getBoolean(VIBRATION_FEEDBACK, true)) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!vibrator.hasVibrator()) {
                vibrator = null;
            }
        }
    }

    protected void vibrator() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        } else {
            vibrator.vibrate(20);
        }
    }

    public abstract View getView();

    protected void postMacroEvent(@NonNull MacroEvent event) {
        executor.execute(() -> {
            SystemClock.sleep(event.delayed);
            for (int key : event.keys)
                setState(key, KEY_DOWN);
            SystemClock.sleep(event.duration);
            for (int key: event.keys)
                setState(key, KEY_UP);
        });
    }

    protected void postMacroEvents(@NonNull Collection<MacroEvent> events) {
        events.forEach(this::postMacroEvent);
    }
}
