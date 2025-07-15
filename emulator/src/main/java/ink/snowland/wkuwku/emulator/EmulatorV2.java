package ink.snowland.wkuwku.emulator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.SparseArray;
import android.view.Surface;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
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
import ink.snowland.wkuwku.common.EmSystemInfo;
import ink.snowland.wkuwku.common.Variable;
import ink.snowland.wkuwku.common.VariableEntry;
import ink.snowland.wkuwku.interfaces.IEmulatorV2;
import ink.snowland.wkuwku.interfaces.OnEmulatorV2EventListener;

public abstract class EmulatorV2 implements IEmulatorV2 {
    protected final EmConfig config;
    protected final Map<String, EmOption> mOptions = new HashMap<>();
    private final SparseArray<Object> mProps = new SparseArray<>();
    private AudioTrack mAudioTrack = null;
    private WeakReference<OnEmulatorV2EventListener> mListener = new WeakReference<>(null);

    public EmulatorV2(@NonNull String alias, @NonNull EmConfig config) {
        mProps.put(PROP_ALIAS, alias);
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
        mProps.put(what, data);
        if (what == PROP_AUDIO_VOLUME && mAudioTrack != null) {
            mAudioTrack.setVolume((float) data);
        }
    }

    @Override
    public Object getProp(int what) {
        return mProps.get(what);
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
        boolean noError = startGame(path);
        if (noError) {
            createAudioTrack();
            mAudioTrack.play();
        }
        return noError;
    }

    @Override
    public void stop() {
        stopGame();
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
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
                Object path = mProps.get(cmd);
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
    public int onNativePollInput(int port, int device, int index, int id) {
        OnEmulatorV2EventListener listener = mListener.get();
        if (listener != null) {
            return listener.onPollInputState(port, device, index, id);
        }
        return 0;
    }

    @Override
    public int onNativeAudioBuffer(short[] data, int frames) {
        if (mAudioTrack != null) {
            mAudioTrack.write(data, 0, frames * 2);
        }
        return frames;
    }

    protected abstract boolean startGame(@NonNull String path);
    protected abstract void stopGame();

    private void createAudioTrack() {
        EmSystemAvInfo systemAvInfo = getSystemAvInfo();
        int sampleRate = (int) systemAvInfo.timing.sampleRate;
        if (sampleRate == 0) {
            sampleRate = 48000;
        }
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack.Builder builder = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(minBufferSize);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
        }
        mAudioTrack = builder.build();
        mAudioTrack.setVolume((float) mProps.get(PROP_AUDIO_VOLUME, 1.f));
    }
}
