package ink.snowland.wkuwku.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;

import java.util.Set;

public final class SettingsManager {
    private SettingsManager() {}

    private static SharedPreferences sSettings;

    public static void initialize(@NonNull Context applicationContext) {
        sSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    public static String getString(@NonNull String key) {
        return getString(key, "");
    }

    public static Set<String> getStringSet(@NonNull String key) {
        return sSettings.getStringSet(key, new ArraySet<String>());
    }

    public static int getInt(@NonNull String key, int defaultValue) {
        return sSettings.getInt(key, defaultValue);
    }

    public static String getString(@NonNull String key, @Nullable String defaultValue) {
        return sSettings.getString(key, defaultValue);
    }

    public static void putString(@NonNull String key, @NonNull String value) {
        sSettings.edit()
                .putString(key, value)
                .apply();
    }

    public static boolean getBoolean(@NonNull String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return sSettings.getBoolean(key, defaultValue);
    }
}
