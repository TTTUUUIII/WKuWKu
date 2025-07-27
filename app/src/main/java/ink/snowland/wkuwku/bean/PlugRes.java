package ink.snowland.wkuwku.bean;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

import ink.snowland.wkuwku.BuildConfig;

public class PlugRes {
    public static final int VERSION_UNKNOW = 0;

    public String name;
    public String author;
    public String iconUrl;
    public String url;
    public String packageName;
    public String version;
    public int versionCode;
    public String md5;
    public String summary;
    public int minAppVersion;
    public int maxAppVersion;

    public String[] supportedABIs;

    @Override
    @NonNull
    public String toString() {
        return "PlugRes{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", versionCode=" + versionCode +
                ", md5='" + md5 + '\'' +
                ", minAppVersion=" + minAppVersion +
                ", maxAppVersion=" + maxAppVersion +
                ", supportedABIs=" + Arrays.toString(supportedABIs) +
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

    public boolean isCompatible() {
        boolean compatible = false;
        if (supportedABIs != null) {
            for (String it: Build.SUPPORTED_ABIS) {
                for (String abi: supportedABIs) {
                    if (it.equals(abi)) {
                        url = url.replaceAll("(?i)\\$\\{ABI\\}", abi);
                        compatible = true;
                        break;
                    }
                }
                if (compatible) break;
            }
        } else {
            url = url.replaceAll("(?i)\\$\\{ABI\\}", Build.SUPPORTED_ABIS[0]);
            compatible = true;
        }
        return compatible
                && BuildConfig.VERSION_CODE >= minAppVersion
                && (maxAppVersion == VERSION_UNKNOW || BuildConfig.VERSION_CODE <= maxAppVersion);
    }
}
