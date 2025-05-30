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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.Bios;
import ink.snowland.wkuwku.exception.FileChecksumException;
import ink.snowland.wkuwku.db.entity.Game;
import io.reactivex.rxjava3.core.Completable;

public class BiosProvider {
    private BiosProvider() {
        throw new UnsupportedOperationException();
    }

    private static final Map<String, Bios> BIOS = new HashMap<>();

    public static void initialize(@NonNull Context context) {
        try {
            parseBiosConfig(context.getResources().getXml(R.xml.bios_config));
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static Completable downloadBiosForGame(@NonNull Game game, @NonNull File systemDir) {
        return Completable.create(emitter -> {
            Bios bios = BIOS.get(game.system);
            if (bios == null) {
                emitter.onComplete();
                return;
            }
            File file = new File(systemDir, bios.filename);
            if (file.exists()) {
                emitter.onComplete();
                return;
            }
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
                emitter.onError(e);
                e.printStackTrace(System.err);
            }
            emitter.onComplete();
        });
    }

    private static void parseBiosConfig(@NonNull XmlResourceParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.START_TAG && "bios".equals(parser.getName())) {
                parseBios(parser);
            }
            event = parser.next();
        }
    }

    private static void parseBios(@NonNull XmlResourceParser parser) throws XmlPullParserException, IOException {
        String title = null;
        String url = null;
        String md5 = null;
        String filename = null;
        String system = Objects.requireNonNull(parser.getAttributeValue(null, "system"));
        int event = parser.getEventType();
        while (event != XmlResourceParser.END_TAG || !"bios".equals(parser.getName())) {
            String name = parser.getName();
            switch (name) {
                case "title":
                    title = parser.nextText();
                    break;
                case "url":
                    url = parser.nextText();
                    break;
                case "md5":
                    md5 = parser.nextText();
                    break;
                case "filename":
                    filename = parser.nextText();
                    break;
            }
            event = parser.next();
        }
        if (url != null && md5 != null) {
            BIOS.put(system, new Bios(title, url, md5, filename));
        }
    }
}
