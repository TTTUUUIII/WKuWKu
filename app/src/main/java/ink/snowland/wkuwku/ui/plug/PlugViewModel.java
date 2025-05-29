package ink.snowland.wkuwku.ui.plug;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.List;

import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public class PlugViewModel extends BaseViewModel {
    public PlugViewModel(@NonNull Application application) {
        super(application);
    }

    public Observable<List<PlugManifestExt>> getInstalledPlug() {
        return AppDatabase.db.plugManifestExtDao().getAll();
    }

    public Completable update(@NonNull PlugManifestExt manifest) {
        return Completable.create(emitter -> {
            AppDatabase.db.plugManifestExtDao().update(manifest);
            emitter.onComplete();
        });
    }

    public Completable delete(@NonNull PlugManifestExt manifest) {
        return Completable.create(emitter -> {
            AppDatabase.db.plugManifestExtDao().delete(manifest);
            emitter.onComplete();
        });
    }
}