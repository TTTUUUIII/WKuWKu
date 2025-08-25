package ink.snowland.wkuwku.device;

import static ink.snowland.wkuwku.interfaces.RetroDefine.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.MacroEvent;
import ink.snowland.wkuwku.common.Controller;
import ink.snowland.wkuwku.databinding.LayoutVirtualControllerBinding;
import ink.snowland.wkuwku.util.SettingsManager;

public class VirtualController implements Controller, View.OnTouchListener {
    private static final String VIBRATION_FEEDBACK = "app_input_vibration_feedback";
    private static final int JOYSTICK_TRIGGER_THRESHOLD = 50;
    public static final String NAME = "Virtual Controller";
    private short mButtonStates = 0;
    private short mAxisX = 0;
    private short mAxisY = 0;
    private short mAxisZ = 0;
    private short mAxisRZ = 0;
    private LayoutVirtualControllerBinding binding;
    private final View mView;
    private Vibrator mVibrator;
    private final Handler mHandler;

    public VirtualController(@NonNull Context context) {
        mView = onCreateView(LayoutInflater.from(context));
        if (SettingsManager.getBoolean(VIBRATION_FEEDBACK, true)) {
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!mVibrator.hasVibrator()) {
                mVibrator = null;
            }
        }
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    protected View onCreateView(LayoutInflater inflater) {
        binding = LayoutVirtualControllerBinding.inflate(inflater);
        bindEvents();
        return binding.getRoot();
    }

    @Override
    public boolean isTypes(int device) {
        return device == RETRO_DEVICE_JOYPAD
                || device == RETRO_DEVICE_ANALOG;
    }

    public int getDeviceId() {
        return VIRTUAL_CONTROLLER_DEVICE_ID;
    }

    public View getView() {
        return mView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        int viewId = v.getId();
        int id;
        if (viewId == R.id.button_a_b) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A, KEY_DOWN);
                setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_B, KEY_DOWN);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A, KEY_UP);
                setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_B, KEY_UP);
            }
        } else {
            if (viewId == R.id.button_select) {
                id = RETRO_DEVICE_ID_JOYPAD_SELECT;
            } else if (viewId == R.id.button_start) {
                id = RETRO_DEVICE_ID_JOYPAD_START;
            } else if (viewId == R.id.button_a) {
                id = RETRO_DEVICE_ID_JOYPAD_A;
            } else if (viewId == R.id.button_b) {
                id = RETRO_DEVICE_ID_JOYPAD_B;
            } else if (viewId == R.id.button_x) {
                id = RETRO_DEVICE_ID_JOYPAD_X;
            } else if (viewId == R.id.button_y) {
                id = RETRO_DEVICE_ID_JOYPAD_Y;
            } else if (viewId == R.id.button_l) {
                id = RETRO_DEVICE_ID_JOYPAD_L;
            } else if (viewId == R.id.button_l2) {
                id = RETRO_DEVICE_ID_JOYPAD_L2;
            } else if (viewId == R.id.button_l3) {
                id = RETRO_DEVICE_ID_JOYPAD_L3;
            } else if (viewId == R.id.button_r) {
                id = RETRO_DEVICE_ID_JOYPAD_R;
            } else if (viewId == R.id.button_r2) {
                id = RETRO_DEVICE_ID_JOYPAD_R2;
            } else if (viewId == R.id.button_r3) {
                id = RETRO_DEVICE_ID_JOYPAD_R3;
            } else {
                return false;
            }
            int state;
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                state = KEY_DOWN;
            } else if (action == MotionEvent.ACTION_UP) {
                state = KEY_UP;
            } else {
                return false;
            }
            setState(RETRO_DEVICE_JOYPAD, 0, id, state);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            vibrator();
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindEvents() {
        binding.buttonSelect.setOnTouchListener(this);
        binding.buttonStart.setOnTouchListener(this);
        binding.buttonA.setOnTouchListener(this);
        binding.buttonB.setOnTouchListener(this);
        binding.buttonX.setOnTouchListener(this);
        binding.buttonY.setOnTouchListener(this);
        binding.buttonL.setOnTouchListener(this);
        binding.buttonL2.setOnTouchListener(this);
        binding.buttonL3.setOnTouchListener(this);
        binding.buttonR.setOnTouchListener(this);
        binding.buttonR2.setOnTouchListener(this);
        binding.buttonR3.setOnTouchListener(this);
        binding.buttonAB.setOnTouchListener(this);
        binding.joystickView.setOnMoveListener((angle, strength) -> {
            double rad = Math.toRadians(angle);
            double dist = strength / 100.0;
            int xpos = (int) Math.round(dist * Math.cos(rad) * 127);
            int ypos = (int) Math.round(dist * Math.sin(rad) * 127);
            setState(RETRO_DEVICE_ANALOG,
                    RETRO_DEVICE_INDEX_ANALOG_LEFT,
                    RETRO_DEVICE_ID_ANALOG_X,
                    xpos * 258);
            setState(RETRO_DEVICE_ANALOG,
                    RETRO_DEVICE_INDEX_ANALOG_LEFT,
                    RETRO_DEVICE_ID_ANALOG_Y,
                    ypos * 258);
            int left = xpos < -JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            int right = xpos > JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            int up = ypos > JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            int down = ypos < -JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_DOWN, down);
            setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_UP, up);
            setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_LEFT, left);
            setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_RIGHT, right);
        });
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescriptor() {
        return VIRTUAL_CONTROLLER_DESCRIPTOR;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    public short getState(int device, int index, int id) {
        if (device == RETRO_DEVICE_JOYPAD) {
            if (id == RETRO_DEVICE_ID_JOYPAD_MASK) {
                return mButtonStates;
            } else {
                return (short) ((mButtonStates >> id) & 0x01);
            }
        } else if (device == RETRO_DEVICE_ANALOG) {
            if (index == RETRO_DEVICE_INDEX_ANALOG_LEFT) {
                if (id == RETRO_DEVICE_ID_ANALOG_X) {
                    return mAxisX;
                } else {
                    return mAxisY;
                }
            } else if (index == RETRO_DEVICE_INDEX_ANALOG_RIGHT) {
                if (id == RETRO_DEVICE_ID_ANALOG_X) {
                    return mAxisZ;
                } else {
                    return mAxisRZ;
                }
            }
        }
        return 0;
    }

    public void setState(int device, int index, int id, int v) {
        if (device == RETRO_DEVICE_JOYPAD) {
            if (v == KEY_DOWN) {
                mButtonStates |= (short) (0x01 << id);
            } else {
                mButtonStates &= (short) ~(0x01 << id);
            }
        } else if (device == RETRO_DEVICE_ANALOG) {
            if (index == RETRO_DEVICE_INDEX_ANALOG_LEFT) {
                if (id == RETRO_DEVICE_ID_ANALOG_X) {
                    mAxisX = (short) v;
                } else {
                    mAxisY = (short) v;
                }
            } else if (index == RETRO_DEVICE_INDEX_ANALOG_RIGHT) {
                if (id == RETRO_DEVICE_ID_ANALOG_X) {
                    mAxisZ = (short) v;
                } else {
                    mAxisRZ = (short) v;
                }
            }
        }
    }

    protected void postMacroEvent(@NonNull MacroEvent event) {
        mHandler.postDelayed(() -> {
            for (int key : event.keys) {
                setState(RETRO_DEVICE_JOYPAD, 0, key, KEY_DOWN);
            }
            mHandler.postDelayed(() -> {
                for (int key : event.keys) {
                    setState(RETRO_DEVICE_JOYPAD, 0, key, KEY_UP);
                }
            }, event.duration);
        }, event.delayed);
    }

    protected void postMacroEvents(@NonNull Collection<MacroEvent> events) {
        events.forEach(this::postMacroEvent);
    }

    public final void vibrator() {
        if (mVibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        } else {
            mVibrator.vibrate(20);
        }
    }
}
