package ink.snowland.wkuwku.ui.play;

import android.app.Application;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;

public class PlayViewModel extends BaseViewModel {
    public PlayViewModel(@NonNull Application application) {
        super(application);
    }

    public Completable update(Game game) {
        return AppDatabase.db.gameInfoDao().update(game);
    }
}
