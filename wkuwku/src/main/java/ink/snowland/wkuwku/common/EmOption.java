package ink.snowland.wkuwku.common;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

public class EmOption implements Cloneable {
    public final String key;
    public String val;
    public final String title;
    public final String[] allowVals;

    private EmOption(@NonNull String key, @NonNull String val, @Nullable String title, @Nullable String ...allowVals) {
        this.key = key;
        this.val = val;
        this.title = title;
        this.allowVals = allowVals;
    }

    public static EmOption create(@NonNull String key, @NonNull String val, @Nullable String title, @Nullable String ...allowVals) {
        return new EmOption(key, val, title, allowVals);
    }

    @NonNull
    @Override
    public EmOption clone() {
        try {
            return (EmOption) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
