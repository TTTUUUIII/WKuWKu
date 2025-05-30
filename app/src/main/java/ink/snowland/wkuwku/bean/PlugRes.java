package ink.snowland.wkuwku.bean;

import androidx.annotation.NonNull;

public class PlugRes {
    public String name;
    public String author;
    public String iconUrl;
    public String url;
    public String packageName;
    public String version;
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
}
