package ink.snowland.wkuwku.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public final class SettingsManager {
    private SettingsManager() {}

    private static SharedPreferences sSettings;

    public static void initialize(@NonNull Context applicationContext) {
        sSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    public static String getString(String key) {
        return sSettings.getString(key, "");
    }
}
