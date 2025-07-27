package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.interfaces.IEmulator.*;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.common.EmOption;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;

public class LaunchViewModel extends BaseViewModel {
    private static final String AUDIO_LOW_LATENCY_MODE = "app_audio_low_latency_mode";
    private static final String AUDIO_API = "app_audio_api";
    private static final String AUDIO_UNDERRUN_OPTIMIZATION = "app_audio_underrun_optimization";
    public static final int NO_ERR = 0;
    public static final int ERR_EMULATOR_NOT_FOUND = 1;
    public static final int ERR_LOAD_FAILED = 2;
    public static final int MAX_COUNT_OF_SNAPSHOT = 4;
    private IEmulator mEmulator;
    private final List<byte[]> mSnapshots = new ArrayList<>();
    private Game mCurrentGame;
    private boolean mPlaying = false;
    public LaunchViewModel(@NonNull Application application) {
        super(application);
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

    public int startEmulator() {
        if (mEmulator != null) {
            applyOptions();
            mEmulator.setProp(PROP_SYSTEM_DIRECTORY, FileManager.getFileDirectory(FileManager.SYSTEM_DIRECTORY));
            mEmulator.setProp(PROP_SAVE_DIRECTORY, FileManager.getFileDirectory(FileManager.SAVE_DIRECTORY));
            mEmulator.setProp(PROP_CORE_ASSETS_DIRECTORY, FileManager.getCacheDirectory());
            mEmulator.setProp(PROP_LOW_LATENCY_AUDIO_ENABLE, SettingsManager.getBoolean(AUDIO_LOW_LATENCY_MODE, true));
            mEmulator.setProp(PROP_AUDIO_UNDERRUN_OPTIMIZATION, SettingsManager.getBoolean(AUDIO_UNDERRUN_OPTIMIZATION, true));
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
                return ERR_LOAD_FAILED;
            }
        }
        return ERR_EMULATOR_NOT_FOUND;
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

    public boolean captureScreen(@NonNull String filepath) {
        if (mEmulator == null) return false;
        return mEmulator.captureScreen(filepath);
    }

    public @Nullable IEmulator getEmulator() {
        return mEmulator;
    }

    public void stopEmulator() {
        if (mEmulator == null) return;
        mEmulator.pause();
        final String prefix = (String) mEmulator.getProp(PROP_ALIAS);
        for (int i = 0; i < mSnapshots.size(); i++) {
            try (FileOutputStream fos = new FileOutputStream(FileManager.getFile(FileManager.STATE_DIRECTORY, String.format(Locale.US, "%s@%s-%02d.st", prefix, mCurrentGame.md5, i + 1)))) {
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
    public boolean saveCurrentSate() {
        if (mEmulator == null) return false;
        byte[] data = mEmulator.getSerializeData();
        if (data == null || data.length == 0) return false;
        if (mSnapshots.size() == MAX_COUNT_OF_SNAPSHOT)
            mSnapshots.remove(0);
        mSnapshots.add(data);
        return true;
    }

    public void loadStateAtLast() {
        loadStateAt(MAX_COUNT_OF_SNAPSHOT - 1);
    }
    public void loadStateAt(int at) {
        if (mSnapshots.isEmpty() || mEmulator == null) return;
        final byte[] data;
        if (at < mSnapshots.size()) {
            data = mSnapshots.get(at);
        } else {
            data = mSnapshots.get(mSnapshots.size() - 1);
        }
        mEmulator.setSerializeData(data);
    }

    public int getSnapshotsCount() {
        return mSnapshots.size();
    }

    private void loadAllStates() {
        if (mEmulator == null) return;
        final String prefix = (String) mEmulator.getProp(PROP_ALIAS);
        for (int i = 0; i < MAX_COUNT_OF_SNAPSHOT; ++i) {
            File statFile = FileManager.getFile(FileManager.STATE_DIRECTORY, String.format(Locale.US, "%s@%s-%02d.st", prefix, mCurrentGame.md5, i + 1));
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
