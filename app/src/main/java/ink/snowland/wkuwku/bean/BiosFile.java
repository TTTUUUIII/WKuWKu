package ink.snowland.wkuwku.bean;

public class BiosFile {
    public final String url;
    public final String md5;
    public final String name;
    public final boolean required;

    public BiosFile(String name, String url, String md5, boolean required) {
        this.url = url;
        this.md5 = md5;
        this.name = name;
        this.required = required;
    }

    @Override
    public String toString() {
        return "BiosFile{" +
                "url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                ", name='" + name + '\'' +
                ", required=" + required +
                '}';
    }
}
