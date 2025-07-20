package ink.snowland.wkuwku.bean;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Arrays;

import ink.snowland.wkuwku.BR;

public class Hotkey extends BaseObservable implements Cloneable{
    public final String key;
    public final String title;
    private boolean mWaiting;
    private final StringBuilder mNameBuilder = new StringBuilder();
    private int[] mKeys = new int[0];
    public Hotkey(String key, String title) {
        this.key = key;
        this.title = title;
        mWaiting = false;
    }

    @Bindable
    public String getName() {
        return mNameBuilder.toString();
    }

    public void setKeys(@NonNull int[] codes, @NonNull SparseArray<String> mapTable) {
        mKeys = Arrays.stream(codes)
                .sorted()
                .toArray();
        updateName(mapTable);
    }

    @NonNull
    public int[] getKeys() {
        return mKeys;
    }

    public void clear() {
        mKeys = new int[0];
        mNameBuilder.delete(0, mNameBuilder.length());
        notifyPropertyChanged(BR.name);
    }

    @Bindable
    public boolean isWaiting() {
        return mWaiting;
    }

    public void setWaiting(boolean waiting) {
        mWaiting = waiting;
        notifyPropertyChanged(BR.waiting);
    }

    private void updateName(@NonNull SparseArray<String> mapTab) {
        mNameBuilder.delete(0, mNameBuilder.length());
        for (int i = 0; i < mKeys.length; i++) {
            mNameBuilder.append(mapTab.get(mKeys[i]));
            if (i != mKeys.length - 1) {
                mNameBuilder.append("+");
            }
        }
        notifyPropertyChanged(BR.name);
    }

    @NonNull
    @Override
    public Hotkey clone() {
        try {
            return (Hotkey) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    @NonNull
    public String toString() {
        return "Hotkey{" +
                "key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", keys=" + Arrays.toString(mKeys) +
                '}';
    }
}
