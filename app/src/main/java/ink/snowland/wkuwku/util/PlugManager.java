package ink.snowland.wkuwku.util;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.File;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import ink.snowland.wkuwku.plug.PlugManifest;
import ink.snowland.wkuwku.plug.PlugUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlugManager {
    private static Context sApplicationContext;

    public static void initialize(Context applicationContext) {
        sApplicationContext = applicationContext;
        Disposable disposable = AppDatabase.db.plugManifestExtDao()
                .getSingleAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(plugs -> {
                    System.out.println(plugs);
                }, error -> {
                    error.printStackTrace(System.err);
                });
    }

    public static void install(String plugPath, @Nullable ActionListener listener) {
        Disposable disposable = Completable.create(emitter -> {
                    PlugManifest manifest = PlugUtils.install(sApplicationContext, new File(plugPath), FileManager.getPlugDirectory());
                    if (manifest == null) {
                        emitter.onError(new RuntimeException("Plug install failed!"));
                        return;
                    }
                    try {
                        AppDatabase.db.plugManifestExtDao()
                                .insert(new PlugManifestExt(manifest));
                        emitter.onComplete();
                    } catch (Exception e) {
                        PlugUtils.uninstall(manifest);
                        emitter.onError(e);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (listener != null)
                        listener.onSuccess();
                }, error -> {
                    if (listener != null)
                        listener.onFailure(error);
                });
    }

    public static void uninstall(PlugManifest manifest, @Nullable ActionListener listener) {
        Disposable disposable = Completable.create(emitter -> {
                    try {
                        AppDatabase.db.plugManifestExtDao()
                                .deleteByPackageName(manifest.packageName);
                        PlugUtils.uninstall(manifest);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (listener != null)
                        listener.onSuccess();
                }, error -> {
                    if (listener != null)
                        listener.onFailure(error);
                });
    }

    public interface ActionListener {
        void onSuccess();

        void onFailure(Throwable e);
    }
}
