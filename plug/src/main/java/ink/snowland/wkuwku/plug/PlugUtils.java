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
import android.os.FileUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class PlugUtils {

    private static final Map<String, Plug> sCache = new HashMap<>();
    public static PlugManifest install(Context context, File plugFile, File installDir) {
        PlugManifest manifest = readManifest(context, plugFile);
        if (manifest == null) return null;
        String plugName = plugFile.getName();
        installDir = new File(installDir, manifest.packageName);
        if (installDir.exists())
            return null;
        if (!installDir.mkdirs())
            return null;
        File plug = new File(installDir, plugName);
        manifest.installPath = installDir.getAbsolutePath();
        manifest.dexFileName = plug.getName();
        if (!plug.exists()) {
            try (FileInputStream from = new FileInputStream(plugFile);
                 FileOutputStream to = new FileOutputStream(plug)){
                copy(from, to);
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
        File dexFile = new File(manifest.installPath, manifest.dexFileName);
        assert dexFile.exists() && dexFile.isFile() && dexFile.canRead();
        DexClassLoader loader = new DexClassLoader(dexFile.getAbsolutePath(), null, new File(manifest.installPath, "lib/" + Build.SUPPORTED_ABIS[0]).getAbsolutePath(), context.getClassLoader());
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
        String plugPath = new File(manifest.installPath, manifest.dexFileName).getAbsolutePath();
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

    private static void copy(InputStream from, OutputStream to) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FileUtils.copy(from, to);
        } else {
            byte[] buffer = new byte[1024];
            int readNumInBytes;
            while ((readNumInBytes = from.read(buffer)) != -1) {
                to.write(buffer, 0, readNumInBytes);
            }
        }
    }

    private static @Nullable PlugManifest readManifest(@NonNull Context context, @NonNull File plug) {
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
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(plug))){
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("lib/" + abi)) {
                    File item = new File(installDir, entry.getName());
                    if (entry.isDirectory()) {
                        boolean success = item.mkdirs();
                    } else {
                        File parent = item.getParentFile();
                        if (parent != null && !parent.exists()) {
                            boolean success = parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(item)){
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
