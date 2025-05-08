package ink.snowland.wkuwku.common;

import android.view.View;

import ink.snowland.wkuwku.interfaces.EmInputDevice;

public abstract class BaseController extends EmInputDevice {
    public BaseController(int port, int device) {
        super(port, device);
    }

    public abstract View getView();
}
