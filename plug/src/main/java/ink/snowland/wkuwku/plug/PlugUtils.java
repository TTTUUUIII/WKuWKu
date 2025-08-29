package ink.snowland.wkuwku.plug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wkuwku.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class PlugUtils {
    private static final String TAG = "PlugUtils";
    private static final String BASE_DEX_NAME = "plug-base.apk";

    private static final Map<String, Plug> sCache = new HashMap<>();
    public static PlugManifest install(Context context, File plugFile, File installDir) {
        PlugManifest manifest = readManifest(context, plugFile);
        if (manifest == null) return null;
        installDir = new File(installDir, manifest.packageName);
        FileUtils.delete(installDir);
        if (!installDir.mkdirs()) {
            Log.e(TAG, "Failed to create dir: " + installDir);
            return null;
        }
        File plug = new File(installDir, BASE_DEX_NAME);
        manifest.installPath = installDir.getAbsolutePath();
        if (!plug.exists()) {
            try (FileInputStream from = new FileInputStream(plugFile);
                 FileOutputStream to = new FileOutputStream(plug)){
                FileUtils.copy(from, to);
                extractLibrary(plug, installDir);
                if (!plug.setReadOnly()) {
                    throw new IOException("Unable set file read only mode!");
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return null;
            }
        }
        boolean noError = install(context, manifest);
        if (!noError) {
            delete(installDir);
        }
        return  noError ? manifest : null;
    }

    @SuppressLint("PrivateApi")
    public static boolean install(Context context, PlugManifest manifest) {
        boolean noError = true;
        File dexFile = new File(manifest.installPath, BASE_DEX_NAME);
        assert dexFile.exists() && dexFile.isFile() && dexFile.canRead();
        DexClassLoader loader = new DexClassLoader(dexFile.getAbsolutePath(), null, new File(manifest.installPath, "lib").getAbsolutePath(), context.getClassLoader());
        Resources resources = null;
        try {
            Class<?> clazz = loader.loadClass(manifest.mainClass);
            AssetManager assetManager = AssetManager.class.newInstance();
            Method method = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            Object ret = method.invoke(assetManager, dexFile.getAbsolutePath());
            if (ret != null && ((int) ret) != 0) {
                resources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
            }
            Plug o = (Plug) clazz.newInstance();
            o.install(context, resources);
            o.setManifest(manifest);
            sCache.put(manifest.packageName, o);
        } catch (Throwable e) {
            noError = false;
            e.printStackTrace(System.err);
        }
        return noError;
    }

    public static boolean uninstall(@NonNull PlugManifest manifest) {
        Plug plug = sCache.get(manifest.packageName);
        if (plug != null) {
            plug.uninstall();
            sCache.remove(manifest.packageName);
        }
        File file = new File(manifest.installPath);
        return !file.exists() || delete(file);
    }

    public static boolean isInstanced(@NonNull PlugManifest manifest) {
        return sCache.get(manifest.packageName) != null;
    }

    public static @Nullable Drawable getPlugIcon(@NonNull Context context, @NonNull PlugManifest manifest) {
        PackageManager packageManager = context.getPackageManager();
        String plugPath = new File(manifest.installPath, BASE_DEX_NAME).getAbsolutePath();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(plugPath, PackageManager.GET_META_DATA);
        if (packageInfo == null || packageInfo.applicationInfo == null) return null;
        packageInfo.applicationInfo.publicSourceDir = plugPath;
        return packageManager.getApplicationIcon(packageInfo.applicationInfo);
    }

    private static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return file.delete();
            for (File child : files) {
                delete(child);
            }
        }
        return file.delete();
    }

    public static @Nullable PlugManifest readManifest(@NonNull Context context, @NonNull File plug) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(plug.getAbsolutePath(), PackageManager.GET_META_DATA);
        if (packageInfo == null) return null;
        String plugPackageName = packageInfo.packageName;
        if (packageInfo.applicationInfo == null) return null;
        Bundle metaData = packageInfo.applicationInfo.metaData;
        String plugName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
        String plugMainClass = metaData.getString("plugMainClass");
        String plugAuthor = metaData.getString("plugAuthor", "");
        String plugSummary = metaData.getString("plugSummary", "");
        if (plugMainClass != null){
            PlugManifest manifest = new PlugManifest(plugName, plugPackageName, plugMainClass, plugAuthor, plugSummary);
            manifest.versionName = packageInfo.versionName;
            manifest.versionCode = packageInfo.versionCode;
            return manifest;
        }
        return null;
    }
    private static void extractLibrary(File plug, File installDir) {
        String abi = Build.SUPPORTED_ABIS[0];
        installDir = new File(installDir, "lib");
        boolean mkdir = installDir.mkdir();
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(plug))){
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("lib/" + abi)) {
                    if (!entry.isDirectory()) {
                        File lib = new File(installDir, new File(entry.getName()).getName());
                        try (FileOutputStream fos = new FileOutputStream(lib)){
                            byte[] buffer = new byte[1024];
                            int readNumInBytes;
                            while ((readNumInBytes = zip.read(buffer)) != -1) {
                                fos.write(buffer, 0, readNumInBytes);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
