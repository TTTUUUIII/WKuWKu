package ink.snowland.wkuwku.ui.macro;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.MacroScript;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MacroViewModel extends BaseViewModel {
    private final Disposable mDisposable;
    private final MutableLiveData<List<MacroScript>> mMacrosList = new MutableLiveData<>();
    public MacroViewModel(@NonNull Application application) {
        super(application);
        mDisposable = AppDatabase.db
                .macroScriptDao()
                .getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe(mMacrosList::postValue);

    }

    public void add(@NonNull MacroScript script) {
        AppDatabase.db.macroScriptDao()
                .add(script)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    public void update(@NonNull MacroScript script) {
        AppDatabase.db.macroScriptDao()
                .update(script)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();
    }

    public void delete(@NonNull MacroScript script) {
        AppDatabase.db.macroScriptDao()
                .delete(script)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onError)
                .onErrorComplete()
                .subscribe();

    }

    public LiveData<List<MacroScript>> getAll() {
        return mMacrosList;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.dispose();
    }
}