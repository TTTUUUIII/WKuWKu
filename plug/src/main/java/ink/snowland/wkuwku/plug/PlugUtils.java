package ink.snowland.wkuwku.plug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class PlugUtils {

    @SuppressLint("PrivateApi")
    public static boolean install(Context context, PlugManifest manifest, File installDir) {
        File file = new File(manifest.path);
        String plugName = file.getName();
        File plug = new File(installDir, plugName);
        boolean noError = true;
        if (!plug.exists()) {
            try (FileInputStream from = new FileInputStream(file);
                 FileOutputStream to = new FileOutputStream(plug)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FileUtils.copy(from, to);
                } else {
                    byte[] buffer = new byte[1024];
                    int readNumInBytes;
                    while ((readNumInBytes = from.read(buffer)) != -1) {
                        to.write(buffer, 0, readNumInBytes);
                    }
                }
                noError = plug.setReadOnly();
                extractLibrary(plug, installDir);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                noError = false;
            }
        }
        if (!noError) return false;
        DexClassLoader loader = new DexClassLoader(plug.getAbsolutePath(), null, new File(installDir, "lib/" + Build.SUPPORTED_ABIS[0]).getAbsolutePath(), context.getClassLoader());
        Resources resources = null;
        try {
            Class<?> clazz = loader.loadClass(manifest.mainClass);
            AssetManager assetManager = AssetManager.class.newInstance();
            Method method = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            Object ret = method.invoke(assetManager, plug.getAbsolutePath());
            if (ret != null && ((int) ret) != 0) {
                resources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
            }
            Plug o = (Plug) clazz.newInstance();
            o.install(context, resources);
        } catch (Exception e) {
            noError = false;
            e.printStackTrace(System.err);
        }
        return noError;
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
                        item.mkdirs();
                    } else {
                        File parent = item.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
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
