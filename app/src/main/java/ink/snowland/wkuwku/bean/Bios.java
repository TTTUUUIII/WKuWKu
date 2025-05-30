package ink.snowland.wkuwku.bean;

public class Bios {
    public final String title;
    public final String url;
    public final String md5;
    public final String filename;

    public Bios(String title, String url, String md5, String filename) {
        this.title = title;
        this.url = url;
        this.md5 = md5;
        this.filename = filename;
    }

    @Override
    public String toString() {
        return "Bios{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
