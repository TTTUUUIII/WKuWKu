package org.wkuwku.util;

public class NumberUtils {
    public static long parseLong(String s, long defaultValue) {
        try {
             return Long.parseLong(s);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    public static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    public static float parseFloat(String s, float defaultValue) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }
}
