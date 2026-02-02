package org.wkuwku.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Logger {
    private static final String TAG = "WkuWku";
    private final String mFormat;
    private final String mLabel;

    public Logger(@Nullable String label) {
        mLabel = label;
        if (label == null || label.isEmpty()) {
            mFormat = "%s%s";
        } else {
            mFormat = "[%s] %s";
        }
    }

    public Logger(Object obj) {
        this(obj.getClass().getSimpleName());
    }

    public void d(@NonNull String fmt, Object...args) {
        Log.d(TAG, String.format(mFormat, mLabel, String.format(fmt, args)));
    }

    public void e(@NonNull String fmt, Object...args) {
        Log.e(TAG, String.format(mFormat, mLabel, String.format(fmt, args)));
    }

    public void i(@NonNull String fmt, Object...args) {
        Log.i(TAG, String.format(mFormat, mLabel, String.format(fmt, args)));
    }

    public void w(@NonNull String fmt, Object...args) {
        Log.w(TAG, String.format(mFormat, mLabel, String.format(fmt, args)));
    }
}
