package ink.snowland.wkuwku.common;

import android.content.Context;
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

import ink.snowland.wkuwku.BuildConfig;
import ink.snowland.wkuwku.util.FileManager;

public class CheckUpdateWorker extends Worker {
    private static final String TAG = "CheckUpdateWorker";
    public CheckUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "INFO: check update");
        try (InputStream config = new URL("https://raw.githubusercontent.com/TTTUUUIII/WKuWKu/refs/heads/main/version_tags.xml").openStream()){
            String version = null;
            String[] md5Array = null;
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
                        md5Array = md5.split("\\|");
                        break;
                    }
                }
                event = xmlPullParser.next();
            }
            if (version == null)
                return Result.success();
            if (!version.equals(BuildConfig.VERSION_NAME)) {
                File apkFile = new File(FileManager.getCacheDirectory(), version + ".apk");
                boolean authed = false;
                try (InputStream ins = new URL(String.format("https://github.com/TTTUUUIII/WKuWKu/releases/download/%s/app-%s-release.apk", version, Build.SUPPORTED_ABIS[0])).openStream()){
                    FileManager.copy(ins, apkFile);
                    String md5 = FileManager.calculateMD5Sum(apkFile);
                    for (String key : md5Array) {
                        if (key.equals(md5)) {
                            authed = true;
                            break;
                        }
                    }
                    if (authed) {
                        Log.d(TAG, "INFO: request install apk");
                    } else {
                        return Result.retry();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Result.failure();
        }
        return Result.success();
    }
}
