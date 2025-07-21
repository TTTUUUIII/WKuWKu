package ink.snowland.wkuwku.util;

import android.os.Build;

import androidx.annotation.NonNull;

public class PathUtils {
    private PathUtils() {}
    private static final String REG_ABI = "(?i)\\$\\{ABI\\}";

    public static String wrapper(@NonNull String origin) {
        return origin.replaceAll(REG_ABI, Build.SUPPORTED_ABIS[0]);
    }
}
