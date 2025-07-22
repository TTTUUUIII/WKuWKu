package ink.snowland.wkuwku.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import ink.snowland.wkuwku.common.ActionListener;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import ink.snowland.wkuwku.plug.PlugManifest;
import ink.snowland.wkuwku.plug.PlugUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlugManager {
    private static final Logger sLogger = new Logger("Manager", "PlugManager");
    private static Context sApplicationContext;

    public static void initialize(Context applicationContext) {
        sApplicationContext = applicationContext;
        Disposable disposable = AppDatabase.db.plugManifestExtDao()
                .getSingleAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(PlugManager::install, error -> {
                    error.printStackTrace(System.err);
                });
    }

    public static void install(File plugFile, @Nullable ActionListener listener) {
        Disposable disposable = Completable.create(emitter -> {
                    PlugManifest manifest = PlugUtils.readManifest(sApplicationContext, plugFile);
                    if (manifest == null) {
                        emitter.onError(new RuntimeException("Invalid package!"));
                        return;
                    }
                    boolean upgrade = new File(FileManager.getPlugDirectory(), manifest.packageName).exists();
                    manifest = PlugUtils.install(sApplicationContext, plugFile, FileManager.getPlugDirectory());
                    if (!upgrade && manifest != null) {
                        try {
                            AppDatabase.db.plugManifestExtDao()
                                    .insert(new PlugManifestExt(manifest));
                            emitter.onComplete();
                        } catch (Exception e) {
                            PlugUtils.uninstall(manifest);
                            emitter.onError(e);
                        }
                    }
                    if (manifest == null) {
                        emitter.onError(new RuntimeException("Plug install failed!"));
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (listener != null)
                        listener.onSuccess();
                })
                .doOnError(error -> {
                    if (listener != null)
                        listener.onFailure(error);
                })
                .subscribe(() -> {
                        },
                        error -> {/*Ignored*/}
                );
    }

    public static void install(@NonNull PlugManifest manifest, @Nullable ActionListener listener) {
        Disposable disposable = Completable.create(emitter -> {
                    if (PlugUtils.install(sApplicationContext, manifest)) {
                        emitter.onComplete();
                    } else {
                        emitter.onError(new UnknownError("Plug install failed!"));
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

    public static @Nullable Drawable getPlugIcon(@NonNull PlugManifest manifest) {
        return PlugUtils.getPlugIcon(sApplicationContext, manifest);
    }

    public static boolean isInstalled(@NonNull PlugManifest manifest) {
        return PlugUtils.isInstanced(manifest);
    }

    private static void install(@NonNull List<PlugManifestExt> plugs) {
        Disposable disposable = Single.create((SingleOnSubscribe<Integer>) emitter -> {
                    int installed = 0;
                    for (PlugManifestExt plug : plugs) {
                        if (!plug.enabled) continue;
                        if (PlugUtils.install(sApplicationContext, plug.origin))
                            installed++;
                    }
                    emitter.onSuccess(installed);
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((installed) -> {
                    sLogger.i(" %d of %d auto installed.", installed, plugs.size());
                });
    }
}
