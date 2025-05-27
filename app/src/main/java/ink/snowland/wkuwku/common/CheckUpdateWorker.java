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

import ink.snowland.wkuwku.util.FileManager;

public class CheckUpdateWorker extends Worker {
    private static final String TAG = "CheckUpdateWorker";
    public CheckUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try (InputStream config = new URL("https://github.com/TTTUUUIII/WKuWKu/blob/main/version_tags.xml").openStream()){
            String version = null;
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(config, "utf-8");
            int event = xmlPullParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = xmlPullParser.getName();
                if (event == XmlPullParser.START_TAG && "tag".equals(name)) {
                    String tag = xmlPullParser.getAttributeValue(null, "name");
                    boolean latest = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, "latest"));
                    Log.d(TAG, "INFO: new tag " + tag + ", latest: " + latest);
                    if (latest) {
                        version = tag;
                        break;
                    }
                }
                event = xmlPullParser.next();
            }
            if (version != null) {
                try (InputStream ins = new URL(String.format("https://github.com/TTTUUUIII/WKuWKu/releases/download/%s/app-%s-release.apk", version, Build.SUPPORTED_ABIS[0])).openStream()){
                    File apkFile = new File(FileManager.getCacheDirectory(), version + ".apk");
                    FileManager.copy(ins, apkFile, (progress, max) -> {
                        Log.d(TAG, "INFO: download progress: " + progress + "/" + max);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return Result.failure();
        }
        return Result.success();
    }
}
