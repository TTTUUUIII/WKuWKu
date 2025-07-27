package ink.snowland.wkuwku.util;

import android.content.Context;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ink.snowland.wkuwku.R;

public class TimeUtils {
    private final static SimpleDateFormat formatter = new SimpleDateFormat("yy/MM/dd HH:mm:ss", Locale.ROOT);
    private TimeUtils() {

    }

    public static String toString(long timestamp) {
        return formatter.format(new Date(timestamp));
    }

    public static String toString(String fmt, long timestamp) {
        return new SimpleDateFormat(fmt, Locale.ROOT).format(new Date(timestamp));
    }

    public static int elapsedDays(long timestamp) {
        long elapsedRealtimeMillis = System.currentTimeMillis() - timestamp;
        long elapsedSeconds = (long) (elapsedRealtimeMillis / 1e3);
        return (int) (elapsedSeconds / 86400);
    }
}
