package ink.snowland.wkuwku.device;

import android.content.Context;

import androidx.annotation.NonNull;

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

    @Override
    public boolean isVirtual() {
        return false;
    }
}
