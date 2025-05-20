package ink.snowland.wkuwku.ui.game;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.util.ArchiveUtils;
import ink.snowland.wkuwku.util.FileManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GamesViewModel extends BaseViewModel {
    public GamesViewModel(@NonNull Application application) {
        super(application);
    }

    public Observable<List<Game>> getGameInfos() {
        return AppDatabase.db.gameInfoDao().getAll();
    }

    public void addGame(@NonNull Game game, @NonNull Uri uri) {
        if (uri.getScheme() != null && uri.getScheme().equals("https")) {
            addGameFormNetwork(game, uri);
            return;
        }
        File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
        if (file.exists()) {
            Disposable disposable = AppDatabase.db.gameInfoDao().findByPathAndState(file.getAbsolutePath(), Game.STATE_DELETED)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError((error) -> {
                        addNewGame(game, uri);
                    })
                    .subscribe(it -> {
                        if (it != null) {
                            it.title = game.title;
                            it.lastPlayedTime = 0;
                            it.addedTime = SystemClock.currentThreadTimeMillis();
                            it.lastModifiedTime = it.addedTime;
                            it.remark = game.remark;
                            it.state = Game.STATE_VALID;
                            updateGame(it);
                        } else {
                            boolean ignored = file.delete();
                            addNewGame(game, uri);
                        }
                    }, error -> {
                        /*Ignored*/
                    });
        } else {
            addNewGame(game, uri);
        }
    }

    private void addGameFormNetwork(@NonNull Game game, @NonNull Uri uri) {
        pendingIndicator.postValue(true);
        pendingMessage.postValue(getString(R.string.downloading));
        Disposable disposable = Single.create((SingleOnSubscribe<String>) emitter -> {
                    try {
                        URL url = new URL(uri.toString());
                        URLConnection conn = url.openConnection();
                        conn.setConnectTimeout(1000 * 5);
                        conn.setReadTimeout(1000 * 8);
                        try (InputStream from = conn.getInputStream()) {
                            FileManager.copy(from, FileManager.ROM_DIRECTORY, game.filepath);
                        }
                        File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
                        emitter.onSuccess(FileManager.calculateMD5Sum(file));
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showErrorToast)
                .doFinally(() -> {
                    pendingIndicator.postValue(false);
                    pendingMessage.postValue("");
                })
                .subscribe(md5 -> {
                    File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
                    assert file.exists() && file.isFile() && file.canRead();
                    game.filepath = file.getAbsolutePath();
                    game.addedTime = System.currentTimeMillis();
                    game.lastModifiedTime = game.addedTime;
                    game.state = Game.STATE_VALID;
                    game.md5 = md5;
                    checkArchiveAndInsert(game);
                }, error -> {/*Ignored*/});
    }

    private void addNewGame(@NonNull Game game, Uri uri) {
        setPendingIndicator(true, R.string.copying_files);
        Disposable disposable = Completable.create(emitter -> {
                    if (!FileManager.copy(FileManager.ROM_DIRECTORY, game.filepath, uri)) {
                        emitter.onError(new IOException(getString(R.string.copy_file_failed)));
                    } else {
                        emitter.onComplete();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    Toast.makeText(getApplication(), R.string.copy_file_failed, Toast.LENGTH_SHORT).show();
                })
                .doFinally(() -> {
                    setPendingIndicator(false);
                })
                .doOnComplete(() -> {
                    File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
                    assert file.exists() && file.isFile() && file.canRead();
                    game.filepath = file.getAbsolutePath();
                    game.addedTime = System.currentTimeMillis();
                    game.lastModifiedTime = game.addedTime;
                    game.state = Game.STATE_VALID;
                    checkArchiveAndInsert(game);
                })
                .subscribe(() -> {/*Ignored*/}, error -> {/*Ignored*/});
    }

    public void updateGame(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao().update(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    showErrorToast(error);
                    error.printStackTrace(System.err);
                })
                .subscribe(() -> {
                }, error -> {/*Ignored*/});
    }

    public void deleteGame(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao().delete(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    showErrorToast(error);
                    error.printStackTrace(System.err);
                })
                .subscribe(() -> {
                    FileManager.delete(game.filepath);
                }, error -> {/*Ignored*/});
    }

    private void checkArchiveAndInsert(@NonNull Game game) {
        if (ArchiveUtils.isSupportedArchiveType(game.filepath)) {
            Disposable disposable = extractArchive(game.filepath)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError((error) -> {
                        Toast.makeText(getApplication(), R.string.failed_to_extract_archive, Toast.LENGTH_SHORT).show();
                    })
                    .doOnSuccess(output -> {
                        Emulator emulator = EmulatorManager.getDefaultEmulator(game.system);
                        assert emulator != null;
                        String filepath = emulator.findRom(output);
                        if (filepath != null) {
                            game.filepath = filepath;
                            game.md5 = FileManager.calculateMD5Sum(new File(game.filepath));
                            insert(game);
                        } else {
                            FileManager.deleteDirectory(output);
                            Toast.makeText(getApplication(), R.string.no_supported_rom_found_in_archive, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .subscribe((output, error) -> {/*Ignored*/});
        } else {
            game.md5 = FileManager.calculateMD5Sum(game.filepath);
            insert(game);
        }
    }

    private void insert(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao()
                .insert(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    if (!(error instanceof SQLiteConstraintException)) {
                        File file = new File(game.filepath).getParentFile();
                        if (file != null && !file.equals(FileManager.getFileDirectory(FileManager.ROM_DIRECTORY))) {
                            FileManager.deleteDirectory(file);
                        } else {
                            FileManager.delete(game.filepath);
                        }
                    }
                    if (error instanceof SQLiteConstraintException) {
                        Toast.makeText(getApplication(), getString(R.string.fmt_game_already_exists, game.title), Toast.LENGTH_SHORT).show();
                    } else {
                        showErrorToast(error);
                    }
                })
                .subscribe(() -> {
                }, error -> {/*Ignored*/});
    }

    private Single<File> extractArchive(@NonNull String filepath) {
        return Single.create((SingleOnSubscribe<File>) emitter -> {
            setPendingIndicator(true, R.string.extracting_archive);
            try {
                File outdir = ArchiveUtils.extract(FileManager.getFileDirectory(FileManager.ROM_DIRECTORY).getAbsolutePath(), filepath);
                emitter.onSuccess(outdir);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                emitter.onError(e);
            }
            FileManager.delete(filepath);
            setPendingIndicator(false);
        });
    }
}
