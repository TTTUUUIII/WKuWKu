package ink.snowland.wkuwku.util;

import android.content.Context;

import androidx.annotation.StringRes;

public class ResourceManager {
    private ResourceManager() {}
    private static Context sApplicationContext;

    public static void initialize(Context applicationContext) {
        sApplicationContext = applicationContext;
    }

    public static String getStringSafe(@StringRes int resId) {
        return sApplicationContext.getString(resId);
    }

    public static String getStringSafe(@StringRes int resId, Object... formatArgs) {
        return sApplicationContext.getString(resId, formatArgs);
    }
    public static Context getApplicationContextSafe() {
        return sApplicationContext;
    }
}
