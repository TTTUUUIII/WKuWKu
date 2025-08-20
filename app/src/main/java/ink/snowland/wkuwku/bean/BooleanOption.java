package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ink.snowland.wkuwku.util.SettingsManager;

public class BooleanOption extends SavableOption<Boolean> {
    private final boolean mDefaultValue;
    public BooleanOption(String key, String title, @Nullable String summary, boolean defaultValue) {
        super(key, title, summary);
        mDefaultValue = defaultValue;
    }

    @NonNull
    @Override
    protected Boolean onLoad() {
        return SettingsManager.getBoolean(key, mDefaultValue);
    }

    @Override
    protected void onSave() {
        SettingsManager.putBoolean(key, value);
    }
}
