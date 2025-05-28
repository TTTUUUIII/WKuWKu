package ink.snowland.wkuwku.plug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PlugManifest {
    public PlugManifest(@NonNull String packageName, @NonNull String mainClass, @Nullable String author) {
        this.packageName = packageName;
        this.mainClass = mainClass;
        this.author = author;
    }

    public final String packageName;
    public final String mainClass;
    public final String author;
    protected String installPath;
    protected String dexPath;
}
