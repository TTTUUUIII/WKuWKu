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

import ink.snowland.wkuwku.activity.UncaughtExceptionActivity;
import ink.snowland.wkuwku.db.AppDatabase;
import ink.snowland.wkuwku.util.FileManager;
import ink.snowland.wkuwku.util.HotkeysManager;
import ink.snowland.wkuwku.util.NotificationManager;
import ink.snowland.wkuwku.util.PlugManager;
import ink.snowland.wkuwku.util.ResourceManager;
import ink.snowland.wkuwku.util.SettingsManager;
import ink.snowland.wkuwku.widget.CheckConfigWorker;

public class App extends Application {
    private static final String WORKER_CHECK_REMOTE_CONFIG = "check_remote_config_worker";

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
        UncaughtExceptionActivity.installForCurrent(getApplicationContext());
        checkRemoteConfig();
    }

    private void checkRemoteConfig() {
        WorkManager workManager = WorkManager.getInstance(this);
        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(CheckConfigWorker.class, 3, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build();

        workManager.enqueueUniquePeriodicWork(WORKER_CHECK_REMOTE_CONFIG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
