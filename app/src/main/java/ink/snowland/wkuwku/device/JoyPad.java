package ink.snowland.wkuwku.device;

import static ink.snowland.wkuwku.interfaces.Emulator.*;

import ink.snowland.wkuwku.interfaces.EmInputDevice;

public class JoyPad extends EmInputDevice {
    private short mState = 0;

    public JoyPad(int port) {
        super(port, RETRO_DEVICE_JOYPAD);
    }
    @Override
    public short getState(int id) {
        if (id == RETRO_DEVICE_ID_JOYPAD_MASK) {
            return mState;
        }
        if (id < 0 || id > RETRO_DEVICE_ID_JOYPAD_R3) return 0;
        return (short) (mState >> id & 0x01);
    }

    @Override
    public void setState(int id, int v) {
        if (id < 0 || (id > RETRO_DEVICE_ID_JOYPAD_R3)) return;
        if (v == KEY_DOWN) {
            mState |= (short) (0x01 << id);
        } else {
            mState &= (short) ~(0x01 << id);
        }
    }
}
