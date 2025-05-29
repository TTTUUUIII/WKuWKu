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

    public PlugManifest(@NonNull PlugManifest manifest) {
        this.name = manifest.name;
        this.packageName = manifest.packageName;
        this.mainClass = manifest.mainClass;
        this.author = manifest.author;
        this.summary = manifest.summary;
        this.installPath = manifest.installPath;
        this.dexFileName = manifest.dexFileName;

    }

    public final String name;
    public final String packageName;
    public final String mainClass;
    public final String author;
    public final String summary;
    protected String installPath;
    protected String dexFileName;

    public void setDexFileName(String dexFileName) {
        this.dexFileName = dexFileName;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getDexFileName() {
        return dexFileName;
    }

    public String getInstallPath() {
        return installPath;
    }

    @Override
    public String toString() {
        return "PlugManifest{" +
                "dexFileName='" + dexFileName + '\'' +
                ", installPath='" + installPath + '\'' +
                ", summary='" + summary + '\'' +
                ", author='" + author + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
