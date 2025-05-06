package ink.snowland.wkuwku.common;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EmOption implements Cloneable , Comparable<EmOption>{
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

    @Override
    public int compareTo(EmOption o) {
        return key.compareTo(o.key);
    }

    public static class Builder {
        private final String key;
        private final String val;
        private String title;
        private String[] allowVals = null;
        private boolean supported = false;

        public Builder(@NonNull String key, @NonNull String val) {
            this.key = key;
            this.val = val;
        }

        public Builder setTitle(@NonNull String title) {
            this.title = title;
            return this;
        }

        public Builder setAllowVals(@Nullable String ...allowVals) {
            this.allowVals = allowVals;
            return this;
        }

        public Builder setSupported(boolean supported) {
            this.supported = supported;
            return this;
        }

        public EmOption build() {
            return new EmOption(key, val, title, supported, allowVals);
        }
    }

    public static Builder builder(@NonNull String key, @NonNull String val) {
        return new Builder(key, val);
    }

//    public static EmOption create(@NonNull String key, @NonNull String val, @Nullable String title, boolean supported, @Nullable String ...allowVals) {
//        return new EmOption(key, val, title, supported, allowVals);
//    }
//
//    public static EmOption create(@NonNull String key, @NonNull String val, @Nullable String title, @Nullable String ...allowVals) {
//        return new EmOption(key, val, title, false, allowVals);
//    }

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
