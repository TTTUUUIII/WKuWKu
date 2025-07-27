package ink.snowland.wkuwku.ui.trash;

import static ink.snowland.wkuwku.util.FileManager.*;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TrashViewModel extends BaseViewModel {
    private final Disposable mDisposable;
    private final MutableLiveData<List<Game>> mTrash = new MutableLiveData<>();

    public TrashViewModel(@NonNull Application application) {
        super(application);
        mDisposable = AppDatabase.db.gameInfoDao()
                .getTrash()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(mTrash::postValue)
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    public LiveData<List<Game>> getTrash() {
        return mTrash;
    }

    public void delete(@NonNull Game game) {
        AppDatabase.db.gameInfoDao().delete(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> clearFiles(game))
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    public void restore(@NonNull Game game) {
        game.state = Game.STATE_VALID;
        game.lastModifiedTime = System.currentTimeMillis();
        AppDatabase.db.gameInfoDao().update(game)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }
}
