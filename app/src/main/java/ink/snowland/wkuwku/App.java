package ink.snowland.wkuwku;

import android.app.Application;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.BiosProvider;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.SettingsManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FileManager.initialize(this);
        AppDatabase.initialize(this);
        SettingsManager.initialize(this);
        try {
            BiosProvider.initialize(this);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
