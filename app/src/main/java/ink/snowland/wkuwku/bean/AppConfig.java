package ink.snowland.wkuwku.bean;

import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wkuwku.util.NumberUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.util.ResourceManager;

public class AppConfig {

    private static final String WEB_URL = ResourceManager.WEB_URL;
    private int mVersionCode;
    private String mVersionName;
    private String mMD5Sum;
    private String mDrawerHeroUrl;

    private AppConfig() {}

    public int getVersionCode() {
        return mVersionCode;
    }

    public String getVersionName() {
        return mVersionName == null ? BuildConfig.VERSION_NAME : mVersionName;
    }

    public String getMD5Sum() {
        return mMD5Sum == null ? "" : mMD5Sum;
    }

    @Nullable
    public String getDrawerHeroUrl() {
        return mDrawerHeroUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "AppConfig{" +
                ", mVersionCode=" + mVersionCode +
                ", mVersionName='" + mVersionName + '\'' +
                ", mMD5Sum='" + mMD5Sum + '\'' +
                ", mDrawerHeroUrl='" + mDrawerHeroUrl + '\'' +
                '}';
    }

    public static AppConfig from(InputStream conf) {
        XmlPullParser xmlPullParser = Xml.newPullParser();
        AppConfig config = new AppConfig();
        try {
            xmlPullParser.setInput(conf, "utf8");
            int eventType = xmlPullParser.getEventType();
            String tagName = xmlPullParser.getName();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (tagName) {
                        case "version":
                            config.mVersionCode = NumberUtils.parseInt(xmlPullParser.getAttributeValue(null, "versionCode"), 0);
                            config.mVersionName = xmlPullParser.getAttributeValue(null, "versionName");
                            config.mMD5Sum = xmlPullParser.getAttributeValue(null, "md5sum");
                            break;
                        case "drawer":
                            config.mDrawerHeroUrl = WEB_URL + xmlPullParser.getAttributeValue(null, "heroPath");
                            break;
                        default:
                    }
                }
                eventType = xmlPullParser.next();
                tagName = xmlPullParser.getName();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
        return config;
    }
}
