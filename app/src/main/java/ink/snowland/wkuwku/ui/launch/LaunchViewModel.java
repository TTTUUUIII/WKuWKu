package ink.snowland.wkuwku.ui.launch;

import android.app.Application;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;

public class LaunchViewModel extends BaseViewModel {
    public LaunchViewModel(@NonNull Application application) {
        super(application);
    }

    public Completable update(Game game) {
        return AppDatabase.db.gameInfoDao().update(game);
    }
}
