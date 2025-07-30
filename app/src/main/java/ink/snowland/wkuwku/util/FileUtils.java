package ink.snowland.wkuwku.util;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;

import ink.snowland.wkuwku.common.ActionListener;
import ink.snowland.wkuwku.common.OnProgressListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FileUtils {
    private static final Logger logger = new Logger("Utils", "FileUtils");
    private FileUtils() {}

    public static String getName(String path) {
        return getName(path, true);
    }

    public static String getName(String path, boolean includeExtension) {
        return getName(new File(path), includeExtension);
    }

    public static String getName(File file, boolean includeExtension) {
        String name = file.getName();
        if (!includeExtension) {
            int index = name.indexOf(".");
            if (index != -1) {
                name = name.substring(0, index);
            }
        }
        return name;
    }

    public static String getMD5Sum(@NonNull File file) {
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

    public static void delete(String file) {
        delete(new File(file));
    }

    public static void asyncDelete(String ...files) {
        Completable.create(emitter -> {
                    delete(Arrays.stream(files)
                            .map(File::new)
                            .toArray(File[]::new));
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public static void asyncDelete(File ...files) {
        Completable.create(emitter -> {
                    delete(files);
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public static void delete(File ...files) {
        for (File file: files) {
            if (!file.exists()) return;
            if (file.isDirectory()) {
                File[] list = file.listFiles();
                if (list != null) {
                    for (File it : list) {
                        delete(it);
                    }
                }
                if (!file.delete()) {
                    logger.e("Unable delete directory at %s", file);
                }
            } else if (!file.delete()){
                logger.e("Unable to delete file at %s.", file);
            }
        }
    }

    public static void asyncCopy(InputStream from, @NonNull File file, @Nullable ActionListener listener) {
        Completable.create(emitter -> {
                    copy(from, file);
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .doOnError((error) -> {
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                })
                .doFinally(from::close)
                .subscribe();
    }

    public static boolean copy(InputStream from, File file) {
        try (FileOutputStream to = new FileOutputStream(file)){
            return copy(from, to);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            delete(file);
        }
        return false;
    }

    public static boolean copy(InputStream from, OutputStream to) {
        try {
            copy(from, to, null);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            logger.e("Failed to copy file.");
            return false;
        }
        return true;
    }
    public static void copy(InputStream from, OutputStream to, @Nullable OnProgressListener listener) throws IOException {
        byte[] buffer = new byte[2048];
        int totalSize = from.available();
        int readBytes = 0;
        int readNumInBytes;
        while ((readNumInBytes = from.read(buffer)) != -1) {
            to.write(buffer, 0, readNumInBytes);
            readBytes += readNumInBytes;
            if (listener != null) {
                listener.update(readBytes, totalSize);
            }
        }
    }
}
