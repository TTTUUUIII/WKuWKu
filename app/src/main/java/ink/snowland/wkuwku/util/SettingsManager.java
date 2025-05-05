package ink.snowland.wkuwku.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

public final class SettingsManager {
    private SettingsManager() {}

    private static SharedPreferences sSettings;

    public static void initialize(@NonNull Context applicationContext) {
        sSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    public static String getString(@NonNull String key) {
        return getString(key, "");
    }

    public static String getString(@NonNull String key, @Nullable String defaultValue) {
        return sSettings.getString(key, defaultValue);
    }

    public static boolean getBoolean(@NonNull String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return sSettings.getBoolean(key, defaultValue);
    }
}
