package ink.snowland.wkuwku.widget;

import static ink.snowland.wkuwku.util.ResourceManager.WEB_URL;
import static ink.snowland.wkuwku.util.FileManager.*;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.activity.MainActivity;
import ink.snowland.wkuwku.bean.AppConfig;
import ink.snowland.wkuwku.util.DownloadManager;
import org.wkuwku.util.FileUtils;
import org.wkuwku.util.Logger;
import ink.snowland.wkuwku.util.NotificationManager;
import ink.snowland.wkuwku.util.SettingsManager;

public class CheckConfigWorker extends Worker {
    private static final Logger logger = new Logger("App", "CheckAppConfigWorker");

    private static final String VERSION_NOTIFICATION = "app_new_version_notification";
    public CheckConfigWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private File mFile;

    @NonNull
    @Override
    public Result doWork() {
        try (InputStream in = new URL(WEB_URL + "app-config.xml").openStream()){
            AppConfig appConfig = AppConfig.from(in);
            if (SettingsManager.getBoolean(VERSION_NOTIFICATION, true) && appConfig.getVersionCode() > BuildConfig.VERSION_CODE) {
                mFile = new File(getCacheDirectory(), SKIP_CLEAN_PREFIX + appConfig.getVersionName() + ".apk");
                final String url = String.format("https://github.com/TTTUUUIII/WKuWKu/releases/download/%s/app-%s-release.apk", appConfig.getVersionName(), Build.SUPPORTED_ABIS[0]);
                if (mFile.exists() && appConfig.getMD5Sum().contains(FileUtils.getMD5Sum(mFile))) {
                    sendVersionNotification(appConfig.getVersionName());
                } else {
                    DownloadManager.newRequest(url, mFile)
                            .doOnComplete(it -> {
                                String md5Sum = FileUtils.getMD5Sum(mFile);
                                if (appConfig.getMD5Sum().contains(md5Sum)) {
                                    sendVersionNotification(appConfig.getVersionName());
                                } else {
                                    logger.e("Failed to verification package. " + md5Sum);
                                }
                            })
                            .doOnError(error -> {
                                logger.e("Failed to download %s from %s", appConfig.getVersionName(), url);
                                error.printStackTrace(System.err);
                            })
                            .submit();
                }
            }
            String url = appConfig.getDrawerHeroUrl();
            if (url != null) {
                File file = getFile(IMAGE_DIRECTORY, FileUtils.getName(url));
                if (!file.exists()) {
                    DownloadManager.download(Pair.create(url, file));
                    FileUtils.delete(SettingsManager.getString(SettingsManager.DRAWER_HERO_IMAGE_PATH, null));
                    SettingsManager.putString(SettingsManager.DRAWER_HERO_IMAGE_PATH, file.getAbsolutePath());
                    logger.i("Updated drawer hero image!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Result.failure();
        }
        return Result.success();
    }

    private void sendVersionNotification(String versionName) {
        Context applicationContext = getApplicationContext();
        Intent intent = new Intent(applicationContext, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_REQUEST_ID, MainActivity.REQUEST_INSTALL_PACKAGE);
        intent.putExtra(MainActivity.EXTRA_PACKAGE_FILE_PATH, mFile.getAbsolutePath());
        PendingIntent pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(applicationContext, NotificationManager.NOTIFICATION_DEFAULT_CHANNEL)
                .setSmallIcon(R.drawable.im_notification)
                .setContentTitle(applicationContext.getString(R.string.version_notification))
                .setContentText(applicationContext.getString(R.string.fmt_click_update_to_new_version, versionName))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager.postNotification(notification);
        logger.i("Notification version %s available!", versionName);
    }
}
