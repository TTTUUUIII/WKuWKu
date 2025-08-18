package ink.snowland.wkuwku.common;

import android.content.res.XmlResourceParser;

import androidx.annotation.NonNull;

import org.wkuwku.util.NumberUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmConfig {
    private EmConfig(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        parseXmlConfig(parser);
    }

    private float mVersion = 1.0f;
    public List<EmOption> options = new ArrayList<>();
    public List<EmSystem> systems = new ArrayList<>();
    public List<String> contentExtensions = new ArrayList<>();

    public static EmConfig fromXml(XmlPullParser xmlPullParser) {
        try {
            return new EmConfig(xmlPullParser);
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseXmlConfig(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlResourceParser.END_DOCUMENT) {
            int event = parser.getEventType();
            if (event == XmlResourceParser.START_TAG) {
                String name = parser.getName();
                switch (name) {
                    case "em-config":
                        mVersion = NumberUtils.parseFloat(parser.getAttributeValue(null, "version"), 1.0f);
                        break;
                    case "option":
                        EmOption option = parseOption(parser);
                        if (option != null)
                            options.add(option);
                        break;
                    case "system":
                        EmSystem system = parseSystem(parser);
                        if (system != null)
                            systems.add(system);
                        break;
                    case "content-extensions":
                        parseContentExtensions(parser);
                        break;
                    default:
                        /*ignored*/
                }
            }
        }
    }

    private void parseContentExtensions(XmlPullParser parser) throws XmlPullParserException, IOException {
        String name = parser.getName();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_TAG || !"content-extensions".equals(name)) {
            if (event == XmlPullParser.START_TAG && "item".equals(name)) {
                String ext = parser.nextText();
                if (ext != null) {
                    contentExtensions.add(ext);
                }
            }
            event = parser.next();
            name = parser.getName();
        }
    }
    private EmSystem parseSystem(XmlPullParser parser) {
        String systemName = parser.getAttributeValue(null, "name");
        String systemTag = parser.getAttributeValue(null, "tag");
        String manufacturer = parser.getAttributeValue(null, "manufacturer");
        if (systemName != null && systemTag != null && manufacturer != null) {
            EmSystem system = new EmSystem(systemName, systemTag, manufacturer);
            try {
                int event = parser.next();
                String tagName = parser.getName();
                while (event != XmlPullParser.END_TAG || !"system".equals(tagName)) {
                    if (event == XmlPullParser.START_TAG && "bios".equals(tagName)) {
                        String name = parser.getAttributeValue(null, "name");
                        String url = parser.getAttributeValue(null, "url");
                        String md5 = parser.getAttributeValue(null, "md5");
                        if (url != null && md5 != null) {
                            system.biosFiles.add(new EmBiosFile(name, url, md5));
                        }
                    }
                    event = parser.next();
                    tagName = parser.getName();
                }
                return system;
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace(System.err);
                return null;
            }
        }
        return null;
    }

    private EmOption parseOption(XmlPullParser parser) throws XmlPullParserException, IOException {
        String key = parser.getAttributeValue(null, "key");
        String defaultValue = null;
        if (mVersion > 1.0) {
            defaultValue = parser.getAttributeValue(null, "value");
        } else {
            defaultValue = parser.getAttributeValue(null, "defaultValue");
        }
        String title = parser.getAttributeValue(null, "title");
        String inputType = parser.getAttributeValue(null, "inputType");
        String text = parser.getAttributeValue(null, "enable");
        boolean enable = true;
        if (text != null) {
            enable = Boolean.parseBoolean(text);
        }
        int event = parser.getEventType();
        String name = parser.getName();
        List<String> allowValues = null;
        while (event != XmlPullParser.END_TAG || !"option".equals(name)) {
            if (event == XmlPullParser.START_TAG) {
                if ("allow-values".equals(name)) {
                    allowValues = new ArrayList<>();
                } else if ("value".equals(name)) {
                    assert allowValues != null;
                    allowValues.add(parser.nextText());
                }
            }
            event = parser.next();
            name = parser.getName();
        }
        if (key != null && defaultValue != null && title != null) {
            EmOption.Builder builder = EmOption.builder(key, defaultValue)
                    .setTitle(title)
                    .setEnable(enable);
            if (allowValues != null)
                builder.setAllowVals(allowValues.toArray(new String[0]));
            if (inputType != null)
                builder.setInputType(inputType);
            return builder.build();
        }
        return null;
    }
}
