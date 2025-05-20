package ink.snowland.wkuwku.emulator;

import android.content.Context;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

import ink.snowland.libwkuwku.R;
import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.interfaces.Emulator;

public class GenesisPlusGX extends Emulator {
    private GenesisPlusGX(@NonNull Context context) throws XmlPullParserException, IOException {
        super(context.getResources(), R.xml.genesis_plus_gx_config);
    }

    public static void registerAsEmulator(@NonNull Context context) {
        if (SHARED_INSTANCE == null) {
            try {
                SHARED_INSTANCE = new GenesisPlusGX(context);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (SHARED_INSTANCE != null) {
            EmulatorManager.registerEmulator(SHARED_INSTANCE);
        }
    }

    private static GenesisPlusGX SHARED_INSTANCE;

    static {
        System.loadLibrary("genesis-plus-gx-bridge");
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

    @Override
    public String getTag() {
        return "genesis-plus-gx";
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

//    <item>m3u</item>
//        <item>mdx</item>
//        <item>md</item>
//        <item>smd</item>
//        <item>gen</item>
//        <item>bin</item>
//        <item>cue</item>
//        <item>iso</item>
//        <item>chd</item>
//        <item>bms</item>
//        <item>gg</item>
//        <item>sg</item>
//        <item>68k</item>
//        <item>sgd</item>
    @Override
    public String findRom(@NonNull File dir) {
        assert dir.exists() && dir.isDirectory();
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith("mdx")
                    || name.endsWith("md")
                    || name.endsWith("smd")
                    || name.endsWith("gen")
                    || name.endsWith("bin")
                    || name.endsWith("cue")
                    || name.endsWith("iso")
                    || name.endsWith("chd")
                    || name.endsWith("bms")
                    || name.endsWith("gg")
                    || name.endsWith("sg")
                    || name.endsWith("68k")
                    || name.endsWith("sgd"))
                return file.getAbsolutePath();
        }
        return null;
    }
}
