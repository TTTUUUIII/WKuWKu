package ink.snowland.wkuwku.device;

import static ink.snowland.wkuwku.interfaces.RetroDefine.*;
import android.content.Context;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ExternalController extends VirtualController {
    private final String mName;
    private final int mDeviceId;
    public ExternalController(@NonNull Context context, String name, int deviceId) {
        super(context);
        mName = name;
        mDeviceId = deviceId;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int getDeviceId() {
        return mDeviceId;
    }

    @Nullable
    @Override
    protected View onCreateView() {
        return null;
    }

    @Override
    public boolean isVirtual() {
        return false;
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
                    setState(RETRO_DEVICE_JOYPAD, 0, key, event.getAction() == MotionEvent.ACTION_DOWN ? KEY_DOWN : KEY_UP);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        /*Joystick*/
        float lx = event.getAxisValue(MotionEvent.AXIS_X);
        float ly = event.getAxisValue(MotionEvent.AXIS_Y);
        float rx = event.getAxisValue(MotionEvent.AXIS_Z);
        float ry = event.getAxisValue(MotionEvent.AXIS_RZ);
        setState(RETRO_DEVICE_ANALOG,
                RETRO_DEVICE_INDEX_ANALOG_LEFT,
                RETRO_DEVICE_ID_ANALOG_X,
                (int) (lx * Short.MAX_VALUE));
        setState(RETRO_DEVICE_ANALOG,
                RETRO_DEVICE_INDEX_ANALOG_LEFT,
                RETRO_DEVICE_ID_ANALOG_Y,
                (int) (ly * Short.MAX_VALUE));
        setState(RETRO_DEVICE_ANALOG,
                RETRO_DEVICE_INDEX_ANALOG_RIGHT,
                RETRO_DEVICE_ID_ANALOG_X,
                (int) (rx * Short.MAX_VALUE));
        setState(RETRO_DEVICE_ANALOG,
                RETRO_DEVICE_INDEX_ANALOG_RIGHT,
                RETRO_DEVICE_ID_ANALOG_Y,
                (int) (ry * Short.MAX_VALUE));

        /*DPad*/
        float dx = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float dy = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_LEFT, dx == -1.f ? KEY_DOWN : KEY_UP);
        setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_RIGHT, dx == 1.f ? KEY_DOWN : KEY_UP);
        setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_UP, dy == -1.f ? KEY_DOWN : KEY_UP);
        setState(RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_DOWN, dy == 1.f ? KEY_DOWN : KEY_UP);
        return true;
    }
}
