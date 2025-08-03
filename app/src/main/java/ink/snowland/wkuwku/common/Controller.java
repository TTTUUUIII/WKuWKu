package ink.snowland.wkuwku.common;

import android.view.KeyEvent;
import android.view.MotionEvent;

public interface Controller {

    int INVALID_CONTROLLER_DEVICE_ID = -1;
    int VIRTUAL_CONTROLLER_DEVICE_ID = 0;
    String VIRTUAL_CONTROLLER_DESCRIPTOR = "3c5feb2d2b68a3bc0e9a0d32a701f44f";
    int KEY_UP = 0;
    int KEY_DOWN = 1;

    boolean isTypes(int device);
    int getDeviceId();
    boolean isVirtual();
    String getName();
    String getDescriptor();
    short getState(int device, int index, int id);
    void setState(int device, int index, int id, int v);
    default boolean dispatchKeyEvent(KeyEvent event) {
        return false;
    }
    default boolean dispatchGenericMotionEvent(MotionEvent event) {
        return false;
    }
}
