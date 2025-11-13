package ink.snowland.wkuwku.bean;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;

public class UiTooltipState extends BaseObservable {

    public static final long LENGTH_SHORT = -1;
    public static final long LENGTH_LONG = 0;

    private static final int HIDDEN_MSG = 0;
    private String mText;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == HIDDEN_MSG) {
                setText(null);
            }
        }
    };

    @Bindable
    public String getText() {
        return mText;
    }

    private void setText(@Nullable String content) {
        mText = content;
        notifyPropertyChanged(BR.text);
    }

    public void show(@NonNull Context context, @StringRes int resId, long duration) {
        show(context.getResources().getString(resId), duration);
    }

    public void show(@NonNull String content, long duration) {
        setText(content);
        if (duration == LENGTH_SHORT) {
            duration = 1000;
        } else if (duration == LENGTH_LONG) {
            duration = 1500;
        }
        mHandler.sendEmptyMessageAtTime(HIDDEN_MSG, SystemClock.uptimeMillis() + duration);
    }
}
