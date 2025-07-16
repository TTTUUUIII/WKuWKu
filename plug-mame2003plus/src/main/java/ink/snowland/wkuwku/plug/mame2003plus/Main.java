package ink.snowland.wkuwku.plug.mame2003plus;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;

public class Main extends Plug {
    @Override
    public void install(Context context, Resources resources) {
        super.install(context, resources);
        Mame.registerAsEmulator(resources);
    }

    @Override
    public void uninstall() {
        Mame.unregisterEmulator();
    }
}
