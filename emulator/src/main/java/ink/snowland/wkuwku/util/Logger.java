package ink.snowland.wkuwku.util;

import android.util.Log;

import androidx.annotation.NonNull;

public class Logger {
    private static final String template = "[%s] %s";
    private final String group;
    private final String file;

    public Logger(String group, String file) {
        this.group = group;
        this.file = file;
    }

    public void d(@NonNull String fmt, Object...args) {
        Log.d(group, String.format(template, file, String.format(fmt, args)));
    }

    public void e(@NonNull String fmt, Object...args) {
        Log.e(group, String.format(template, file, String.format(fmt, args)));
    }

    public void i(@NonNull String fmt, Object...args) {
        Log.i(group, String.format(template, file, String.format(fmt, args)));
    }

    public void w(@NonNull String fmt, Object...args) {
        Log.w(group, String.format(template, file, String.format(fmt, args)));
    }
}
