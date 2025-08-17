package ink.snowland.wkuwku.interfaces;

import android.app.Activity;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.EmSystem;
import ink.snowland.wkuwku.common.EmSystemAvInfo;
import ink.snowland.wkuwku.common.EmSystemInfo;

public interface IEmulator extends RetroDefine {

    int PROP_SYSTEM_DIRECTORY                   = RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY;
    int PROP_SAVE_DIRECTORY                     = RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY;
    int PROP_CORE_ASSETS_DIRECTORY              = RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY;
    int PROP_ALIAS                              = 101;
    int PROP_OBOE_ENABLE                        = 102;
    int PROP_LOW_LATENCY_AUDIO_ENABLE           = 103;
    int PROP_AUDIO_UNDERRUN_OPTIMIZATION        = 104;

    int FEAT_SAVE_STATE                         = 1000;
    int FEAT_LOAD_STATE                         = 1001;
    int FEAT_SCREENSHOT                         = 1002;

    boolean hasFeature(int feat);
    void setProp(int what, Object data);
    boolean captureScreen(String savePath);
    <T> T getProp(int what, Class<T> clazz);
    <T> T getProp(int what, T defaultValue);
    List<EmOption> getOptions();
    void setOption(EmOption option);
    void setOnEventListener(@NonNull OnEmulatorV2EventListener listener);
    boolean isSupportedSystem(@NonNull EmSystem system);
    boolean isSupportedSystem(@NonNull String systemTag);
    List<EmSystem> getSupportedSystems();
    File searchSupportedContent(File file);
    void attachSurface(@Nullable Activity activity, @NonNull Surface surface);
    void attachSurface(@NonNull Surface surface);
    void adjustSurface(int vw, int vh);
    void detachSurface();
    boolean start(@NonNull String path);
    void pause();
    void resume();
    void stop();
    void reset();
    EmSystemInfo getSystemInfo();
    EmSystemAvInfo getSystemAvInfo();
    byte[] getSerializeData();
    void setSerializeData(final byte[] data);
    byte[] getMemoryData(int type);
    void setMemoryData(int type, final byte[] data);
    void setControllerPortDevice(int port, int device);

    boolean onNativeRumbleState(int port, int effect, int strength);
    boolean onNativeEnvironment(int cmd, Object data);
    void onNativeVideoSizeChanged(int vw, int vh, int rotation);
    int onNativeAudioBuffer(final short[] data, int frames);
    int onNativePollInput(int port, int device, int index, int id);
}
