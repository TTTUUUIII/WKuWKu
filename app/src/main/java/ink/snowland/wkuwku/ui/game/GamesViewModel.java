package ink.snowland.wkuwku.ui.game;

import static ink.snowland.wkuwku.util.FileManager.*;
import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ink.snowland.wkuwku.EmulatorManager;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.ActionListener;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.util.ArchiveUtils;
import ink.snowland.wkuwku.util.DownloadManager;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.FileUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.io.FileAlreadyExistsException;

public class GamesViewModel extends BaseViewModel {
    private final MutableLiveData<List<Game>> mAllGames = new MutableLiveData<>();
    private final Disposable mDisposable;

    public GamesViewModel(@NonNull Application application) {
        super(application);
        mDisposable = AppDatabase.db.gameInfoDao()
                .getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAllGames::postValue, error -> error.printStackTrace(System.err));
    }

    public LiveData<List<Game>> getAll() {
        return mAllGames;
    }

    private void copyFiles(@NonNull String filename, @NonNull Uri uri, @NonNull ActionListener listener) {
        if ("https".equals(uri.getScheme())) {
            setPendingIndicator(true, R.string.please_wait);
            DownloadManager.newRequest(uri.toString(), FileManager.getFile(FileManager.ROM_DIRECTORY, filename))
                    .doOnProgressUpdate((progress, max) -> {
                        setPendingMessage(getString(R.string.fmt_downloading, (float) progress / max * 100));
                    })
                    .donOnStateChanged(newState -> {
                        if (newState == DownloadManager.SESSION_STATE_CONNECTING) {
                            setPendingMessage(R.string.connecting);
                        }
                    })
                    .doOnComplete(file -> {
                        listener.onSuccess();
                    })
                    .doOnFinally(() -> {
                        setPendingIndicator(false);
                    })
                    .doOnError(listener::onFailure)
                    .submit();
        } else {
            Completable.create(emitter -> {
                        setPendingIndicator(true, R.string.copying_files);
                        File to = FileManager.getFile(FileManager.ROM_DIRECTORY, filename);
                        boolean noError;
                        try (InputStream from = getApplication().getContentResolver().openInputStream(uri)) {
                            noError = from != null;
                            if (from != null) {
                                noError = FileUtils.copy(from, to);
                            }
                        }
                        if (noError) {
                            emitter.onComplete();
                        } else {
                            emitter.onError(new IOException());
                        }
                    }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete(listener::onSuccess)
                    .doOnError(listener::onFailure)
                    .doFinally(() -> {
                        setPendingIndicator(false);
                    })
                    .subscribe();
        }
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
        copyFiles(filename, uri, new ActionListener() {
            @Override
            public void onSuccess() {
                boolean noError = true;
                IEmulator emulator = EmulatorManager.getDefaultEmulator(game.system);
                File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
                if (emulator == null) return;
                if ((infoMask & ArchiveUtils.FLAG_ARCHIVE_FILE_TYPE) == ArchiveUtils.FLAG_ARCHIVE_FILE_TYPE
                        && (infoMask & ArchiveUtils.FLAG_SUPPORTED_ARCHIVE_FILE_TYPE) == ArchiveUtils.FLAG_SUPPORTED_ARCHIVE_FILE_TYPE) {
                    boolean emulatorSupportedArchive = emulator.searchSupportedContent(file) != null;
                    if (!emulatorSupportedArchive) {
                        setPendingIndicator(true, R.string.unzipping_files);
                        File originFile = file;
                        try {
                            String unzippedPath = ArchiveUtils.extract(originFile);
                            file = new File(unzippedPath);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
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
                        } finally {
                            FileUtils.delete(originFile);
                        }
                    }
                }
                if (!noError) return;
                if (file.isDirectory()) {
                    File romFile = emulator.searchSupportedContent(file);
                    if (romFile == null) {
                        FileUtils.delete(file);
                    }
                    file = romFile;
                }
                if (file == null || !file.exists()) {
                    post(() -> {
                        Toast.makeText(getApplication(), R.string.could_not_find_valid_rom_file, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                game.filepath = file.getAbsolutePath();
                game.addedTime = System.currentTimeMillis();
                game.lastModifiedTime = game.addedTime;
                game.state = Game.STATE_VALID;
                game.md5 = FileUtils.getMD5Sum(file);
                insert(game);
            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace(System.err);
            }
        });
    }

    public void update(@NonNull Game game) {
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

    public void delete(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao().delete(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    showErrorToast(error);
                    error.printStackTrace(System.err);
                })
                .subscribe(() -> {
                    FileUtils.delete(game.filepath);
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
                        if (!parent.equals(getFileDirectory(ROM_DIRECTORY))) {
                            FileUtils.delete(parent);
                        } else {
                            FileUtils.delete(game.filepath);
                        }
                        post(() -> {
                            Toast.makeText(getApplication(), getString(R.string.fmt_game_already_exists, game.title), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        showErrorToast(error);
                    }
                })
                .subscribe(() -> {
                }, error -> {/*Ignored*/});
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.dispose();
    }
}
