package ink.snowland.wkuwku.ui.library;

import android.app.Application;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.util.FileManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LibraryViewModel extends BaseViewModel {
    public LibraryViewModel(@NonNull Application application) {
        super(application);
    }

    public Observable<List<Game>> getGameInfos() {
        return AppDatabase.db.gameInfoDao().getAll();
    }

    public void addGame(@NonNull Game game, @NonNull Uri uri) {
        File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
        if (file.exists()) {
            Disposable disposable = AppDatabase.db.gameInfoDao().findByPathAndState(file.getAbsolutePath(), Game.STATE_DELETED)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
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
                    });
        } else {
            addNewGame(game, uri);
        }
    }

    private void addNewGame(@NonNull Game game, Uri uri) {
        if (!FileManager.copy(FileManager.ROM_DIRECTORY, game.filepath, uri)) {
            Toast.makeText(getApplication(), R.string.copy_file_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        File file = FileManager.getFile(FileManager.ROM_DIRECTORY, game.filepath);
        assert file.exists() && file.isFile() && file.canRead();
        game.filepath = file.getAbsolutePath();
        game.addedTime = System.currentTimeMillis();
        game.lastModifiedTime = game.addedTime;
        game.state = Game.STATE_VALID;
        Disposable disposable = AppDatabase.db.gameInfoDao()
                .insert(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    FileManager.delete(file);
                    showErrorToast(error);
                })
                .subscribe(() -> {
                    Toast.makeText(getApplication(), R.string.successful, Toast.LENGTH_SHORT).show();
                });
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
                    Toast.makeText(getApplication(), R.string.successful, Toast.LENGTH_SHORT).show();
                });
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
                    Toast.makeText(getApplication(), R.string.successful, Toast.LENGTH_SHORT).show();
                });
    }
}
