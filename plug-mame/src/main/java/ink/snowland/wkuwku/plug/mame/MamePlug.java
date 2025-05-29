package ink.snowland.wkuwku.plug.mame;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import ink.snowland.wkuwku.plug.Plug;

public class MamePlug extends Plug {
    @Override
    public void install(Context context, Resources resources) {
        super.install(context, resources);
        Mame.registerAsEmulator(resources);
    }

    @Override
    public void uninstall() {
        Mame.unregisterEmulator();
    }

    @Nullable
    @Override
    public Bitmap getIcon() {
        Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.mame_logo, null);
        if (drawable == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
