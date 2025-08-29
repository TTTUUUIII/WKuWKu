package ink.snowland.wkuwku.util;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.wkuwku.interfaces.ActionListener;
import org.wkuwku.util.FileUtils;
import org.wkuwku.util.Logger;

import java.io.File;
import java.util.List;
import java.util.Set;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import ink.snowland.wkuwku.plug.PlugManifest;
import ink.snowland.wkuwku.plug.PlugUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlugManager {
    private static final String UPGRADE_LIST = "plug_upgrade_list";
    private static final Logger sLogger = new Logger("Plug", "PlugManager");
    private static Context sApplicationContext;

    public static void initialize(Context applicationContext) {
        sApplicationContext = applicationContext;
        AppDatabase.getDefault()
                .plugManifestExtDao()
                .getSingleAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(PlugManager::install)
                .doOnError(error -> error.printStackTrace(System.err))
                .onErrorComplete()
                .subscribe();
    }

    public static void install(File plugFile, @Nullable ActionListener listener) {
        Completable.create(emitter -> {
                    PlugManifest manifest = PlugUtils.readManifest(sApplicationContext, plugFile);
                    if (manifest == null) {
                        emitter.onError(new RuntimeException("Invalid package!"));
                        return;
                    }
                    boolean upgrade = new File(FileManager.getPlugDirectory(), manifest.packageName).exists();
                    manifest = PlugUtils.install(sApplicationContext, plugFile, FileManager.getPlugDirectory());
                    if (!upgrade && manifest != null) {
                        try {
                            AppDatabase.getDefault()
                                    .plugManifestExtDao()
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
                .onErrorComplete()
                .subscribe();
    }

    public static void upgrade(@NonNull String packageName, @NonNull File file) {
        ArraySet<String> plugs = new ArraySet<>(SettingsManager.getStringSet(UPGRADE_LIST));
        File validPathFile = new File(FileManager.getCacheDirectory(), String.format("%s%s.apk", FileManager.SKIP_CLEAN_PREFIX, packageName));
        FileUtils.delete(validPathFile);
        if (file.renameTo(validPathFile)) {
            plugs.add(packageName);
            SettingsManager.putStringSet(UPGRADE_LIST, plugs);
        } else {
            sLogger.e("Failed to add plug to upgrade list. %s", packageName);
        }
    }

    public static void install(@NonNull PlugManifest manifest, @Nullable ActionListener listener) {
        Completable.create(emitter -> {
                    if (PlugUtils.install(sApplicationContext, manifest)) {
                        emitter.onComplete();
                    } else {
                        emitter.onError(new UnknownError("Plug install failed!"));
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (listener != null)
                        listener.onSuccess();
                })
                .doOnError(error -> {
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                })
                .onErrorComplete()
                .subscribe();
    }

    public static void uninstall(PlugManifest manifest, @Nullable ActionListener listener) {
        Completable.create(emitter -> {
                    try {
                        AppDatabase.getDefault()
                                .plugManifestExtDao()
                                .deleteByPackageName(manifest.packageName);
                        PlugUtils.uninstall(manifest);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
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
                .subscribe();
    }

    public static @Nullable Drawable getPlugIcon(@NonNull PlugManifest manifest) {
        return PlugUtils.getPlugIcon(sApplicationContext, manifest);
    }

    public static boolean isInstalled(@NonNull PlugManifest manifest) {
        return PlugUtils.isInstanced(manifest);
    }

    private static void install(@NonNull List<PlugManifestExt> plugs) {
        Completable.create(emitter -> {
                    Set<String> upgradeList = SettingsManager.getStringSet(UPGRADE_LIST);
                    for (PlugManifestExt plug : plugs) {
                        if (!plug.enabled) {
                            sLogger.w("Skipped: %s", plug.packageName);
                            continue;
                        }
                        if (upgradeList.contains(plug.packageName)) {
                            String filename = String.format("%s%s.apk", FileManager.SKIP_CLEAN_PREFIX, plug.packageName);
                            File file = new File(FileManager.getCacheDirectory(), filename);
                            if (file.exists()) {
                                PlugManifest manifest = PlugUtils.install(sApplicationContext, file, FileManager.getPlugDirectory());
                                if (manifest != null) {
                                    plug.origin = manifest;
                                    AppDatabase.getDefault()
                                            .plugManifestExtDao()
                                            .update(plug);
                                    sLogger.i("Upgraded: %s", plug.packageName);
                                }
                                FileUtils.delete(file);
                            }
                        } else if (PlugUtils.install(sApplicationContext, plug.origin)){
                            sLogger.i("Installed: %s", plug.packageName);
                        }
                    }
                    SettingsManager.remove(UPGRADE_LIST);
                    emitter.onComplete();
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorComplete()
                .subscribe();
    }
}
