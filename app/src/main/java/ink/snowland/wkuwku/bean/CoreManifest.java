package ink.snowland.wkuwku.bean;

import android.os.Build;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoreManifest {
    public final List<CoreElement> cores = new ArrayList<>();

    public static class CoreElement {
        public String alias;
        public List<FileElement> files = new ArrayList<>();

        @Override
        public String toString() {
            return "CoreElement{" +
                    "alias='" + alias + '\'' +
                    ", files=" + files +
                    '}';
        }
    }

    public static class FileElement {
        public String path;

        @Override
        public String toString() {
            return "FileElement{" +
                    "path='" + path + '\'' +
                    '}';
        }
    }

    public static CoreManifest fromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        String tagName;
        CoreManifest source = new CoreManifest();
        while (event != XmlPullParser.END_DOCUMENT) {
            tagName = parser.getName();
            if (event == XmlPullParser.START_TAG && "core".equals(tagName)) {
                source.cores.add(parseCoreElement(parser));
            }
            event = parser.next();
        }
        return source;
    }

    private static CoreElement parseCoreElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        CoreElement core = new CoreElement();
        core.alias = parser.getAttributeValue(null, "alias");
        int event = parser.getEventType();
        String tagName = parser.getName();
        while (event != XmlPullParser.END_TAG || !"core".equals(tagName)) {
            if (event == XmlPullParser.START_TAG && "file".equals(tagName)) {
                FileElement it = new FileElement();
                it.path = parser.getAttributeValue(null, "path");
                if (it.path != null) {
                    it.path = it.path.replace("${ABI}", Build.SUPPORTED_ABIS[0]);
                }
                core.files.add(it);
            }
            event = parser.next();
            tagName = parser.getName();
        }
        if (core.files.isEmpty()) {
            return null;
        }
        return core;
    }
}