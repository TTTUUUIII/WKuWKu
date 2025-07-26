package ink.snowland.wkuwku.common;

public class EmBiosFile {
    public final String name;
    public final String url;
    public final String md5;

    public EmBiosFile(String name, String url, String md5) {
        this.name = name;
        this.url = url;
        this.md5 = md5;
    }

    @Override
    public String toString() {
        return "EmBiosFile{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
