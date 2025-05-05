package ink.snowland.wkuwku.emulator;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmThread;
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
    private EmThread mMainThread;
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
                EmOption setting = OPTIONS.get(entry.key);
                if (setting != null) {
                    entry.value =  setting.val;
                } else {
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
            mMainThread = new EmThread() {
                @Override
                protected void next() {
                    if (mState == STATE_RUNNING) {
                        nativeRun();
                    } else if (mState == STATE_INVALID) {
                        interrupt();
                    }
                }
            };
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
                mAudioDeviceRef = null;
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
    public void setOption(@NonNull EmOption option) {
        EmOption opt = OPTIONS.get(option.key);
        if (opt != null) {
            opt.val = option.val;
        }
    }

    @Override
    public Collection<EmOption> getOptions() {
        return OPTIONS.values().stream()
                .map(EmOption::clone)
                .collect(Collectors.toList());
    }

    @Override
    public String getTag() {
        return "fceumm";
    }

    @Override
    public void setSystemDirectory(@NonNull File systemDirectory) {
        sSystemDirectory = systemDirectory.getAbsolutePath();
    }

    private static final String TAG = Fceumm.class.getSimpleName();
    private static final Map<String, EmOption> OPTIONS = new HashMap<>();

    static {
        System.loadLibrary("nes");
        initialize();
    }

    private static void initialize() {
        OPTIONS.put(
                "fceumm_game_genie",
                EmOption.create("fceumm_game_genie", "disabled", "Genie enable", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_ramstate",
                EmOption.create("fceumm_ramstate", "random", "RAM power up state", "random", "fill $00"));
        OPTIONS.put(
                "fceumm_ntsc_filter",
                EmOption.create("fceumm_ntsc_filter", "disabled", "NTSC filter", "disabled", "composite", "svideo", "rgb", "monochrome"));
        OPTIONS.put(
                "fceumm_palette",
                EmOption.create("fceumm_palette", "default", "Color palette", "default", "asqrealc", "nintendo-vc", "rgb", "yuv-v3", "unsaturated-final", "sony-cxa2025as-us", "pal", "bmf-final2", "bmf-final3", "smooth-fbx", "composite-direct-fbx", "pvm-style-d93-fbx", "ntsc-hardware-fbx", "nes-classic-fbx-fs", "nescap", "wavebeam", "raw"/*, "custom"*/));
        OPTIONS.put(
                "fceumm_up_down_allowed",
                EmOption.create("fceumm_up_down_allowed", "disabled", "Allow opposing directions", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_nospritelimit",
                EmOption.create("fceumm_nospritelimit", "enabled", "No sprite limit", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_overclocking",
                EmOption.create("fceumm_overclocking", "disabled", "Overclocking", "disabled", "2x-Postrender", "2x-VBlank")
        );
        OPTIONS.put(
                "fceumm_zapper_mode",
                EmOption.create("fceumm_zapper_mode", "touchscreen", "Zapper mode", "lightgun", "touchscreen", "mouse")
        );
        OPTIONS.put(
                "fceumm_arkanoid_mode",
                EmOption.create("fceumm_arkanoid_mode", "touchscreen", "Arkanoid mode", "touchscreen", "abs_mouse", "stelladaptor")
        );
        OPTIONS.put(
                "fceumm_zapper_tolerance",
                EmOption.create("fceumm_zapper_tolerance", "4", "Zapper tolerance")
        );
        OPTIONS.put(
                "fceumm_mouse_sensitivity",
                EmOption.create("fceumm_mouse_sensitivity", "100", "Mouse sensitivity")
        );
        OPTIONS.put(
                "fceumm_show_crosshair",
                EmOption.create("fceumm_show_crosshair", "disabled", "Show crosshair", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_zapper_trigger",
                EmOption.create("fceumm_zapper_trigger", "disabled", "Zapper trigger", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_zapper_sensor",
                EmOption.create("fceumm_zapper_sensor", "disabled", "Zapper sensor", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_overscan",
                EmOption.create("fceumm_overscan", "disabled", "Crop overscan", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_overscan_h_left",
                EmOption.create("fceumm_overscan_h_left", "8", "Crop overscan HL")
        );
        OPTIONS.put(
                "fceumm_overscan_h_right",
                EmOption.create("fceumm_overscan_h_right", "8", "Crop overscan HR")
                );
        OPTIONS.put(
                "fceumm_overscan_v_top",
                EmOption.create("fceumm_overscan_v_top", "8", "Crop overscan VT")
                );
        OPTIONS.put(
                "fceumm_overscan_v_bottom",
                EmOption.create("fceumm_overscan_v_bottom", "8", "Crop overscan VB")
        );
        OPTIONS.put(
                "fceumm_aspect",
                EmOption.create("fceumm_aspect", "8:7 PAR", "Preferred aspect ratio", "8:7 PAR", "4:3")
        );
        OPTIONS.put(
                "fceumm_turbo_enable",
                EmOption.create("fceumm_turbo_enable", "None", "Turbo enable", "None", "Player 1", "Player 2", "Both")
        );
        OPTIONS.put(
                "fceumm_turbo_delay",
                EmOption.create("fceumm_turbo_delay", "3", "Turbo delay (in frames)")
        );
        OPTIONS.put(
                "fceumm_region",
                EmOption.create("fceumm_region", "Auto", "Region", "Auto", "NTSC", "PAL", "Dendy")
        );
        OPTIONS.put(
                "fceumm_sndquality",
                EmOption.create("fceumm_sndquality", "High", "Sound quality", "Low", "High", "Very High")
        );
        OPTIONS.put(
                "fceumm_sndlowpass",
                EmOption.create("fceumm_sndlowpass", "enabled", "Sound low pass", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_sndstereodelay",
                EmOption.create("fceumm_sndstereodelay", "disabled", "Sound stereo delay", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_sndvolume",
                EmOption.create("fceumm_sndvolume", "10", "Sound volume (0 - 10)")
        );
        OPTIONS.put(
                "fceumm_swapduty",
                EmOption.create("fceumm_swapduty", "disabled", "Swap duty cycles", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_apu_1",
                EmOption.create("fceumm_apu_1", "enabled", "APU 1", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_apu_2",
                EmOption.create("fceumm_apu_2", "enabled", "APU 2", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_apu_3",
                EmOption.create("fceumm_apu_3", "enabled", "APU 3", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_apu_4",
                EmOption.create("fceumm_apu_4", "enabled", "APU 4", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_apu_5",
                EmOption.create("fceumm_apu_5", "enabled", "APU 5", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_apu_6",
                EmOption.create("fceumm_apu_6", "enabled", "APU 6", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_show_adv_system_options",
                EmOption.create("fceumm_show_adv_system_options", "disabled", "Show advanced system options", "disabled", "enabled")
        );
        OPTIONS.put(
                "fceumm_show_adv_sound_options",
                EmOption.create("fceumm_show_adv_sound_options", "disabled", "Show advanced sound options", "enabled")
        );
    }

    private static final Fceumm INSTANCE = new Fceumm();
    public static void registerAsEmulator() {
        EmulatorManager.registerEmulator(INSTANCE);
    }
}
