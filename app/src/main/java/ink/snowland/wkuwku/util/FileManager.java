package ink.snowland.wkuwku.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

import ink.snowland.wkuwku.common.ActionListener;
import ink.snowland.wkuwku.common.OnProgressListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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

    public static void delete(String type, String filename) {
        File file = new File(sApplicationContext.getExternalFilesDir(type), filename);
        delete(file);
    }

    public static void delete(String filepath) {
        delete(new File(filepath));
    }

    public static void delete(File file) {
        if (!file.exists()) return;
        boolean deleted;
        if (file.isDirectory()) {
            deleted = deleteDirectory(file);
        } else {
            deleted = file.delete();
        }
        if (!deleted) {
            Log.e(TAG, "Unable delete file " + file.getPath());
        }
    }

    private static boolean deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return file.delete();
            for (File child : files) {
                deleteDirectory(child);
            }
        }
        return file.delete();
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

    public static void copy(@NonNull URL url, @NonNull File file, @Nullable OnProgressListener listener) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(1000 * 5);
        conn.setReadTimeout(1000 * 8);
        final long total = NumberUtils.parseLong(conn.getHeaderField("Content-Length"), 1);
        if (listener != null) {
            copy(conn.getInputStream(), file, (progress, max) -> {
                listener.update(progress, total);
            });
        } else {
            copy(conn.getInputStream(), file, null);
        }
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

    public static void copyAsync(@NonNull Uri uri, @NonNull File file, @Nullable ActionListener listener) {
        Completable.create(emitter -> {
                    try (InputStream from = sApplicationContext.getContentResolver().openInputStream(uri)) {
                        copy(from, file);
                        emitter.onComplete();
                    } catch (IOException e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .doOnError((error) -> {
                    delete(file);
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                })
                .subscribe();
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
