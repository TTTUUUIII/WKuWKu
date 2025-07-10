package ink.snowland.plug.theme;

import android.content.Context;
import android.content.res.Resources;

import ink.snowland.wkuwku.plug.Plug;
//import ink.snowland.wkuwku.util.ThemeManager;

public class Main extends Plug {
    @Override
    protected void install(Context context, Resources resources) {
        super.install(context, resources);
        Resources.Theme theme = resources.newTheme();
        theme.applyStyle(R.style.MainTheme, true);
//        ThemeManager.put("avocado", theme);
    }

    @Override
    protected void uninstall() {
        super.uninstall();
//        ThemeManager.remove("avocado");
    }
}
