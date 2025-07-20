package ink.snowland.wkuwku;

import android.app.Application;
import android.os.Process;

import java.io.IOException;
import java.io.PrintWriter;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.BiosProvider;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.HotkeysManager;
import ink.snowland.wkuwku.util.PlugManager;
import ink.snowland.wkuwku.util.SettingsManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FileManager.initialize(getApplicationContext());
        AppDatabase.initialize(getApplicationContext());
        SettingsManager.initialize(getApplicationContext());
        HotkeysManager.initialize(getApplicationContext());
        EmulatorManager.initialize(getApplicationContext());
        BiosProvider.initialize(getApplicationContext());
        PlugManager.initialize(getApplicationContext());
        Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
    }

    private final Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = (thread, throwable) -> {
        try (PrintWriter writer = new PrintWriter(FileManager.getFile("crash", System.currentTimeMillis() + ".log"))){
            throwable.printStackTrace(writer);
        } catch (IOException ignored) {}
        Process.killProcess(Process.myPid());
    };
}
