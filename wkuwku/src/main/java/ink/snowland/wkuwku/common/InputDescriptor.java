package ink.snowland.wkuwku.common;

import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_ANALOG;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_JOYPAD;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_KEYBOARD;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_LIGHTGUN;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_MOUSE;
import static ink.snowland.wkuwku.interfaces.Emulator.RETRO_DEVICE_POINTER;

public class InputDescriptor {
    public final int port;
    public final int device;
    public final int index;
    public final int id;
    public final String description;

    public InputDescriptor(int port, int device, int index, int id, String description) {
        this.port = port;
        this.device = device;
        this.index = index;
        this.id = id;
        this.description = description;
    }

    @Override
    public String toString() {
        return "InputDescriptor{" +
                "port=" + port +
                ", device=" + getDeviceName(device) +
                ", index=" + index +
                ", id=" + id +
                ", description='" + description + '\'' +
                '}';
    }

    private String getDeviceName(int device) {
        switch (device) {
            case RETRO_DEVICE_JOYPAD:
                return "JPYPAD";
            case RETRO_DEVICE_ANALOG:
                return "ANALOG";
            case RETRO_DEVICE_MOUSE:
                return "MOUSE";
            case RETRO_DEVICE_LIGHTGUN:
                return "LIGHTGUN";
            case RETRO_DEVICE_KEYBOARD:
                return "KEYBOARD";
            case RETRO_DEVICE_POINTER:
                return "POINTER";
            default:
                return "NONE";
        }
    }
}
