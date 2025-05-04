package ink.snowland.wkuwku.ui.play;

import androidx.lifecycle.ViewModel;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;

public class PlayViewModel extends ViewModel {
    public Completable update(Game game) {
        return AppDatabase.db.gameInfoDao().update(game);
    }
}
