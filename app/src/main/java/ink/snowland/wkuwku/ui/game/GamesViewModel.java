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
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.interfaces.IEmulator;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.util.ArchiveUtils;
import ink.snowland.wkuwku.util.DownloadManager;

import org.wkuwku.interfaces.ActionListener;
import org.wkuwku.util.FileUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GamesViewModel extends BaseViewModel {
    private final MutableLiveData<List<Game>> mAllGames = new MutableLiveData<>();
    private final Disposable mDisposable;

    public GamesViewModel(@NonNull Application application) {
        super(application);
        mDisposable = AppDatabase.getDefault()
                .gameInfoDao()
                .getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAllGames::postValue, error -> error.printStackTrace(System.err));
    }

    public LiveData<List<Game>> getAll() {
        return mAllGames;
    }

    private void copyFiles(@NonNull Uri uri, @NonNull File file, @NonNull ActionListener listener) {
        if ("https".equals(uri.getScheme())) {
            DownloadManager.newRequest(uri.toString(), file)
                    .doOnProgressUpdate((progress, max) -> {
                        setPendingMessage(getString(R.string.fmt_downloading, (float) progress / max * 100));
                    })
                    .donOnStateChanged(newState -> {
                        if (newState == DownloadManager.SESSION_STATE_CONNECTING) {
                            setPendingMessage(R.string.connecting);
                        }
                    })
                    .doOnComplete(it -> {
                        listener.onSuccess();
                    })
                    .doOnError(listener::onFailure)
                    .submit();
        } else {
            setPendingMessage(R.string.copying_files);
            Completable.create(emitter -> {
                        boolean noError = false;
                        try (InputStream from = getApplication().getContentResolver().openInputStream(uri)) {
                            if (from != null) {
                                noError = FileUtils.copy(from, file);
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
                    .subscribe();
        }
    }

    public void addGame(@NonNull Game game, @NonNull Uri uri) {
        final File file = getFile(ROM_DIRECTORY, game.filepath);
        boolean isArchiveType = ArchiveUtils.isArchiveType(file);
        if (isArchiveType && !ArchiveUtils.isSupported(file)) {
            Toast.makeText(getApplication(), R.string.unsupported_archive_format, Toast.LENGTH_SHORT).show();
            return;
        }
        setPendingIndicator(true, R.string.please_wait);
        copyFiles(uri, file, new ActionListener() {
            @Override
            public void onSuccess() {
                IEmulator emulator = EmulatorManager.getDefaultEmulator(game.system);
                File content = emulator.searchSupportedContent(file);
                if (content != null) {
                    game.filepath = content.getAbsolutePath();
                    game.addedTime = System.currentTimeMillis();
                    game.lastModifiedTime = game.addedTime;
                    game.state = Game.STATE_VALID;
                    game.md5 = FileUtils.getMD5Sum(file);
                    insert(game);
                    setPendingIndicator(false);
                } else if (isArchiveType) {
                    setPendingMessage(R.string.unzipping_files);
                    File outdir = new File(file.getParentFile(), FileUtils.getName(file, false));
                    ArchiveUtils.asyncExtract(file, outdir, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            File content = emulator.searchSupportedContent(outdir);
                            if (content != null) {
                                game.filepath = content.getAbsolutePath();
                                game.addedTime = System.currentTimeMillis();
                                game.lastModifiedTime = game.addedTime;
                                game.state = Game.STATE_VALID;
                                game.md5 = FileUtils.getMD5Sum(file);
                                insert(game);
                            } else {
                                Toast.makeText(getApplication(), R.string.could_not_find_valid_rom_file, Toast.LENGTH_LONG).show();
                            }
                            FileUtils.asyncDelete(file);
                            setPendingIndicator(false);
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            FileUtils.asyncDelete(file);
                            setPendingIndicator(false);
                            Toast.makeText(getApplication(), R.string.failed_extract, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(getApplication(), R.string.could_not_find_valid_rom_file, Toast.LENGTH_LONG).show();
                    setPendingIndicator(false);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                GamesViewModel.this.onError(e);
                e.printStackTrace(System.err);
                setPendingIndicator(false);
            }
        });
    }

    public void update(@NonNull Game game) {
        AppDatabase.getDefault()
                .gameInfoDao().update(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    public void delete(@NonNull Game game) {
        AppDatabase.getDefault()
                .gameInfoDao().delete(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> FileUtils.delete(game.filepath))
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    private void insert(@NonNull Game game) {
        AppDatabase.getDefault()
                .gameInfoDao()
                .insert(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    if (!(error instanceof SQLiteConstraintException)) {
                        File parent = new File(game.filepath).getParentFile();
                        if (parent != null && !parent.equals(getFileDirectory(ROM_DIRECTORY))) {
                            FileUtils.asyncDelete(parent);
                        } else {
                            FileUtils.asyncDelete(game.filepath);
                        }
                        Toast.makeText(getApplication(), getString(R.string.fmt_game_already_exists, game.title), Toast.LENGTH_SHORT).show();
                    } else {
                        this.onError(error);
                    }
                })
                .onErrorComplete()
                .subscribe();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.dispose();
    }
}
