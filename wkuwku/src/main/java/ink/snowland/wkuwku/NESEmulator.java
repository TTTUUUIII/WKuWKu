package ink.snowland.wkuwku;

import android.util.Log;

import ink.snowland.wkuwku.interfaces.Emulator;

public class NESEmulator implements Emulator {
    private static final String TAG = NESEmulator.class.getSimpleName();
    static {
        System.loadLibrary("nes");
    }

    private static void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        Log.d(TAG, "onVideoRefresh: " + width + ", " + height);
    }

    private static void onAudioSampleBatch(final short[] data, int frames) {

    }

    private static native void nativePowerOn();
    private static native void nativePowerOff();
    private static native void nativeReset();
    private static native boolean nativeLoadGame(byte[] data);
    private static native void nativeNext();
    private static native int nativeGetVersion();

    @Override
    public void powerOn() {
        nativePowerOn();
    }

    @Override
    public void powerOff() {
        nativePowerOff();
    }

    @Override
    public void reset() {
        nativeReset();
    }

    @Override
    public boolean loadGame(byte[] data) {
         return nativeLoadGame(data);
    }

    @Override
    public void next() {
        nativeNext();
    }

    @Override
    public int getVersion() {
        return nativeGetVersion();
    }
}
