package ink.snowland.wkuwku.emulator;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

import ink.snowland.libwkuwku.R;
import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmScheduledThread;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;
import ink.snowland.wkuwku.interfaces.EmAudioDevice;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.interfaces.EmulatorDevice;
import ink.snowland.wkuwku.interfaces.EmInputDevice;
import ink.snowland.wkuwku.interfaces.EmVideoDevice;

public class Fceumm extends Emulator {
    private final byte[] mLock = new byte[0];
    private static final String TAG = "Fceumm";
    private static final int STATE_INVALID = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;
    private volatile int mState = STATE_INVALID;
    private EmVideoDevice mVideoDevice;
    private EmAudioDevice mAudioDevice;
    private EmInputDevice mInputDevice0;
    private EmInputDevice mInputDevice1;
    private EmInputDevice mInputDevice2;
    private EmInputDevice mInputDevice3;
    private EmScheduledThread mMainThread;

    private Fceumm(@NonNull Context context) throws XmlPullParserException, IOException {
        super("fceumm", context.getResources(), R.xml.fceumm_config);
    }

    @Override
    protected boolean onEnvironment(int cmd, Object data) {
        if (cmd == RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE) {
            return false;
        }
        Variable variable;
        VariableEntry entry;
        switch (cmd) {
            case RETRO_ENVIRONMENT_GET_VARIABLE:
                entry = (VariableEntry) data;
                EmOption option = options.get(entry.key);
                Log.d(TAG, entry.key);
                if (option != null) {
                    entry.value = option.val;
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
                variable.value = systemDir;
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


    @Override
    protected void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        if (mVideoDevice == null) return;
        mVideoDevice.refresh(data, width, height, pitch);
    }

    @Override
    protected void onAudioSampleBatch(final short[] data, int frames) {
        if (mAudioDevice == null) return;
        if (!mAudioDevice.isOpen()) {
            mAudioDevice.open(EmAudioDevice.PCM_16BIT, 48000, 2);
        }
        if (mAudioDevice.isOpen()) {
            mAudioDevice.play(data, frames);
        }
    }

    @Override
    protected int onInputState(int port, int device, int index, int id) {
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

    @Override
    protected void onInputPoll() {

    }

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
        mMainThread.schedule(systemAvInfo.timing.fps);
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
    public EmSystemInfo getSystemInfo() {
        return nativeGetSystemInfo();
    }

    @Override
    public EmSystemAvInfo getSystemAvInfo() {
        return nativeGetSystemAvInfo();
    }

    public static void registerAsEmulator(@NonNull Context context) {
        if (SHARED_INSTANCE == null) {
            try {
                SHARED_INSTANCE = new Fceumm(context);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (SHARED_INSTANCE != null) {
            EmulatorManager.registerEmulator(SHARED_INSTANCE);
        }
    }

    private static Fceumm SHARED_INSTANCE;

    static {
        System.loadLibrary("fceumm-bridge");
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
