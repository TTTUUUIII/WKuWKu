package ink.snowland.wkuwku.ui.plug;

import static ink.snowland.wkuwku.AppConfig.*;

import android.app.Application;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import ink.snowland.wkuwku.util.NumberUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.MutableStateFlow;

public class PlugViewModel extends BaseViewModel {
    private final MutableLiveData<List<PlugManifestExt>> mInstalledPlugs = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCurrentPagePosition = new MutableLiveData<>(0);
    private final Disposable mDisposable;
    public PlugViewModel(@NonNull Application application) {
        super(application);
        mDisposable = AppDatabase.db.plugManifestExtDao().getAll()
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(mInstalledPlugs::postValue, error -> error.printStackTrace(System.err));

    }

    public LiveData<List<PlugManifestExt>> getAll() {
        return mInstalledPlugs;
    }

    public LiveData<Integer> getPagePosition() {
        return mCurrentPagePosition;
    }

    public void updatePagePosition(int position) {
        mCurrentPagePosition.postValue(position);
    }

    public PlugManifestExt findInstalledPlug(@NonNull String packageName) {
        List<PlugManifestExt> plugs = mInstalledPlugs.getValue();
        if (plugs == null) return null;
        for (PlugManifestExt plug : plugs) {
            if (plug.packageName.equals(packageName)) {
                return plug;
            }
        }
        return null;
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
                        if (plugInfo != null) {
                            list.add(plugInfo);
                        }
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
                    info.versionCode = NumberUtils.parseInt(xmlPullParser.getAttributeValue(null, "versionCode"), 0);
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

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.dispose();
    }
}