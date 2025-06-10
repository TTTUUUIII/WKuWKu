package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;

import java.util.Objects;

public class PlugRes {
    public String name;
    public String author;
    public String iconUrl;
    public String url;
    public String packageName;
    public String version;
    public int versionCode;
    public String md5;
    public String summary;

    @NonNull
    @Override
    public String toString() {
        return "PlugRes{" +
                "name='" + name + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", url='" + url + '\'' +
                ", packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlugRes plugRes = (PlugRes) o;
        return Objects.equals(packageName, plugRes.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(packageName);
    }
}
