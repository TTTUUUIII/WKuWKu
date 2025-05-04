package ink.snowland.wkuwku.ui.history;

import androidx.lifecycle.ViewModel;

import java.util.List;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Observable;

public class HistoryViewModel extends ViewModel {
    public Observable<List<Game>> getHistory() {
        return AppDatabase.db.gameInfoDao().getHistory();
    }
}
