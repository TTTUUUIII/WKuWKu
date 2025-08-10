package org.wkuwku.plug.vbanext;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;

public class Main extends Plug {

    @Override
    protected void install(Context context, Resources resources) {
        super.install(context, resources);
        VbaNext.registerAsEmulator(resources);
    }

    @Override
    protected void uninstall() {
        super.uninstall();
        VbaNext.unregisterEmulator();
    }
}
