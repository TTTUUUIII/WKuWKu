package ink.snowland.wkuwku.plug.snes9x;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;

public class Main extends Plug {

    @Override
    protected void install(Context context, Resources resources) {
        super.install(context, resources);
        Snes9x.registerAsEmulator(resources);
    }

    @Override
    protected void uninstall() {
        super.uninstall();
        Snes9x.unregisterEmulator();
    }
}
