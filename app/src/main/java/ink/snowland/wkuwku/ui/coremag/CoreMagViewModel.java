package ink.snowland.wkuwku.ui.coremag;

import android.app.Application;
import android.os.Build;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ink.snowland.wkuwku.AppConfig;
import ink.snowland.wkuwku.bean.CoreManifest;
import ink.snowland.wkuwku.common.BaseViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CoreMagViewModel extends BaseViewModel {
    private final Disposable mDisposable;
    private final MutableLiveData<CoreManifest> mCoreManifest = new MutableLiveData<>();
    public CoreMagViewModel(@NonNull Application application) {
        super(application);
        mDisposable = fetchManifest()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((manifest, error) -> {
                    if (error != null) {
                        error.printStackTrace(System.err);
                        return;
                    }
                    mCoreManifest.postValue(manifest);
                });
    }

    public LiveData<CoreManifest> getManifest() {
        return mCoreManifest;
    }
    private Single<CoreManifest> fetchManifest() {
        return Single.create(emitter -> {
            try (InputStream fis = new URL(AppConfig.WEB_URL + "core-list.xml").openStream()){
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, "utf8");
                CoreManifest manifest = CoreManifest.fromXml(parser);
                emitter.onSuccess(manifest);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }
}
