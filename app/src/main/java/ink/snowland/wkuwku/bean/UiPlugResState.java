package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import ink.snowland.wkuwku.BR;

public class UiPlugResState extends BaseObservable {
    public final PlugRes origin;
    private boolean mInstallable = false;
    private boolean mUpgrade = false;
    private String mText = "";

    private UiPlugResState(PlugRes origin) {
        this.origin = origin;
    }

    @Bindable
    public boolean isInstallable() {
        return mInstallable;
    }

    public void setInstallable(boolean installable) {
        if (mInstallable != installable) {
            mInstallable = installable;
            notifyPropertyChanged(BR.installable);
        }
    }

    @Bindable
    public boolean isUpgrade() {
        return mUpgrade;
    }

    public void setUpgrade(boolean upgrade) {
        if (mUpgrade != upgrade) {
            mUpgrade = upgrade;
            notifyPropertyChanged(BR.upgrade);
        }
    }

    @Bindable
    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
        notifyPropertyChanged(BR.text);
    }

    public static UiPlugResState from(@NonNull PlugRes res) {
        return new UiPlugResState(res);
    }
}
