package ink.snowland.wkuwku.emulator;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.util.stream.Collectors;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmScheduledThread;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;
import ink.snowland.wkuwku.interfaces.EmAudioDevice;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.annotations.CallFromJni;
import ink.snowland.wkuwku.interfaces.EmulatorDevice;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.interfaces.EmVideoDevice;

public class Fceumm implements Emulator {
    private final byte[] mLock = new byte[0];
    private static final String TAG = "Fceumm";
    private static final int STATE_INVALID = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;

    private final EmSystemAvInfo AV_IFNO;
    private String sSystemDirectory = "";
    private volatile int mState = STATE_INVALID;
    private EmVideoDevice mVideoDevice;
    private EmAudioDevice mAudioDevice;
    private EmInputDevice mInputDevice0;
    private EmInputDevice mInputDevice1;
    private EmInputDevice mInputDevice2;
    private EmInputDevice mInputDevice3;
    private EmScheduledThread mMainThread;

    private Fceumm() {
        AV_IFNO = nativeGetSystemAvInfo();
    }

    @CallFromJni
    private boolean onEnvironment(int cmd, Object data) {
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
                    entry.value = setting.val;
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
    private void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        if (mVideoDevice == null) return;
        mVideoDevice.refresh(data, width, height, pitch);
    }

    @CallFromJni
    private void onAudioSampleBatch(final short[] data, int frames) {
        if (mAudioDevice == null) return;
        if (!mAudioDevice.isOpen()) {
            mAudioDevice.open(EmAudioDevice.PCM_16BIT, 48000, 2);
        }
        if (mAudioDevice.isOpen()) {
            mAudioDevice.play(data, frames);
        }
    }

    @CallFromJni
    private int onInputState(int port, int device, int index, int id) {
        EmInputDevice it = null;
        if (port == 0) {
            it = mInputDevice0;
        } else if (port == 1) {
            it = mInputDevice1;
        } else if (port == 2) {
            it = mInputDevice2;
        } else if (port == 3) {
            it = mInputDevice3;
        }
        if (it != null && it.device == device) {
            return it.getState(id);
        }
        return 0;
    }

    @CallFromJni
    private void onInputPoll() {

    }

    private native void nativePowerOn();

    private native void nativePowerOff();

    private native void nativeReset();

    private native boolean nativeLoad(@NonNull String path);

    private native void nativeRun();

    private native EmSystemAvInfo nativeGetSystemAvInfo();
    private native EmSystemInfo nativeGetSystemInfo();
    private native boolean nativeSaveMemoryRam(@NonNull String path);
    private native boolean nativeSaveState(@NonNull String path);
    private native boolean nativeLoadMemoryRam(@NonNull String path);
    private native boolean nativeLoadState(@NonNull String path);

    @Override
    public boolean run(@NonNull File rom) {
        if (!rom.exists() || !rom.canRead()) return false;
        if (mState == STATE_INVALID) {
            nativePowerOn();
        }
        if (!nativeLoad(rom.getAbsolutePath())) {
            nativePowerOff();
            mState = STATE_INVALID;
            return false;
        }
        assert mMainThread == null;
        mMainThread = new EmScheduledThread() {
            @Override
            public void next() {
                synchronized (mLock) {
                    if (mState == STATE_RUNNING) {
                        nativeRun();
                    } else if (mState == STATE_PAUSED) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace(System.err);
                        }
                    } else {
                        interrupt();
                    }
                }
            }
        };
        mState = STATE_RUNNING;
        mMainThread.schedule(AV_IFNO.timing.fps);
        return true;
    }

    @Override
    public void pause() {
        if (mState != STATE_RUNNING) return;
        mState = STATE_PAUSED;
        if (mAudioDevice != null) {
            mAudioDevice.pause();
        }
    }

    @Override
    public void resume() {
        if (mState != STATE_PAUSED) return;
        synchronized (mLock) {
            mState = STATE_RUNNING;
            mLock.notify();
        }
    }

    @Override
    public void reset() {
        if (mState == STATE_INVALID) return;
        nativeReset();
    }

    @Override
    public void suspend() {
        if (mMainThread != null) {
            mMainThread.cancel();
            mMainThread = null;
        }
        if (mAudioDevice != null) {
            mAudioDevice.close();
            mAudioDevice = null;
        }
        if (mVideoDevice != null) {
            mVideoDevice = null;
        }
        if (mState == STATE_INVALID) return;
        nativePowerOff();
        mState = STATE_INVALID;
    }

    @Override
    public void attachDevice(int target, @Nullable EmulatorDevice device) {
        switch (target) {
            case AUDIO_DEVICE:
                mAudioDevice = (EmAudioDevice) device;
                break;
            case VIDEO_DEVICE:
                mVideoDevice = (EmVideoDevice) device;
                break;
            case INPUT_DEVICE:
                if (device instanceof EmInputDevice) {
                    EmInputDevice it = (EmInputDevice) device;
                    if (it.port == 1) {
                        mInputDevice1 = it;
                    } else if (it.port == 2) {
                        mInputDevice2 = it;
                    } else if (it.port == 3) {
                        mInputDevice3 = it;
                    } else {
                        mInputDevice0 = it;
                    }
                }
                break;
            default:
        }
    }

    @Override
    public void setOption(@NonNull EmOption option) {
        if (!option.supported) return;
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
    public boolean save(int type, @NonNull File file) {
        if (type == SAVE_MEMORY_RAM) {
            return nativeSaveMemoryRam(file.getAbsolutePath());
        } else if (type == SAVE_STATE) {
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
    public void setSystemDirectory(@NonNull File systemDirectory) {
        sSystemDirectory = systemDirectory.getAbsolutePath();
    }

    private static final Map<String, EmOption> OPTIONS = new HashMap<>();

    public static void registerAsEmulator() {
        EmulatorManager.registerEmulator(SHARED_INSTANCE);
    }

    private final static Fceumm SHARED_INSTANCE;

    static {
        System.loadLibrary("nes");
        SHARED_INSTANCE = new Fceumm();
        OPTIONS.put(
                "fceumm_game_genie",
                EmOption.builder("fceumm_game_genie", "disabled")
                        .setTitle("Genie enable")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
        OPTIONS.put(
                "fceumm_ramstate",
                EmOption.builder("fceumm_ramstate", "random")
                        .setTitle("RAM power up state")
                        .setAllowVals("random", "fill $00", "fill $FF")
                        .build()
        );
        OPTIONS.put(
                "fceumm_ntsc_filter",
                EmOption.builder("fceumm_ntsc_filter", "disabled")
                        .setTitle("NTSC filter")
                        .setAllowVals("disabled", "composite", "svideo", "rgb", "monochrome")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_palette",
                EmOption.builder("fceumm_palette", "default")
                        .setTitle("Color palette")
                        .setAllowVals("default", "asqrealc", "nintendo-vc", "rgb", "yuv-v3", "unsaturated-final", "sony-cxa2025as-us", "pal", "bmf-final2", "bmf-final3", "smooth-fbx", "composite-direct-fbx", "pvm-style-d93-fbx", "ntsc-hardware-fbx", "nes-classic-fbx-fs", "nescap", "wavebeam", "raw", "custom")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_up_down_allowed",
                EmOption.builder("fceumm_up_down_allowed", "disabled")
                        .setTitle("Allow opposing directions")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
        OPTIONS.put(
                "fceumm_nospritelimit",
                EmOption.builder("fceumm_nospritelimit", "enabled")
                        .setTitle("No sprite limit")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_overclocking",
                EmOption.builder("fceumm_overclocking", "disabled")
                        .setTitle("Overclocking")
                        .setAllowVals("disabled", "2x-Postrender", "2x-VBlank")
                        .setSupported(true)
                        .build()

        );
        OPTIONS.put(
                "fceumm_zapper_mode",
                EmOption.builder("fceumm_zapper_mode", "touchscreen")
                        .setTitle("Zapper mode")
                        .setAllowVals("lightgun", "touchscreen", "mouse")
                        .build()
        );
        OPTIONS.put(
                "fceumm_arkanoid_mode",
                EmOption.builder("fceumm_arkanoid_mode", "touchscreen")
                        .setTitle("Arkanoid mode")
                        .setAllowVals("touchscreen", "abs_mouse", "stelladaptor")
                        .build()
        );
        OPTIONS.put(
                "fceumm_zapper_tolerance",
                EmOption.builder("fceumm_zapper_tolerance", "4")
                        .setTitle("Zapper tolerance")
                        .build()
        );
        OPTIONS.put(
                "fceumm_mouse_sensitivity",
                EmOption.builder("fceumm_mouse_sensitivity", "100")
                        .setTitle("Mouse sensitivity")
                        .build()
        );
        OPTIONS.put(
                "fceumm_show_crosshair",
                EmOption.builder("fceumm_show_crosshair", "disabled")
                        .setTitle("Show crosshair")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_zapper_trigger",
                EmOption.builder("fceumm_zapper_trigger", "disabled")
                        .setTitle("Zapper trigger")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
        OPTIONS.put(
                "fceumm_zapper_sensor",
                EmOption.builder("fceumm_zapper_sensor", "disabled")
                        .setTitle("Zapper sensor")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
        OPTIONS.put(
                "fceumm_overscan",
                EmOption.builder("fceumm_overscan", "disabled")
                        .setTitle("Crop overscan")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
        OPTIONS.put(
                "fceumm_overscan_h_left",
                EmOption.builder("fceumm_overscan_h_left", "8")
                        .setTitle("Crop overscan HL")
                        .build()
        );
        OPTIONS.put(
                "fceumm_overscan_h_right",
                EmOption.builder("fceumm_overscan_h_right", "8")
                        .setTitle("Crop overscan HR")
                        .build()
        );
        OPTIONS.put(
                "fceumm_overscan_v_top",
                EmOption.builder("fceumm_overscan_v_top", "8")
                        .setTitle("Crop overscan VT")
                        .build()
        );
        OPTIONS.put(
                "fceumm_overscan_v_bottom",
                EmOption.builder("fceumm_overscan_v_bottom", "8")
                        .setTitle("Crop overscan VB")
                        .build()
        );
        OPTIONS.put(
                "fceumm_aspect",
                EmOption.builder("fceumm_aspect", "8:7 PAR")
                        .setTitle("Preferred aspect ratio")
                        .setAllowVals("8:7 PAR", "4:3")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_turbo_enable",
                EmOption.builder("fceumm_turbo_enable", "None")
                        .setTitle("Turbo enable")
                        .setAllowVals("None", "Player 1", "Player 2", "Both")
                        .build()
        );
        OPTIONS.put(
                "fceumm_turbo_delay",
                EmOption.builder("fceumm_turbo_delay", "3")
                        .setTitle("Turbo delay (in frames)")
                        .build()
        );
        OPTIONS.put(
                "fceumm_region",
                EmOption.builder("fceumm_region", "Auto")
                        .setTitle("Region")
                        .setAllowVals("Auto", "NTSC", "PAL", "Dendy")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_sndquality",
                EmOption.builder("fceumm_sndquality", "High")
                        .setTitle("Sound quality")
                        .setAllowVals("Low", "High", "Very High")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_sndlowpass",
                EmOption.builder("fceumm_sndlowpass", "enabled")
                        .setTitle("Sound low pass")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_sndstereodelay",
                EmOption.builder("fceumm_sndstereodelay", "disabled")
                        .setTitle("Sound stereo delay")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_sndvolume",
                EmOption.builder("fceumm_sndvolume", "10")
                        .setTitle("Sound volume (0 - 10)")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_swapduty",
                EmOption.builder("fceumm_swapduty", "disabled")
                        .setTitle("Swap duty cycles")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_apu_1",
                EmOption.builder("fceumm_apu_1", "enabled")
                        .setTitle("APU 1")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_apu_2",
                EmOption.builder("fceumm_apu_2", "enabled")
                        .setTitle("APU 2")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_apu_3",
                EmOption.builder("fceumm_apu_3", "enabled")
                        .setTitle("APU 3")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_apu_4",
                EmOption.builder("fceumm_apu_4", "enabled")
                        .setTitle("APU 4")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_apu_5",
                EmOption.builder("fceumm_apu_5", "enabled")
                        .setTitle("APU 5")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_apu_6",
                EmOption.builder("fceumm_apu_6", "enabled")
                        .setTitle("APU 6")
                        .setAllowVals("disabled", "enabled")
                        .setSupported(true)
                        .build()
        );
        OPTIONS.put(
                "fceumm_show_adv_system_options",
                EmOption.builder("fceumm_show_adv_system_options", "disabled")
                        .setTitle("Show advanced system options")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
        OPTIONS.put(
                "fceumm_show_adv_sound_options",
                EmOption.builder("fceumm_show_adv_sound_options", "disabled")
                        .setTitle("Show advanced sound options")
                        .setAllowVals("disabled", "enabled")
                        .build()
        );
    }
}
