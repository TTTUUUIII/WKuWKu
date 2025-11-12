package ink.snowland.wkuwku.bean;

import androidx.annotation.Nullable;

public class DrawerState {
    private String mHereImgUrl;
    private String mSubtitle;

    public void setHereImgUrl(String url) {
        mHereImgUrl = url;
    }

    @Nullable
    public String getHereImgUrl() {
        return mHereImgUrl;
    }

    public void setSubtitle(String subtitle) {
        mSubtitle = subtitle;
    }

    @Nullable
    public String getSubtitle() {
        return mSubtitle;
    }

    @Override
    public String toString() {
        return "DrawerState{" +
                "hereImgUrl='" + mHereImgUrl + '\'' +
                ", subtitle='" + mSubtitle + '\'' +
                '}';
    }
}
