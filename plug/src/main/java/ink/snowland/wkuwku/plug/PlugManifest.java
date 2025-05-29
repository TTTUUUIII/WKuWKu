package ink.snowland.wkuwku.plug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PlugManifest {

    public PlugManifest(@NonNull String name, @NonNull String packageName, @NonNull String mainClass, @Nullable String author, @Nullable String summary) {
        this.name = name;
        this.packageName = packageName;
        this.mainClass = mainClass;
        this.author = author;
        this.summary = summary;
    }

    public final String name;
    public final String packageName;
    public final String mainClass;
    public final String author;
    public final String summary;
    protected String installPath;
    protected String dexFileName;
}
