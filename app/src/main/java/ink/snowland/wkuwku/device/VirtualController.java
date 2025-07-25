package ink.snowland.wkuwku.device;

import static ink.snowland.wkuwku.interfaces.RetroDefine.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseController;
import ink.snowland.wkuwku.bean.MacroEvent;
import ink.snowland.wkuwku.databinding.LayoutVirtualControllerBinding;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.MacroScript;
import ink.snowland.wkuwku.util.MacroCompiler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VirtualController extends BaseController implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {
    private static final int JOYSTICK_TRIGGER_THRESHOLD = 50;
    public static final String NAME = "Virtual Controller";
    private short mState = 0;
    private LayoutVirtualControllerBinding binding;
    private final View mView;

    public VirtualController(@NonNull Context context) {
        super(context, RETRO_DEVICE_JOYPAD);
        mView = onCreateView();
    }

    @Nullable
    protected View onCreateView() {
        binding = LayoutVirtualControllerBinding.inflate(getLayoutInflater());
        bindMacros();
        bindEvents();
        return binding.getRoot();
    }

    @Override
    public int getDeviceId() {
        return 0;
    }

    @Override
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
                setState(RETRO_DEVICE_ID_JOYPAD_A, KEY_DOWN);
                setState(RETRO_DEVICE_ID_JOYPAD_B, KEY_DOWN);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                setState(RETRO_DEVICE_ID_JOYPAD_A, KEY_UP);
                setState(RETRO_DEVICE_ID_JOYPAD_B, KEY_UP);
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
            setState(id, state);
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
        binding.buttonM1.setOnClickListener(this);
        binding.buttonM2.setOnClickListener(this);
        binding.buttonM1.setOnLongClickListener(this);
        binding.buttonM2.setOnLongClickListener(this);
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

    private final Map<String, MacroScript> mAllValidMacros = new HashMap<>();
    private void bindMacros() {
        ArrayList<String> macroTitles = new ArrayList<>();
        Disposable disposable = AppDatabase.db.macroScriptDao()
                .getList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((scripts, throwable) -> {
                    if (throwable == null) {
                        scripts.forEach(script -> {
                            macroTitles.add(script.title);
                            mAllValidMacros.put(script.title, script);
                        });
                        binding.m1Marco.setAdapter(new ArrayAdapter<String>(binding.getRoot().getContext(), R.layout.layout_simple_text, macroTitles.toArray(new String[0])));
                        binding.m2Marco.setAdapter(new ArrayAdapter<String>(binding.getRoot().getContext(), R.layout.layout_simple_text, macroTitles.toArray(new String[0])));
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        String macroTitle = "";
        if (viewId == R.id.button_m2) {
            Object item = binding.m2Marco.getSelectedItem();
            if (item != null)
                macroTitle = item.toString();
        } else if (viewId == R.id.button_m1) {
            Object item = binding.m1Marco.getSelectedItem();
            if (item != null)
                macroTitle = item.toString();
        }
        if (!macroTitle.isEmpty()) {
            MacroScript script = mAllValidMacros.get(macroTitle);
            if (script != null) {
                List<MacroEvent> events = MacroCompiler.compile(script);
                postMacroEvents(events);
            }
        }
        vibrator();
    }

    @Override
    public boolean onLongClick(View v) {
        int viewId = v.getId();
        if (mAllValidMacros == null || mAllValidMacros.isEmpty()) return true;
        if (binding.layoutMacrosControl.getVisibility() == View.VISIBLE) {
            binding.layoutMacrosControl.setVisibility(View.GONE);
        } else {
            binding.layoutMacrosControl.setVisibility(View.VISIBLE);
        }
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
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

    public short getState(int id) {
        if (id == RETRO_DEVICE_ID_JOYPAD_MASK) {
            return mState;
        }
        if (id < 0 || id > RETRO_DEVICE_ID_JOYPAD_R3) return 0;
        return (short) ((mState >> id) & 0x01);
    }

    public void setState(int id, int v) {
        if (id < 0 || (id > RETRO_DEVICE_ID_JOYPAD_R3)) return;
        if (v == KEY_DOWN) {
            mState |= (short) (0x01 << id);
        } else {
            mState &= (short) ~(0x01 << id);
        }
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
}
