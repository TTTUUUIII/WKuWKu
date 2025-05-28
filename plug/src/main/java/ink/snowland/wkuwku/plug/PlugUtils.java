package ink.snowland.wkuwku.plug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.FileUtils;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;

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
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class PlugUtils {

    private static final Map<String, Plug> sCache = new HashMap<>();
    @SuppressLint("PrivateApi")
    public static PlugManifest install(Context context, File plugFile, File installDir) {
        PlugManifest manifest = readManifest(plugFile);
        if (manifest == null) return null;
        String plugName = plugFile.getName();
        installDir = new File(installDir, manifest.packageName);
        if (installDir.exists())
            return null;
        if (!installDir.mkdirs())
            return null;
        manifest.installPath = installDir.getAbsolutePath();
        File plug = new File(installDir, plugName);
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
        return install(context, manifest) ? manifest : null;
    }

    @SuppressLint("PrivateApi")
    public static boolean install(Context context, PlugManifest manifest) {
        boolean noError = true;
        DexClassLoader loader = new DexClassLoader(manifest.dexPath, null, new File(manifest.installPath, "lib/" + Build.SUPPORTED_ABIS[0]).getAbsolutePath(), context.getClassLoader());
        Resources resources = null;
        try {
            Class<?> clazz = loader.loadClass(manifest.mainClass);
            AssetManager assetManager = AssetManager.class.newInstance();
            Method method = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            Object ret = method.invoke(assetManager, manifest.dexPath);
            if (ret != null && ((int) ret) != 0) {
                resources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
            }
            Plug o = (Plug) clazz.newInstance();
            o.install(context, resources);
            sCache.put(manifest.packageName, o);
        } catch (Exception e) {
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
        return !file.exists() || file.delete();
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

    private static @Nullable PlugManifest readManifest(@NonNull File plug) {
        String plugPackageName = null;
        String plugMainClass = null;
        String plugAuthor = null;
        try (ZipFile zip = new ZipFile(plug)){
            ZipEntry entry = zip.getEntry("AndroidManifest.xml");
            if (entry != null) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(zip.getInputStream(entry), "utf-8");
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String tagName = parser.getName();
                    if (eventType == XmlPullParser.START_TAG && "meta-data".equals(tagName)) {
                        String key = parser.getAttributeValue("android", "name");
                        String value = parser.getAttributeValue("android", "value");
                        if ("plugMainClass".equals(key))
                            plugMainClass = value;
                        else if ("plugPackageName".equals(key))
                            plugPackageName = value;
                        else if ("plugAuthor".equals(key))
                            plugAuthor = value;
                    }
                    eventType = parser.next();
                }
            }
            if (plugPackageName != null && plugMainClass != null) {
                return new PlugManifest(plugPackageName, plugMainClass, plugAuthor);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
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
