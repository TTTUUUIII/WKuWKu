package ink.snowland.wkuwku.plug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

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
    protected String versionName;
    protected int versionCode;

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlugManifest that = (PlugManifest) o;
        return Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(packageName);
    }

    @NonNull
    @Override
    public String toString() {
        return "PlugManifest{" +
                ", installPath='" + installPath + '\'' +
                ", summary='" + summary + '\'' +
                ", author='" + author + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
