package ink.snowland.wkuwku.plug;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

public interface Plug {
    void install(Context context, Resources resources);
    void uninstall();
}
