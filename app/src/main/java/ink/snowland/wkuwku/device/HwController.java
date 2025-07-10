package ink.snowland.wkuwku.device;

import android.content.Context;

import androidx.annotation.NonNull;

public class HwController extends VirtualController {
    private final String mName;
    private final int mDeviceId;
    public HwController(@NonNull Context context, String name, int deviceId) {
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
}
