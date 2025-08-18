package ink.snowland.wkuwku.device;

import static ink.snowland.wkuwku.interfaces.RetroDefine.*;

import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.common.Controller;

public class ExternalController implements Controller {
    private final String mName;
    private final int mDeviceId;
    private final String mDescriptor;
    private short mButtonStates = 0;
    private short mAxisX = 0;
    private short mAxisY = 0;
    private short mAxisZ = 0;
    private short mAxisRZ = 0;
    public ExternalController(int deviceId, String name, String descriptor) {
        mName = name;
        mDeviceId = deviceId;
        mDescriptor = descriptor;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isTypes(int device) {
        return device == RETRO_DEVICE_JOYPAD
                || device == RETRO_DEVICE_ANALOG;
    }

    @Override
    public int getDeviceId() {
        return mDeviceId;
    }

    @Override
    public String getDescriptor() {
        return mDescriptor;
    }

    @Override
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

    @Override
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

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
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
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
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

    public static boolean isSupportedDevice(InputDevice device) {
        if (device.isVirtual() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !device.isExternal())) {
            return false;
        }
        int sources = device.getSources();
        return (sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
                || (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    public static Controller from(@NonNull InputDevice device) {
        return new ExternalController(device.getId(), device.getName(), device.getDescriptor());
    }
}
