package ink.snowland.wkuwku.widget;
import static ink.snowland.wkuwku.GlobalConfig.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.util.FileManager;

public class CheckLatestVersionWorker extends Worker {

    public static final String ACTION_UPDATE_APK = "ink.snowland.wkuwku.action.UPDATE_APK";
    public static final String EXTRA_APK_PATH = "apk.path";
    public static final String EXTRA_APK_VERSION = "apk.version";

    private static final String TAG = "CheckLatestVersionWorker";
    public CheckLatestVersionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "INFO: start check latest version.");
        try (InputStream config = new URL(WEB_URL + "app-version.xml").openStream()){
            String version = null;
            List<String> md5List = null;
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(config, "utf-8");
            int event = xmlPullParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = xmlPullParser.getName();
                if (event == XmlPullParser.START_TAG && "tag".equals(name)) {
                    String tag = xmlPullParser.getAttributeValue(null, "name");
                    String md5 = xmlPullParser.getAttributeValue(null, "md5");
                    if (md5 == null)
                        md5 = "";
                    boolean latest = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, "latest"));
                    Log.d(TAG, "INFO: latest tag " + tag);
                    if (latest) {
                        version = tag;
                        md5List = Arrays.asList(md5.split("\\|"));
                        break;
                    }
                }
                event = xmlPullParser.next();
            }
            if (version == null)
                return Result.success();
            if (!version.equals(BuildConfig.VERSION_NAME)) {
                File apkFile = new File(FileManager.getCacheDirectory(), version + ".apk");
                boolean requestInstall = false;
                if (apkFile.exists()) {
                    String md5 = FileManager.calculateMD5Sum(apkFile);
                    if (md5List.contains(md5))
                        requestInstall = true;
                }
                if (!requestInstall) {
                    try (InputStream ins = new URL(String.format("https://github.com/TTTUUUIII/WKuWKu/releases/download/%s/app-%s-release.apk", version, Build.SUPPORTED_ABIS[0])).openStream()){
                        FileManager.copy(ins, apkFile);
                        String md5 = FileManager.calculateMD5Sum(apkFile);
                        if (md5List.contains(md5))
                            requestInstall = true;
                    }
                }
                if (requestInstall) {
                    Intent intent = new Intent(ACTION_UPDATE_APK);
                    intent.putExtra(EXTRA_APK_PATH, apkFile.getAbsolutePath());
                    intent.putExtra(EXTRA_APK_VERSION, version);
                    Log.i(TAG, "INFO: request update version: " + version);
                    getApplicationContext().sendBroadcast(intent);
                }
            } else {
                Log.i(TAG, "INFO: new version not found.");
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Result.failure();
        }
        return Result.success();
    }
}
