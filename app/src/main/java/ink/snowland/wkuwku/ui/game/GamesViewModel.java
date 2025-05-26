package ink.snowland.wkuwku.ui.game;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
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
import ink.snowland.wkuwku.common.NumberUtils;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.interfaces.Emulator;
import ink.snowland.wkuwku.util.ArchiveUtils;
import ink.snowland.wkuwku.util.FileManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.io.FileAlreadyExistsException;

public class GamesViewModel extends BaseViewModel {
    public GamesViewModel(@NonNull Application application) {
        super(application);
    }
    public Observable<List<Game>> getGameInfos() {
        return AppDatabase.db.gameInfoDao().getAll();
    }

    private Completable copyFiles(@NonNull String filename, @NonNull Uri uri) {
        return Completable.create(emitter -> {
            boolean noError = true;
            if (uri.getScheme() != null && uri.getScheme().equals("https")) {
                setPendingIndicator(true, R.string.connecting);
                try {
                    URL url = new URL(uri.toString());
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(1000 * 5);
                    conn.setReadTimeout(1000 * 8);
                    try (InputStream from = conn.getInputStream()) {
                        final long total = NumberUtils.parseLong(conn.getHeaderField("Content-Length"), 0);
                        FileManager.copy(from, FileManager.ROM_DIRECTORY, filename, (progress, max) -> {
                            if (total != 0)
                                setPendingMessage(getString(R.string.fmt_downloading, (float) progress / total * 100));
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    post(() -> showErrorToast(e));
                    noError = false;
                }
            } else {
                setPendingIndicator(true, R.string.copying_files);
                noError = FileManager.copy(FileManager.ROM_DIRECTORY, filename, uri);
            }
            if (noError) {
                emitter.onComplete();
            } else {
                emitter.onError(new IOException(getString(R.string.copy_file_failed)));
            }
            setPendingIndicator(false);
        });
    }

    public void addGame(@NonNull Game game, @NonNull Uri uri) {
        final String filename = game.filepath;
        int infoMask = ArchiveUtils.getFileInfoMask(filename);
        if ((infoMask & ArchiveUtils.FLAG_ARCHIVE_FILE_TYPE) == ArchiveUtils.FLAG_ARCHIVE_FILE_TYPE
                && (infoMask & ArchiveUtils.FLAG_SUPPORTED_ARCHIVE_FILE_TYPE) != ArchiveUtils.FLAG_SUPPORTED_ARCHIVE_FILE_TYPE) {
            post(() -> {
                Toast.makeText(getApplication(), R.string.unsupported_archive_format, Toast.LENGTH_SHORT).show();
            });
            return;
        }
        Disposable disposable = copyFiles(filename, uri)
                .subscribeOn(Schedulers.io())
                .doOnComplete(() -> {
                    boolean noError = true;
                    File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
                    assert file.exists() && file.isFile() && file.canRead();
                    if ((infoMask & ArchiveUtils.FLAG_ARCHIVE_FILE_TYPE) == ArchiveUtils.FLAG_ARCHIVE_FILE_TYPE
                            && (infoMask & ArchiveUtils.FLAG_SUPPORTED_ARCHIVE_FILE_TYPE) == ArchiveUtils.FLAG_SUPPORTED_ARCHIVE_FILE_TYPE) {
                        setPendingIndicator(true, R.string.unzipping_files);
                        try {
                            String unzippedPath = ArchiveUtils.extract(file);
                            file = new File(unzippedPath);
                        } catch (IOException e) {
                            if (e instanceof FileAlreadyExistsException) {
                                post(() -> {
                                    Toast.makeText(getApplication(), getString(R.string.file_already_exists), Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                post(() -> {
                                    showErrorToast(e);
                                });
                            }
                            noError = false;
                        }
                    }
                    FileManager.delete(file);
                    if (!noError) return;
                    if (file.isDirectory()) {
                        Emulator emulator = EmulatorManager.getDefaultEmulator(game.system);
                        assert emulator != null;
                        File romFile = emulator.findLoaderFile(file);
                        if (romFile == null) {
                            FileManager.delete(file);
                        }
                        file = romFile;
                    }
                    if (file == null) {
                        post(() -> {
                            Toast.makeText(getApplication(), R.string.could_not_find_valid_rom_file, Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    game.filepath = file.getAbsolutePath();
                    game.addedTime = System.currentTimeMillis();
                    game.lastModifiedTime = game.addedTime;
                    game.state = Game.STATE_VALID;
                    game.md5 = FileManager.calculateMD5Sum(file);
                    insert(game);
                })
                .subscribe(() -> {
                }, (error) -> {/*Ignored*/});
    }

    public void update(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao().update(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    showErrorToast(error);
                    error.printStackTrace(System.err);
                })
                .subscribe(() -> {}, error -> {/*Ignored*/});
    }

    public void delete(@NonNull Game game) {
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

    private void insert(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao()
                .insert(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    if (!(error instanceof SQLiteConstraintException)) {
                        File parent = new File(game.filepath).getParentFile();
                        assert parent != null;
                        if (!parent.equals(FileManager.getFileDirectory(FileManager.ROM_DIRECTORY))) {
                            FileManager.delete(parent);
                        } else {
                            FileManager.delete(game.filepath);
                        }
                        post(() -> {
                            Toast.makeText(getApplication(), getString(R.string.fmt_game_already_exists, game.title), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        showErrorToast(error);
                    }
                })
                .subscribe(() -> { }, error -> {/*Ignored*/});
    }
}
