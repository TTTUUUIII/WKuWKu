package ink.snowland.wkuwku.plug.pokemini;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;

public class Main extends Plug {

    @Override
    protected void install(Context context, Resources resources) {
        super.install(context, resources);
        PokeMini.registerAsEmulator(resources);
    }

    @Override
    protected void uninstall() {
        super.uninstall();
        PokeMini.unregisterEmulator();
    }
}
