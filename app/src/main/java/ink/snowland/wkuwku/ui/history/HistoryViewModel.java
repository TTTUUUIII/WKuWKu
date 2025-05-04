package ink.snowland.wkuwku.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.List;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Observable;

public class HistoryViewModel extends BaseViewModel {
    public HistoryViewModel(@NonNull Application application) {
        super(application);
    }

    public Observable<List<Game>> getHistory() {
        return AppDatabase.db.gameInfoDao().getHistory();
    }
}
