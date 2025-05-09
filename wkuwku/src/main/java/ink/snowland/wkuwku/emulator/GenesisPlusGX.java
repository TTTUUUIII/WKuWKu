package ink.snowland.wkuwku.emulator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.interfaces.EmulatorDevice;

public class GenesisPlusGX extends Emulator {

    @Override
    public boolean run(@NonNull File rom) {
        return false;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void suspend() {

    }

    @Override
    public void setSystemDirectory(@NonNull File systemDirectory) {

    }

    @Override
    public void attachDevice(int target, @Nullable EmulatorDevice device) {

    }

    @Override
    public void setOption(@NonNull EmOption option) {

    }

    @Override
    public Collection<EmOption> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getTag() {
        return "genesis-plus-gx";
    }

    @Override
    public boolean save(int type, @NonNull File file) {
        if (type == SAVE_STATE) {
            return nativeSaveState(file.getAbsolutePath());
        }
        return false;
    }

    @Override
    public boolean load(int type, @Nullable File file) {
        if (file == null) return true;
        if (type == LOAD_STATE) {
            nativeLoadState(file.getAbsolutePath());
        }
        return false;
    }

    @Override
    public EmSystemInfo getSystemInfo() {
        return nativeGetSystemInfo();
    }

    @Override
    protected boolean onEnvironment(int cmd, Object data) {
        return false;
    }

    @Override
    protected void onVideoRefresh(byte[] data, int width, int height, int pitch) {

    }

    @Override
    protected void onAudioSampleBatch(short[] data, int frames) {

    }

    @Override
    protected int onInputState(int port, int device, int index, int id) {
        return 0;
    }

    @Override
    protected void onInputPoll() {

    }

    public static void registerAsEmulator() {
        EmulatorManager.registerEmulator(SHARED_INSTANCE);
    }

    private final static GenesisPlusGX SHARED_INSTANCE;

    static {
        System.loadLibrary("genesis-plus-gx-bridge");
        SHARED_INSTANCE = new GenesisPlusGX();
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
