package ink.snowland.wkuwku.plug.mame;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;

public class MamePlug implements Plug {
    @Override
    public void install(Context context, Resources resources) {
        Mame.registerAsEmulator(resources);
    }

    @Override
    public void uninstall() {
        Mame.unregisterEmulator();
    }
}
