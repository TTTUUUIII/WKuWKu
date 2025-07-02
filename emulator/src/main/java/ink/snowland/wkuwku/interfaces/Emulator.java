package ink.snowland.wkuwku.interfaces;

import static ink.snowland.wkuwku.interfaces.RetroDefine.*;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.annotations.CallFromJni;
import ink.snowland.wkuwku.common.EmConfig;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;

public abstract class Emulator {

    private static final int FLAG_ENABLE_VIDEO = 1;
    private static final int FLAG_ENABLE_AUDIO = 1 << 1;
    private static final int FLAG_USE_FAST_SAVE_STATE = 1 << 2;
    private static final int FLAG_HARD_DISABLE_AUDIO =  1 << 3;

    public static final int VIDEO_DEVICE = 1;
    public static final int INPUT_DEVICE = 2;

    public static final int SAVE_MEMORY_RAM = 1;
    public static final int SAVE_STATE = 2;

    public static final int LOAD_STATE = 2;

    public static final int SYSTEM_DIR = 1;
    public static final int SAVE_DIR = 2;
    protected final String TAG = getClass().getSimpleName();
    private static final int STATE_INVALID = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;

    private volatile int mState = STATE_INVALID;

    protected final byte[] lock = new byte[0];
    protected final EmConfig config;
    protected final Map<String, EmOption> options = new HashMap<>();
    protected EmSystemAvInfo systemAvInfo;
    protected String systemDir;
    protected String saveDir;
    protected OnEmulatorEventListener mEventListener;
    private AudioTrack mAudioTrack;
    private float mVolume = 1.0f;
    private int mPixelFormat = RETRO_PIXEL_FORMAT_RGB565;
    private int mScreenRotation = 0;

    public int getRotation() {
        return mScreenRotation;
    }

    public int getPixelFormat() {
        return mPixelFormat;
    }

    /**
     * Start game
     * @param fullPath file path of the game
     * @return true if no error happened
     */
    public boolean start(@NonNull String fullPath) {
        if (mState != STATE_INVALID)
            return false;
        onPowerOn();
        if (!onLoadGame(fullPath)) {
            onPowerOff();
            mState = STATE_INVALID;
            return false;
        }
        mState = STATE_RUNNING;
        schedule();
        Process.setThreadPriority(sMainThread.getThreadId(), Process.THREAD_PRIORITY_AUDIO);
        return true;
    }

    /**
     * Pause running game
     */
    public void pause() {
        if (mState != STATE_RUNNING) return;
        mState = STATE_PAUSED;
        mAudioTrack.pause();
    }

    /**
     * Resume paused game
     */
    public void resume() {
        if (mState != STATE_PAUSED) return;
        mAudioTrack.play();
        mState = STATE_RUNNING;
    }

    /**
     * Reset emulator
     */
    public void reset() {
        if (mState == STATE_INVALID) return;
        onReset();
    }

    /**
     * Stop game
     */
    public void stop() {
        if (mState == STATE_INVALID) return;
        mState = STATE_INVALID;
        Process.setThreadPriority(sMainThread.getThreadId(), Process.THREAD_PRIORITY_LOWEST);
    }

    public void setDirectory(int type, @NonNull File dir) {
        if (!dir.exists() || !dir.isDirectory())
            throw new InvalidParameterException(dir + " not exists or not is a directory!");
        switch (type) {
            case SYSTEM_DIR:
                systemDir = dir.getAbsolutePath();
                break;
            case SAVE_DIR:
                saveDir = dir.getAbsolutePath();
                break;
            default:
                /*ignored*/
        }
    }

    public void setOnEmulatorEventListener(OnEmulatorEventListener listener) {
        mEventListener = listener;
        mEventListener.onRotationChanged(mScreenRotation);
        mEventListener.onPixelFormatChanged(mPixelFormat);
    }

    public Emulator(@NonNull Resources res, @XmlRes int configResId) throws XmlPullParserException, IOException {
        this(EmConfig.fromXmlConfig(res, configResId));
    }

    public Emulator(@NonNull EmConfig config) throws XmlPullParserException, IOException {
        this.config = config;
        config.options.forEach(option -> options.put(option.key, option));
    }

    public void setOption(@NonNull EmOption option) {
        if (!option.enable) return;
        EmOption opt = options.get(option.key);
        if (opt != null) {
            opt.val = option.val;
        }
    }

    public Collection<EmOption> getOptions() {
        return config.options.stream()
                .map(EmOption::clone)
                .collect(Collectors.toList());
    }

    public boolean isSupportedSystem(@NonNull EmSystem system) {
        return config.systems.contains(system);
    }


    public boolean isSupportedSystem(@NonNull String systemTag) {
        return config.systems.stream().anyMatch(it -> it.tag.equals(systemTag));
    }

    public List<EmSystem> getSupportedSystems() {
        return config.systems;
    }

    public void setAudioVolume(float volume) {
        mVolume = volume;
        if (mAudioTrack != null)
            mAudioTrack.setVolume(mVolume);
    }

    @CallFromJni
    protected boolean onEnvironment(int cmd, Object data) {
        if (cmd == RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE || cmd == RETRO_ENVIRONMENT_GET_FASTFORWARDING) {
            return false;
        }
        Variable variable;
        VariableEntry entry;
        switch (cmd) {
            case RETRO_ENVIRONMENT_GET_VARIABLE:
                entry = (VariableEntry) data;
                EmOption option = options.get(entry.key);
                if (option != null) {
                    entry.value = option.val;
                } else {
                    Log.w(TAG, "Option \"" + entry.key + "\" requested but not found in config!");
                    return false;
                }
                break;
            case RETRO_ENVIRONMENT_SET_ROTATION:
                variable = (Variable) data;
                mScreenRotation = (int) variable.value;
                if (mEventListener != null) {
                    mEventListener.onRotationChanged(mScreenRotation);
                }
                break;
            case RETRO_ENVIRONMENT_SET_VARIABLE:
            case RETRO_ENVIRONMENT_SET_VARIABLES:
                if (data != null) {
                    /*pass*/
                }
                break;
            case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
                variable = (Variable) data;
                Log.i(TAG, "Core options version: " + variable.value);
                break;
            case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
                systemAvInfo = (EmSystemAvInfo) data;
//                mMainThread.setScheduleFps((int) systemAvInfo.timing.fps);
                break;
            case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION:
                variable = (Variable) data;
                variable.value = 1;
                break;
            case RETRO_ENVIRONMENT_SET_MESSAGE:
            case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
                if (mEventListener != null)
                    mEventListener.onMessage((EmMessageExt) data);
                break;
            case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
                variable = (Variable) data;
                mPixelFormat = (int) variable.value;
                if (mEventListener != null) {
                    mEventListener.onPixelFormatChanged(mPixelFormat);
                }
                break;
            case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
                variable = (Variable) data;
                variable.value = FLAG_ENABLE_AUDIO | FLAG_ENABLE_VIDEO;
                break;
            case RETRO_ENVIRONMENT_SET_MINIMUM_AUDIO_LATENCY:
                variable = (Variable) data;
                Log.i(TAG, "Minimum audio latency: " + variable.value);
                break;
            case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
                break;
            case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
                variable = (Variable) data;
                variable.value = systemDir;
                break;
            case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
                variable = (Variable) data;
                variable.value = saveDir;
                break;
            case RETRO_ENVIRONMENT_GET_LANGUAGE:
                variable = (Variable) data;
                variable.value = RETRO_LANGUAGE_ENGLISH;
                break;
            case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
//                List<InputDescriptor> descriptors = (ArrayList<InputDescriptor>) data;
                break;
            case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
//                ArrayList<ControllerDescription> descriptors = (ArrayList<ControllerDescription>) data;
                break;
            case RETRO_ENVIRONMENT_SET_GEOMETRY:
//                EmGameGeometry geometry = (EmGameGeometry) data;
                break;
            default:
                return false;
        }
        return true;
    }
    @CallFromJni
    protected void onVideoRefresh(final byte[] data, int width, int height, int pitch) {
        if (mEventListener == null) return;
        mEventListener.onDrawFramebuffer(data, width, height, pitch);
    }

    @CallFromJni
    protected void onAudioBufferState(boolean active, int occupancy, boolean underrunLikely) {
        Log.i(TAG, String.format("onAudioBufferState: active=%b, occupancy=%d, underrunLikely=%b", active, occupancy, underrunLikely));
    }

    @CallFromJni
    protected void onAudioSampleBatch(final short[] data, int frames) {
        if (systemAvInfo == null)
            systemAvInfo = getSystemAvInfo();
        if (mAudioTrack == null) {
            createAudioTrack();
            mAudioTrack.play();
        }
        mAudioTrack.write(data, 0, frames * 2);
    }

    @CallFromJni
    protected int onInputState(int port, int device, int index, int id) {
        if (mEventListener == null) return 0;
        return mEventListener.onGetInputState(port, device, index, id);
    }

    @CallFromJni
    protected void onInputPoll() {

    }

    @CallFromJni
    protected boolean onRumbleState(int port, int effect, int strength) {
        if (mEventListener == null) return false;
        return mEventListener.onRumbleEvent(port, effect, strength);
    }

    protected void post(@NonNull Runnable r) {
        sHandler.post(r);
    }

    private void schedule() {
        if (mState == STATE_INVALID) {
            release();
        } else {
            int delayed = 0;
            if (mState == STATE_RUNNING) {
                synchronized (lock) {
                    onNext();
                }
            } else {
                delayed = 300;
            }
            sHandler.postDelayed(this::schedule, delayed);
        }
    }

    private void createAudioTrack() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        int sampleRateInHz = systemAvInfo.timing.sampleRate != 0 ? (int) systemAvInfo.timing.sampleRate : 48000;
        int minBufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setSampleRate(sampleRateInHz)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();
        AudioTrack.Builder trackBuilder = new AudioTrack.Builder()
                .setAudioFormat(audioFormat)
                .setAudioAttributes(audioAttributes)
                .setBufferSizeInBytes(minBufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STREAM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
        }
        mAudioTrack = trackBuilder.build();
        mAudioTrack.setVolume(mVolume);
    }

    private void releaseAudioTrack() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    private void release() {
        onPowerOff();
        releaseAudioTrack();
        mEventListener = null;
    }

    public @Nullable File findContent(@NonNull File src) {
        if (src.isDirectory()) {
            File[] files = src.listFiles();
            if (files == null) return null;
            for (String extension : config.contentExtensions) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(extension))
                        return file;
                }
            }
        } else {
            String name = src.getName();
            for (String extension : config.contentExtensions) {
                if (name.endsWith(extension)) {
                    return src;
                }
            }
        }
        return null;
    }

    public final byte[] getSnapshot() {
        synchronized (lock) {
            return getState();
        }
    }

    public final boolean setSnapshot(@NonNull final byte[] snapshot) {
        synchronized (lock) {
            return setState(snapshot);
        }
    }

    public boolean setControllerPortDevice(int port, int device) {
        return false;
    }

    public abstract String getTag();
    public abstract EmSystemInfo getSystemInfo();
    protected abstract EmSystemAvInfo getSystemAvInfo();
    protected abstract void onPowerOn();
    protected abstract boolean onLoadGame(@NonNull String fullPath);
    protected abstract void onNext();
    protected abstract void onReset();
    protected abstract void onLoadState(@NonNull String fullPath);
    protected abstract boolean onSaveState(@NonNull String savePath);
    protected abstract void onPowerOff();

    @Nullable
    protected byte[] getState() {
        return null;
    }

    protected boolean setState(final byte[] data) {
        return false;
    }

    private static final HandlerThread sMainThread;
    private static Handler sHandler;

    static {
        sMainThread = new HandlerThread("RetroEmulator", HandlerThread.MAX_PRIORITY) {
            @Override
            protected void onLooperPrepared() {
                sHandler = new Handler(getLooper());
                Process.setThreadPriority(getThreadId(), Process.THREAD_PRIORITY_LOWEST);
            }
        };
        sMainThread.start();
    }
}
