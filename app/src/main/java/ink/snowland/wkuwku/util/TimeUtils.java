package ink.snowland.wkuwku.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

}
