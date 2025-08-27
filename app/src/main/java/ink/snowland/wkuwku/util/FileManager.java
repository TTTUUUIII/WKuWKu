package ink.snowland.wkuwku.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FileManager {
    public static final String SKIP_CLEAN_PREFIX = "keep_";

    public static final String ROM_DIRECTORY = "rom";
    public static final String STATE_DIRECTORY = "state";
    public static final String SAVE_DIRECTORY = "save";
    public static final String IMAGE_DIRECTORY = "img";
    public static final String SYSTEM_DIRECTORY = "system";

    private FileManager() {}

    private static Context sApplicationContext;

    public static File getCacheDirectory() {
        return sApplicationContext.getExternalCacheDir();
    }

    public static File getFileDirectory(String type) {
        return sApplicationContext.getExternalFilesDir(type);
    }

    public static File getPlugDirectory() {
        return sApplicationContext.getDir("plug", Context.MODE_PRIVATE);
    }

    public static File getFile(String type, String filename) {
        return new File(sApplicationContext.getExternalFilesDir(type), filename);
    }

    public static void initialize(Context context) {
        sApplicationContext = context;
        Completable.create(emitter -> {
                    File cacheDir = getCacheDirectory();
                    File[] files = cacheDir.listFiles();
                    if (files == null) return;
                    for (File file : files) {
                        if (file.getName().startsWith(SKIP_CLEAN_PREFIX)) continue;
                        FileUtils.delete(file);
                    }
                    int maxStorageDays = sApplicationContext.getResources().getInteger(R.integer.trash_storage_days);
                    long expiredTimeMillis = System.currentTimeMillis() - (long) maxStorageDays * 24 * 60 * 60 * 1000;
                    List<Game> list = AppDatabase.db.gameInfoDao()
                            .findTrashByModifiedLT(expiredTimeMillis);
                    delete(list);
                    emitter.onComplete();
                }).subscribeOn(Schedulers.io())
                .doOnError(error -> error.printStackTrace(System.err))
                .onErrorComplete()
                .subscribe();
    }

    private static void delete(@Nullable List<Game> games) {
        if (games == null || games.isEmpty()) return;
        AppDatabase.db.gameInfoDao()
                .delete(games);
        for (Game game : games) {
            clearFiles(game);
        }
    }

    public static void clearFiles(@NonNull Game game) {
        /*ROM*/
        File parent = new File(game.filepath).getParentFile();
        if (parent != null && !parent.equals(getFileDirectory(ROM_DIRECTORY))) {
            FileUtils.delete(parent);
        } else {
            FileUtils.delete(game.filepath);
        }

        /*Screenshot*/
        FileUtils.delete(getFile(IMAGE_DIRECTORY, game.id + ".png"));

        /*Status*/
        File stateDir = getFileDirectory(STATE_DIRECTORY);
        final File[] files = stateDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (name.contains(game.md5)) {
                FileUtils.delete(file);
            }
        }
    }
}
