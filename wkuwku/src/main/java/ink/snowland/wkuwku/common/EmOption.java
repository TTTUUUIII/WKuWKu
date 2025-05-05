package ink.snowland.wkuwku.common;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EmOption implements Cloneable {
    public final String key;
    public String val;
    public final String title;
    public final String[] allowVals;
    public final boolean supported;

    private EmOption(@NonNull String key, @NonNull String val, @Nullable String title, boolean supported, @Nullable String ...allowVals) {
        this.key = key;
        this.val = val;
        this.title = title;
        this.allowVals = allowVals;
        this.supported = supported;
    }

    public static EmOption create(@NonNull String key, @NonNull String val, @Nullable String title, boolean supported, @Nullable String ...allowVals) {
        return new EmOption(key, val, title, supported, allowVals);
    }

    public static EmOption create(@NonNull String key, @NonNull String val, @Nullable String title, @Nullable String ...allowVals) {
        return new EmOption(key, val, title, false, allowVals);
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


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EmOption emOption = (EmOption) o;
        return Objects.equals(key, emOption.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }
}
