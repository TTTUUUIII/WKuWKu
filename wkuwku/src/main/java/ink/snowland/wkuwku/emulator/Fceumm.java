package ink.snowland.wkuwku.emulator;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;
import ink.snowland.wkuwku.interfaces.EmAudioDevice;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.annotations.CallFromJni;
import ink.snowland.wkuwku.interfaces.EmulatorDevice;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.interfaces.EmVideoDevice;

public class Fceumm implements Emulator {

    private static final int STATE_INVALID = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;

    private static String sSystemDirectory = "";
    private volatile int mState = STATE_INVALID;
    private static WeakReference<EmVideoDevice> mVideoDeviceRef;
    private static WeakReference<EmAudioDevice> mAudioDeviceRef;
    private static WeakReference<EmInputDevice> mInputDevice0Ref;
    private static WeakReference<EmInputDevice> mInputDevice1Ref;
    private static WeakReference<EmInputDevice> mInputDevice2Ref;
    private static WeakReference<EmInputDevice> mInputDevice3Ref;
    private MainThread mMainThread;
    @CallFromJni
    private static boolean onEnvironment(int cmd, Object data) {
        if (cmd == RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE) {
            return false;
        }
        Variable variable;
        VariableEntry entry;
        switch (cmd) {
            case RETRO_ENVIRONMENT_GET_VARIABLE:
                entry = (VariableEntry) data;
                entry.value = CONFIGURATION.get(entry.key);
                if (entry.value == null) {
                    Log.d(TAG, entry.key);
                }
                break;
            case RETRO_ENVIRONMENT_SET_VARIABLE:
            case RETRO_ENVIRONMENT_SET_VARIABLES:
                if (data != null) {
                    /*pass*/
                }
                break;
            case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
                break;
            case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
                variable = (Variable) data;
                variable.value = sSystemDirectory;
                break;
            case RETRO_ENVIRONMENT_GET_LANGUAGE:
                variable = (Variable) data;
                variable.value = RETRO_LANGUAGE_ENGLISH;
                break;
            case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
//                List<InputDescriptor> descriptors = (ArrayList<InputDescriptor>) data;
                break;
            default:
                return false;
        }
        return true;
    }

    @CallFromJni
    private static void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        if (mVideoDeviceRef == null) return;
        EmVideoDevice device = mVideoDeviceRef.get();
        if (device != null) {
            device.refresh(data, width, height, pitch);
        }
    }

    @CallFromJni
    private static void onAudioSampleBatch(final short[] data, int frames) {
        if (mAudioDeviceRef == null) return;
        EmAudioDevice device = mAudioDeviceRef.get();
        if (device != null && device.isOpen()) {
            device.play(data, frames);
        }
    }

    @CallFromJni
    private static int onInputState(int port, int device, int index, int id) {
        EmInputDevice it = null;
        if (port == 0) {
            if (mInputDevice0Ref != null) {
                it = mInputDevice0Ref.get();
            }
        } else if (port == 1) {
            if (mInputDevice1Ref != null) {
                it = mInputDevice1Ref.get();
            }
        } else if (port == 2) {
            if (mInputDevice2Ref != null) {
                it = mInputDevice2Ref.get();
            }
        } else if (port == 3) {
            if (mInputDevice3Ref != null) {
                it = mInputDevice3Ref.get();
            }
        }
        if (it != null && it.device == device) {
            return it.getState(id);
        }
        return 0;
    }

    @CallFromJni
    private static void onInputPoll() {

    }

    private static native void nativePowerOn();

    private static native void nativePowerOff();

    private static native void nativeReset();

    private static native boolean nativeLoad(@NonNull String path);

    private static native void nativeRun();

    @Override
    public boolean run(@NonNull File rom) {
        if (!rom.exists() || !rom.canRead()) return false;
        if (mState == STATE_INVALID) {
            nativePowerOn();
        }
        if (mState == STATE_RUNNING || mState == STATE_PAUSED) {
            mMainThread.interrupt();
            mMainThread = null;
        }
        if (nativeLoad(rom.getAbsolutePath())) {
            if (mAudioDeviceRef != null) {
                EmAudioDevice device = mAudioDeviceRef.get();
                if (device != null) {
                    device.open(EmAudioDevice.PCM_16BIT, 48000, 2);
                }
            }
            mMainThread = new MainThread();
            mMainThread.start();
            mState = STATE_RUNNING;
        }
        return true;
    }

    @Override
    public void pause() {
        if (mState != STATE_RUNNING) return;
        mState = STATE_PAUSED;
        if (mAudioDeviceRef != null) {
            EmAudioDevice device = mAudioDeviceRef.get();
            if (device != null) {
                device.pause();
            }
        }
    }

    @Override
    public void resume() {
        if (mState != STATE_PAUSED) return;
        mState = STATE_RUNNING;
    }

    @Override
    public void reset() {
        if (mState == STATE_INVALID) return;
        nativeReset();
    }

    @Override
    public void suspend() {
        if (mState != STATE_INVALID) {
            if (mMainThread != null) {
                mMainThread.interrupt();
                mMainThread = null;
            }
            nativePowerOff();
            if (mAudioDeviceRef != null) {
                EmAudioDevice device = mAudioDeviceRef.get();
                if (device != null) {
                    device.close();
                }
            }
        }
        mState = STATE_INVALID;
    }

    @Override
    public void attachDevice(int target, @Nullable EmulatorDevice device) {
        switch (target) {
            case AUDIO_DEVICE:
                if (device instanceof EmAudioDevice) {
                    mAudioDeviceRef = new WeakReference<>((EmAudioDevice) device);
                } else {
                    mAudioDeviceRef = null;
                }
                break;
            case VIDEO_DEVICE:
                if (device instanceof EmVideoDevice) {
                    mVideoDeviceRef = new WeakReference<>((EmVideoDevice) device);
                } else {
                    mVideoDeviceRef = null;
                }
                break;
            case INPUT_DEVICE:
                if (device instanceof EmInputDevice) {
                    EmInputDevice it = (EmInputDevice) device;
                    if (it.port == 1) {
                        mInputDevice1Ref = new WeakReference<>(it);
                    } else if (it.port == 2) {
                        mInputDevice2Ref = new WeakReference<>(it);
                    } else if (it.port == 3) {
                        mInputDevice3Ref = new WeakReference<>(it);
                    } else {
                        mInputDevice0Ref = new WeakReference<>(it);
                    }
                }
                break;
            default:
        }
    }

    @Override
    public void configure(String k, String v) {

    }

    @Override
    public void setSystemDirectory(@NonNull File systemDirectory) {
        sSystemDirectory = systemDirectory.getAbsolutePath();
    }

    private static final String TAG = Fceumm.class.getSimpleName();
    private static final Map<String, String> CONFIGURATION = new HashMap<>();

    static {
        System.loadLibrary("nes");
        initialize();
    }

    private class MainThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted() && mState != STATE_INVALID) {
                if (mState == STATE_RUNNING) {
                    nativeRun();
                }
            }
        }
    }

    private static void initialize() {
        CONFIGURATION.put("fceumm_game_genie", "disabled");
        CONFIGURATION.put("fceumm_ramstate", "random");
        CONFIGURATION.put("fceumm_ntsc_filter", "disabled");
        CONFIGURATION.put("fceumm_palette", "default");
        CONFIGURATION.put("fceumm_up_down_allowed", "enabled");
        CONFIGURATION.put("fceumm_nospritelimit", "enabled");
        CONFIGURATION.put("fceumm_overclocking", "disabled");
        CONFIGURATION.put("fceumm_zapper_mode", "default");
        CONFIGURATION.put("fceumm_arkanoid_mode", "default");
        CONFIGURATION.put("fceumm_zapper_tolerance", "4");
        CONFIGURATION.put("fceumm_mouse_sensitivity", "100");
        CONFIGURATION.put("fceumm_show_crosshair", "disabled");
        CONFIGURATION.put("fceumm_zapper_trigger", "disabled");
        CONFIGURATION.put("fceumm_zapper_sensor", "disabled");
        CONFIGURATION.put("fceumm_overscan", "disabled");
        CONFIGURATION.put("fceumm_overscan_h_left", "8");
        CONFIGURATION.put("fceumm_overscan_h_right", "8");
        CONFIGURATION.put("fceumm_overscan_v_top", "8");
        CONFIGURATION.put("fceumm_overscan_v_bottom", "8");
        CONFIGURATION.put("fceumm_aspect", "8:7 PAR");
        CONFIGURATION.put("fceumm_turbo_enable", "Both");
        CONFIGURATION.put("fceumm_turbo_delay", "300");
        CONFIGURATION.put("fceumm_region", "Auto");
        CONFIGURATION.put("fceumm_sndquality", "High");
        CONFIGURATION.put("fceumm_sndlowpass", "enabled");
        CONFIGURATION.put("fceumm_sndstereodelay", "disabled");
        CONFIGURATION.put("fceumm_sndvolume", "1.0");
        CONFIGURATION.put("fceumm_swapduty", "disabled");
        CONFIGURATION.put("fceumm_apu_1", "enabled");
        CONFIGURATION.put("fceumm_apu_2", "enabled");
        CONFIGURATION.put("fceumm_apu_3", "enabled");
        CONFIGURATION.put("fceumm_apu_4", "enabled");
        CONFIGURATION.put("fceumm_apu_5", "enabled");
        CONFIGURATION.put("fceumm_apu_6", "enabled");
        CONFIGURATION.put("fceumm_show_adv_system_options", "disabled");
        CONFIGURATION.put("fceumm_show_adv_sound_options", "disabled");
    }
}
