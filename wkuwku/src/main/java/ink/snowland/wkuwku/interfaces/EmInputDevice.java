package ink.snowland.wkuwku.interfaces;

public abstract class EmInputDevice implements EmulatorDevice {
    public static final int KEY_UP = 0;
    public static final int KEY_DOWN = 1;
    public final int port;
    public final int device;

    public EmInputDevice(int port, int device) {
        this.port = port;
        this.device = device;
    }

    public abstract void setState(int id, int state);
    public abstract short getState(int id);
}
