package ink.snowland.wkuwku.util;

import android.content.Context;

import java.io.File;

public class FileManager {
    public static final String ROM_DIRECTORY = "rom";
    public static final String STATE_DIRECTORY = "state";
    public static final String SAVE_DIRECTORY = "save";
    public static final String IMAGE_DIRECTORY = "img";
    public static final String SYSTEM_DIRECTORY = "system";

    private FileManager() {
    }

    private static Context sApplicationContext;

    public static File getCacheDirectory() {
        return sApplicationContext.getExternalCacheDir();
    }

    public static File getFileDirectory(String type) {
        return sApplicationContext.getExternalFilesDir(type);
    }

    public static File getPlugDirectory() {
        return sApplicationContext.getDir("plug", Context.MODE_PRIVATE);
    }

    public static File getFile(String type, String filename) {
        return new File(sApplicationContext.getExternalFilesDir(type), filename);
    }

    public static void initialize(Context context) {
        sApplicationContext = context;
        File cacheDir = getCacheDirectory();
        File[] files = cacheDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            FileUtils.delete(file);
        }
    }
}
