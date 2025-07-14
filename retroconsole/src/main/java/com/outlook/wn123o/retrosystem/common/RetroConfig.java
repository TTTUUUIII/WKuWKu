package com.outlook.wn123o.retrosystem.common;

import android.content.res.XmlResourceParser;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RetroConfig {
    private RetroConfig(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        parseXmlConfig(parser);
    }

    public List<RetroOption> options = new ArrayList<>();
    public List<RetroSystem> systems = new ArrayList<>();
    public List<String> contentExtensions = new ArrayList<>();

    @Nullable
    public static RetroConfig fromXml(@NonNull File file) {
        if (file.exists() && file.canRead()) {
            try (FileInputStream fis = new FileInputStream(file)){
                XmlPullParser xmlPullParser = Xml.newPullParser();
                xmlPullParser.setInput(fis, "utf-8");
                return new RetroConfig(xmlPullParser);
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }

    private void parseXmlConfig(XmlPullParser parser) throws XmlPullParserException, IOException {
        while (parser.next() != XmlResourceParser.END_DOCUMENT) {
            int event = parser.getEventType();
            if (event == XmlResourceParser.START_TAG) {
                String name = parser.getName();
                switch (name) {
                    case "option":
                        RetroOption option = parseOption(parser);
                        if (option != null)
                            options.add(option);
                        break;
                    case "system":
                        RetroSystem system = parseSystem(parser);
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
    private RetroSystem parseSystem(XmlPullParser parser) {
        String systemName = parser.getAttributeValue(null, "name");
        String systemTag = parser.getAttributeValue(null, "tag");
        String manufacturer = parser.getAttributeValue(null, "manufacturer");
        if (systemName != null && systemTag != null && manufacturer != null) {
            return new RetroSystem(systemName, systemTag, manufacturer);
        }
        return null;
    }

    private RetroOption parseOption(XmlPullParser parser) throws XmlPullParserException, IOException {
        String key = parser.getAttributeValue(null, "key");
        String defaultValue = parser.getAttributeValue(null, "defaultValue");
        String title = parser.getAttributeValue(null, "title");
        String inputType = parser.getAttributeValue(null, "inputType");
        boolean enable = Boolean.parseBoolean(parser.getAttributeValue(null, "enable") == null ? "true" : parser.getAttributeValue(null, "enable"));
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
            RetroOption.Builder builder = RetroOption.builder(key, defaultValue)
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
