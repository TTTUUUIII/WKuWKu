package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import ink.snowland.wkuwku.BR;

public abstract class SavableOption<T> extends BaseObservable {
    public final String key;
    public final String title;
    public final String summary;
    protected T value = null;

    public SavableOption(String key, String title, String summary) {
        this.key = key;
        this.title = title;
        this.summary = summary;
    }

    public void setValue(T value) {
        if (this.value == value) return;
        this.value = value;
        onSave();
        notifyPropertyChanged(BR.value);
    }

    @Bindable
    public T getValue() {
        if (value == null) {
            value = onLoad();
        }
        return value;
    }

    @NonNull
    protected abstract T onLoad();
    protected abstract void onSave();
}
