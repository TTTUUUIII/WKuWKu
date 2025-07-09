package ink.snowland.wkuwku.bean;

import android.os.Build;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoreManifest {
    public final List<ManufacturerElement> manufacturers = new ArrayList<>();

    @Override
    public String toString() {
        return "CoreManifest{" +
                "manufacturers=" + manufacturers +
                '}';
    }

    public static class ManufacturerElement {
        public String name;
        public final List<SystemElement> systems = new ArrayList<>();

        @NonNull
        @Override
        public String toString() {
            return "ManufacturerElement{" +
                    "name='" + name + '\'' +
                    ", systems=" + systems +
                    '}';
        }
    }

    public static class SystemElement {
        public String name;
        public final List<CoreElement> cores = new ArrayList<>();

        @NonNull
        @Override
        public String toString() {
            return "SystemElement{" +
                    "name='" + name + '\'' +
                    ", cores=" + cores +
                    '}';
        }
    }

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

    public static CoreManifest fromConfig(XmlPullParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        String tagName;
        CoreManifest source = new CoreManifest();
        while (event != XmlPullParser.END_DOCUMENT) {
            tagName = parser.getName();
            if (event == XmlPullParser.START_TAG && "manufacturer".equals(tagName)) {
                source.manufacturers.add(parseManufacturerElement(parser));
            }
            event = parser.next();
        }
        return source;
    }

    private static ManufacturerElement parseManufacturerElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        ManufacturerElement manufacturer = new ManufacturerElement();
        manufacturer.name = parser.getAttributeValue(null, "name");
        int event = parser.getEventType();
        String tagName = parser.getName();
        while (event != XmlPullParser.END_TAG || !"manufacturer".equals(tagName)) {
            if (event == XmlPullParser.START_TAG && "system".equals(tagName)) {
                manufacturer.systems.add(parseSystemElement(parser));
            }
            event = parser.next();
            tagName = parser.getName();
        }
        return manufacturer;
    }

    private static SystemElement parseSystemElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        SystemElement system = new SystemElement();
        system.name = parser.getAttributeValue(null, "name");
        int event = parser.getEventType();
        String tagName = parser.getName();
        while (event != XmlPullParser.END_TAG || !"system".equals(tagName)) {
            if (event == XmlPullParser.START_TAG && "core".equals(tagName)) {
                CoreElement it = parseCoreElement(parser);
                if (it != null) {
                    system.cores.add(it);
                }
            }
            event = parser.next();
            tagName = parser.getName();
        }
        return system;
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