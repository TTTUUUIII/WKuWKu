package ink.snowland.wkuwku.ui.launch;

import static ink.snowland.wkuwku.interfaces.Emulator.SAVE_DIR;
import static ink.snowland.wkuwku.interfaces.Emulator.SYSTEM_DIR;
import android.app.Application;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.outlook.wn123o.retrosystem.RetroSystem;

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
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchViewModel extends BaseViewModel {
    public static final int NO_ERR = 0;
    public static final int ERR_EMULATOR_NOT_FOUND = 1;
    public static final int ERR_LOAD_FAILED = 2;
    public static final int MAX_COUNT_OF_SNAPSHOT = 4;

    private final List<byte[]> mSnapshots = new ArrayList<>();
    private Game mCurrentGame;
    public LaunchViewModel(@NonNull Application application) {
        super(application);
    }

    public int startEmulator(@NonNull Game game) {
        RetroSystem.start(game.filepath);
//        onSelectEmulator(game);
//        if (mEmulator != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                mEmulator.setAudioVolume(mAudioManager.requestAudioFocus(mAudioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? 1.0f : 0.0f);
//            }
//            onApplyOptions();
//            mEmulator.setSystemDirectory(SYSTEM_DIR, FileManager.getFileDirectory(FileManager.SYSTEM_DIRECTORY));
//            mEmulator.setSystemDirectory(SAVE_DIR, FileManager.getFileDirectory(FileManager.SAVE_DIRECTORY + "/" + mEmulator.getTag()));
//            if (mEmulator.run(game.filepath, game.system)) {
//                mCurrentGame = game;
//                handler.postDelayed(this::onStarted, 300);
//                return NO_ERR;
//            } else {
//                mEmulator = null;
//                return ERR_LOAD_FAILED;
//            }
//        }
        return NO_ERR;
    }

    public boolean saveCurrentSate() {
        byte[] data = RetroSystem.getSerializeData();
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
        final byte[] snapshot;
        if (at < mSnapshots.size()) {
            snapshot = mSnapshots.get(at);
        } else {
            snapshot = mSnapshots.get(mSnapshots.size() - 1);
        }
        RetroSystem.setSerializeData(snapshot);
    }

    public int getSnapshotsCount() {
        return mSnapshots.size();
    }

    private void onStarted() {
//        for (int i = 0; i < MAX_COUNT_OF_SNAPSHOT; ++i) {
//            File statFile = FileManager.getFile(FileManager.STATE_DIRECTORY, String.format(Locale.US, "%s@%s-%02d.st", prefix, mCurrentGame.md5, i + 1));
//            if (statFile.exists() && statFile.length() != 0) {
//                try (FileInputStream fis = new FileInputStream(statFile)) {
//                    byte[] data = new byte[(int) statFile.length()];
//                    int readNumInBytes = fis.read(data, 0, data.length);
//                    if (readNumInBytes == statFile.length()) {
//                        mSnapshots.add(data);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace(System.err);
//                }
//            }
//        }
    }

    private void onApplyOptions() {
//        assert mEmulator != null;
//        Collection<EmOption> options = mEmulator.getOptions();
//        for (EmOption option : options) {
//            if (!option.enable) continue;
//            String val = SettingsManager.getString(option.key);
//            if (val.isEmpty()) continue;
//            option.val = val;
//            mEmulator.setOption(option);
//        }
    }

    @Override
    protected void onCleared() {
//        for (int i = 0; i < mSnapshots.size(); i++) {
//            try (FileOutputStream fos = new FileOutputStream(FileManager.getFile(FileManager.STATE_DIRECTORY, String.format(Locale.US, "%s@%s-%02d.st", prefix, mCurrentGame.md5, i + 1)))) {
//                fos.write(mSnapshots.get(i));
//            } catch (IOException e) {
//                e.printStackTrace(System.err);
//            }
//        }
    }
}
