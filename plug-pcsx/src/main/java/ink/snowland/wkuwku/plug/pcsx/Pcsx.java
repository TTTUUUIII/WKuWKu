package ink.snowland.wkuwku.plug.pcsx;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.interfaces.Emulator;

public class Pcsx extends Emulator {

    public Pcsx(@NonNull Resources res, int configResId) throws XmlPullParserException, IOException {
        super(res, configResId);
    }

    @Override
    public String getTag() {
        return "pcsx";
    }

    @Override
    public EmSystemInfo getSystemInfo() {
        return nativeGetSystemInfo();
    }

    @Override
    protected EmSystemAvInfo getSystemAvInfo() {
        return nativeGetSystemAvInfo();
    }

    @Override
    public void onPowerOn() {
        nativePowerOn();
    }

    @Override
    public boolean onLoadGame(@NonNull String fullPath) {
        return nativeLoad(fullPath);
    }

    @Override
    public void onNext() {
        nativeRun();
    }

    @Override
    public void onReset() {
        nativeReset();
    }

    @Override
    public void onLoadState(@NonNull String fullPath) {
        nativeLoadState(fullPath);
    }

    @Override
    public boolean onSaveState(@NonNull String savePath) {
        return nativeSaveState(savePath);
    }

    @Override
    public void onPowerOff() {
        nativePowerOff();
    }

    public static void registerAsEmulator(@NonNull Resources resources) {
        if (SHARED_INSTANCE == null) {
            try {
                SHARED_INSTANCE = new Pcsx(resources, R.xml.pcsx_config);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (SHARED_INSTANCE != null) {
            EmulatorManager.registerEmulator(SHARED_INSTANCE);
        }
    }

    public static void unregisterEmulator() {
        if (SHARED_INSTANCE != null)
            EmulatorManager.unregisterEmulator(SHARED_INSTANCE);
    }

    private static Pcsx SHARED_INSTANCE;

    static {
        System.loadLibrary("pcsx");
    }

    protected native void nativePowerOn();
    protected native void nativePowerOff();
    protected native void nativeReset();
    protected native boolean nativeLoad(@NonNull String path);
    protected native void nativeRun();
    protected native EmSystemAvInfo nativeGetSystemAvInfo();
    protected native EmSystemInfo nativeGetSystemInfo();
    protected native boolean nativeSaveMemoryRam(@NonNull String path);
    protected native boolean nativeLoadMemoryRam(@NonNull String path);
    protected native boolean nativeSaveState(@NonNull String path);
    protected native boolean nativeLoadState(@NonNull String path);
    protected native byte[] nativeGetState();
    protected native boolean nativeSetState(final byte[] data);
}
