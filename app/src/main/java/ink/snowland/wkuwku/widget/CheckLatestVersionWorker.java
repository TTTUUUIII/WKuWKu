package ink.snowland.wkuwku.widget;

import static ink.snowland.wkuwku.util.FileManager.*;
import static ink.snowland.wkuwku.AppConfig.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.util.DownloadManager;
import ink.snowland.wkuwku.util.FileUtils;
import ink.snowland.wkuwku.util.Logger;
import ink.snowland.wkuwku.util.NumberUtils;

public class CheckLatestVersionWorker extends Worker {
    private static final Logger logger = new Logger("App", "CheckLatestVersionWorker");
    public static final String ACTION_UPDATE_APK = "ink.snowland.wkuwku.action.UPDATE_APK";
    public static final String EXTRA_APK_PATH = "apk.path";
    public static final String EXTRA_APK_VERSION = "apk.version";
    public CheckLatestVersionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private String mVersionName;
    private int mVersionCode;
    private String mMD5Sums;

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
                File file = new File(getCacheDirectory(), SKIP_CLEAN_PREFIX + mVersionName + ".apk");
                final String url = String.format("https://github.com/TTTUUUIII/WKuWKu/releases/download/%s/app-%s-release.apk", mVersionName, Build.SUPPORTED_ABIS[0]);
                if (file.exists() && mMD5Sums.contains(FileUtils.getMD5Sum(file))) {
                    Intent intent = new Intent(ACTION_UPDATE_APK);
                    intent.putExtra(EXTRA_APK_PATH, file.getAbsolutePath());
                    intent.putExtra(EXTRA_APK_VERSION, mVersionName);
                    logger.i("Request update version to %s", mVersionName);
                    getApplicationContext().sendBroadcast(intent);
                } else {
                    DownloadManager.newRequest(url, file)
                            .doOnComplete(it -> {
                                if (mMD5Sums.contains(FileUtils.getMD5Sum(file))) {
                                    Intent intent = new Intent(ACTION_UPDATE_APK);
                                    intent.putExtra(EXTRA_APK_PATH, it.getAbsolutePath());
                                    intent.putExtra(EXTRA_APK_VERSION, mVersionName);
                                    logger.i("Request update version to %s", mVersionName);
                                    getApplicationContext().sendBroadcast(intent);
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
}
