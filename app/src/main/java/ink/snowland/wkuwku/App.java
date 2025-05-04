package ink.snowland.wkuwku;

import android.app.Application;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.FileManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FileManager.initialize(this);
        AppDatabase.initialize(this);
    }
}
