package ink.snowland.wkuwku.common;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class EmOption implements Cloneable , Comparable<EmOption>{

    public static final String NUMBER = "number";
    public static final String NUMBER_DECIMAL = "numberDecimal";
    public static final String NUMBER_SIGNED = "numberSigned";
    public static final String TEXT = "text";

    public final String key;
    public String val;
    public final String title;
    public final String[] allowVals;
    public final boolean enable;
    public final String inputType;

    private EmOption(@NonNull String key, @NonNull String val, @Nullable String title, boolean enable, String inputType, @Nullable String ...allowVals) {
        this.key = key;
        this.val = val;
        this.title = title;
        this.allowVals = allowVals;
        this.enable = enable;
        this.inputType = inputType;
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
        private boolean enable = true;
        private String inputType;

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

        public Builder setEnable(boolean enable) {
            this.enable = enable;
            return this;
        }

        public Builder setInputType(String inputType) {
            this.inputType = inputType;
            return this;
        }

        public EmOption build() {
            return new EmOption(key, val, title, enable, inputType, allowVals);
        }
    }

    public static Builder builder(@NonNull String key, @NonNull String val) {
        return new Builder(key, val);
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
        EmOption option = (EmOption) o;
        return enable == option.enable && Objects.equals(key, option.key) && Objects.equals(val, option.val) && Objects.equals(title, option.title) && Objects.deepEquals(allowVals, option.allowVals) && Objects.equals(inputType, option.inputType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, val, title, Arrays.hashCode(allowVals), enable, inputType);
    }

    @Override
    public String toString() {
        return "EmOption{" +
                "val='" + val + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
