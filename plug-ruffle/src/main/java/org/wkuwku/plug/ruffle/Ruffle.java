package org.wkuwku.plug.ruffle;

import android.app.Activity;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wkuwku.util.NumberUtils;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmConfig;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.emulator.Emulator;
import ink.snowland.wkuwku.interfaces.IEmulator;

public class Ruffle extends Emulator {

    static {
        System.loadLibrary("ruffle");
    }
    private static final String PROP_KEY_SCALE_FACTOR = "ruffle_scale_factor";
    private static final String PROP_KEY_SAVE_DIRECTORY = "ruffle_save_directory";
    private final Resources mResource;

    private final EmSystemInfo mSystemInfo;
    private Ruffle(@NonNull Resources resources) {
        super("ruffle", EmConfig.fromXml(resources.getXml(R.xml.ruffle_config)));
        props.put(FEAT_LOAD_STATE, false);
        props.put(FEAT_SAVE_STATE, false);
        props.put(FEAT_SCREENSHOT, false);
        mSystemInfo = new EmSystemInfo("ruffle", "1.0", "swf");
        mResource = resources;
    }

    @Override
    public void setProp(int what, Object data) {
        super.setProp(what, data);
        if (what == PROP_SAVE_DIRECTORY) {
            nativeSetProp(PROP_KEY_SAVE_DIRECTORY, data);
        }
    }

    @Override
    protected boolean startGame(@NonNull String path) {
        EmOption option = getOptionByKey(PROP_KEY_SCALE_FACTOR);
        float scaleFactor = 0f;
        if (option != null) {
            scaleFactor = NumberUtils.parseFloat(option.val, 0f);
        }
        if (scaleFactor == 0f) {
            scaleFactor = mResource.getDisplayMetrics().density;
        }
        nativeSetProp(PROP_KEY_SCALE_FACTOR, scaleFactor);
        return nativeStart(path);
    }

    @Override
    protected void stopGame() {
        nativeStop();
    }

    @Override
    public boolean captureScreen(String savePath) {
        /*Unsupported*/
        return false;
    }

    @Override
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
        /*Unsupported*/
    }

    @Override
    public void resume() {
        /*Unsupported*/
    }

    @Override
    public void reset() {
        /*Unsupported*/
    }

    @Override
    public EmSystemInfo getSystemInfo() {
        return mSystemInfo;
    }

    @Override
    public EmSystemAvInfo getSystemAvInfo() {
        /*Unsupported*/
        return null;
    }

    @Override
    public byte[] getSerializeData() {
        /*Unsupported*/
        return null;
    }

    @Override
    public void setSerializeData(byte[] data) {
        /*Unsupported*/
    }

    @Override
    public byte[] getMemoryData(int type) {
        /*Unsupported*/
        return null;
    }

    @Override
    public void setMemoryData(int type, byte[] data) {
        /*Unsupported*/
    }

    @Override
    public void setControllerPortDevice(int port, int device) {
        /*Unsupported*/
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return nativeDispatchKeyEvent(event);
    }

    private static IEmulator sInstance;
    public static void registerAsEmulator(Resources res) {
        if (sInstance == null) {
            sInstance = new Ruffle(res);
        }
        EmulatorManager.registerEmulator(sInstance);
    }

    public static void unregisterEmulator() {
        EmulatorManager.unregisterEmulator(sInstance);
    }

    private native void nativeAttachSurface(@Nullable Activity activity, @NonNull Surface surface);
    private native void nativeAdjustSurface(int vw, int vh);
    private native void nativeDetachSurface();
    private native boolean nativeStart(@NonNull String path);
    private native void nativeStop();
    private native void nativeSetProp(String key, Object prop);
    private native boolean nativeDispatchKeyEvent(@NonNull KeyEvent event);
}
