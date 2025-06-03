package ink.snowland.wkuwku.ui.plug;

import static ink.snowland.wkuwku.GlobalConfig.*;

import android.app.Application;
import android.util.Xml;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ink.snowland.wkuwku.bean.PlugRes;
import ink.snowland.wkuwku.common.BaseViewModel;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.db.entity.PlugManifestExt;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;

public class PlugViewModel extends BaseViewModel {
    public PlugViewModel(@NonNull Application application) {
        super(application);
    }

    public Observable<List<PlugManifestExt>> getInstalledPlug() {
        return AppDatabase.db.plugManifestExtDao().getAll();
    }

    public Completable update(@NonNull PlugManifestExt manifest) {
        return Completable.create(emitter -> {
            AppDatabase.db.plugManifestExtDao().update(manifest);
            emitter.onComplete();
        });
    }

    public Completable delete(@NonNull PlugManifestExt manifest) {
        return Completable.create(emitter -> {
            AppDatabase.db.plugManifestExtDao().delete(manifest);
            emitter.onComplete();
        });
    }

    public Single<List<PlugRes>> getAvailablePlugInfos() {
        return Single.create((SingleOnSubscribe<List<PlugRes>>) emitter -> {
            URL url = new URL(WEB_URL + "plug-list.xml");
            try (InputStream xml = url.openStream()){
                XmlPullParser xmlPullParser = Xml.newPullParser();
                xmlPullParser.setInput(xml, "utf-8");
                int eventType = xmlPullParser.getEventType();
                ArrayList<PlugRes> list = new ArrayList<>();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String tagName = xmlPullParser.getName();
                    if (eventType == XmlPullParser.START_TAG && "plug".equals(tagName)) {
                        PlugRes plugInfo = parsePlugInfo(xmlPullParser);
                        if (plugInfo == null) continue;
                        list.add(plugInfo);
                    }
                    eventType = xmlPullParser.next();
                }
                emitter.onSuccess(list);
            }
        });
    }

    private PlugRes parsePlugInfo(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        PlugRes info = new PlugRes();
        info.name = xmlPullParser.getAttributeValue(null, "name");
        if (info.name == null) return null;
        int eventType = xmlPullParser.getEventType();
        String tagName = xmlPullParser.getName();
        while (eventType != XmlPullParser.END_TAG || !"plug".equals(tagName)) {
            if (eventType == XmlPullParser.START_TAG) {
                if ("author".equals(tagName)) {
                    info.author = xmlPullParser.nextText();
                } else if ("iconPath".equals(tagName)) {
                    String iconPath = xmlPullParser.nextText();
                    if (iconPath == null) continue;
                    info.iconUrl = WEB_URL + iconPath;
                } else if ("filePath".equals(tagName)) {
                    String filePath = xmlPullParser.nextText();
                    if (filePath == null) return null;
                    info.url = WEB_URL + filePath;
                } else if ("packageName".equals(tagName)) {
                    info.packageName = xmlPullParser.nextText();
                } else if ("version".equals(tagName)) {
                    info.version = xmlPullParser.nextText();
                } else if ("md5".equals(tagName)) {
                    info.md5 = xmlPullParser.nextText();
                } else if ("summary".equals(tagName)) {
                    info.summary = xmlPullParser.nextText();
                }
            }
            eventType = xmlPullParser.next();
            tagName = xmlPullParser.getName();
        }
        if (info.md5 != null
        && info.packageName != null
        && info.url != null
        && info.name != null) {
            return info;
        }
        return null;
    }
}