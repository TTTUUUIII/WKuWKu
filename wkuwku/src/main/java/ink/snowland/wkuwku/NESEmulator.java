package ink.snowland.wkuwku;

import android.util.Log;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.interfaces.Emulator;

public class NESEmulator implements Emulator {

    private boolean mPowerOn = false;

    private static boolean onEnvironment(int cmd, Object data) {
//        if (!strcmp(var->key, "fceumm_ramstate"))
//            var->value = "random";
//        else if (!strcmp(var->key, "fceumm_ntsc_filter"))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_palette"))
//            var->value = "default";
//        else if(!strcmp(var->key, "fceumm_up_down_allowed"))
//            var->value = "enabled";
//        else if(!strcmp(var->key, "fceumm_nospritelimit"))
//            var->value = "enable";
//        else if(!strcmp(var->key, "fceumm_overclocking"))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_zapper_mode"))
//            var->value = "default";
//        else if(!strcmp(var->key, "fceumm_arkanoid_mode"))
//            var->value = "default";
//        else if(!strcmp(var->key, "fceumm_zapper_tolerance"))
//            var->value = "4";
//        else if(!strcmp(var->key, "fceumm_mouse_sensitivity"))
//            var->value = "100";
//        else if(!strcmp(var->key, "fceumm_show_crosshair"))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_zapper_trigger"))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_zapper_sensor"))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_overscan"))
//            var->value = "disabled";
//        else if(!strncmp(var->key, "fceumm_overscan_", 16))
//            var->value = "8";
//        else if(!strcmp(var->key, "fceumm_aspect"))
//            var->value = "8:7 PAR";
//        else if(!strcmp(var->key , "fceumm_turbo_enable"))
//            var->value = "Both";
//        else if(!strcmp(var->key, "fceumm_turbo_delay"))
//            var->value = "300";
//        else if(!strcmp(var->key, "fceumm_region"))
//            var->value = "Auto";
//        else if(!strcmp(var->key, "fceumm_sndquality"))
//            var->value = "Hign";
//        else if(!strcmp(var->key, "fceumm_sndlowpass"))
//            var->value = "enabled";
//        else if(!strcmp(var->key, "fceumm_sndstereodelay"))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_sndvolume"))
//            var->value = "0.5f";
//        else if(!strcmp(var->key, "fceumm_swapduty"))
//            var->value = "disabled";
//        else if(!strncmp(var->key, "fceumm_apu_", 11))
//            var->value = "disabled";
//        else if(!strcmp(var->key, "fceumm_show_adv_system_options"))
//            var->value = "disabled";
        return true;
    }
    private static void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        Log.d(TAG, "onVideoRefresh: " + width + ", " + height);
    }

    private static void onAudioSampleBatch(final short[] data, int frames) {
        Log.d(TAG, "onAudioSampleBatch: " + frames);
    }

    private static int onInputState(int port, int device, int index, int id) {
        return 0;
    }

    private static void onInputPoll() {

    }

    private static native void nativePowerOn();
    private static native void nativePowerOff();
    private static native void nativeReset();
    private static native boolean nativeLoad(@NonNull String path);
    private static native void nativeRun();

    @Override
    public boolean load(String path) {
        if (!mPowerOn) {
            nativePowerOn();
            mPowerOn = true;
        }
        return nativeLoad(path);
    }

    @Override
    public void run() {
        checkPowerOn();
        nativeRun();
    }

    @Override
    public void reset() {
        checkPowerOn();
        nativeReset();
    }

    @Override
    public void suspend() {
        if (mPowerOn) {
            nativePowerOff();
            mPowerOn = false;
        }
    }

    private void checkPowerOn() {
        if (!mPowerOn) {
            throw new UnsupportedOperationException("ERROR: emulator is currently powered off!");
        }
    }

    private static final String TAG = NESEmulator.class.getSimpleName();
    static {
        System.loadLibrary("nes");
    }
}
