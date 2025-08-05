package com.outlook.wn123o.wkukwu.bsnes;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;

public class Main extends Plug {

    @Override
    protected void install(Context context, Resources resources) {
        super.install(context, resources);
        Bsnes.registerAsEmulator(resources);
    }

    @Override
    protected void uninstall() {
        super.uninstall();
        Bsnes.unregisterEmulator();
    }
}
