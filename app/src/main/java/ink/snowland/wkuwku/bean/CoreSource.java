package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoreSource {
    public final List<ManufacturerElement> manufacturers = new ArrayList<>();

    @NonNull
    @Override
    public String toString() {
        return "CoreSource{" +
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
        public String url;
        public String md5sum;

        @NonNull
        @Override
        public String toString() {
            return "CoreElement{" +
                    "alias='" + alias + '\'' +
                    ", url='" + url + '\'' +
                    ", md5sum='" + md5sum + '\'' +
                    '}';
        }
    }

    public static CoreSource fromConfig(XmlPullParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        String tagName;
        CoreSource source = new CoreSource();
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
                CoreElement core = new CoreElement();
                core.alias = parser.getAttributeValue(null, "alias");
                core.url = parser.getAttributeValue(null, "url");
                core.md5sum = parser.getAttributeValue(null, "md5sum");
                system.cores.add(core);
            }
            event = parser.next();
            tagName = parser.getName();
        }
        return system;
    }
}