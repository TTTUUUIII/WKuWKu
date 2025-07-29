package ink.snowland.wkuwku;

import android.app.Application;
import android.os.Process;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.HotkeysManager;
import ink.snowland.wkuwku.util.NotificationManager;
import ink.snowland.wkuwku.util.PlugManager;
import ink.snowland.wkuwku.util.ResourceManager;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.CheckVersionWorker;

public class App extends Application {

    private static final String NEW_VERSION_NOTIFICATION = "app_new_version_notification";
    private static final String WORK_ID_VERSION_CHECK = "weekly_version_check";

    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase.initialize(getApplicationContext());
        FileManager.initialize(getApplicationContext());
        SettingsManager.initialize(getApplicationContext());
        HotkeysManager.initialize(getApplicationContext());
        EmulatorManager.initialize(getApplicationContext());
        PlugManager.initialize(getApplicationContext());
        ResourceManager.initialize(getApplicationContext());
        NotificationManager.initialize(getApplicationContext());
        Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
        setVersionCheckEnable(SettingsManager.getBoolean(NEW_VERSION_NOTIFICATION, true));
    }

    private final Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = (thread, throwable) -> {
        try (PrintWriter writer = new PrintWriter(FileManager.getFile("crash", System.currentTimeMillis() + ".log"))) {
            throwable.printStackTrace(writer);
        } catch (IOException ignored) {
        }
        Process.killProcess(Process.myPid());
    };

    private void setVersionCheckEnable(boolean enable) {
        WorkManager workManager = WorkManager.getInstance(this);
        if (enable) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .setRequiresDeviceIdle(true)
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(CheckVersionWorker.class, 3, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .build();

            workManager.enqueueUniquePeriodicWork(WORK_ID_VERSION_CHECK,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
        } else {
            workManager.cancelUniqueWork(WORK_ID_VERSION_CHECK);
        }
    }
}
