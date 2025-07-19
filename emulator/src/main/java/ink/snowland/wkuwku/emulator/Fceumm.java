package ink.snowland.wkuwku.emulator;


import android.app.Activity;
import android.content.Context;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ink.snowland.wkuwku.common.EmConfig;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;

public class Fceumm extends Emulator {

    static {
        System.loadLibrary("fceumm");
    }

    public Fceumm(@NonNull Context context) {
        super("fceumm", EmConfig.fromXml(context.getResources().getXml(R.xml.fceumm_config)));
    }

    @Override
    public boolean captureScreen(String savePath) {
        return nativeCaptureScreen(savePath);
    }

    public void attachSurface(@Nullable Activity activity, @NonNull Surface surface) {
        nativeAttachSurface(activity, surface);
    }
    @Override
    public void attachSurface(@NonNull Surface surface) {
        nativeAttachSurface(null, surface);
    }

    @Override
    public void adjustSurface(int vw, int vh) {
        nativeAdjustSurface(vw, vh);
    }

    @Override
    public void detachSurface() {
        nativeDetachSurface();
    }

    @Override
    public void pause() {
        nativePause();
    }

    @Override
    public void resume() {
        nativeResume();
    }

    @Override
    public void reset() {
        nativeReset();
    }

    @Override
    public EmSystemInfo getSystemInfo() {
        return nativeGetSystemInfo();
    }

    @Override
    public EmSystemAvInfo getSystemAvInfo() {
        return nativeGetSystemAvInfo();
    }

    @Override
    public byte[] getSerializeData() {
        return nativeGetSerializeData();
    }

    @Override
    public void setSerializeData(byte[] data) {
        nativeSetSerializeData(data);
    }

    @Override
    public byte[] getMemoryData(int type) {
        return nativeGetMemoryData(type);
    }

    @Override
    public void setMemoryData(int type, byte[] data) {
        nativeSetMemoryData(type, data);
    }

    @Override
    public void setControllerPortDevice(int port, int device) {
        nativeSetControllerPortDevice(port, device);
    }

    @Override
    public void setProp(int what, Object data) {
        super.setProp(what, data);
        nativeSetProp(what, data);
    }

    @Override
    protected boolean startGame(@NonNull String path) {
        return nativeStart(path);
    }

    @Override
    protected void stopGame() {
        nativeStop();
    }

    private native boolean nativeCaptureScreen(String savePath);
    private native void nativeAttachSurface(@Nullable Activity activity, @NonNull Surface surface);
    private native void nativeAdjustSurface(int vw, int vh);
    private native void nativeDetachSurface();
    private native boolean nativeStart(@NonNull String path);
    private native void nativePause();
    private native void nativeResume();
    private native void nativeStop();
    private native void nativeReset();
    private native EmSystemInfo nativeGetSystemInfo();
    private native EmSystemAvInfo nativeGetSystemAvInfo();
    private native byte[] nativeGetSerializeData();
    private native void nativeSetSerializeData(final byte[] data);
    private native byte[] nativeGetMemoryData(int type);
    private native void nativeSetMemoryData(int type, byte[] data);
    private native void nativeSetControllerPortDevice(int port, int device);
    private native void nativeSetProp(int prop, Object val);
}
