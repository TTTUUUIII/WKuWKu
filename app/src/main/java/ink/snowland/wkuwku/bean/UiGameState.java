package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import ink.snowland.wkuwku.BR;
import ink.snowland.wkuwku.db.entity.Game;

public class UiGameState extends BaseObservable {
    public final Game origin;

    private boolean mHidden = false;

    private UiGameState(@NonNull Game origin) {
        this.origin = origin;
    }

    public void setHidden(boolean hidden) {
        if (mHidden != hidden) {
            mHidden = hidden;
            notifyPropertyChanged(BR.hidden);
        }
    }

    @Bindable
    public boolean isHidden() {
        return mHidden;
    }

    public static UiGameState from(@NonNull Game game) {
        return new UiGameState(game);
    }
}
