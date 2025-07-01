package ink.snowland.wkuwku.util;

import android.content.Context;
import android.content.res.XmlResourceParser;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.BiosFile;
import ink.snowland.wkuwku.exception.FileChecksumException;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;

public class BiosProvider {
    private BiosProvider() {
        throw new UnsupportedOperationException();
    }

    private static final Map<String, List<BiosFile>> BIOS_SOURCE = new HashMap<>();
    private static final String DOWNLOAD_OPTIONAL_BIOS = "app_download_optional_bios";

    public static void initialize(@NonNull Context context) {
        try {
            parseBiosConfig(context.getResources().getXml(R.xml.bios_config));
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static Completable downloadBiosForGame(@NonNull Game game, @NonNull File systemDir) {
        return Completable.create(emitter -> {
            List<BiosFile> files = BIOS_SOURCE.get(game.coreAlias);
            if (files == null || files.isEmpty()) {
                emitter.onComplete();
                return;
            }
            boolean downloadOptionalFile = SettingsManager.getBoolean(DOWNLOAD_OPTIONAL_BIOS, false);
            for (BiosFile bios : files) {
                File file = new File(systemDir, bios.name);
                if (!bios.required && !downloadOptionalFile || file.exists()) continue;
                URL url = new URL(bios.url);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                try (InputStream from = conn.getInputStream()) {
                    FileManager.copy(from, file);
                    String md5 = FileManager.calculateMD5Sum(file);
                    if (!bios.md5.equals(md5)) {
                        emitter.onError(new FileChecksumException(bios.md5, md5));
                        FileManager.delete(file);
                        return;
                    }
                } catch (Exception e) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                    e.printStackTrace(System.err);
                }
            }
            emitter.onComplete();
        });
    }

    private static void parseBiosConfig(@NonNull XmlResourceParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.START_TAG && "system".equals(parser.getName())) {
                parseSystemBios(parser);
            }
            event = parser.next();
        }
    }

    private static void parseSystemBios(@NonNull XmlResourceParser parser) throws XmlPullParserException, IOException {
        String systemTag = Objects.requireNonNull(parser.getAttributeValue(null, "tag"));
        ArrayList<BiosFile> biosFileList = new ArrayList<>();
        int event = parser.getEventType();
        String tagName = parser.getName();
        while (event != XmlResourceParser.END_TAG || !"system".equals(tagName)) {
            if (event == XmlResourceParser.START_TAG && "bios-file".equals(tagName)) {
                String url = parser.getAttributeValue(null, "url");
                String md5 = parser.getAttributeValue(null, "md5");
                if (url == null || md5 == null) continue;
                String filename = parser.getAttributeValue(null, "name");
                boolean required = parser.getAttributeBooleanValue(null, "required", false);
                biosFileList.add(new BiosFile(filename, url, md5, required));
            }
            event = parser.next();
            tagName = parser.getName();
        }
        BIOS_SOURCE.put(systemTag, biosFileList);
    }
}
