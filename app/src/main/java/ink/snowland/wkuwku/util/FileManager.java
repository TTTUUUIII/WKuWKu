package ink.snowland.wkuwku.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManager {
    public static final String ROM_DIRECTORY = "rom";
    private FileManager() {}
    private static Context sApplicationContext;
    public static File getFile(String type, String filename) {
        return new File(sApplicationContext.getExternalFilesDir(type), filename);
    }


    public static void delete(String type, String filename) {
        File file = new File(sApplicationContext.getExternalFilesDir(type), filename);
        delete(file);
    }

    public static void delete(String filepath) {
        delete(new File(filepath));
    }

    public static void delete(File file) {
        if (file.exists() && !file.delete()) {
            Log.e(TAG, "ERROR: failed to delete file \"" + file + "\"");
        }
    }

    public static boolean copy(String type, String filename, Uri uri) {
        try (InputStream from = sApplicationContext.getContentResolver().openInputStream(uri)) {
            if (from == null) return false;
            try (FileOutputStream to = new FileOutputStream(new File(sApplicationContext.getExternalFilesDir(type), filename))){
                copy(from, to);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    public static void copy(InputStream from, OutputStream to) throws IOException {
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

    public static void initialize(Context context) {
        sApplicationContext = context;
    }

    private static final String TAG = FileManager.class.getSimpleName();
}
