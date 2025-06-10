package ink.snowland.wkuwku.util;

public class NumberUtils {
    public static long parseLong(String s, long defaultValue) {
        try {
             return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
