package ink.snowland.wkuwku.util;

import androidx.annotation.NonNull;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.*;

public class ArchiveUtils {

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
        String filenameNotExt = filename.substring(0, filename.lastIndexOf("."));
        File outputDir = new File(outputPath, filenameNotExt);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory! \"" + outputPath + "\"");
        }
        if (filename.endsWith(".zip"))
            extractZip(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".7z"))
            extractSeven7(outputDir.getAbsolutePath(), archive);
        else if (filename.endsWith(".tar"))
            extractTar(outputDir.getAbsolutePath(), archive);
        else
            throw new UnsupportedOperationException();
        return outputDir.getAbsolutePath();
    }

    private static void extractTar(String output, File file) throws IOException{
        try (TarArchiveInputStream archive = new TarArchiveInputStream(new FileInputStream(file))){
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                File item = new File(output, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    try (FileOutputStream fos = new FileOutputStream(new File(output, entry.getName()))){
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

    private static void extractSeven7(String output, File file) throws IOException {
        try (SevenZFile archive = new SevenZFile.Builder()
                .setFile(file)
                .get()){
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                File item = new File(output, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    try (FileOutputStream fos = new FileOutputStream(new File(output, entry.getName()))){
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
        try (ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(file))){
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                File item = new File(output, entry.getName());
                if (entry.isDirectory()) {
                    if (!item.mkdirs())
                        System.err.println("Failed to create directory! \"" + item + "\"");
                } else {
                    try (FileOutputStream fos = new FileOutputStream(new File(output, entry.getName()))){
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
}
