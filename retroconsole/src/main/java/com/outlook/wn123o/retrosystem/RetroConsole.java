package com.outlook.wn123o.retrosystem;

import static com.outlook.wn123o.retrosystem.common.RetroDefine.*;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.SparseArray;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.outlook.wn123o.retrosystem.common.MediaInfo;
import com.outlook.wn123o.retrosystem.common.Option;
import com.outlook.wn123o.retrosystem.common.RetroConfig;
import com.outlook.wn123o.retrosystem.common.RetroOption;
import com.outlook.wn123o.retrosystem.common.SystemInfo;
import com.outlook.wn123o.retrosystem.common.Value;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class RetroConsole {

    public static final int SET_SYSTEM_DIRECTORY        = 1;
    public static final int SET_SAVE_DIRECTORY          = 2;
    public static final int SET_CORE_ASSETS_DIRECTORY   = 3;
    public static final int SET_AUDIO_VOLUME            = 4;

    private static final SparseArray<String> sDirectories = new SparseArray<>();
    private static final Map<String, RetroConfig> sRetroConfigs = new HashMap<>();
    private static final Map<String, String> sConfigs = new HashMap<>();

    private static String sCurrentAlias;
    private static float sAudioVolume = 1.f;
    private static AudioTrack sAudioTrack = null;
    private static WeakReference<OnEventListener> sListener = new WeakReference<>(null);

    static {
        System.loadLibrary("retrosystem");
    }

    /**
     * add a retro core lib from the path, and set an alias.
     * @param alias   unique alias
     * @param path  core path
     */
    public static void add(@NonNull String alias, @NonNull String path) {
        nativeAdd(alias, path);
        int index = path.lastIndexOf(".");
        RetroConfig config = RetroConfig.fromXml(new File(path.substring(0, index) + ".xml"));
        if (config != null) {
            sRetroConfigs.put(alias, config);
            for (RetroOption option : config.options) {
                sConfigs.put(option.key, option.val);
            }
        }
    }

    /**
     * Switch current core (from core list of loaded).
     * @param alias   unique alias
     * @return true if switch success
     */
    public static boolean use(@NonNull String alias) {
        boolean noError = nativeUse(alias);
        if (noError) {
            sCurrentAlias = alias;
        }
        return noError;
    }

    /**
     * Attach a valid surface for render to display video content.
     * @param surface valid surface
     */
    public static boolean attachSurface(@NonNull Surface surface) {
        return nativeAttachSurface(surface);
    }

    /**
     * Tell render surface size changed.
     * @param vw width
     * @param vh height
     */
    public static void adjustSurface(int vw, int vh) {
        nativeAdjustSurface(vw, vh);
    }

    /**
     * When surface destroyed, it should been detach from render.
     */
    public static void detachSurface() {
        nativeDetachSurface();
    }

    /**
     * Use current core launch the game.
     * @param path  The game's path
     * @return true if no error happen
     */
    public static boolean start(@NonNull String path) {
        return nativeStart(path);
    }

    /**
     * If current have running game, so it will been paused.
     */
    public static void pause() {
       nativePause();
    }

    /**
     * If current have paused game, so it will been resumed.
     */
    public static void resume() {
       nativeResume();
    }

    /**
     * Reset current game.
     */
    public static void reset() {
        nativeReset();
    }

    /**
     * Unload current game (whether it is running or paused).
     */
    public static void stop() {
        nativeStop();
        sAudioTrack.stop();
        sAudioTrack.release();
        sAudioTrack = null;
    }

    public static byte[] getSerializeData() {
        return nativeGetSerializeData();
    }

    public static boolean setSerializeData(final byte[] data) {
        return nativeSetSerializeData(data);
    }

    public static void setOnEventListener(OnEventListener listener) {
        sListener = new WeakReference<>(listener);
    }

    public static void set(int what, Object arg) {
        switch (what) {
            case SET_SYSTEM_DIRECTORY:
                sDirectories.put(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, (String) arg);
                break;
            case SET_SAVE_DIRECTORY:
                sDirectories.put(RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY, (String) arg);
                break;
            case SET_CORE_ASSETS_DIRECTORY:
                sDirectories.put(RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY, (String) arg);
                break;
            case SET_AUDIO_VOLUME:
                sAudioVolume = (float) arg;
                if (sAudioTrack != null) {
                    sAudioTrack.setVolume(sAudioVolume);
                }
                break;
            default:
        }
    }

    private static void createAudioTrack() {
        MediaInfo mediaInfo = nativeGetMediaInfo();
        int sampleRate = (int) mediaInfo.timing.sampleRate;
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
        sAudioTrack = builder.build();
        sAudioTrack.setVolume(sAudioVolume);
    }

    private static native void nativeAdd(@NonNull String alias, @NonNull String path);
    private static native boolean nativeUse(@NonNull String alias);
    private static native boolean nativeAttachSurface(@NonNull Surface surface);
    private static native void nativeAdjustSurface(int vw, int vh);
    private static native void nativeDetachSurface();
    private static native boolean nativeStart(@NonNull String path);
    private static native void nativePause();
    private static native void nativeResume();
    private static native void nativeStop();
    private static native void nativeReset();
    private static native SystemInfo nativeGetSystemInfo();
    private static native MediaInfo nativeGetMediaInfo();
    private static native byte[] nativeGetSerializeData();
    private static native boolean nativeSetSerializeData(final byte[] data);

    private static boolean onNativeEnvironmentCallback(int cmd, Object data) {
        boolean supported = false;
        Option option;
        Value value;
        switch (cmd) {
            case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
                String path = sDirectories.get(cmd);
                if (path != null) {
                    path = path.replace("${alias}", sCurrentAlias);
                    value = (Value) data;
                    value.v = path;
                    supported = true;
                }
                break;
            case RETRO_ENVIRONMENT_GET_VARIABLE:
                option = (Option) data;
                option.value = sConfigs.get(option.key);
                supported = option.value != null;
                break;
            default:
        }
        return supported;
    }

    private static int onNativeAudioCallback(final short[] data, int frames) {
        if (sAudioTrack == null) {
            createAudioTrack();
            sAudioTrack.play();
        }
        sAudioTrack.write(data, 0, frames * 2);
        return frames;
    }

    private static int onNativeInputCallback(int port, int device, int index, int id) {
        OnEventListener listener = sListener.get();
        if (listener != null){
            return listener.onInputCallback(port, device, index, id);
        }
        return 0;
    }

    private static void onNativeInputPollCallback() {

    }

    private static boolean onNativeRumbleCallback(int port, int effect, int strength) {
        return true;
    }

    private static void onNativeVideoSizeChanged(int width, int height) {
        OnEventListener listener = sListener.get();
        if (listener != null){
            listener.onVideoSizeChanged(width, height);
        }
    }

    public interface OnEventListener {
        void onVideoSizeChanged(int width, int height);
        int onInputCallback(int port, int device, int index, int id);
    }
}