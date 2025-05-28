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

import kotlin.io.FileAlreadyExistsException;

public class ArchiveUtils {
    public static final int FLAG_ARCHIVE_FILE_TYPE = 1;
    public static final int FLAG_SUPPORTED_ARCHIVE_FILE_TYPE = 2;

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

    public static String extract(@NonNull File file) throws IOException {
        File parent = file.getParentFile();
        assert parent != null;
        return extract(parent.getAbsolutePath(), file);
    }

    public static String extract(String outputPath, String archive) throws IOException {
        return extract(outputPath, new File(archive));
    }

    public static String extract(String outputPath, File archive) throws IOException {
        if (!archive.exists() || !archive.isFile()) {
            throw new RuntimeException(new FileNotFoundException(archive + " not found!"));
        }
        String filename = archive.getName();
        File outputDir = getOutputFile(outputPath, filename);
        if (filename.endsWith(".zip"))
            extractZip(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".jar"))
            extractJar(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".7z"))
            extractSevenZ(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".tar"))
            extractTar(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".ar"))
            extractAr(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".tar.gz"))
            extractTarGz(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".tar.xz"))
            extractTarXz(outputDir.getAbsolutePath(), archive);
        else
            throw new UnsupportedOperationException();
        return outputDir.getAbsolutePath();
    }

    @NonNull
    private static File getOutputFile(String base, String filename) throws IOException {
        String filenameNotExt = filename.substring(0, filename.lastIndexOf("."));
        if (filenameNotExt.endsWith(".tar"))
            filenameNotExt = filename.substring(0, filenameNotExt.lastIndexOf("."));
        File outputDir = new File(base, filenameNotExt);
        if (outputDir.exists())
            throw new FileAlreadyExistsException(outputDir, null, null);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory! \"" + outputDir + "\"");
        }
        return outputDir;
    }

    private static void extractJar(String output, File file) throws IOException {
        try (JarArchiveInputStream archive = new JarArchiveInputStream(new FileInputStream(file))) {
            extract(output, archive);
        }
    }

    private static void extractTarXz(String output, File file) throws IOException {
        try (XZCompressorInputStream archive = new XZCompressorInputStream(new FileInputStream(file));
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(archive)) {
            extract(output, tarArchiveInputStream);
        }
    }

    private static void extractTarGz(String output, File file) throws IOException {
        try (GzipCompressorInputStream archive = new GzipCompressorInputStream(new FileInputStream(file));
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(archive)) {
            extract(output, tarArchiveInputStream);
        }
    }

    private static void extractAr(String output, File file) throws IOException {
        try (ArArchiveInputStream archive = new ArArchiveInputStream(new FileInputStream(file))) {
            extract(output, archive);
        }
    }

    private static void extractTar(String output, File file) throws IOException {
        try (TarArchiveInputStream archive = new TarArchiveInputStream(new FileInputStream(file))) {
            extract(output, archive);
        }
    }

    private static void extractSevenZ(String output, File file) throws IOException {
        try (SevenZFile archive = new SevenZFile.Builder()
                .setFile(file)
                .get()) {
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                File item = new File(output, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    try (FileOutputStream fos = new FileOutputStream(new File(output, entry.getName()))) {
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

    private static void extractZip(String output, File file) throws IOException {
        try (ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(file))) {
            extract(output, archive);
        }
    }

    private static void extract(String output, ArchiveInputStream<?> archiveInputStream) throws IOException {
        ArchiveEntry entry;
        try {
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                File item = new File(output, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    File parent = item.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        System.err.println("Failed to create directory! \"" + item + "\"");
                        return;
                    }
                    try (FileOutputStream fos = new FileOutputStream(new File(output, entry.getName()))) {
                        byte[] buffer = new byte[1024];
                        int readNumInBytes;
                        while ((readNumInBytes = archiveInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, readNumInBytes);
                        }
                    }
                }
            }
        } catch (IOException e) {
            FileManager.delete(output);
            throw e;
        }
    }
}
