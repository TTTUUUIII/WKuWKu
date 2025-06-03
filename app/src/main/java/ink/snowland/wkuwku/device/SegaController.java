package ink.snowland.wkuwku.device;
import static ink.snowland.wkuwku.interfaces.Emulator.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseController;
import ink.snowland.wkuwku.databinding.LayoutSegaControllerBinding;

public class SegaController extends BaseController implements View.OnTouchListener {
    private LayoutSegaControllerBinding binding;
    private static final int JOYSTICK_TRIGGER_THRESHOLD = 50;
    private short mState = 0;
    public SegaController(int port, @NonNull Context context) {
        super(context, port, RETRO_DEVICE_JOYPAD);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull Context context) {
        binding = LayoutSegaControllerBinding.inflate(LayoutInflater.from(context));
        bindEvents();
        return binding.getRoot();
    }

    @Override
    public View getView() {
        return binding.getRoot();
    }

    @Override
    public void setState(int id, int v) {
        if (id < 0 || (id > RETRO_DEVICE_ID_JOYPAD_R)) return;
        if (v == KEY_DOWN) {
            mState |= (short) (0x01 << id);
        } else {
            mState &= (short) ~(0x01 << id);
        }
    }

    @Override
    public short getState(int id) {
        if (id == RETRO_DEVICE_ID_JOYPAD_MASK) {
            return mState;
        }
        if (id < 0 || id > RETRO_DEVICE_ID_JOYPAD_R) return 0;
        return (short) (mState >> id & 0x01);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        int viewId = v.getId();
        int id;
        if (viewId == R.id.button_mode) {
            id = RETRO_DEVICE_ID_JOYPAD_SELECT;
        } else if (viewId == R.id.button_start) {
            id = RETRO_DEVICE_ID_JOYPAD_START;
        } else if (viewId == R.id.button_a) {
            id = RETRO_DEVICE_ID_JOYPAD_Y;
        } else if (viewId == R.id.button_b) {
            id = RETRO_DEVICE_ID_JOYPAD_B;
        } else if (viewId == R.id.button_c) {
            id = RETRO_DEVICE_ID_JOYPAD_A;
        } else if (viewId == R.id.button_x) {
            id = RETRO_DEVICE_ID_JOYPAD_L;
        } else if (viewId == R.id.button_y) {
            id = RETRO_DEVICE_ID_JOYPAD_X;
        } else if (viewId == R.id.button_z) {
            id = RETRO_DEVICE_ID_JOYPAD_R;
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
        setState(id, state);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            vibrator();
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindEvents() {
        binding.buttonMode.setOnTouchListener(this);
        binding.buttonStart.setOnTouchListener(this);
        binding.buttonA.setOnTouchListener(this);
        binding.buttonB.setOnTouchListener(this);
        binding.buttonX.setOnTouchListener(this);
        binding.buttonY.setOnTouchListener(this);
        binding.buttonZ.setOnTouchListener(this);
        binding.buttonC.setOnTouchListener(this);
        binding.joystickView.setOnMoveListener((angle, strength) -> {
            double rad = Math.toRadians(angle);
            double dist = strength / 100.0;
            int xpos = (int) (dist * Math.cos(rad) * 127);
            int ypos = (int) (dist * Math.sin(rad) * 127);
            int left = xpos < -JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            int right = xpos > JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            int up = ypos > JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            int down = ypos < -JOYSTICK_TRIGGER_THRESHOLD ? KEY_DOWN : KEY_UP;
            setState(RETRO_DEVICE_ID_JOYPAD_DOWN, down);
            setState(RETRO_DEVICE_ID_JOYPAD_UP, up);
            setState(RETRO_DEVICE_ID_JOYPAD_LEFT, left);
            setState(RETRO_DEVICE_ID_JOYPAD_RIGHT, right);
        });
    }
}
