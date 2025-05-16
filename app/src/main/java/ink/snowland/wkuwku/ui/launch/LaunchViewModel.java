package ink.snowland.wkuwku.ui.launch;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import ink.snowland.wkuwku.db.entity.MacroScript;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LaunchViewModel extends BaseViewModel {
    public LaunchViewModel(@NonNull Application application) {
        super(application);
        Disposable disposable = AppDatabase.db.macroScriptDao()
                .getList()
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((macroScripts, throwable) -> {
                    if (throwable != null) {
                        showErrorToast(throwable);
                    } else {
                    }
                });
    }

    public Completable update(Game game) {
        return AppDatabase.db.gameInfoDao().update(game);
    }

    public Single<List<MacroScript>> getMacros() {
        return AppDatabase.db.macroScriptDao().getList();
    }
}
