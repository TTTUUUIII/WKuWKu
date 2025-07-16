package ink.snowland.wkuwku.plug.sameboy;

import android.content.res.Resources;
import android.view.Surface;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmConfig;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.emulator.Emulator;
import ink.snowland.wkuwku.interfaces.IEmulator;

public class SameBoy extends Emulator {

    static {
        System.loadLibrary("sameboy");
    }

    public SameBoy(@NonNull Resources resources) {
        super("sameboy", EmConfig.fromXml(resources.getXml(R.xml.sameboy_config)));
    }

    @Override
    public boolean captureScreen(String savePath) {
        return nativeCaptureScreen(savePath);
    }

    @Override
    public void attachSurface(@NonNull Surface surface) {
        nativeAttachSurface(surface);
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
    public boolean setSerializeData(byte[] data) {
        return nativeSetSerializeData(data);
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
    protected boolean startGame(@NonNull String path) {
        return nativeStart(path);
    }

    @Override
    protected void stopGame() {
        nativeStop();
    }

    public static IEmulator sInstance;

    public static void registerAsEmulator(@NonNull Resources resources) {
        if (sInstance == null) {
            sInstance = new SameBoy(resources);
        }
        EmulatorManager.registerEmulator(sInstance);
    }

    public static void unregisterEmulator() {
        EmulatorManager.unregisterEmulator(sInstance);
    }

    private native boolean nativeCaptureScreen(String savePath);
    private native void nativeAttachSurface(@NonNull Surface surface);
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
    private native boolean nativeSetSerializeData(final byte[] data);
    private native byte[] nativeGetMemoryData(int type);
    private native void nativeSetMemoryData(int type, byte[] data);
    private native void nativeSetControllerPortDevice(int port, int device);
}
