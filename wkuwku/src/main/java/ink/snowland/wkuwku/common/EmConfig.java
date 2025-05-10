package ink.snowland.wkuwku.common;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmConfig {
    private EmConfig(@NonNull XmlResourceParser parser) throws XmlPullParserException, IOException {
        parseXmlConfig(parser);
    }

    public List<EmOption> options = new ArrayList<>();
    public List<EmSystem> systems = new ArrayList<>();
    public List<String> contentExtensions = new ArrayList<>();

    public static EmConfig fromXmlConfig(@NonNull Resources res, @XmlRes int resId) throws XmlPullParserException, IOException {
       return new EmConfig(res.getXml(resId));
    }

    private void parseXmlConfig(XmlResourceParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlResourceParser.END_DOCUMENT) {
            int event = parser.getEventType();
            if (event == XmlResourceParser.START_TAG) {
                String name = parser.getName();
                switch (name) {
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

    private void parseContentExtensions(XmlResourceParser parser) throws XmlPullParserException, IOException {
        String name = parser.getName();
        int event = parser.getEventType();
        while (event != XmlResourceParser.END_TAG || !"content-extensions".equals(name)) {
            if (event == XmlResourceParser.START_TAG && "item".equals(name)) {
                String ext = parser.nextText();
                if (ext != null) {
                    contentExtensions.add(ext);
                }
            }
            event = parser.next();
            name = parser.getName();
        }
    }
    private EmSystem parseSystem(XmlResourceParser parser) {
        String systemName = parser.getAttributeValue(null, "name");
        String systemTag = parser.getAttributeValue(null, "tag");
        String manufacturer = parser.getAttributeValue(null, "manufacturer");
        if (systemName != null && systemTag != null && manufacturer != null) {
            return new EmSystem(systemName, systemTag, manufacturer);
        }
        return null;
    }

    private EmOption parseOption(XmlResourceParser parser) throws XmlPullParserException, IOException {
        String key = parser.getAttributeValue(null, "key");
        String defaultValue = parser.getAttributeValue(null, "defaultValue");
        String title = parser.getAttributeValue(null, "title");
        boolean enable = parser.getAttributeBooleanValue(null, "enable", true);
        int event = parser.getEventType();
        String name = parser.getName();
        List<String> allowValues = null;
        while (event != XmlResourceParser.END_TAG || !"option".equals(name)) {
            if (event == XmlResourceParser.START_TAG) {
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
            if (allowValues != null) {
                builder.setAllowVals(allowValues.toArray(new String[0]));
            }
            return builder.build();
        }
        return null;
    }
}
