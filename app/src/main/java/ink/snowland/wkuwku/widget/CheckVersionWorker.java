package ink.snowland.wkuwku.widget;

import static ink.snowland.wkuwku.util.FileManager.*;
import static ink.snowland.wkuwku.AppConfig.*;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.activity.MainActivity;
import ink.snowland.wkuwku.util.DownloadManager;
import ink.snowland.wkuwku.util.FileUtils;
import ink.snowland.wkuwku.util.Logger;
import ink.snowland.wkuwku.util.NotificationManager;
import ink.snowland.wkuwku.util.NumberUtils;

public class CheckVersionWorker extends Worker {
    private static final Logger logger = new Logger("App", "CheckLatestVersionWorker");
    public CheckVersionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private String mVersionName;
    private int mVersionCode;
    private String mMD5Sums;
    private File mFile;

    @NonNull
    @Override
    public Result doWork() {
        logger.i("Start check latest version.");
        try (InputStream config = new URL(WEB_URL + "app-version.xml").openStream()){
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(config, "utf-8");
            int event = xmlPullParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = xmlPullParser.getName();
                if (event == XmlPullParser.START_TAG && "tag".equals(name)) {
                    boolean latest = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, "latest"));
                    if (latest) {
                        mMD5Sums = xmlPullParser.getAttributeValue(null, "md5");
                        mVersionName = xmlPullParser.getAttributeValue(null, "name");
                        mVersionCode = NumberUtils.parseInt(xmlPullParser.getAttributeValue(null, "code"), 0);
                        logger.d("Latest version is %s(%d).", mVersionName, mVersionCode);
                        break;
                    }
                }
                event = xmlPullParser.next();
            }
            if (mVersionCode > BuildConfig.VERSION_CODE && mMD5Sums != null) {
                mFile = new File(getCacheDirectory(), SKIP_CLEAN_PREFIX + mVersionName + ".apk");
                final String url = String.format("https://github.com/TTTUUUIII/WKuWKu/releases/download/%s/app-%s-release.apk", mVersionName, Build.SUPPORTED_ABIS[0]);
                if (mFile.exists() && mMD5Sums.contains(FileUtils.getMD5Sum(mFile))) {
                    sendNotification();
                } else {
                    DownloadManager.newRequest(url, mFile)
                            .doOnComplete(it -> {
                                if (mMD5Sums.contains(FileUtils.getMD5Sum(mFile))) {
                                    sendNotification();
                                } else {
                                    logger.e("Failed to verification package.");
                                }
                            })
                            .doOnError(error -> {
                                logger.e("Failed to download %s from %s", mVersionName, url);
                                error.printStackTrace(System.err);
                            })
                            .submit();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Result.failure();
        }
        return Result.success();
    }

    private void sendNotification() {
        Context applicationContext = getApplicationContext();
        Intent intent = new Intent(applicationContext, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_REQUEST_ID, MainActivity.REQUEST_INSTALL_PACKAGE);
        intent.putExtra(MainActivity.EXTRA_PACKAGE_FILE_PATH, mFile.getAbsolutePath());
        PendingIntent pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(applicationContext, NotificationManager.NOTIFICATION_DEFAULT_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(applicationContext.getString(R.string.version_notification))
                .setContentText(applicationContext.getString(R.string.fmt_click_update_to_new_version, mVersionName))
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager.postNotification(notification);
        logger.i("Notification version %s available!", mVersionName);
    }
}
