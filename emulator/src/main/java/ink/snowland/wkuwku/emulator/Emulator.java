package ink.snowland.wkuwku.emulator;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ink.snowland.wkuwku.common.EmConfig;
import ink.snowland.wkuwku.common.EmMessageExt;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.interfaces.OnEmulatorV2EventListener;
import ink.snowland.wkuwku.util.Logger;

public abstract class Emulator implements IEmulator {
    protected final EmConfig config;
    protected final Map<String, EmOption> mOptions = new HashMap<>();
    protected final SparseArray<Object> props = new SparseArray<>();
    protected final Logger logger = new Logger("EmulatorV2", getClass().getSimpleName());
    private AudioTrack mAudioTrack = null;
    private WeakReference<OnEmulatorV2EventListener> mListener = new WeakReference<>(null);

    public Emulator(@NonNull String alias, @NonNull EmConfig config) {
        props.put(PROP_ALIAS, alias);
        this.config = config;
        for (EmOption option : config.options) {
            mOptions.put(option.key, option);
        }
    }

    @Override
    public void setOnEventListener(@NonNull OnEmulatorV2EventListener listener) {
        mListener = new WeakReference<>(listener);
    }

    @Override
    public boolean isSupportedSystem(@NonNull EmSystem system) {
        return config.systems.contains(system);
    }

    @Override
    public boolean isSupportedSystem(@NonNull String systemTag) {
        return config.systems.stream().anyMatch(it -> it.tag.equals(systemTag));
    }

    @Override
    public List<EmSystem> getSupportedSystems() {
        return config.systems;
    }

    @Override
    public File searchSupportedContent(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return null;
            for (String extension : config.contentExtensions) {
                for (File it : files) {
                    String name = it.getName();
                    if (name.endsWith(extension))
                        return it;
                }
            }
        } else {
            String name = file.getName();
            for (String extension : config.contentExtensions) {
                if (name.endsWith(extension)) {
                    return file;
                }
            }
        }
        return null;
    }

    @Override
    public void setProp(int what, Object data) {
        props.put(what, data);
    }

    @Override
    public Object getProp(int what) {
        return props.get(what);
    }

    protected <T> T getProp(int what, T defaultValue) {
        return (T) props.get(what, defaultValue);
    }

    @Override
    public List<EmOption> getOptions() {
        return config.options.stream()
                .map(EmOption::clone)
                .collect(Collectors.toList());
    }

    @Override
    public void setOption(EmOption option) {
        if (!option.enable) return;
        EmOption opt = mOptions.get(option.key);
        if (opt != null) {
            opt.val = option.val;
        }
    }

    @Override
    public boolean start(@NonNull String path) {
        return startGame(path);
    }

    @Override
    public void stop() {
        stopGame();
        releaseAudioTrack();
    }

    @Override
    public boolean onNativeEnvironment(int cmd, Object data) {
        boolean supported = false;
        Variable variable;
        VariableEntry option;
        EmMessageExt msg;
        switch (cmd) {
            case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
                Object path = props.get(cmd);
                if (path != null) {
                    variable = (Variable) data;
                    if (path instanceof File) {
                        variable.value = ((File) path).getAbsolutePath();
                    } else {
                        variable.value = path;
                    }
                    supported = true;
                }
                break;
            case RETRO_ENVIRONMENT_GET_VARIABLE:
                option = (VariableEntry) data;
                EmOption opt = mOptions.get(option.key);
                if (opt != null) {
                    option.value = opt.val;
                    supported = true;
                }
                break;
            case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            case RETRO_ENVIRONMENT_SET_GEOMETRY:
            case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
                supported = true;
                break;
            case RETRO_ENVIRONMENT_SET_MESSAGE:
            case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
                OnEmulatorV2EventListener listener = mListener.get();
                if (listener != null) {
                    msg = (EmMessageExt) data;
                    listener.onMessage(msg);
                    supported = true;
                }
                break;
            default:
        }
        return supported;
    }

    @Override
    public void onNativeVideoSizeChanged(int vw, int vh) {
        OnEmulatorV2EventListener listener = mListener.get();
        if (listener != null) {
            listener.onVideoSizeChanged(vw, vh);
        }
    }

    @Override
    public int onNativeAudioBuffer(short[] data, int frames) {
        if (mAudioTrack == null) {
            createAudioTrack();
            mAudioTrack.play();
        }
        int written = mAudioTrack.write(data, 0, frames * 2);
        if (written > 0) {
            return written / 2;
        }
        return frames;
    }

    @Override
    public int onNativePollInput(int port, int device, int index, int id) {
        OnEmulatorV2EventListener listener = mListener.get();
        if (listener != null) {
            return listener.onPollInputState(port, device, index, id);
        }
        return 0;
    }

    private void createAudioTrack() {
        EmSystemAvInfo avInfo = getSystemAvInfo();
        int sampleRate = (int) avInfo.timing.sampleRate;
        if (sampleRate == 0) {
            sampleRate = 48000;
        }
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack.Builder builder = new AudioTrack.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                )
                .setAudioFormat(
                        new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .setSampleRate(sampleRate)
                                .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM);
        boolean lowLatencyEnable = getProp(PROP_LOW_LATENCY_AUDIO_ENABLE, true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O && lowLatencyEnable) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
        }
        mAudioTrack = builder.build();
        logger.i("AudioTrack created, SampleRate=%s, LowLatency=%b", sampleRate, lowLatencyEnable);
    }

    private void releaseAudioTrack() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
            logger.i("AudioTrack released.");
        }
    }

    protected abstract boolean startGame(@NonNull String path);
    protected abstract void stopGame();
}
