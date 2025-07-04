package ink.snowland.wkuwku.ui.coremag;

import android.app.Application;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ink.snowland.wkuwku.AppConfig;
import ink.snowland.wkuwku.common.BaseViewModel;

public class CoreMagViewModel extends BaseViewModel {
    public CoreMagViewModel(@NonNull Application application) {
        super(application);

        try (InputStream ins = new URL(AppConfig.WEB_URL + "all_cores_config.xml").openStream()){

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
