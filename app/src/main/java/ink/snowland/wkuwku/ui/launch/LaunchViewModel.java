package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.util.FileManager.*;
import static ink.snowland.wkuwku.interfaces.IEmulator.*;
import static ink.snowland.wkuwku.common.Errors.*;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wkuwku.util.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.BooleanOption;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.common.Errors;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.util.SettingsManager;

public class LaunchViewModel extends BaseViewModel {
    private static final int MIN_INTERVAL_FOR_SAVE_LOAD_STATE = 1100;
    private static final String AUDIO_LOW_LATENCY_MODE = "app_audio_low_latency_mode";
    private static final String AUDIO_API = "app_audio_api";
    private static final String AUDIO_UNDERRUN_OPTIMIZATION = "app_audio_underrun_optimization";
    public static final int MAX_COUNT_OF_SNAPSHOT = 5;
    private IEmulator mEmulator;
    private final List<byte[]> mSnapshots = new ArrayList<>();
    private Game mCurrentGame;
    private boolean mPlaying = false;
    private long mPrevSaveStateUptimeMillis;
    private long mPrevLoadStateUptimeMillis;
    public final BooleanOption virtualControllerOption;
    public final BooleanOption topControlMenuOption;
    public final BooleanOption saveStateWhenExitOption;

    public LaunchViewModel(@NonNull Application application) {
        super(application);
        virtualControllerOption = new BooleanOption(
                "virtual_controller_enabled",
                application.getString(R.string.virtual_controller),
                application.getString(R.string.summary_enable_virtual_controller),
                true
        );
        topControlMenuOption = new BooleanOption(
                "top_control_menu_enabled",
                application.getString(R.string.top_menu),
                application.getString(R.string.summary_enable_top_menu),
                true
        );
        saveStateWhenExitOption = new BooleanOption(
                "save_state_when_exit",
                application.getString(R.string.save_state),
                null,
                true

        );
    }

    public void selectEmulator(@NonNull Game game) {
        String tag = SettingsManager.getString(String.format(Locale.ROOT, "app_%s_core", game.system));
        if (tag.isEmpty()) {
            mEmulator = EmulatorManager.getDefaultEmulator(game.system);
        } else {
            mEmulator = EmulatorManager.getEmulator(tag);
            if (mEmulator == null)
                mEmulator = EmulatorManager.getDefaultEmulator(game.system);
        }
        mCurrentGame = game;
    }

    public @Errors int startEmulator() {
        if (mEmulator != null) {
            applyOptions();
            mEmulator.setProp(PROP_SYSTEM_DIRECTORY, getFileDirectory(SYSTEM_DIRECTORY));
            mEmulator.setProp(PROP_SAVE_DIRECTORY, getFileDirectory(SAVE_DIRECTORY));
            mEmulator.setProp(PROP_CORE_ASSETS_DIRECTORY, getCacheDirectory());
            mEmulator.setProp(PROP_LOW_LATENCY_AUDIO_ENABLE, SettingsManager.getBoolean(AUDIO_LOW_LATENCY_MODE, true));
            mEmulator.setProp(PROP_AUDIO_UNDERRUN_OPTIMIZATION, SettingsManager.getBoolean(AUDIO_UNDERRUN_OPTIMIZATION, true));
            mEmulator.setProp(PROP_VIDEO_FILTER, NumberUtils.parseInt(SettingsManager.getString("video_filter", "0"), 0));
            if ("oboe".equals(SettingsManager.getString(AUDIO_API, "oboe"))) {
                mEmulator.setProp(PROP_OBOE_ENABLE, true);
            } else {
                mEmulator.setProp(PROP_OBOE_ENABLE, false);
            }
            if (mEmulator.start(mCurrentGame.filepath)) {
                loadAllStates();
                mPlaying = true;
                return NO_ERR;
            } else {
                mEmulator = null;
                return ERR;
            }
        }
        return ERR_NOT_FOUND;
    }

    public void pauseEmulator() {
        if (mEmulator == null) return;
        mEmulator.pause();
    }

    public void resumeEmulator() {
        if (mEmulator == null) return;
        mEmulator.resume();
    }

    public void resetEmulator() {
        if (mEmulator == null) return;
        mEmulator.reset();
    }

    public int captureScreen(@NonNull String filepath) {
        if (mEmulator == null) return ERR;
        if (!mEmulator.hasFeature(FEAT_SCREENSHOT)) return ERR_NOT_SUPPORTED;
        if (mEmulator.captureScreen(filepath)) {
            return NO_ERR;
        } else {
            return ERR;
        }
    }

    @Nullable
    public IEmulator getEmulator() {
        return mEmulator;
    }

    public void stopEmulator() {
        if (mEmulator == null) return;
        final String prefix = mEmulator.getProp(PROP_ALIAS, String.class);
        for (int i = 0; i < mSnapshots.size(); i++) {
            try (FileOutputStream fos = new FileOutputStream(getFile(STATE_DIRECTORY, String.format(Locale.US, "%s@%s-%02d.st", prefix, mCurrentGame.md5, i + 1)))) {
                fos.write(mSnapshots.get(i));
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        mEmulator.stop();
        mEmulator = null;
        mCurrentGame = null;
        mPlaying = false;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    public @Errors int saveCurrentSate() {
        if (mEmulator == null
                || SystemClock.uptimeMillis() - mPrevSaveStateUptimeMillis < MIN_INTERVAL_FOR_SAVE_LOAD_STATE) {
            return ERR;
        }
        if (!mEmulator.hasFeature(FEAT_SAVE_STATE)) {
            return ERR_NOT_SUPPORTED;
        }
        byte[] data = mEmulator.getSerializeData();
        if (data == null || data.length == 0) return ERR;
        if (mSnapshots.size() == MAX_COUNT_OF_SNAPSHOT)
            mSnapshots.remove(0);
        mSnapshots.add(data);
        mPrevSaveStateUptimeMillis = SystemClock.uptimeMillis();
        return NO_ERR;
    }

    public @Errors int loadStateAt(int at) {
        if (mEmulator == null
                || mSnapshots.isEmpty()
                || SystemClock.uptimeMillis() - mPrevLoadStateUptimeMillis < MIN_INTERVAL_FOR_SAVE_LOAD_STATE) {
            return ERR;
        }
        if (!mEmulator.hasFeature(FEAT_LOAD_STATE)) {
            return ERR_NOT_SUPPORTED;
        }
        final byte[] data;
        if (at < mSnapshots.size()) {
            data = mSnapshots.get(at);
        } else {
            data = mSnapshots.get(mSnapshots.size() - 1);
        }
        mEmulator.setSerializeData(data);
        mPrevLoadStateUptimeMillis = SystemClock.uptimeMillis();
        return NO_ERR;
    }

    public int getSnapshotsCount() {
        return mSnapshots.size();
    }

    private void loadAllStates() {
        if (mEmulator == null) return;
        final String prefix = mEmulator.getProp(PROP_ALIAS, String.class);
        for (int i = 0; i < MAX_COUNT_OF_SNAPSHOT; ++i) {
            File statFile = getFile(STATE_DIRECTORY, String.format(Locale.US, "%s@%s-%02d.st", prefix, mCurrentGame.md5, i + 1));
            if (statFile.exists() && statFile.length() != 0) {
                try (FileInputStream fis = new FileInputStream(statFile)) {
                    byte[] data = new byte[(int) statFile.length()];
                    int readNumInBytes = fis.read(data, 0, data.length);
                    if (readNumInBytes == statFile.length()) {
                        mSnapshots.add(data);
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    private void applyOptions() {
        assert mEmulator != null;
        Collection<EmOption> options = mEmulator.getOptions();
        for (EmOption option : options) {
            if (!option.enable) continue;
            String val = SettingsManager.getString(option.key);
            if (val.isEmpty()) continue;
            option.val = val;
            mEmulator.setOption(option);
        }
    }
}
