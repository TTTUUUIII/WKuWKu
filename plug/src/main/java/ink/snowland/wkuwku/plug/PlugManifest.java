package ink.snowland.wkuwku.plug;

public class PlugManifest {
    public final String name;
    public final String path;
    public final String mainClass;

    public PlugManifest(String name, String path, String mainClass) {
        this.name = name;
        this.path = path;
        this.mainClass = mainClass;
    }
}
