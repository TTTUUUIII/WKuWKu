package ink.snowland.wkuwku;

import android.app.Application;
import android.os.Process;

import com.outlook.wn123o.retrosystem.RetroSystem;

import java.io.IOException;
import java.io.PrintWriter;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.BiosProvider;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.PlugManager;
import ink.snowland.wkuwku.util.SettingsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FileManager.initialize(getApplicationContext());
        AppDatabase.initialize(getApplicationContext());
        SettingsManager.initialize(getApplicationContext());
        EmulatorManager.initialize(getApplicationContext());
        BiosProvider.initialize(getApplicationContext());
        PlugManager.initialize(getApplicationContext());
        Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);

        RetroSystem.setDirectory(RetroSystem.TYPE_SYSTEM, FileManager.getFileDirectory(FileManager.SYSTEM_DIRECTORY).getAbsolutePath());
        RetroSystem.setDirectory(RetroSystem.TYPE_SAVE, FileManager.getFileDirectory(FileManager.SAVE_DIRECTORY).getAbsolutePath());
        RetroSystem.setDirectory(RetroSystem.TYPE_CORE_ASSETS, FileManager.getCacheDirectory().getAbsolutePath());
        AppDatabase.db.gameCoreDao().getList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(it -> it.forEach(core -> RetroSystem.add(core.alias, core.path)))
                .subscribe();
    }

    private final Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = (thread, throwable) -> {
        try (PrintWriter writer = new PrintWriter(FileManager.getFile("crash", System.currentTimeMillis() + ".log"))) {
            throwable.printStackTrace(writer);
        } catch (IOException ignored) {
        }
        Process.killProcess(Process.myPid());
    };
}
