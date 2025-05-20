package ink.snowland.wkuwku.emulator;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

import ink.snowland.libwkuwku.R;
import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.interfaces.Emulator;

public class Pcsx extends Emulator {
    public Pcsx(@NonNull Context context) throws XmlPullParserException, IOException {
        super(context.getResources(), R.xml.pcsx_config);
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

//<item>bin</item>
//        <item>cue</item>
//        <item>img</item>
//        <item>mdf</item>
//        <item>pbp</item>
//        <item>toc</item>
//        <item>cbn</item>
//        <item>m3u</item>
//        <item>ccd</item>
//        <item>chd</item>
//        <item>iso</item>
//        <item>exe</item>
    @Override
    public String findRom(@NonNull File dir) {
        assert dir.exists() && dir.isDirectory();
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".cue")
            || name.endsWith(".chd")
            || name.endsWith(".iso"))
                return file.getAbsolutePath();
        }
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".bin"))
                return file.getAbsolutePath();
        }
        return null;
    }

    public static void registerAsEmulator(@NonNull Context context) {
        if (SHARED_INSTANCE == null) {
            try {
                SHARED_INSTANCE = new Pcsx(context);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (SHARED_INSTANCE != null) {
            EmulatorManager.registerEmulator(SHARED_INSTANCE);
        }
    }

    private static Pcsx SHARED_INSTANCE;

    static {
        System.loadLibrary("pcsx-bridge");
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
}
