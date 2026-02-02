package ink.snowland.wkuwku.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SettingsManager {
    public static final String DRAWER_HERO_IMAGE_PATH = "drawer_hero";
    public static final String DRAWER_SUBTITLE = "drawer_subtitle";
    public static final String PERFORMANCE_MODE = "app_performance_mode";
    public static final String NAVIGATION_ANIMATION = "app_nav_transition_anim";
    private SettingsManager() {}
    private static final List<OnSettingChangedListener> mClientListeners = new ArrayList<>();
    private static final SharedPreferences.OnSharedPreferenceChangeListener sListener = (sharedPreferences, key) -> {
        if (key == null) return;
        for (OnSettingChangedListener listener : mClientListeners) {
            listener.onSettingChanged(key);
        }
    };

    private static SharedPreferences sSettings;

    public static void initialize(@NonNull Context applicationContext) {
        sSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        sSettings.registerOnSharedPreferenceChangeListener(sListener);
    }

    public static String getString(@NonNull String key) {
        return getString(key, "");
    }

    public static Set<String> getStringSet(@NonNull String key) {
        return sSettings.getStringSet(key, new ArraySet<String>());
    }

    public static void putStringSet(@NonNull String key, Set<String> v) {
        sSettings.edit()
                .putStringSet(key, v)
                .apply();
    }

    public static int getInt(@NonNull String key, int defaultValue) {
        return sSettings.getInt(key, defaultValue);
    }

    public static void putInt(@NonNull String key, int v) {
        sSettings.edit()
                .putInt(key, v)
                .apply();
    }

    public static String getString(@NonNull String key, @Nullable String defaultValue) {
        return sSettings.getString(key, defaultValue);
    }

    public static void remove(@NonNull String key) {
        sSettings.edit()
                .remove(key)
                .apply();
    }

    public static void putString(@NonNull String key, @NonNull String value) {
        sSettings.edit()
                .putString(key, value)
                .apply();
    }

    public static boolean getBoolean(@NonNull String key) {
        return getBoolean(key, false);
    }
    public static void putBoolean(@NonNull String key, boolean value) {
        sSettings.edit()
                .putBoolean(key, value)
                .apply();
    }

    public static boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return sSettings.getBoolean(key, defaultValue);
    }

    public static void addSettingChangedListener(OnSettingChangedListener listener) {
        if (mClientListeners.contains(listener)) return;
        mClientListeners.add(listener);
    }

    public static void removeSettingChangedListener(OnSettingChangedListener listener) {
        mClientListeners.remove(listener);
    }

    public static boolean equals(String key, String val) {
        return Objects.equals(sSettings.getString(key, null), val);
    }

    public interface OnSettingChangedListener {
        void onSettingChanged(@NonNull String key);
    }
}
