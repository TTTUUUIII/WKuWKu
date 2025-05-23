package ink.snowland.wkuwku.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.FileUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import ink.snowland.wkuwku.common.OnProgressListener;

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
        if (!file.exists()) return;
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean delete = file.delete();
        }
    }

    private static void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File child : files) {
                deleteDirectory(child);
            }
        }
        boolean delete = file.delete();
    }

    public static boolean copy(String type, String filename, Uri uri) {
        try (InputStream from = sApplicationContext.getContentResolver().openInputStream(uri)) {
            if (from == null) return false;
            copy(from, new File(sApplicationContext.getExternalFilesDir(type), filename), null);
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    public static void copy(InputStream from, String type, String filename) throws IOException {
        copy(from, new File(sApplicationContext.getExternalFilesDir(type), filename), null);
    }
    public static void copy(InputStream from, String type, String filename, @Nullable OnProgressListener listener) throws IOException {
        copy(from, new File(sApplicationContext.getExternalFilesDir(type), filename), listener);
    }

    public static void copy(InputStream from, @NonNull File file) throws IOException {
        copy(from, file, null);
    }

    public static void copy(InputStream from, @NonNull File file, @Nullable OnProgressListener listener) throws IOException {
        long total = from.available();
        long read = 0;
        try (FileOutputStream to = new FileOutputStream(file)) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                FileUtils.copy(from, to);
//            } else {
            byte[] buffer = new byte[1024];
            int readNumInBytes;
            while ((readNumInBytes = from.read(buffer)) != -1) {
                to.write(buffer, 0, readNumInBytes);
                read += readNumInBytes;
                if (listener != null)
                    listener.update(read, total);
            }
//            }
        } catch (IOException e) {
            delete(file);
            throw e;
        }
    }

    public static String calculateMD5Sum(@NonNull String path) {
        return calculateMD5Sum(new File(path));
    }

    public static String calculateMD5Sum(@NonNull File file) {
        if (!file.exists() || !file.isFile() || !file.canRead()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }

            fis.close();
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return "";
    }

    public static String getExtension(@NonNull String filename, boolean includeDot) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return includeDot ? filename.substring(dotIndex) : filename.substring(dotIndex + 1);
    }

    public static void initialize(Context context) {
        sApplicationContext = context;
    }

    private static final String TAG = FileManager.class.getSimpleName();
}
