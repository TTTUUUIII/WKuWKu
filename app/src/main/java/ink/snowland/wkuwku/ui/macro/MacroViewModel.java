package ink.snowland.wkuwku.ui.macro;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

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
                .subscribe(mMacrosList::postValue, error -> {
                    error.printStackTrace(System.err);
                });

    }

    public void add(@NonNull MacroScript script) {
        Disposable disposable = AppDatabase.db.macroScriptDao()
                .add(script)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, this::showErrorToast);
    }

    public void update(@NonNull MacroScript script) {
        Disposable disposable = AppDatabase.db.macroScriptDao()
                .update(script)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {}, this::showErrorToast);
    }

    public void delete(@NonNull MacroScript script) {
        Disposable disposable = AppDatabase.db.macroScriptDao()
                .delete(script)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, this::showErrorToast);

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