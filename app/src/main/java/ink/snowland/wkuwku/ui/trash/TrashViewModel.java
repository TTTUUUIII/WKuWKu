package ink.snowland.wkuwku.ui.trash;

import android.app.Application;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.util.FileManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TrashViewModel extends BaseViewModel {
    public TrashViewModel(@NonNull Application application) {
        super(application);
    }

    public Observable<List<Game>> getTrash() {
        return AppDatabase.db.gameInfoDao().getTrash();
    }

    public void delete(@NonNull Game game) {
        Disposable disposable = AppDatabase.db.gameInfoDao().delete(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showErrorToast)
                .subscribe(() -> {
                    File file = new File(game.filepath).getParentFile();
                    if (file != null && !file.equals(FileManager.getFileDirectory(FileManager.ROM_DIRECTORY))) {
                        FileManager.deleteDirectory(file);
                    } else {
                        FileManager.delete(game.filepath);
                    }
                }, error -> {/*ignored*/});
    }

    public void restore(@NonNull Game game) {
        game.state = Game.STATE_VALID;
        game.lastModifiedTime = System.currentTimeMillis();
        Disposable disposable = AppDatabase.db.gameInfoDao().update(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showErrorToast)
                .subscribe(() -> {}, error -> {/*Ignored*/});
    }
}
