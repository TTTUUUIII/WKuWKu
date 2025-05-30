package ink.snowland.wkuwku.common;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;

import ink.snowland.wkuwku.bean.MacroEvent;
import ink.snowland.wkuwku.db.entity.MacroScript;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.core.Single;

public abstract class BaseController extends EmInputDevice {
    private static final String VIBRATION_FEEDBACK = "app_input_vibration_feedback";
    protected Vibrator vibrator;
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
        handler.postDelayed(() -> {
            for (int key : event.keys)
                setState(key, KEY_DOWN);
        }, event.delayed);
        handler.postDelayed(() -> {
            for (int key: event.keys)
                setState(key, KEY_UP);
        }, event.duration);
    }

    protected void postMacroEvents(@NonNull Collection<MacroEvent> events) {
        events.forEach(this::postMacroEvent);
    }

    public void setMacros(Single<List<MacroScript>> macros) {
    }
}
