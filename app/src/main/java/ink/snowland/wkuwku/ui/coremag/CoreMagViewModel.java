package ink.snowland.wkuwku.ui.coremag;

import android.app.Application;
import android.content.res.XmlResourceParser;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.bean.CoreSource;
import ink.snowland.wkuwku.common.BaseViewModel;

public class CoreMagViewModel extends BaseViewModel {
    public CoreMagViewModel(@NonNull Application application) {
        super(application);

        try (XmlResourceParser xml = application.getResources().getXml(R.xml.core_list)){
            CoreSource source = CoreSource.fromConfig(xml);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace(System.err);
        }
    }
}
