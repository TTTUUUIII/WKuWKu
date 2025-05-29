package ink.snowland.wkuwku.plug;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Plug {
    protected PlugManifest manifest;
    protected Resources resources;
    protected void install(Context context, Resources resources) {
        this.resources = resources;
    }
    protected void uninstall() {

    }
    public @Nullable Bitmap getIcon() {
        return null;
    }
    public PlugManifest getManifest() {
        return manifest;
    }
    public void setManifest(@NonNull PlugManifest manifest) {
        this.manifest = manifest;
    }
}
