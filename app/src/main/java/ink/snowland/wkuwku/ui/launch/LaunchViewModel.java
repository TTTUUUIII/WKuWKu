package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.interfaces.IEmulator.*;
import android.app.Application;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

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
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchViewModel extends BaseViewModel implements AudioManager.OnAudioFocusChangeListener {
    public static final int NO_ERR = 0;
    public static final int ERR_EMULATOR_NOT_FOUND = 1;
    public static final int ERR_LOAD_FAILED = 2;
    public static final int MAX_COUNT_OF_SNAPSHOT = 4;
    private IEmulator mEmulator;
    private AudioManager mAudioManager = null;
    private AudioFocusRequest mAudioFocusRequest = null;
    private final List<byte[]> mSnapshots = new ArrayList<>();
    private Game mCurrentGame;
    public LaunchViewModel(@NonNull Application application) {
        super(application);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .build();
            mAudioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        }
        Disposable disposable = AppDatabase.db.macroScriptDao()
                .getList()
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((macroScripts, throwable) -> {
                    if (throwable != null) {
                        showErrorToast(throwable);
                    } else {
//                        throwable.printStackTrace(System.err);
                    }
                });
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mEmulator.setProp(PROP_AUDIO_VOLUME, mAudioManager.requestAudioFocus(mAudioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? 1.0f : 0.0f);
            }
            onApplyOptions();
            mEmulator.setProp(PROP_SYSTEM_DIRECTORY, FileManager.getFileDirectory(FileManager.SYSTEM_DIRECTORY));
            mEmulator.setProp(PROP_SAVE_DIRECTORY, FileManager.getFileDirectory(FileManager.SAVE_DIRECTORY));
            mEmulator.setProp(PROP_CORE_ASSETS_DIRECTORY, FileManager.getCacheDirectory());
            if (mEmulator.start(mCurrentGame.filepath)) {
                loadAllStates();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        }
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

    private void onApplyOptions() {
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

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (mEmulator == null) return;
        mEmulator.setProp(PROP_AUDIO_VOLUME, focusChange == AudioManager.AUDIOFOCUS_GAIN ? 1.0f : 0.0f);
    }
}
