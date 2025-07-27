package ink.snowland.wkuwku.util;

import androidx.annotation.NonNull;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.*;

import ink.snowland.wkuwku.common.ActionListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ArchiveUtils {
    public static final int FLAG_ARCHIVE_TYPE = 1;
    public static final int FLAG_SUPPORTED = 2;

    public static int getFileInfoMask(@NonNull String filename) {
        if (filename.endsWith(".zip")
                || filename.endsWith(".7z")
                || filename.endsWith(".tar")
                || filename.endsWith(".ar")
                || filename.endsWith(".jar")
                || filename.endsWith(".tar.gz")
                || filename.endsWith(".tar.xz")) {
            return 0x03;
        } else if (filename.endsWith(".rar")) {
            return 0x01;
        }
        return 0;
    }

    public static boolean isArchiveType(File file) {
        int mask = getFileInfoMask(file.getName());
        return (mask & FLAG_ARCHIVE_TYPE) == FLAG_ARCHIVE_TYPE;
    }

    public static boolean isSupported(File file) {
        int mask = getFileInfoMask(file.getName());
        return (mask & FLAG_SUPPORTED) == FLAG_SUPPORTED;
    }

    public static void asyncExtract(@NonNull File archive, @NonNull File out, @NonNull ActionListener listener) {
        Completable.create(emitter -> {
                    extract(archive, out);
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(listener::onSuccess)
                .doOnError(listener::onFailure)
                .onErrorComplete()
                .subscribe();
    }

    public static void extract(File archive, File out) throws IOException {
        if (!archive.exists() || !archive.isFile()) {
            throw new RuntimeException(new FileNotFoundException(archive + " not found!"));
        }
        String filename = archive.getName();
        if (filename.endsWith(".zip"))
            extractZip(out, archive);
        else if (filename.endsWith(".jar"))
            extractJar(out, archive);
        else if (filename.endsWith(".7z"))
            extractSevenZ(out, archive);
        else if (filename.endsWith(".tar"))
            extractTar(out, archive);
        else if (filename.endsWith(".ar"))
            extractAr(out, archive);
        else if (filename.endsWith(".tar.gz"))
            extractTarGz(out, archive);
        else if (filename.endsWith(".tar.xz"))
            extractTarXz(out, archive);
        else
            throw new UnsupportedOperationException();
    }

    private static void extractJar(File out, File file) throws IOException {
        try (JarArchiveInputStream archive = new JarArchiveInputStream(new FileInputStream(file))) {
            extract(out, archive);
        }
    }

    private static void extractTarXz(File out, File file) throws IOException {
        try (XZCompressorInputStream archive = new XZCompressorInputStream(new FileInputStream(file));
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(archive)) {
            extract(out, tarArchiveInputStream);
        }
    }

    private static void extractTarGz(File out, File file) throws IOException {
        try (GzipCompressorInputStream archive = new GzipCompressorInputStream(new FileInputStream(file));
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(archive)) {
            extract(out, tarArchiveInputStream);
        }
    }

    private static void extractAr(File out, File file) throws IOException {
        try (ArArchiveInputStream archive = new ArArchiveInputStream(new FileInputStream(file))) {
            extract(out, archive);
        }
    }

    private static void extractTar(File out, File file) throws IOException {
        try (TarArchiveInputStream archive = new TarArchiveInputStream(new FileInputStream(file))) {
            extract(out, archive);
        }
    }

    private static void extractSevenZ(File out, File file) throws IOException {
        try (SevenZFile archive = new SevenZFile.Builder()
                .setFile(file)
                .get()) {
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                File item = new File(out, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    try (FileOutputStream fos = new FileOutputStream(new File(out, entry.getName()))) {
                        byte[] buffer = new byte[1024];
                        int readNumInBytes;
                        while ((readNumInBytes = archive.read(buffer)) != -1) {
                            fos.write(buffer, 0, readNumInBytes);
                        }
                    }
                }
            }
        }
    }

    private static void extractZip(File out, File file) throws IOException {
        try (ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(file))) {
            extract(out, archive);
        }
    }

    private static void extract(File out, ArchiveInputStream<?> archiveInputStream) throws IOException {
        ArchiveEntry entry;
        try {
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                File item = new File(out, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    File parent = item.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        System.err.println("Failed to create directory! \"" + item + "\"");
                        return;
                    }
                    try (FileOutputStream fos = new FileOutputStream(new File(out, entry.getName()))) {
                        byte[] buffer = new byte[1024];
                        int readNumInBytes;
                        while ((readNumInBytes = archiveInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, readNumInBytes);
                        }
                    }
                }
            }
        } catch (IOException e) {
            FileUtils.delete(out);
            throw e;
        }
    }
}
